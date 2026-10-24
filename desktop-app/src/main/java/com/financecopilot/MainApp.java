
package com.financecopilot;

import com.financecopilot.database.Database;
import com.financecopilot.models.Transaction;
import com.financecopilot.services.TransactionDAO;
import com.financecopilot.services.AnalysisService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import org.json.JSONObject;

public class MainApp extends Application {
    private final TransactionDAO dao = new TransactionDAO();
    private final AnalysisService analysisService = new AnalysisService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final TableView<Transaction> table = new TableView<>();
    private final Label incomeLabel = new Label("₹0.00");
    private final Label expenseLabel = new Label("₹0.00");
    private final Label balanceLabel = new Label("₹0.00");
    private final TextField searchField = new TextField();
    private final ComboBox<String> filterType = new ComboBox<>();
    private List<Transaction> allTransactions = new ArrayList<>();

    private Label monthlyIncomeLabel;
    private Label monthlyExpenseLabel;
    private Label monthlyBalanceLabel;
    private ComboBox<String> monthSelector;
    private ComboBox<Integer> yearSelector;
    private BarChart<String, Number> monthlyBarChart;
    private PieChart expensePieChart;

    private Label aiIncomeLabel;
    private Label aiExpenseLabel;
    private Label aiBalanceLabel;
    private Label aiSavingsLabel;
    private Label aiCategoryLabel;
    private Label aiStatusLabel;
    private Button analyzeButton;

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

        Label monthlyTitle = new Label("Monthly Financial Analysis");
        monthlyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        monthSelector = new ComboBox<>();
        monthSelector.getItems().addAll(
                "January", "February", "March", "April",
                "May", "June", "July", "August",
                "September", "October", "November", "December");

        yearSelector = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();

        for (int year = currentYear - 5; year <= currentYear + 1; year++) {
            yearSelector.getItems().add(year);
        }

        monthSelector.setValue(LocalDate.now().getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        yearSelector.setValue(currentYear);

        HBox monthControls = new HBox(10,
                new Label("Month:"), monthSelector,
                new Label("Year:"), yearSelector);

        monthlyIncomeLabel = new Label("Income: ₹0.00");
        monthlyExpenseLabel = new Label("Expenses: ₹0.00");
        monthlyBalanceLabel = new Label("Balance: ₹0.00");

        monthlyIncomeLabel.setStyle("-fx-font-weight: bold;");
        monthlyExpenseLabel.setStyle("-fx-font-weight: bold;");
        monthlyBalanceLabel.setStyle("-fx-font-weight: bold;");

        HBox monthlySummary = new HBox(25,
                monthlyIncomeLabel,
                monthlyExpenseLabel,
                monthlyBalanceLabel);

        VBox monthlySection = new VBox(12,
                monthlyTitle,
                new Label("Select a month and year to view its financial summary"),
                monthControls,
                monthlySummary);

        monthlySection.setPadding(new Insets(15));
        monthlySection.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;");

        createCharts();

        HBox chartsSection = new HBox(15, monthlyBarChart, expensePieChart);
        HBox.setHgrow(monthlyBarChart, Priority.ALWAYS);
        HBox.setHgrow(expensePieChart, Priority.ALWAYS);
        chartsSection.setPadding(new Insets(10, 0, 10, 0));

        monthSelector.setOnAction(e -> loadMonthlyAnalysis());
        yearSelector.setOnAction(e -> loadMonthlyAnalysis());

        VBox aiSection = createAIAnalysisSection();

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
        addButton.setStyle(
                "-fx-background-color: #2457C5; -fx-text-fill: white;");

        addButton.setOnAction(e -> {
            try {
                String desc = description.getText().trim();
                String cat = category.getText().trim();
                double value = Double.parseDouble(amount.getText().trim());

                if (desc.isEmpty() || cat.isEmpty()
                        || value <= 0 || date.getValue() == null) {
                    showAlert("Enter valid transaction details.");
                    return;
                }

                Transaction transaction = new Transaction(
                        0, desc, cat, type.getValue(),
                        value, date.getValue().toString());

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
        HBox searchBar = new HBox(10,
                searchField, filterType, resetButton);

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
                heading,
                subtitle,
                summary,
                monthlySection,
                chartsSection,
                aiSection,
                formTitle,
                form,
                historyTitle,
                searchBar,
                table,
                deleteButton);

        content.setPadding(new Insets(25));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background: #F5F7FB; -fx-background-color: #F5F7FB;");

        Scene scene = new Scene(scrollPane, 1100, 900);
        stage.setTitle("Finance Copilot");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> executor.shutdownNow());

        loadTransactions();
    }

    private VBox createAIAnalysisSection() {
        Label title = new Label("Python Financial Analysis");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        aiIncomeLabel = new Label("Total Income: —");
        aiExpenseLabel = new Label("Total Expenses: —");
        aiBalanceLabel = new Label("Balance: —");
        aiSavingsLabel = new Label("Savings Rate: —");
        aiCategoryLabel = new Label("Largest Expense Category: —");
        aiStatusLabel = new Label("Click Refresh Analysis to analyze your transactions.");

        aiStatusLabel.setWrapText(true);

        analyzeButton = new Button("Refresh Analysis");
        analyzeButton.setStyle(
                "-fx-background-color: #2457C5; -fx-text-fill: white;");
        analyzeButton.setOnAction(e -> refreshAIAnalysis());

        VBox section = new VBox(12,
                title,
                aiIncomeLabel,
                aiExpenseLabel,
                aiBalanceLabel,
                aiSavingsLabel,
                aiCategoryLabel,
                new HBox(12, analyzeButton, aiStatusLabel));

        section.setPadding(new Insets(15));
        section.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;");

        return section;
    }

    private void refreshAIAnalysis() {
        analyzeButton.setDisable(true);
        aiStatusLabel.setText("Analyzing transactions...");

        executor.submit(() -> {
            try {
                List<Transaction> transactions = dao.getAll();

                if (transactions.isEmpty()) {
                    Platform.runLater(() -> {
                        aiStatusLabel.setText("Add transactions before analyzing.");
                        analyzeButton.setDisable(false);
                    });
                    return;
                }

                String response = analysisService.analyzeTransactions(transactions);
                JSONObject result = new JSONObject(response);

                Platform.runLater(() -> {
                    aiIncomeLabel.setText(String.format(
                            "Total Income: ₹%.2f",
                            result.getDouble("total_income")));
                    aiExpenseLabel.setText(String.format(
                            "Total Expenses: ₹%.2f",
                            result.getDouble("total_expenses")));
                    aiBalanceLabel.setText(String.format(
                            "Balance: ₹%.2f",
                            result.getDouble("balance")));
                    aiSavingsLabel.setText(String.format(
                            "Savings Rate: %.2f%%",
                            result.getDouble("savings_rate_percent")));

                    Object category = result.opt("largest_expense_category");
                    aiCategoryLabel.setText(
                            "Largest Expense Category: "
                                    + (category == null || category == JSONObject.NULL
                                    ? "None" : category));

                    aiStatusLabel.setText("Analysis updated successfully.");
                    analyzeButton.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    aiStatusLabel.setText(
                            "Analysis failed. Is the Python server running?");
                    analyzeButton.setDisable(false);
                });
            }
        });
    }

    private void createCharts() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Transaction Type");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (₹)");
        yAxis.setForceZeroInRange(true);

        monthlyBarChart = new BarChart<>(xAxis, yAxis);
        monthlyBarChart.setTitle("Monthly Income vs Expenses");
        monthlyBarChart.setLegendVisible(false);
        monthlyBarChart.setAnimated(false);
        monthlyBarChart.setPrefHeight(320);
        monthlyBarChart.setMinWidth(300);

        expensePieChart = new PieChart();
        expensePieChart.setTitle("Expense Breakdown by Category");
        expensePieChart.setLegendVisible(true);
        expensePieChart.setLabelsVisible(true);
        expensePieChart.setAnimated(false);
        expensePieChart.setPrefHeight(320);
        expensePieChart.setMinWidth(300);
    }

    private VBox createCard(String title, Label value) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
        value.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        VBox card = new VBox(10, heading, value);
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;");

        return card;
    }

    private void setupTable() {
        TableColumn<Transaction, String> descriptionCol =
                new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDescription()));

        TableColumn<Transaction, String> categoryCol =
                new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCategory()));

        TableColumn<Transaction, String> typeCol =
                new TableColumn<>("Type");
        typeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getType()));

        TableColumn<Transaction, String> amountCol =
                new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("₹%.2f", data.getValue().getAmount())));

        TableColumn<Transaction, String> dateCol =
                new TableColumn<>("Date");
        dateCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDate()));

        table.getColumns().addAll(
                descriptionCol, categoryCol, typeCol, amountCol, dateCol);

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(300);
    }

    private void filterTransactions() {
        String search = searchField.getText().trim().toLowerCase();
        String filter = filterType.getValue();

        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> t.getDescription().toLowerCase().contains(search)
                        || t.getCategory().toLowerCase().contains(search))
                .filter(t -> "All".equals(filter)
                        || ("Income".equals(filter)
                        && t.getType().equals("INCOME"))
                        || ("Expenses".equals(filter)
                        && t.getType().equals("EXPENSE")))
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
                    showAlert(
                            "Could not delete transaction: " + ex.getMessage());
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

            loadMonthlyAnalysis();
        } catch (Exception ex) {
            showAlert("Could not load transactions: " + ex.getMessage());
        }
    }

    private void loadMonthlyAnalysis() {
        try {
            if (monthSelector == null || yearSelector == null
                    || monthSelector.getValue() == null
                    || yearSelector.getValue() == null) {
                return;
            }

            int monthNumber =
                    monthSelector.getSelectionModel().getSelectedIndex() + 1;

            String month = String.format("%04d-%02d",
                    yearSelector.getValue(), monthNumber);

            List<Transaction> transactions = dao.getByMonth(month);

            double income = 0;
            double expenses = 0;
            Map<String, Double> categoryTotals = new TreeMap<>();

            for (Transaction transaction : transactions) {
                if (transaction.getType().equals("INCOME")) {
                    income += transaction.getAmount();
                } else {
                    expenses += transaction.getAmount();

                    categoryTotals.merge(
                            transaction.getCategory(),
                            transaction.getAmount(),
                            Double::sum);
                }
            }

            monthlyIncomeLabel.setText(
                    String.format("Income: ₹%.2f", income));
            monthlyExpenseLabel.setText(
                    String.format("Expenses: ₹%.2f", expenses));
            monthlyBalanceLabel.setText(
                    String.format("Balance: ₹%.2f", income - expenses));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Amount");

            series.getData().add(
                    new XYChart.Data<>("Income", income));
            series.getData().add(
                    new XYChart.Data<>("Expenses", expenses));

            monthlyBarChart.getData().setAll(series);

            List<PieChart.Data> pieData = new ArrayList<>();

            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                pieData.add(new PieChart.Data(
                        entry.getKey(),
                        entry.getValue()));
            }

            expensePieChart.setData(
                    FXCollections.observableArrayList(pieData));

        } catch (Exception ex) {
            showAlert("Could not load monthly analysis: " + ex.getMessage());
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
