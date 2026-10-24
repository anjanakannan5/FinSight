
package com.financecopilot.services;

import com.financecopilot.database.Database;
import com.financecopilot.models.Transaction;
import java.util.List;

public class AnalysisServiceTest {
    public static void main(String[] args) {
        try {
            Database.initialize();

            TransactionDAO dao = new TransactionDAO();
            List<Transaction> transactions = dao.getAll();

            AnalysisService service = new AnalysisService();

            System.out.println("Python API: " + service.checkHealth());
            System.out.println("Transactions found: " + transactions.size());

            String result = service.analyzeTransactions(transactions);

            System.out.println("Financial Analysis:");
            System.out.println(result);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
