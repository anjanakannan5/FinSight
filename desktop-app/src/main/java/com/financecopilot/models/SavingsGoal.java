
package com.financecopilot.models;

public class SavingsGoal {
    private int id;
    private String name;
    private double targetAmount;
    private double savedAmount;
    private String targetDate;

    public SavingsGoal(int id, String name, double targetAmount,
                       double savedAmount, String targetDate) {
        this.id = id;
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
        this.targetDate = targetDate;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public double getSavedAmount() {
        return savedAmount;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public double getRemainingAmount() {
        return Math.max(0, targetAmount - savedAmount);
    }

    public double getProgress() {
        return Math.min(1.0, savedAmount / targetAmount);
    }

    public boolean isCompleted() {
        return savedAmount >= targetAmount;
    }

    public void addSavings(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                "Savings contribution must be a positive number."
            );
        }

        savedAmount += amount;
    }
}
