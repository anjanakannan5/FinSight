
package com.financecopilot;

import com.financecopilot.database.Database;
import com.financecopilot.models.Transaction;
import com.financecopilot.services.TransactionDAO;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {
    private final TransactionDAO dao = new TransactionDAO();
    private final TableView<Transaction> table = new TableView<>();
    private final Label incomeLabel = new Label("₹0.00");
    private final Label expenseLabel = new Label("₹0.00");
    private final Label balanceLabel = new Label("₹0.00");
    private final TextField searchField = new TextField();
    private final ComboBox<String> filterType = new ComboBox<>();
    private List<Transaction> allTransactions = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        Database.initialize();

        Label heading = new Label("Finance Copilot");
        heading.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitle = new Label("Your personal finance dashboard");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        HBox summary = new HBox(15,
                createCard("Total Income", incomeLabel),
                createCard("Total Expenses", expenseLabel),
                createCard("Balance", balanceLabel));
        summary.setPadding(new Insets(15, 0, 15, 0));

        TextField description = new TextField();
        description.setPromptText("Description");

        TextField category = new TextField();
        category.setPromptText("Category");

        ComboBox<String> type = new ComboBox<>(
                FXCollections.observableArrayList("INCOME", "EXPENSE"));
        type.setValue("EXPENSE");

        TextField amount = new TextField();
        amount.setPromptText("Amount");

        DatePicker date = new DatePicker(LocalDate.now());

        Button addButton = new Button("Add Transaction");
        addButton.setStyle("-fx-background-color: #2457C5; -fx-text-fill: white;");

        addButton.setOnAction(e -> {
            try {
                String desc = description.getText().trim();
                String cat = category.getText().trim();
                double value = Double.parseDouble(amount.getText().trim());

                if (desc.isEmpty() || cat.isEmpty() || value <= 0 || date.getValue() == null) {
                    showAlert("Enter valid transaction details.");
                    return;
                }

                Transaction transaction = new Transaction(
                        0, desc, cat, type.getValue(), value, date.getValue().toString());

                dao.add(transaction);

                description.clear();
                category.clear();
                amount.clear();
                type.setValue("EXPENSE");
                date.setValue(LocalDate.now());

                loadTransactions();
            } catch (NumberFormatException ex) {
                showAlert("Amount must be a valid number.");
            } catch (Exception ex) {
                showAlert("Could not add transaction: " + ex.getMessage());
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(description, 0, 0);
        form.add(category, 1, 0);
        form.add(type, 2, 0);
        form.add(amount, 0, 1);
        form.add(date, 1, 1);
        form.add(addButton, 2, 1);

        form.getColumnConstraints().addAll(
                new ColumnConstraints(150),
                new ColumnConstraints(150),
                new ColumnConstraints(150));

        setupTable();

        searchField.setPromptText("Search description or category...");
        searchField.setPrefWidth(350);

        filterType.getItems().addAll("All", "Income", "Expenses");
        filterType.setValue("All");

        Button resetButton = new Button("Reset");
        HBox searchBar = new HBox(10, searchField, filterType, resetButton);
        searchBar.setPadding(new Insets(5, 0, 10, 0));

        searchField.textProperty().addListener(
                (obs, oldValue, newValue) -> filterTransactions());
        filterType.setOnAction(e -> filterTransactions());

        resetButton.setOnAction(e -> {
            searchField.clear();
            filterType.setValue("All");
            filterTransactions();
        });

        Button deleteButton = new Button("Delete Selected Transaction");
        deleteButton.setOnAction(e -> deleteTransaction());

        Label formTitle = new Label("Add a Transaction");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label historyTitle = new Label("Transaction History");
        historyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox content = new VBox(10,
                heading, subtitle, summary,
                formTitle, form, historyTitle, searchBar, table, deleteButton);
        content.setPadding(new Insets(25));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F5F7FB; -fx-background-color: #F5F7FB;");

        Scene scene = new Scene(scrollPane, 1050, 700);
        stage.setTitle("Finance Copilot");
        stage.setScene(scene);
        stage.show();

        loadTransactions();
    }

    private VBox createCard(String title, Label value) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
        value.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        VBox card = new VBox(10, heading, value);
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        return card;
    }

    private void setupTable() {
        TableColumn<Transaction, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDescription()));

        TableColumn<Transaction, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCategory()));

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getType()));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("₹%.2f", data.getValue().getAmount())));

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDate()));

        table.getColumns().addAll(
                descriptionCol, categoryCol, typeCol, amountCol, dateCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(300);
    }

    private void filterTransactions() {
        String search = searchField.getText().trim().toLowerCase();
        String filter = filterType.getValue();

        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> t.getDescription().toLowerCase().contains(search)
                        || t.getCategory().toLowerCase().contains(search))
                .filter(t -> "All".equals(filter)
                        || ("Income".equals(filter) && t.getType().equals("INCOME"))
                        || ("Expenses".equals(filter) && t.getType().equals("EXPENSE")))
                .toList();

        table.setItems(FXCollections.observableArrayList(filtered));
    }

    private void deleteTransaction() {
        Transaction selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Please select a transaction to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Transaction");
        confirm.setHeaderText("Delete " + selected.getDescription() + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    dao.delete(selected.getId());
                    loadTransactions();
                } catch (Exception ex) {
                    showAlert("Could not delete transaction: " + ex.getMessage());
                }
            }
        });
    }

    private void loadTransactions() {
        try {
            allTransactions = dao.getAll();
            filterTransactions();

            double income = 0;
            double expenses = 0;

            for (Transaction transaction : allTransactions) {
                if (transaction.getType().equals("INCOME")) {
                    income += transaction.getAmount();
                } else {
                    expenses += transaction.getAmount();
                }
            }

            incomeLabel.setText(String.format("₹%.2f", income));
            expenseLabel.setText(String.format("₹%.2f", expenses));
            balanceLabel.setText(String.format("₹%.2f", income - expenses));
        } catch (Exception ex) {
            showAlert("Could not load transactions: " + ex.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Finance Copilot");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
