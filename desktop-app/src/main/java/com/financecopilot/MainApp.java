
package com.financecopilot;

import com.financecopilot.database.Database;
import com.financecopilot.models.Transaction;
import com.financecopilot.services.TransactionDAO;
import com.financecopilot.services.AnalysisService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainApp extends Application {

    private final TransactionDAO dao = new TransactionDAO();
    private final AnalysisService analysisService = new AnalysisService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final HttpClient httpClient = HttpClient.newHttpClient();

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

    private Label insightsStatus;
    private Label insightsContent;
    private Label anomalyContent;
    private Button insightsButton;

    private ComboBox<String> compareMonth1;
    private ComboBox<Integer> compareYear1;
    private ComboBox<String> compareMonth2;
    private ComboBox<Integer> compareYear2;
    private Label comparisonContent;
    private Label comparisonStatus;
    private Button compareButton;

    private static final String API_URL = "http://127.0.0.1:5000";

    private final String[] months = {
        "January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"
    };

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
            createCard("Balance", balanceLabel)
        );
        summary.setPadding(new Insets(15, 0, 15, 0));

        Label monthlyTitle = new Label("Monthly Financial Analysis");
        monthlyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        monthSelector = new ComboBox<>();
        monthSelector.getItems().addAll(months);

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
            new Label("Year:"), yearSelector
        );

        monthlyIncomeLabel = new Label("Income: ₹0.00");
        monthlyExpenseLabel = new Label("Expenses: ₹0.00");
        monthlyBalanceLabel = new Label("Balance: ₹0.00");

        HBox monthlySummary = new HBox(25,
            monthlyIncomeLabel, monthlyExpenseLabel, monthlyBalanceLabel
        );

        VBox monthlySection = new VBox(12,
            monthlyTitle,
            new Label("Select a month and year to view its financial summary"),
            monthControls,
            monthlySummary
        );
        styleSection(monthlySection);

        createCharts();

        HBox chartsSection = new HBox(15, monthlyBarChart, expensePieChart);
        HBox.setHgrow(monthlyBarChart, Priority.ALWAYS);
        HBox.setHgrow(expensePieChart, Priority.ALWAYS);

        monthSelector.setOnAction(e -> loadMonthlyAnalysis());
        yearSelector.setOnAction(e -> loadMonthlyAnalysis());

        VBox aiSection = createAIAnalysisSection();
        VBox insightsSection = createInsightsSection();
        VBox comparisonSection = createComparisonSection();

        TextField description = new TextField();
        description.setPromptText("Description");

        TextField category = new TextField();
        category.setPromptText("Category");

        ComboBox<String> type = new ComboBox<>(
            FXCollections.observableArrayList("INCOME", "EXPENSE")
        );
        type.setValue("EXPENSE");

        TextField amount = new TextField();
        amount.setPromptText("Amount");

        DatePicker date = new DatePicker(LocalDate.now());

        Button addButton = new Button("Add Transaction");
        addButton.setStyle(
            "-fx-background-color: #2457C5; -fx-text-fill: white;"
        );

        addButton.setOnAction(e -> {
            try {
                String desc = description.getText().trim();
                String cat = category.getText().trim();

                if (desc.isEmpty() || cat.isEmpty()
                    || amount.getText().trim().isEmpty()
                    || date.getValue() == null) {
                    showAlert("Please enter all transaction details.");
                    return;
                }

                double value = Double.parseDouble(amount.getText().trim());

                if (!Double.isFinite(value) || value <= 0) {
                    showAlert("Amount must be a positive number.");
                    return;
                }

                Transaction transaction = new Transaction(
                    0, desc, cat, type.getValue(),
                    value, date.getValue().toString()
                );

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
            new ColumnConstraints(150)
        );

        setupTable();

        searchField.setPromptText("Search description or category...");
        searchField.setPrefWidth(350);

        filterType.getItems().addAll("All", "Income", "Expenses");
        filterType.setValue("All");

        Button resetButton = new Button("Reset");
        HBox searchBar = new HBox(10, searchField, filterType, resetButton);

        searchField.textProperty().addListener(
            (obs, oldValue, newValue) -> filterTransactions()
        );
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

        VBox content = new VBox(15,
            heading, subtitle, summary,
            monthlySection, chartsSection,
            aiSection, insightsSection, comparisonSection,
            formTitle, form, historyTitle,
            searchBar, table, deleteButton
        );
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: #F5F7FB;");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background: #F5F7FB; -fx-background-color: #F5F7FB;"
        );

        stage.setTitle("Finance Copilot");
        stage.setScene(new Scene(scrollPane, 1100, 950));
        stage.setOnCloseRequest(e -> executor.shutdownNow());
        stage.show();

        loadTransactions();
    }

    private void styleSection(VBox section) {
        section.setPadding(new Insets(15));
        section.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10;"
        );
    }

    private VBox createAIAnalysisSection() {
        Label title = new Label("Python Financial Analysis");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        aiIncomeLabel = new Label("Total Income: —");
        aiExpenseLabel = new Label("Total Expenses: —");
        aiBalanceLabel = new Label("Balance: —");
        aiSavingsLabel = new Label("Savings Rate: —");
        aiCategoryLabel = new Label("Largest Expense Category: —");

        aiStatusLabel = new Label("Refresh to analyze transactions.");
        aiStatusLabel.setWrapText(true);

        analyzeButton = new Button("Refresh Analysis");
        analyzeButton.setOnAction(e -> refreshAIAnalysis());

        VBox section = new VBox(10,
            title, aiIncomeLabel, aiExpenseLabel, aiBalanceLabel,
            aiSavingsLabel, aiCategoryLabel,
            new HBox(12, analyzeButton, aiStatusLabel)
        );

        styleSection(section);
        return section;
    }

    private VBox createInsightsSection() {
        Label title = new Label("Spending Insights & Unusual Expenses");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        insightsContent = new Label("Click the button to load spending insights.");
        insightsContent.setWrapText(true);

        anomalyContent = new Label("Unusual expense results will appear here.");
        anomalyContent.setWrapText(true);

        insightsStatus = new Label("Python service status: not checked");

        insightsButton = new Button("Refresh Spending Insights");
        insightsButton.setOnAction(e -> refreshInsights());

        VBox section = new VBox(12,
            title, insightsContent,
            new Separator(),
            new Label("Unusual Expense Detection"),
            anomalyContent,
            new HBox(12, insightsButton, insightsStatus)
        );

        styleSection(section);
        return section;
    }

    private VBox createComparisonSection() {
        Label title = new Label("Month-to-Month Comparison");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        int currentYear = LocalDate.now().getYear();

        compareMonth1 = new ComboBox<>();
        compareMonth2 = new ComboBox<>();
        compareMonth1.getItems().addAll(months);
        compareMonth2.getItems().addAll(months);

        compareYear1 = new ComboBox<>();
        compareYear2 = new ComboBox<>();

        for (int year = currentYear - 5; year <= currentYear + 1; year++) {
            compareYear1.getItems().add(year);
            compareYear2.getItems().add(year);
        }

        LocalDate now = LocalDate.now();
        LocalDate previous = now.minusMonths(1);

        compareMonth1.setValue(previous.getMonth()
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        compareYear1.setValue(previous.getYear());

        compareMonth2.setValue(now.getMonth()
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        compareYear2.setValue(now.getYear());

        HBox firstRow = new HBox(10,
            new Label("First month:"), compareMonth1, compareYear1
        );

        HBox secondRow = new HBox(10,
            new Label("Second month:"), compareMonth2, compareYear2
        );

        comparisonContent = new Label("Select two months and compare.");
        comparisonContent.setWrapText(true);
        comparisonStatus = new Label();

        compareButton = new Button("Compare Months");
        compareButton.setOnAction(e -> compareMonths());

        VBox section = new VBox(12,
            title, firstRow, secondRow,
            compareButton, comparisonStatus, comparisonContent
        );

        styleSection(section);
        return section;
    }

    private JSONArray transactionArray(List<Transaction> transactions) {
        JSONArray array = new JSONArray();

        for (Transaction t : transactions) {
            JSONObject item = new JSONObject();
            item.put("description", t.getDescription());
            item.put("category", t.getCategory());
            item.put("type", t.getType());
            item.put("amount", t.getAmount());
            item.put("date", t.getDate());
            array.put(item);
        }

        return array;
    }

    private JSONObject transactionRequest(List<Transaction> transactions) {
        JSONObject body = new JSONObject();
        body.put("transactions", transactionArray(transactions));
        return body;
    }

    private JSONObject postJson(String endpoint, JSONObject body)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        JSONObject result;

        try {
            result = new JSONObject(response.body());
        } catch (Exception ex) {
            throw new Exception("Python service returned an invalid response.");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception(result.optString(
                "error", "Request failed: HTTP " + response.statusCode()
            ));
        }

        return result;
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
                        result.optDouble("total_income", 0)
                    ));
                    aiExpenseLabel.setText(String.format(
                        "Total Expenses: ₹%.2f",
                        result.optDouble("total_expenses", 0)
                    ));
                    aiBalanceLabel.setText(String.format(
                        "Balance: ₹%.2f",
                        result.optDouble("balance", 0)
                    ));
                    aiSavingsLabel.setText(String.format(
                        "Savings Rate: %.2f%%",
                        result.optDouble("savings_rate_percent", 0)
                    ));
                    aiCategoryLabel.setText(
                        "Largest Expense Category: "
                        + result.optString("largest_expense_category", "None")
                    );
                    aiStatusLabel.setText("Analysis updated successfully.");
                    analyzeButton.setDisable(false);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    aiStatusLabel.setText(
                        "Analysis failed. Check whether the Python server is running."
                    );
                    analyzeButton.setDisable(false);
                });
            }
        });
    }

    private void refreshInsights() {
        insightsButton.setDisable(true);
        insightsStatus.setText("Loading insights...");

        executor.submit(() -> {
            try {
                List<Transaction> transactions = dao.getAll();

                if (transactions.isEmpty()) {
                    Platform.runLater(() -> {
                        insightsContent.setText("Add transactions before analysis.");
                        anomalyContent.setText("No transactions available.");
                        insightsStatus.setText("No transactions");
                        insightsButton.setDisable(false);
                    });
                    return;
                }

                JSONObject body = transactionRequest(transactions);
                JSONObject insights = postJson("/insights", body);
                JSONObject anomalies = postJson(
                    "/detect-anomalies", transactionRequest(transactions)
                );

                StringBuilder insightText = new StringBuilder();

                insightText.append(String.format(
                    "Top spending category: %s (₹%.2f, %.2f%% of expenses)%n",
                    insights.optString("top_spending_category", "None"),
                    insights.optDouble("top_category_amount", 0),
                    insights.optDouble("top_category_share_percent", 0)
                ));

                insightText.append(String.format(
                    "Average expense: ₹%.2f%n",
                    insights.optDouble("average_expense", 0)
                ));

                insightText.append("Category spending alerts:\n");

                JSONArray alerts = insights.optJSONArray("alerts");

                if (alerts == null || alerts.length() == 0) {
                    insightText.append("No category alerts.");
                } else {
                    for (int i = 0; i < alerts.length(); i++) {
                        insightText.append("• ")
                            .append(alerts.getJSONObject(i).optString("message"))
                            .append("\n");
                    }
                }

                StringBuilder anomalyText = new StringBuilder();
                JSONArray found = anomalies.optJSONArray("anomalies");

                if (found == null || found.length() == 0) {
                    anomalyText.append(anomalies.optString(
                        "message", "No unusual expenses detected."
                    ));
                } else {
                    anomalyText.append("Detected ")
                        .append(found.length())
                        .append(" unusual expense(s):\n");

                    for (int i = 0; i < found.length(); i++) {
                        JSONObject item = found.getJSONObject(i);

                        anomalyText.append(String.format(
                            "• %s — %s: ₹%.2f (usual upper limit: ₹%.2f)%n",
                            item.optString("description", "Expense"),
                            item.optString("category", "Other"),
                            item.optDouble("amount", 0),
                            item.optDouble("usual_upper_limit", 0)
                        ));
                    }
                }

                String finalInsights = insightText.toString();
                String finalAnomalies = anomalyText.toString();

                Platform.runLater(() -> {
                    insightsContent.setText(finalInsights);
                    anomalyContent.setText(finalAnomalies);
                    insightsStatus.setText("Insights updated successfully.");
                    insightsButton.setDisable(false);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    insightsStatus.setText(
                        "Failed to connect or analyze: " + ex.getMessage()
                    );
                    insightsButton.setDisable(false);
                });
            }
        });
    }

    private void compareMonths() {
        if (compareMonth1.getValue() == null || compareYear1.getValue() == null
            || compareMonth2.getValue() == null || compareYear2.getValue() == null) {
            comparisonStatus.setText("Select both months and years.");
            return;
        }

        int firstMonthNumber = compareMonth1.getSelectionModel().getSelectedIndex() + 1;
        int secondMonthNumber = compareMonth2.getSelectionModel().getSelectedIndex() + 1;

        String first = String.format(
            "%04d-%02d", compareYear1.getValue(), firstMonthNumber
        );

        String second = String.format(
            "%04d-%02d", compareYear2.getValue(), secondMonthNumber
        );

        if (first.equals(second)) {
            comparisonStatus.setText("Choose two different months.");
            return;
        }

        compareButton.setDisable(true);
        comparisonStatus.setText("Comparing months...");

        executor.submit(() -> {
            try {
                List<Transaction> transactions = dao.getAll();
                JSONObject body = transactionRequest(transactions);
                body.put("month1", first);
                body.put("month2", second);

                JSONObject result = postJson("/compare", body);
                JSONObject firstMonth = result.getJSONObject("month1");
                JSONObject secondMonth = result.getJSONObject("month2");
                JSONObject changes = result.getJSONObject("changes");

                String output = String.format(
                    "%s: Income ₹%.2f | Expenses ₹%.2f | Balance ₹%.2f%n"
                    + "%s: Income ₹%.2f | Expenses ₹%.2f | Balance ₹%.2f%n%n"
                    + "Income change: %s%n"
                    + "Expense change: %s%n"
                    + "Balance change: %s%n"
                    + "Spending trend: %s",
                    first,
                    firstMonth.optDouble("income", 0),
                    firstMonth.optDouble("expenses", 0),
                    firstMonth.optDouble("balance", 0),
                    second,
                    secondMonth.optDouble("income", 0),
                    secondMonth.optDouble("expenses", 0),
                    secondMonth.optDouble("balance", 0),
                    formatChange(changes.getJSONObject("income")),
                    formatChange(changes.getJSONObject("expenses")),
                    formatChange(changes.getJSONObject("balance")),
                    result.optString("spending_trend", "unknown")
                );

                Platform.runLater(() -> {
                    comparisonContent.setText(output);
                    comparisonStatus.setText("Comparison completed.");
                    compareButton.setDisable(false);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    comparisonStatus.setText(
                        "Comparison failed: " + ex.getMessage()
                    );
                    compareButton.setDisable(false);
                });
            }
        });
    }

    private String formatChange(JSONObject change) {
        double amount = change.optDouble("amount", 0);
        Object percentage = change.opt("percentage");

        String percentText;

        if (percentage == null || percentage == JSONObject.NULL) {
            percentText = "N/A (previous value was zero)";
        } else if (percentage instanceof Number) {
            percentText = String.format(
                "%.2f%%", ((Number) percentage).doubleValue()
            );
        } else {
            percentText = "N/A";
        }

        return String.format("%+.2f (%s)", amount, percentText);
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
            "-fx-background-color: white; -fx-background-radius: 10;"
        );

        return card;
    }

    private void setupTable() {
        TableColumn<Transaction, String> descriptionCol =
            new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDescription()
            )
        );

        TableColumn<Transaction, String> categoryCol =
            new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getCategory()
            )
        );

        TableColumn<Transaction, String> typeCol =
            new TableColumn<>("Type");
        typeCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getType()
            )
        );

        TableColumn<Transaction, String> amountCol =
            new TableColumn<>("Amount");
        amountCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                String.format("₹%.2f", data.getValue().getAmount())
            )
        );

        TableColumn<Transaction, String> dateCol =
            new TableColumn<>("Date");
        dateCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDate()
            )
        );

        table.getColumns().addAll(
            descriptionCol, categoryCol, typeCol, amountCol, dateCol
        );

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(300);
    }

    private void filterTransactions() {
        String search = searchField.getText().trim().toLowerCase();
        String filter = filterType.getValue();

        List<Transaction> filtered = allTransactions.stream()
            .filter(t ->
                t.getDescription().toLowerCase().contains(search)
                || t.getCategory().toLowerCase().contains(search)
            )
            .filter(t ->
                "All".equals(filter)
                || ("Income".equals(filter) && t.getType().equals("INCOME"))
                || ("Expenses".equals(filter) && t.getType().equals("EXPENSE"))
            )
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
                if ("INCOME".equals(transaction.getType())) {
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

            if (monthNumber < 1 || monthNumber > 12) {
                return;
            }

            String month = String.format(
                "%04d-%02d", yearSelector.getValue(), monthNumber
            );

            List<Transaction> transactions = dao.getByMonth(month);

            double income = 0;
            double expenses = 0;
            Map<String, Double> categoryTotals = new TreeMap<>();

            for (Transaction transaction : transactions) {
                if ("INCOME".equals(transaction.getType())) {
                    income += transaction.getAmount();
                } else {
                    expenses += transaction.getAmount();
                    categoryTotals.merge(
                        transaction.getCategory(),
                        transaction.getAmount(),
                        Double::sum
                    );
                }
            }

            monthlyIncomeLabel.setText(
                String.format("Income: ₹%.2f", income)
            );
            monthlyExpenseLabel.setText(
                String.format("Expenses: ₹%.2f", expenses)
            );
            monthlyBalanceLabel.setText(
                String.format("Balance: ₹%.2f", income - expenses)
            );

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Amount");
            series.getData().add(new XYChart.Data<>("Income", income));
            series.getData().add(new XYChart.Data<>("Expenses", expenses));
            monthlyBarChart.getData().setAll(series);

            List<PieChart.Data> pieData = new ArrayList<>();

            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                pieData.add(
                    new PieChart.Data(entry.getKey(), entry.getValue())
                );
            }

            expensePieChart.setData(FXCollections.observableArrayList(pieData));

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
