
package com.financecopilot.services;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RecurringTransactionDAO {
    private final String url = "jdbc:sqlite:../data/finance_copilot.db";

    public RecurringTransactionDAO() {
        try (Connection con = DriverManager.getConnection(url);
             Statement st = con.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS recurring_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    day_of_month INTEGER NOT NULL
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS recurring_generated (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rule_id INTEGER NOT NULL,
                    month TEXT NOT NULL,
                    UNIQUE(rule_id, month)
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Could not create recurring tables", e);
        }
    }

    public void addRule(String description, String category, String type,
                        double amount, int day) throws SQLException {
        String sql = """
            INSERT INTO recurring_transactions
            (description, category, type, amount, day_of_month)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection con = DriverManager.getConnection(url);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setString(2, category);
            ps.setString(3, type);
            ps.setDouble(4, amount);
            ps.setInt(5, day);
            ps.executeUpdate();
        }
    }

    public List<Rule> getRules() throws SQLException {
        List<Rule> rules = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT * FROM recurring_transactions ORDER BY id DESC")) {
            while (rs.next()) {
                rules.add(new Rule(
                    rs.getInt("id"),
                    rs.getString("description"),
                    rs.getString("category"),
                    rs.getString("type"),
                    rs.getDouble("amount"),
                    rs.getInt("day_of_month")
                ));
            }
        }
        return rules;
    }

    
    public int generateForMonth(String month) throws SQLException {
        YearMonth ym = YearMonth.parse(month);
        int generated = 0;

        try (Connection con = DriverManager.getConnection(url)) {
            con.setAutoCommit(false);

            try {
                List<Rule> rules = new ArrayList<>();

                try (Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery(
                        "SELECT * FROM recurring_transactions ORDER BY id DESC")) {
                    while (rs.next()) {
                        rules.add(new Rule(
                            rs.getInt("id"),
                            rs.getString("description"),
                            rs.getString("category"),
                            rs.getString("type"),
                            rs.getDouble("amount"),
                            rs.getInt("day_of_month")
                        ));
                    }
                }

                if (rules.isEmpty()) {
                    throw new SQLException(
                        "No recurring rules found in the database. Add a rule first.");
                }

                for (Rule rule : rules) {
                    boolean alreadyGenerated;

                    try (PreparedStatement check = con.prepareStatement(
                        "SELECT 1 FROM recurring_generated WHERE rule_id=? AND month=?")) {
                        check.setInt(1, rule.getId());
                        check.setString(2, month);

                        try (ResultSet rs = check.executeQuery()) {
                            alreadyGenerated = rs.next();
                        }
                    }

                    if (alreadyGenerated) continue;

                    int day = Math.min(rule.getDay(), ym.lengthOfMonth());
                    String transactionDate = ym.atDay(day).toString();

                    try (PreparedStatement insert = con.prepareStatement("""
                        INSERT INTO transactions
                        (description, category, type, amount, date)
                        VALUES (?, ?, ?, ?, ?)
                    """)) {
                        insert.setString(1, rule.getDescription());
                        insert.setString(2, rule.getCategory());
                        insert.setString(3, rule.getType());
                        insert.setDouble(4, rule.getAmount());
                        insert.setString(5, transactionDate);
                        insert.executeUpdate();
                    }

                    try (PreparedStatement mark = con.prepareStatement(
                        "INSERT INTO recurring_generated(rule_id, month) VALUES (?, ?)")) {
                        mark.setInt(1, rule.getId());
                        mark.setString(2, month);
                        mark.executeUpdate();
                    }

                    generated++;
                }

                con.commit();
                return generated;

            } catch (Exception e) {
                con.rollback();

                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }

                throw new SQLException("Could not generate recurring transactions.", e);
            }
        }
    }

    public void deleteRule(int id) throws SQLException {
        try (Connection con = DriverManager.getConnection(url)) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM recurring_transactions WHERE id=?");
                 PreparedStatement generated = con.prepareStatement(
                    "DELETE FROM recurring_generated WHERE rule_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                generated.setInt(1, id);
                generated.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public static class Rule {
        private final int id;
        private final String description, category, type;
        private final double amount;
        private final int day;

        public Rule(int id, String description, String category,
                    String type, double amount, int day) {
            this.id = id;
            this.description = description;
            this.category = category;
            this.type = type;
            this.amount = amount;
            this.day = day;
        }

        public int getId() { return id; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public String getType() { return type; }
        public double getAmount() { return amount; }
        public int getDay() { return day; }
    }
}
