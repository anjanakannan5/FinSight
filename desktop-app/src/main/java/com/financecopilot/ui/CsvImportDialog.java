package com.financecopilot.ui;

import com.financecopilot.models.CsvRowPreview;
import com.financecopilot.models.Transaction;
import com.financecopilot.services.CsvImportService.CsvImportResult;
import com.financecopilot.services.TransactionDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

public class CsvImportDialog {

    private final Window owner;
    private final CsvImportResult importResult;
    private final TransactionDAO transactionDAO;
    private boolean importedSuccessfully = false;
    private int importedCount = 0;
    private int skippedCount = 0;

    public CsvImportDialog(Window owner, CsvImportResult importResult, TransactionDAO transactionDAO) {
        this.owner = owner;
        this.importResult = importResult;
        this.transactionDAO = transactionDAO;
    }

    public boolean showAndWait() {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Import CSV Statement — FinSight");

        // Header
        Label titleLabel = new Label("Review & Import Statement");
        titleLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #362E28;");

        Label fileLabel = new Label("Source file: " + importResult.getFile().getName());
        fileLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        // Summary Badges
        HBox badgesBox = new HBox(12);
        badgesBox.setAlignment(Pos.CENTER_LEFT);

        badgesBox.getChildren().addAll(
            createBadge("Total: " + importResult.getTotalRows(), "#E6D9C8", "#362E28"),
            createBadge("Ready: " + importResult.getValidRows(), "#DDE5D6", "#3E5C3B"),
            createBadge("Duplicates: " + importResult.getDuplicateRows(), "#EFE6D8", "#4A4036"),
            createBadge("Errors: " + importResult.getInvalidRows(), "#F8E4DF", "#B96F52")
        );

        // Policy callout
        Label policyLabel = new Label(
            "Note: Duplicate entries matching existing records (same date, description, category, type, and amount) are safely skipped to prevent double counting."
        );
        policyLabel.setWrapText(true);
        policyLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #4A4036; -fx-background-color: #EFE6D8; -fx-padding: 8 12; -fx-background-radius: 4;");

        // Table Preview
        TableView<CsvRowPreview> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(importResult.getRows()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(340);

        TableColumn<CsvRowPreview, String> lineCol = new TableColumn<>("Line");
        lineCol.setMinWidth(50);
        lineCol.setMaxWidth(60);
        lineCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getLineNumber())));

        TableColumn<CsvRowPreview, String> statusCol = new TableColumn<>("Status");
        statusCol.setMinWidth(110);
        statusCol.setMaxWidth(130);
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getLabel()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Valid")) {
                        setStyle("-fx-text-fill: #3E5C3B; -fx-font-weight: bold;");
                    } else if (item.contains("Duplicate")) {
                        setStyle("-fx-text-fill: #4A4036; -fx-font-style: italic; -fx-font-weight: 500;");
                    } else {
                        setStyle("-fx-text-fill: #B96F52; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<CsvRowPreview, String> dateCol = new TableColumn<>("Date");
        dateCol.setMinWidth(95);
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNormalizedDate()));

        TableColumn<CsvRowPreview, String> descCol = new TableColumn<>("Description");
        descCol.setMinWidth(160);
        descCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNormalizedDescription()));

        TableColumn<CsvRowPreview, String> catCol = new TableColumn<>("Category");
        catCol.setMinWidth(100);
        catCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNormalizedCategory()));

        TableColumn<CsvRowPreview, String> typeCol = new TableColumn<>("Type");
        typeCol.setMinWidth(80);
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNormalizedType()));

        TableColumn<CsvRowPreview, String> amtCol = new TableColumn<>("Amount");
        amtCol.setMinWidth(95);
        amtCol.setCellValueFactory(c -> {
            if (c.getValue().isValid() || c.getValue().isDuplicate()) {
                return new SimpleStringProperty(String.format("₹%.2f", c.getValue().getNormalizedAmount()));
            } else {
                return new SimpleStringProperty(c.getValue().getRawAmount());
            }
        });

        TableColumn<CsvRowPreview, String> noteCol = new TableColumn<>("Validation Note");
        noteCol.setMinWidth(180);
        noteCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMessage()));

        table.getColumns().addAll(lineCol, statusCol, dateCol, descCol, catCol, typeCol, amtCol, noteCol);

        // Buttons
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #362E28; "
            + "-fx-border-color: #D8CCBC; -fx-border-radius: 4; -fx-padding: 8 18; -fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> stage.close());

        Button importButton = new Button("Import " + importResult.getValidRows() + " Valid Rows");
        importButton.setStyle(
            "-fx-background-color: #7D8661; -fx-text-fill: #FFFCF7; "
            + "-fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 8 20; -fx-cursor: hand;"
        );

        if (importResult.getValidRows() == 0) {
            importButton.setDisable(true);
            importButton.setTooltip(new Tooltip("No valid new transactions to import."));
        }

        importButton.setOnAction(e -> {
            importButton.setDisable(true);
            cancelButton.setDisable(true);
            try {
                List<Transaction> validTxns = importResult.getValidTransactions();
                int[] stats = transactionDAO.addBatch(validTxns, true);
                importedCount = stats[0];
                skippedCount = stats[1] + importResult.getDuplicateRows();
                importedSuccessfully = true;

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("CSV Import Complete");
                alert.setHeaderText("Transactions Successfully Imported");
                alert.setContentText(String.format(
                    "Successfully added %d transaction(s) to your ledger.%nSkipped %d duplicate record(s).%n%d invalid record(s) were excluded.",
                    importedCount, skippedCount, importResult.getInvalidRows()
                ));
                alert.initOwner(stage);
                alert.showAndWait();

                stage.close();
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Import Error");
                errorAlert.setHeaderText("Failed to Save Transactions");
                errorAlert.setContentText("A database error occurred during import: " + ex.getMessage());
                errorAlert.initOwner(stage);
                errorAlert.showAndWait();
                importButton.setDisable(false);
                cancelButton.setDisable(false);
            }
        });

        HBox actionsBox = new HBox(12, cancelButton, importButton);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(15, titleLabel, fileLabel, badgesBox, policyLabel, table, actionsBox);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: #F6F0E7;");

        Scene scene = new Scene(layout, 920, 580);
        stage.setScene(scene);
        stage.showAndWait();

        return importedSuccessfully;
    }

    public int getImportedCount() { return importedCount; }
    public int getSkippedCount() { return skippedCount; }

    private Label createBadge(String text, String bgHex, String textHex) {
        Label lbl = new Label(text);
        lbl.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; "
            + "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 12px;",
            bgHex, textHex
        ));
        return lbl;
    }
}
