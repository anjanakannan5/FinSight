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
            ps.setString(1, t.getDescription());
            ps.setString(2, t.getCategory());
            ps.setString(3, t.getType());
            ps.setDouble(4, t.getAmount());
            ps.setString(5, t.getDate());
            ps.executeUpdate();
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
}