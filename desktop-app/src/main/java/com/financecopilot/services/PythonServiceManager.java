package com.financecopilot.services;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PythonServiceManager {

    private static final String HEALTH_URL = "http://127.0.0.1:5000/health";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private static Process pythonProcess = null;
    private static boolean serviceAvailable = false;

    static {
        // Clean shutdown hook to avoid orphaned child process
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
        }, "PythonService-Shutdown"));
    }

    public static synchronized boolean ensureRunning() {
        if (isHealthy()) {
            serviceAvailable = true;
            return true;
        }

        System.out.println("Python analysis service not detected on " + HEALTH_URL + ". Attempting automatic startup...");

        Path pythonExe = findPythonExecutable();
        Path scriptPath = findAppScript();

        if (pythonExe == null || scriptPath == null) {
            System.err.println("Warning: Python executable or analysis-service/app.py could not be located.");
            if (pythonExe != null) System.err.println("Found python: " + pythonExe);
            if (scriptPath != null) System.err.println("Found script: " + scriptPath);
            serviceAvailable = false;
            return false;
        }

        try {
            System.out.println("Launching Python service: " + pythonExe + " " + scriptPath);
            ProcessBuilder pb = new ProcessBuilder(
                pythonExe.toString(),
                scriptPath.toString()
            );

            // Working directory is analysis-service directory
            if (scriptPath.getParent() != null) {
                pb.directory(scriptPath.getParent().toFile());
            }

            // Redirect output so stdout/stderr buffers do not block
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

            pythonProcess = pb.start();
            System.out.println("Spawned Python analysis service with PID: " + pythonProcess.pid());

            // Poll for health up to 14 times (approx 7 seconds)
            for (int i = 0; i < 14; i++) {
                Thread.sleep(500);
                if (!pythonProcess.isAlive()) {
                    System.err.println("Python service process terminated prematurely.");
                    break;
                }
                if (isHealthy()) {
                    System.out.println("Python analysis service is online and healthy.");
                    serviceAvailable = true;
                    return true;
                }
            }

            serviceAvailable = isHealthy();
            return serviceAvailable;

        } catch (Exception e) {
            System.err.println("Failed to start Python analysis service: " + e.getMessage());
            serviceAvailable = false;
            return false;
        }
    }

    public static boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HEALTH_URL))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains("healthy");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isServiceAvailable() {
        return serviceAvailable;
    }

    public static synchronized void stop() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            System.out.println("Stopping background Python analysis service (PID " + pythonProcess.pid() + ")...");
            pythonProcess.destroy();
            try {
                if (!pythonProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException ignored) {
                pythonProcess.destroyForcibly();
            }
            pythonProcess = null;
        }
    }

    private static Path getApplicationBaseDir() {
        try {
            URI codeSourceUri = PythonServiceManager.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path jarOrClassDir = Path.of(codeSourceUri);
            if (Files.isRegularFile(jarOrClassDir)) {
                Path parent = jarOrClassDir.getParent(); // target/ or dist/FinanceCopilot/app/
                if (parent != null && parent.getFileName().toString().equalsIgnoreCase("app")) {
                    return parent.getParent(); // dist/FinanceCopilot/
                }
                return parent;
            }
            return jarOrClassDir;
        } catch (Exception e) {
            return Path.of(".").toAbsolutePath();
        }
    }

    private static Path findPythonExecutable() {
        // 1. Explicit override
        String prop = System.getProperty("financecopilot.python.path");
        if (prop != null && !prop.isBlank()) {
            Path p = Path.of(prop);
            if (Files.exists(p)) return p.toAbsolutePath();
        }

        Path appBase = getApplicationBaseDir();

        // 2. Check candidate paths relative to appBase and current working dir
        List<Path> candidates = new ArrayList<>();
        candidates.add(appBase.resolve(".venv/Scripts/python.exe"));
        candidates.add(appBase.resolve("python/python.exe"));
        candidates.add(appBase.resolve("../.venv/Scripts/python.exe"));
        candidates.add(appBase.resolve("../../.venv/Scripts/python.exe"));

        candidates.add(Path.of(".venv", "Scripts", "python.exe"));
        candidates.add(Path.of("..", ".venv", "Scripts", "python.exe"));
        candidates.add(Path.of("..", "..", ".venv", "Scripts", "python.exe"));

        for (Path c : candidates) {
            if (Files.exists(c)) {
                return c.toAbsolutePath().normalize();
            }
        }

        // 3. System python on PATH
        String os = System.getProperty("os.name", "").toLowerCase();
        String pythonCmd = os.contains("win") ? "python.exe" : "python3";

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                Path p = Path.of(dir, pythonCmd);
                if (Files.exists(p)) {
                    return p.toAbsolutePath().normalize();
                }
            }
        }

        return null;
    }

    private static Path findAppScript() {
        Path appBase = getApplicationBaseDir();

        List<Path> candidates = new ArrayList<>();
        candidates.add(appBase.resolve("analysis-service/app.py"));
        candidates.add(appBase.resolve("../analysis-service/app.py"));
        candidates.add(appBase.resolve("../../analysis-service/app.py"));

        candidates.add(Path.of("analysis-service", "app.py"));
        candidates.add(Path.of("..", "analysis-service", "app.py"));
        candidates.add(Path.of("..", "..", "analysis-service", "app.py"));
        candidates.add(Path.of("app.py"));

        for (Path c : candidates) {
            if (Files.exists(c)) {
                return c.toAbsolutePath().normalize();
            }
        }

        return null;
    }
}
