package com.financecopilot.services;

import com.financecopilot.models.CsvRowPreview;
import com.financecopilot.models.Transaction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class CsvImportService {

    private final TransactionDAO transactionDAO;

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy")
    };

    public CsvImportService() {
        this(new TransactionDAO());
    }

    public CsvImportService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    public static class CsvImportResult {
        private final File file;
        private final List<CsvRowPreview> rows;
        private final int totalRows;
        private final int validRows;
        private final int duplicateRows;
        private final int invalidRows;
        private final String headerError;

        public CsvImportResult(File file, List<CsvRowPreview> rows, int totalRows,
                               int validRows, int duplicateRows, int invalidRows, String headerError) {
            this.file = file;
            this.rows = rows;
            this.totalRows = totalRows;
            this.validRows = validRows;
            this.duplicateRows = duplicateRows;
            this.invalidRows = invalidRows;
            this.headerError = headerError;
        }

        public File getFile() { return file; }
        public List<CsvRowPreview> getRows() { return rows; }
        public int getTotalRows() { return totalRows; }
        public int getValidRows() { return validRows; }
        public int getDuplicateRows() { return duplicateRows; }
        public int getInvalidRows() { return invalidRows; }
        public String getHeaderError() { return headerError; }
        public boolean hasHeaderError() { return headerError != null; }

        public List<Transaction> getValidTransactions() {
            List<Transaction> list = new ArrayList<>();
            for (CsvRowPreview row : rows) {
                if (row.isValid()) {
                    list.add(row.toTransaction());
                }
            }
            return list;
        }
    }

    public CsvImportResult parseAndValidate(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        List<List<String>> allTokens = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            allTokens = readCsvTokens(reader);
        }

        if (allTokens.isEmpty()) {
            return new CsvImportResult(file, Collections.emptyList(), 0, 0, 0, 0,
                "The selected CSV file is empty.");
        }

        // Identify headers
        List<String> headerRow = allTokens.get(0);
        Map<String, Integer> columnMap = mapHeaders(headerRow);

        List<String> missingHeaders = new ArrayList<>();
        if (!columnMap.containsKey("date")) missingHeaders.add("date");
        if (!columnMap.containsKey("description")) missingHeaders.add("description");
        if (!columnMap.containsKey("category")) missingHeaders.add("category");
        if (!columnMap.containsKey("type")) missingHeaders.add("type");
        if (!columnMap.containsKey("amount")) missingHeaders.add("amount");

        if (!missingHeaders.isEmpty()) {
            String errorMsg = "Missing required column(s): " + String.join(", ", missingHeaders)
                + ". Required headers: date, description, category, type, amount.";
            return new CsvImportResult(file, Collections.emptyList(), 0, 0, 0, 0, errorMsg);
        }

        int dateIdx = columnMap.get("date");
        int descIdx = columnMap.get("description");
        int catIdx = columnMap.get("category");
        int typeIdx = columnMap.get("type");
        int amtIdx = columnMap.get("amount");

        List<CsvRowPreview> previewRows = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int validCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;

        for (int i = 1; i < allTokens.size(); i++) {
            int lineNum = i + 1; // 1-based line number in file
            List<String> rowTokens = allTokens.get(i);

            // Skip empty rows
            if (rowTokens.isEmpty() || (rowTokens.size() == 1 && rowTokens.get(0).isBlank())) {
                continue;
            }

            String rawDate = safeGet(rowTokens, dateIdx);
            String rawDesc = safeGet(rowTokens, descIdx);
            String rawCat = safeGet(rowTokens, catIdx);
            String rawType = safeGet(rowTokens, typeIdx);
            String rawAmt = safeGet(rowTokens, amtIdx);

            CsvRowPreview preview = new CsvRowPreview(lineNum, rawDate, rawDesc, rawCat, rawType, rawAmt);
            List<String> rowErrors = new ArrayList<>();

            // 1. Validate Date
            String normalizedDate = null;
            if (rawDate.isBlank()) {
                rowErrors.add("Date is empty");
            } else {
                normalizedDate = parseDate(rawDate);
                if (normalizedDate == null) {
                    rowErrors.add("Invalid date format '" + rawDate + "'. Expected YYYY-MM-DD or DD/MM/YYYY");
                } else {
                    preview.setNormalizedDate(normalizedDate);
                }
            }

            // 2. Validate Description
            if (rawDesc.isBlank()) {
                rowErrors.add("Description is empty");
            } else {
                preview.setNormalizedDescription(rawDesc.trim());
            }

            // 3. Validate Category
            if (rawCat.isBlank()) {
                rowErrors.add("Category is empty");
            } else {
                preview.setNormalizedCategory(rawCat.trim());
            }

            // 4. Validate Type
            String normalizedType = null;
            if (rawType.isBlank()) {
                rowErrors.add("Type is empty");
            } else {
                normalizedType = normalizeType(rawType);
                if (normalizedType == null) {
                    rowErrors.add("Type must be INCOME or EXPENSE (got '" + rawType + "')");
                } else {
                    preview.setNormalizedType(normalizedType);
                }
            }

            // 5. Validate Amount
            Double parsedAmount = null;
            if (rawAmt.isBlank()) {
                rowErrors.add("Amount is empty");
            } else {
                parsedAmount = parseAmount(rawAmt);
                if (parsedAmount == null) {
                    rowErrors.add("Amount must be a valid number (got '" + rawAmt + "')");
                } else if (!Double.isFinite(parsedAmount) || parsedAmount <= 0) {
                    rowErrors.add("Amount must be greater than zero");
                } else {
                    preview.setNormalizedAmount(parsedAmount);
                }
            }

            if (!rowErrors.isEmpty()) {
                preview.setStatus(CsvRowPreview.Status.ERROR);
                preview.setMessage(String.join("; ", rowErrors));
                invalidCount++;
            } else {
                // Check in-file duplicate
                String fingerprint = normalizedDate + "|"
                    + preview.getNormalizedDescription().toLowerCase() + "|"
                    + preview.getNormalizedCategory().toLowerCase() + "|"
                    + normalizedType + "|"
                    + String.format(Locale.US, "%.2f", parsedAmount);

                if (!seenInFile.add(fingerprint)) {
                    preview.setStatus(CsvRowPreview.Status.DUPLICATE);
                    preview.setMessage("Duplicate record within this CSV file (skipped)");
                    duplicateCount++;
                } else {
                    // Check database duplicate
                    boolean existsInDb = false;
                    try {
                        existsInDb = transactionDAO.exists(
                            normalizedDate,
                            preview.getNormalizedDescription(),
                            preview.getNormalizedCategory(),
                            normalizedType,
                            parsedAmount
                        );
                    } catch (Exception ex) {
                        System.err.println("Warning: Could not check duplicate in DB: " + ex.getMessage());
                    }

                    if (existsInDb) {
                        preview.setStatus(CsvRowPreview.Status.DUPLICATE);
                        preview.setMessage("Already exists in database (skipped to prevent double-entry)");
                        duplicateCount++;
                    } else {
                        preview.setStatus(CsvRowPreview.Status.VALID);
                        preview.setMessage("Ready to import");
                        validCount++;
                    }
                }
            }

            previewRows.add(preview);
        }

        return new CsvImportResult(file, previewRows, previewRows.size(), validCount, duplicateCount, invalidCount, null);
    }

    private static String safeGet(List<String> list, int index) {
        if (index >= 0 && index < list.size()) {
            return list.get(index).trim();
        }
        return "";
    }

    private Map<String, Integer> mapHeaders(List<String> headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).trim().toLowerCase();
            // Clean UTF-8 BOM if in first header
            if (i == 0 && h.startsWith("\uFEFF")) {
                h = h.substring(1).trim();
            }

            if (h.equals("date") || h.equals("transaction date") || h.equals("txn date") || h.equals("date of transaction")) {
                map.put("date", i);
            } else if (h.equals("description") || h.equals("desc") || h.equals("narration") || h.equals("particulars") || h.equals("details") || h.equals("memo")) {
                map.put("description", i);
            } else if (h.equals("category") || h.equals("cat") || h.equals("tag") || h.equals("classification")) {
                map.put("category", i);
            } else if (h.equals("type") || h.equals("transaction type") || h.equals("txn type") || h.equals("cr/dr")) {
                map.put("type", i);
            } else if (h.equals("amount") || h.equals("amt") || h.equals("value") || h.equals("total")) {
                map.put("amount", i);
            }
        }
        return map;
    }

    private String parseDate(String text) {
        text = text.trim();
        for (DateTimeFormatter dtf : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(text, dtf);
                return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String normalizeType(String text) {
        text = text.trim().toUpperCase();
        if (text.equals("INCOME") || text.equals("CREDIT") || text.equals("CR")) {
            return "INCOME";
        }
        if (text.equals("EXPENSE") || text.equals("DEBIT") || text.equals("DR") || text.equals("EXPENSES")) {
            return "EXPENSE";
        }
        return null;
    }

    private Double parseAmount(String text) {
        text = text.trim();
        // Remove currency symbols and thousand separators: ₹, $, €, £, commas
        text = text.replaceAll("[₹$€£, ]", "");
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * RFC 4180 compliant CSV tokenizer that handles quotes, escaped quotes,
     * commas inside quotes, and line breaks.
     */
    public static List<List<String>> readCsvTokens(BufferedReader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder curField = new StringBuilder();
        boolean inQuotes = false;
        int chInt;

        while ((chInt = reader.read()) != -1) {
            char ch = (char) chInt;

            // Strip UTF-8 BOM if at the very start
            if (ch == '\uFEFF') {
                continue;
            }

            if (inQuotes) {
                if (ch == '"') {
                    // Lookahead to check if this is an escaped double quote ("")
                    reader.mark(1);
                    int nextInt = reader.read();
                    if (nextInt == '"') {
                        curField.append('"');
                    } else {
                        inQuotes = false;
                        if (nextInt != -1) {
                            reader.reset();
                        }
                    }
                } else {
                    curField.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    currentRow.add(curField.toString().trim());
                    curField.setLength(0);
                } else if (ch == '\r') {
                    // Check for \n
                    reader.mark(1);
                    int nextInt = reader.read();
                    if (nextInt != '\n' && nextInt != -1) {
                        reader.reset();
                    }
                    currentRow.add(curField.toString().trim());
                    curField.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                } else if (ch == '\n') {
                    currentRow.add(curField.toString().trim());
                    curField.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                } else {
                    curField.append(ch);
                }
            }
        }

        if (curField.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(curField.toString().trim());
            rows.add(currentRow);
        }

        return rows;
    }
}
