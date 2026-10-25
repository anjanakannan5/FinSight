
package com.financecopilot.services;

import com.financecopilot.database.Database;
import com.financecopilot.models.SavingsGoal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SavingsGoalDAO {

    public void addGoal(String name, double targetAmount, String targetDate)
            throws Exception {
        String sql = """
            INSERT INTO savings_goals (name, target_amount, saved_amount, target_date)
            VALUES (?, ?, 0, ?)
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, targetAmount);
            ps.setString(3, targetDate);
            ps.executeUpdate();
        }
    }

    public List<SavingsGoal> getAllGoals() throws Exception {
        List<SavingsGoal> goals = new ArrayList<>();
        String sql = "SELECT * FROM savings_goals ORDER BY id DESC";

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                goals.add(new SavingsGoal(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("target_amount"),
                    rs.getDouble("saved_amount"),
                    rs.getString("target_date")
                ));
            }
        }

        return goals;
    }

    public void addSavings(int id, double amount) throws Exception {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                "Contribution must be a positive number."
            );
        }

        String sql = """
            UPDATE savings_goals
            SET saved_amount = saved_amount + ?
            WHERE id = ?
            """;

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, id);

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Savings goal not found.");
            }
        }
    }

    public void deleteGoal(int id) throws Exception {
        String sql = "DELETE FROM savings_goals WHERE id = ?";

        try (Connection con = Database.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
