package com.financecopilot.database;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Centralized SQLite Database connection and schema management for FinSight.
 * Ensures a stable, writable per-user application-data location on Windows
 * (%APPDATA%\FinSight\data\finsight.db) while supporting development overrides
 * and backward compatibility with earlier FinanceCopilot data.
 */
public class Database {

    private static Path currentDbPath;
    private static String currentDbUrl;

    static {
        currentDbPath = resolveDatabasePath();
        currentDbUrl = "jdbc:sqlite:" + currentDbPath.toAbsolutePath();
        System.out.println("FinSight database resolved at: " + currentDbPath.toAbsolutePath());
    }

    public static synchronized void setDatabasePath(Path customPath) {
        try {
            if (customPath.getParent() != null) {
                Files.createDirectories(customPath.getParent());
            }
            currentDbPath = customPath.toAbsolutePath();
            currentDbUrl = "jdbc:sqlite:" + currentDbPath;
        } catch (Exception e) {
            throw new RuntimeException("Could not set database path to " + customPath, e);
        }
    }

    public static synchronized Path getDatabasePath() {
        String prop = System.getProperty("finsight.db.path");
        if (prop == null || prop.isBlank()) {
            prop = System.getProperty("financecopilot.db.path");
        }
        if (prop != null && !prop.isBlank()) {
            Path p = Path.of(prop).toAbsolutePath();
            if (!p.equals(currentDbPath)) {
                setDatabasePath(p);
            }
        }
        return currentDbPath;
    }

    public static synchronized String getDatabaseUrl() {
        getDatabasePath(); // ensure synchronized with system property
        return currentDbUrl;
    }

    private static Path resolveDatabasePath() {
        try {
            // 1. Explicit system property override
            String customPath = System.getProperty("finsight.db.path");
            if (customPath == null || customPath.isBlank()) {
                customPath = System.getProperty("financecopilot.db.path");
            }
            if (customPath != null && !customPath.isBlank()) {
                Path p = Path.of(customPath).toAbsolutePath();
                if (p.getParent() != null) {
                    Files.createDirectories(p.getParent());
                }
                return p;
            }

            // 2. Explicit environment variable override
            String envPath = System.getenv("FINSIGHT_DB");
            if (envPath == null || envPath.isBlank()) {
                envPath = System.getenv("FINANCE_COPILOT_DB");
            }
            if (envPath != null && !envPath.isBlank()) {
                Path p = Path.of(envPath).toAbsolutePath();
                if (p.getParent() != null) {
                    Files.createDirectories(p.getParent());
                }
                return p;
            }

            // 3. Development mode detection
            String devProp = System.getProperty("finsight.dev");
            if (devProp == null) devProp = System.getProperty("financecopilot.dev");
            if (Boolean.parseBoolean(devProp)) {
                Path devFolder = Path.of("..", "data").toAbsolutePath();
                if (!Files.exists(devFolder)) {
                    devFolder = Path.of("data").toAbsolutePath();
                }
                Files.createDirectories(devFolder);
                return devFolder.resolve("finsight.db");
            }

            // 4. Production Windows AppData location: %APPDATA%\FinSight\data\finsight.db
            String appData = System.getenv("APPDATA");
            Path targetDir;
            if (appData != null && !appData.isBlank()) {
                targetDir = Path.of(appData, "FinSight", "data");
            } else {
                targetDir = Path.of(System.getProperty("user.home"), ".finsight", "data");
            }

            Files.createDirectories(targetDir);
            Path prodDb = targetDir.resolve("finsight.db");

            // Safe migration: check if previous FinanceCopilot db exists in AppData
            if (!Files.exists(prodDb)) {
                if (appData != null) {
                    Path legacyAppData = Path.of(appData, "FinanceCopilot", "data", "finance_copilot.db");
                    if (Files.exists(legacyAppData)) {
                        try {
                            Files.copy(legacyAppData, prodDb);
                            System.out.println("Migrated existing user database from " + legacyAppData + " to " + prodDb);
                            return prodDb;
                        } catch (Exception ignored) {}
                    }
                }

                // Or check project dev data
                Path existingDev = Path.of("..", "data", "finance_copilot.db").toAbsolutePath();
                if (!Files.exists(existingDev)) {
                    existingDev = Path.of("data", "finance_copilot.db").toAbsolutePath();
                }
                if (Files.exists(existingDev)) {
                    try {
                        Files.copy(existingDev, prodDb);
                        System.out.println("Migrated database from project folder to: " + prodDb);
                    } catch (Exception ex) {
                        System.err.println("Note: Initializing fresh FinSight database: " + ex.getMessage());
                    }
                }
            }

            return prodDb;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve or initialize database path", e);
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(getDatabaseUrl());
    }

    public static void initialize() {
        String transactionsSQL = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                type TEXT NOT NULL CHECK(type IN ('INCOME', 'EXPENSE')),
                amount REAL NOT NULL CHECK(amount > 0),
                date TEXT NOT NULL
            );
            """;

        String goalsSQL = """
            CREATE TABLE IF NOT EXISTS savings_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                target_amount REAL NOT NULL CHECK(target_amount > 0),
                saved_amount REAL NOT NULL DEFAULT 0 CHECK(saved_amount >= 0),
                target_date TEXT NOT NULL
            );
            """;

        String budgetsSQL = """
            CREATE TABLE IF NOT EXISTS budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                month TEXT NOT NULL,
                limit_amount REAL NOT NULL CHECK(limit_amount > 0),
                UNIQUE(category, month)
            );
            """;

        String recurringSQL = """
            CREATE TABLE IF NOT EXISTS recurring_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                type TEXT NOT NULL CHECK(type IN ('INCOME', 'EXPENSE')),
                amount REAL NOT NULL CHECK(amount > 0),
                day_of_month INTEGER NOT NULL CHECK(day_of_month BETWEEN 1 AND 31)
            );
            """;

        String recurringGeneratedSQL = """
            CREATE TABLE IF NOT EXISTS recurring_generated (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rule_id INTEGER NOT NULL,
                month TEXT NOT NULL,
                UNIQUE(rule_id, month)
            );
            """;

        try (Connection con = connect();
             Statement stmt = con.createStatement()) {
            stmt.execute(transactionsSQL);
            stmt.execute(goalsSQL);
            stmt.execute(budgetsSQL);
            stmt.execute(recurringSQL);
            stmt.execute(recurringGeneratedSQL);
            System.out.println("All FinSight database tables verified and ready at " + getDatabaseUrl());
        } catch (Exception e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}
