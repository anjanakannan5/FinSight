package com.financecopilot.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;

public class Database {
    private static final String DB_URL;

    static {
        try {
            Path folder = Path.of("..", "data").toAbsolutePath();
            Files.createDirectories(folder);
            DB_URL = "jdbc:sqlite:" + folder.resolve("finance_copilot.db");
        } catch (Exception e) {
            throw new RuntimeException("Database setup failed", e);
        }
    }

    public static Connection connect() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initialize() {
        String sql = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                type TEXT NOT NULL CHECK(type IN ('INCOME', 'EXPENSE')),
                amount REAL NOT NULL CHECK(amount > 0),
                date TEXT NOT NULL
            )
            """;

        try (Connection con = connect();
             Statement stmt = con.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database connected successfully.");
        } catch (Exception e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}