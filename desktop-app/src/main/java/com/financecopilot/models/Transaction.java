package com.financecopilot.models;

public class Transaction {
    private int id;
    private String description;
    private String category;
    private String type;
    private double amount;
    private String date;

    public Transaction(int id, String description, String category,
                       String type, double amount, String date) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.date = date;
    }

    public int getId() { return id; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
}