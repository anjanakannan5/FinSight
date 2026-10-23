package com.financecopilot;

import com.financecopilot.database.Database;
import com.financecopilot.models.Transaction;
import com.financecopilot.services.TransactionDAO;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.List;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Database.initialize();

        TransactionDAO dao = new TransactionDAO();

        if (dao.getAll().isEmpty()) {
            dao.add(new Transaction(0, "Monthly Salary",
                "Salary", "INCOME", 45000, "2026-10-01"));

            dao.add(new Transaction(0, "Grocery Shopping",
                "Food", "EXPENSE", 2500, "2026-10-05"));

            dao.add(new Transaction(0, "Internet Bill",
                "Utilities", "EXPENSE", 999, "2026-10-10"));
        }

        List<Transaction> transactions = dao.getAll();

        System.out.println("\n--- Finance Copilot Transactions ---");
        for (Transaction t : transactions) {
            System.out.println(t.getId() + " | " + t.getDescription()
                + " | " + t.getCategory() + " | " + t.getType()
                + " | Rs. " + t.getAmount() + " | " + t.getDate());
        }

        System.out.println("\nTotal transactions: " + transactions.size());

        stage.setTitle("Finance Copilot - Database Test");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}