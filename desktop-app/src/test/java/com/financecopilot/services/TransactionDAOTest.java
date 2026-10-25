package com.financecopilot.services;

import com.financecopilot.database.Database;
import com.financecopilot.models.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionDAOTest {

    @BeforeAll
    public static void setupDb(@TempDir Path dbDir) {
        System.setProperty("financecopilot.db.path", dbDir.resolve("test_dao.db").toString());
        Database.initialize();
    }

    @Test
    public void testAddAndGetByMonth() throws Exception {
        TransactionDAO dao = new TransactionDAO();

        Transaction t1 = new Transaction(0, "Salary Oct", "Salary", "INCOME", 50000, "2026-10-01");
        Transaction t2 = new Transaction(0, "Rent Oct", "Housing", "EXPENSE", 15000, "2026-10-02");
        Transaction t3 = new Transaction(0, "Rent Sep", "Housing", "EXPENSE", 15000, "2026-09-02");

        dao.add(t1);
        dao.add(t2);
        dao.add(t3);

        List<Transaction> octTxns = dao.getByMonth("2026-10");
        assertTrue(octTxns.size() >= 2);

        boolean foundSalary = octTxns.stream().anyMatch(t -> t.getDescription().equals("Salary Oct"));
        assertTrue(foundSalary);

        boolean foundSep = octTxns.stream().anyMatch(t -> t.getDescription().equals("Rent Sep"));
        assertFalse(foundSep);
    }

    @Test
    public void testDuplicateDetectionAndBatchAdd() throws Exception {
        TransactionDAO dao = new TransactionDAO();

        Transaction base = new Transaction(0, "Book Purchase", "Education", "EXPENSE", 650, "2026-10-15");
        dao.add(base);

        // Verify exists check
        boolean exists = dao.exists("2026-10-15", "Book Purchase", "Education", "EXPENSE", 650);
        assertTrue(exists);

        boolean notExists = dao.exists("2026-10-15", "Different Book", "Education", "EXPENSE", 650);
        assertFalse(notExists);

        // Test addBatch with duplicate skipping
        List<Transaction> batch = new ArrayList<>();
        batch.add(new Transaction(0, "Book Purchase", "Education", "EXPENSE", 650, "2026-10-15")); // duplicate
        batch.add(new Transaction(0, "Stationery", "Education", "EXPENSE", 120, "2026-10-16")); // new

        int[] stats = dao.addBatch(batch, true);
        assertEquals(1, stats[0], "1 new transaction should be inserted");
        assertEquals(1, stats[1], "1 duplicate transaction should be skipped");
    }
}
