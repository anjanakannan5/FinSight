
package com.financecopilot.services;

import com.financecopilot.models.Transaction;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class AnalysisService {
    private static final String API_URL = "http://127.0.0.1:5000";
    private final HttpClient client;

    public AnalysisService() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String checkHealth() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/health"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Python API returned status " + response.statusCode());
        }

        return response.body();
    }

    public String analyzeTransactions(List<Transaction> transactions)
            throws Exception {
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Add transactions to SQLite before running analysis.");
        }

        StringBuilder json = new StringBuilder("{\"transactions\":[");

        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                    .append("\"type\":\"").append(escape(t.getType())).append("\",")
                    .append("\"category\":\"").append(escape(t.getCategory())).append("\",")
                    .append("\"amount\":").append(t.getAmount())
                    .append("}");
        }

        json.append("]}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/analyze"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Analysis failed: " + response.body());
        }

        return response.body();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
