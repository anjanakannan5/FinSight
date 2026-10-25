package com.financecopilot.services;

import com.financecopilot.database.Database;
import com.financecopilot.models.Transaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public void add(Transaction t) throws Exception {
        String sql = """
            INSERT INTO transactions
            (description, category, type, amount, date)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getDescription().trim());
            ps.setString(2, t.getCategory().trim());
            ps.setString(3, t.getType().trim().toUpperCase());
            ps.setDouble(4, t.getAmount());
            ps.setString(5, t.getDate().trim());
            ps.executeUpdate();
        }
    }

    /**
     * Checks whether an identical transaction already exists in the database.
     * Used for duplicate prevention.
     */
    public boolean exists(String date, String description, String category, String type, double amount) throws SQLException {
        String sql = """
            SELECT 1 FROM transactions
            WHERE date = ? AND LOWER(TRIM(description)) = LOWER(TRIM(?))
              AND LOWER(TRIM(category)) = LOWER(TRIM(?)) AND type = ?
              AND ABS(amount - ?) < 0.001
            LIMIT 1
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, date.trim());
            ps.setString(2, description.trim());
            ps.setString(3, category.trim());
            ps.setString(4, type.trim().toUpperCase());
            ps.setDouble(5, amount);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Inserts a list of transactions inside a single database transaction.
     * Safely rolls back if any database error occurs.
     * 
     * @param transactions The transactions to insert
     * @param skipDuplicates Whether to skip records that already exist
     * @return int[] array: [insertedCount, skippedDuplicatesCount]
     */
    public int[] addBatch(List<Transaction> transactions, boolean skipDuplicates) throws SQLException {
        int inserted = 0;
        int skipped = 0;

        String checkSql = """
            SELECT 1 FROM transactions
            WHERE date = ? AND LOWER(TRIM(description)) = LOWER(TRIM(?))
              AND LOWER(TRIM(category)) = LOWER(TRIM(?)) AND type = ?
              AND ABS(amount - ?) < 0.001
            LIMIT 1
            """;

        String insertSql = """
            INSERT INTO transactions (description, category, type, amount, date)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection con = Database.connect()) {
            con.setAutoCommit(false);

            try (PreparedStatement checkPs = con.prepareStatement(checkSql);
                 PreparedStatement insertPs = con.prepareStatement(insertSql)) {

                for (Transaction t : transactions) {
                    if (skipDuplicates) {
                        checkPs.setString(1, t.getDate().trim());
                        checkPs.setString(2, t.getDescription().trim());
                        checkPs.setString(3, t.getCategory().trim());
                        checkPs.setString(4, t.getType().trim().toUpperCase());
                        checkPs.setDouble(5, t.getAmount());

                        try (ResultSet rs = checkPs.executeQuery()) {
                            if (rs.next()) {
                                skipped++;
                                continue;
                            }
                        }
                    }

                    insertPs.setString(1, t.getDescription().trim());
                    insertPs.setString(2, t.getCategory().trim());
                    insertPs.setString(3, t.getType().trim().toUpperCase());
                    insertPs.setDouble(4, t.getAmount());
                    insertPs.setString(5, t.getDate().trim());
                    insertPs.executeUpdate();
                    inserted++;
                }

                con.commit();
                return new int[]{inserted, skipped};

            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete transaction", e);
        }
    }

    public List<Transaction> getAll() throws Exception {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY date DESC, id DESC";

        try (Connection con = Database.connect();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Transaction(
                    rs.getInt("id"),
                    rs.getString("description"),
                    rs.getString("category"),
                    rs.getString("type"),
                    rs.getDouble("amount"),
                    rs.getString("date")
                ));
            }
        }
        return list;
    }

    public List<Transaction> getByMonth(String month) throws Exception {
        List<Transaction> list = new ArrayList<>();

        String sql = """
            SELECT * FROM transactions
            WHERE substr(date, 1, 7) = ?
            ORDER BY date DESC, id DESC
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, month);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Transaction(
                        rs.getInt("id"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getString("date")
                    ));
                }
            }
        }

        return list;
    }
}
