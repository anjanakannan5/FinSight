package com.financecopilot.services;

import com.financecopilot.database.Database;
import com.financecopilot.models.CsvRowPreview;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class CsvImportServiceTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    public static void setupDb(@TempDir Path dbDir) {
        System.setProperty("financecopilot.db.path", dbDir.resolve("test_copilot.db").toString());
        Database.initialize();
    }

    @Test
    public void testValidStandardCsv() throws IOException {
        File csvFile = tempDir.resolve("valid_standard.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write("date,description,category,type,amount\n");
            writer.write("2026-10-01,Salary,Income,INCOME,45000\n");
            writer.write("2026-10-02,Groceries,Food,EXPENSE,1500\n");
            writer.write("2026-10-04,Electricity,Bills,EXPENSE,1200\n");
            writer.write("2026-10-05,Bus Pass,Transport,EXPENSE,800\n");
        }

        CsvImportService service = new CsvImportService();
        CsvImportService.CsvImportResult result = service.parseAndValidate(csvFile);

        assertFalse(result.hasHeaderError());
        assertEquals(4, result.getTotalRows());
        assertEquals(4, result.getValidRows());
        assertEquals(0, result.getInvalidRows());
        assertEquals(0, result.getDuplicateRows());
        assertEquals(4, result.getValidTransactions().size());
    }

    @Test
    public void testQuotedCsvWithCommas() throws IOException {
        File csvFile = tempDir.resolve("quoted_commas.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write("date,description,category,type,amount\n");
            writer.write("2026-10-03,\"Supermarket, Vegetables & Fruits\",Food,EXPENSE,1250.50\n");
            writer.write("2026-10-05,\"Online \"\"Pro\"\" Subscription\",Software,EXPENSE,499.00\n");
        }

        CsvImportService service = new CsvImportService();
        CsvImportService.CsvImportResult result = service.parseAndValidate(csvFile);

        assertFalse(result.hasHeaderError());
        assertEquals(2, result.getTotalRows());
        assertEquals(2, result.getValidRows());
        assertEquals("Supermarket, Vegetables & Fruits", result.getRows().get(0).getNormalizedDescription());
        assertEquals("Online \"Pro\" Subscription", result.getRows().get(1).getNormalizedDescription());
    }

    @Test
    public void testValidationErrors() throws IOException {
        File csvFile = tempDir.resolve("errors.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write("date,description,category,type,amount\n");
            writer.write("not-a-date,Valid Desc,Food,EXPENSE,100\n"); // invalid date
            writer.write("2026-10-02,,Food,EXPENSE,200\n"); // missing desc
            writer.write("2026-10-03,Groceries,,EXPENSE,300\n"); // missing category
            writer.write("2026-10-04,Shopping,Retail,INVALID_TYPE,400\n"); // invalid type
            writer.write("2026-10-05,Coffee,Food,EXPENSE,-50\n"); // negative amount
            writer.write("2026-10-06,Valid Txn,General,EXPENSE,150\n"); // valid
        }

        CsvImportService service = new CsvImportService();
        CsvImportService.CsvImportResult result = service.parseAndValidate(csvFile);

        assertFalse(result.hasHeaderError());
        assertEquals(6, result.getTotalRows());
        assertEquals(1, result.getValidRows());
        assertEquals(5, result.getInvalidRows());

        // Check line numbers and error status
        assertEquals(CsvRowPreview.Status.ERROR, result.getRows().get(0).getStatus());
        assertTrue(result.getRows().get(0).getMessage().contains("date"));

        assertEquals(CsvRowPreview.Status.ERROR, result.getRows().get(1).getStatus());
        assertTrue(result.getRows().get(1).getMessage().contains("Description"));

        assertEquals(CsvRowPreview.Status.ERROR, result.getRows().get(2).getStatus());
        assertTrue(result.getRows().get(2).getMessage().contains("Category"));

        assertEquals(CsvRowPreview.Status.ERROR, result.getRows().get(3).getStatus());
        assertTrue(result.getRows().get(3).getMessage().contains("Type"));

        assertEquals(CsvRowPreview.Status.ERROR, result.getRows().get(4).getStatus());
        assertTrue(result.getRows().get(4).getMessage().contains("greater than zero"));

        assertEquals(CsvRowPreview.Status.VALID, result.getRows().get(5).getStatus());
    }

    @Test
    public void testInFileDuplicateDetection() throws IOException {
        File csvFile = tempDir.resolve("duplicates.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write("date,description,category,type,amount\n");
            writer.write("2026-10-10,Monthly Internet,Bills,EXPENSE,799\n");
            writer.write("2026-10-10,Monthly Internet,Bills,EXPENSE,799\n"); // duplicate of row above
        }

        CsvImportService service = new CsvImportService();
        CsvImportService.CsvImportResult result = service.parseAndValidate(csvFile);

        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getValidRows());
        assertEquals(1, result.getDuplicateRows());
        assertEquals(CsvRowPreview.Status.DUPLICATE, result.getRows().get(1).getStatus());
    }

    @Test
    public void testUtf8BomHandling() throws IOException {
        File csvFile = tempDir.resolve("bom.csv").toFile();
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = "date,description,category,type,amount\n2026-10-11,Tea,Food,EXPENSE,40\n".getBytes(StandardCharsets.UTF_8);

        byte[] all = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, all, 0, bom.length);
        System.arraycopy(content, 0, all, bom.length, content.length);

        Files.write(csvFile.toPath(), all);

        CsvImportService service = new CsvImportService();
        CsvImportService.CsvImportResult result = service.parseAndValidate(csvFile);

        assertFalse(result.hasHeaderError());
        assertEquals(1, result.getValidRows());
    }

    @Test
    public void testMissingHeaders() throws IOException {
        File csvFile = tempDir.resolve("bad_headers.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write("colA,colB,colC\n1,2,3\n");
        }

        CsvImportService service = new CsvImportService();
        CsvImportService.CsvImportResult result = service.parseAndValidate(csvFile);

        assertTrue(result.hasHeaderError());
        assertTrue(result.getHeaderError().contains("Missing required column(s)"));
    }
}
