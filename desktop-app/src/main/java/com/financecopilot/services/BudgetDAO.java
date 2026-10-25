package com.financecopilot.services;

import com.financecopilot.database.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetDAO {

    public BudgetDAO() {
        // Table schema is guaranteed by Database.initialize()
    }

    public void saveBudget(String category, String month, double limit) throws SQLException {
        String sql = """
            INSERT INTO budgets(category, month, limit_amount)
            VALUES(?, ?, ?)
            ON CONFLICT(category, month)
            DO UPDATE SET limit_amount = excluded.limit_amount
        """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setString(2, month);
            ps.setDouble(3, limit);
            ps.executeUpdate();
        }
    }

    public List<BudgetSummary> getBudgets(String month) throws SQLException {
        List<BudgetSummary> list = new ArrayList<>();

        String sql = """
            SELECT b.id, b.category, b.month, b.limit_amount,
            COALESCE(SUM(t.amount), 0) AS spent
            FROM budgets b
            LEFT JOIN transactions t
            ON t.category = b.category
            AND t.type = 'EXPENSE'
            AND substr(t.date, 1, 7) = b.month
            WHERE b.month = ?
            GROUP BY b.id, b.category, b.month, b.limit_amount
            ORDER BY b.category
        """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, month);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BudgetSummary(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getString("month"),
                        rs.getDouble("limit_amount"),
                        rs.getDouble("spent")
                    ));
                }
            }
        }
        return list;
    }

    public void deleteBudget(int id) throws SQLException {
        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM budgets WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static class BudgetSummary {
        private final int id;
        private final String category;
        private final String month;
        private final double limit;
        private final double spent;

        public BudgetSummary(int id, String category, String month,
                             double limit, double spent) {
            this.id = id;
            this.category = category;
            this.month = month;
            this.limit = limit;
            this.spent = spent;
        }

        public int getId() { return id; }
        public String getCategory() { return category; }
        public String getMonth() { return month; }
        public double getLimit() { return limit; }
        public double getSpent() { return spent; }
        public double getRemaining() { return limit - spent; }
        public double getProgress() {
            return limit > 0 ? Math.min(1.0, spent / limit) : 0;
        }
        public String getStatus() {
            if (spent > limit) return "OVER BUDGET";
            if (spent >= limit * 0.8) return "NEAR LIMIT";
            return "Within Budget";
        }
    }
}
