package com.financecopilot.models;

public class CsvRowPreview {

    public enum Status {
        VALID("Valid"),
        DUPLICATE("Duplicate (Skipped)"),
        ERROR("Invalid");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final int lineNumber;
    private final String rawDate;
    private final String rawDescription;
    private final String rawCategory;
    private final String rawType;
    private final String rawAmount;

    private String normalizedDate;
    private String normalizedDescription;
    private String normalizedCategory;
    private String normalizedType;
    private double normalizedAmount;

    private Status status;
    private String message;

    public CsvRowPreview(int lineNumber, String rawDate, String rawDescription,
                         String rawCategory, String rawType, String rawAmount) {
        this.lineNumber = lineNumber;
        this.rawDate = rawDate != null ? rawDate : "";
        this.rawDescription = rawDescription != null ? rawDescription : "";
        this.rawCategory = rawCategory != null ? rawCategory : "";
        this.rawType = rawType != null ? rawType : "";
        this.rawAmount = rawAmount != null ? rawAmount : "";
        this.status = Status.VALID;
        this.message = "";
    }

    public int getLineNumber() { return lineNumber; }
    public String getRawDate() { return rawDate; }
    public String getRawDescription() { return rawDescription; }
    public String getRawCategory() { return rawCategory; }
    public String getRawType() { return rawType; }
    public String getRawAmount() { return rawAmount; }

    public String getNormalizedDate() { return normalizedDate != null ? normalizedDate : rawDate; }
    public void setNormalizedDate(String normalizedDate) { this.normalizedDate = normalizedDate; }

    public String getNormalizedDescription() { return normalizedDescription != null ? normalizedDescription : rawDescription; }
    public void setNormalizedDescription(String normalizedDescription) { this.normalizedDescription = normalizedDescription; }

    public String getNormalizedCategory() { return normalizedCategory != null ? normalizedCategory : rawCategory; }
    public void setNormalizedCategory(String normalizedCategory) { this.normalizedCategory = normalizedCategory; }

    public String getNormalizedType() { return normalizedType != null ? normalizedType : rawType; }
    public void setNormalizedType(String normalizedType) { this.normalizedType = normalizedType; }

    public double getNormalizedAmount() { return normalizedAmount; }
    public void setNormalizedAmount(double normalizedAmount) { this.normalizedAmount = normalizedAmount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isValid() { return status == Status.VALID; }
    public boolean isDuplicate() { return status == Status.DUPLICATE; }

    public Transaction toTransaction() {
        return new Transaction(
            0,
            getNormalizedDescription(),
            getNormalizedCategory(),
            getNormalizedType(),
            getNormalizedAmount(),
            getNormalizedDate()
        );
    }
}
