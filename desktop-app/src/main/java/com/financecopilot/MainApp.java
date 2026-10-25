package com.financecopilot;

import com.financecopilot.database.Database;
import com.financecopilot.models.SavingsGoal;
import com.financecopilot.models.Transaction;
import com.financecopilot.services.AnalysisService;
import com.financecopilot.services.SavingsGoalDAO;
import com.financecopilot.services.TransactionDAO;
import com.financecopilot.services.BudgetDAO;
import com.financecopilot.services.RecurringTransactionDAO;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

import org.json.JSONArray;
import org.json.JSONObject;

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
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.util.HashMap;

public class MainApp extends Application {

    private final TransactionDAO dao = new TransactionDAO();
    private final AnalysisService analysisService = new AnalysisService();
    private final SavingsGoalDAO savingsGoalDAO = new SavingsGoalDAO();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String API_URL = "http://127.0.0.1:5000";

    private final String[] months = {
        "January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"
    };

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

    private Label totalGoalsLabel;
    private Label totalTargetLabel;
    private Label totalSavedLabel;
    private Label overallProgressLabel;
    private ProgressBar overallProgressBar;

    private ListView<SavingsGoal> goalsList;
    private TextField goalNameField;
    private TextField goalTargetField;
    private TextField contributionField;
    private DatePicker goalDatePicker;
    private Label goalsStatus;
    private Button addGoalButton;
    private Button contributeButton;
    private Button deleteGoalButton;

    private final BudgetDAO budgetDAO = new BudgetDAO();
    private TableView<BudgetDAO.BudgetSummary> budgetTable;
    private TextField budgetCategoryField;
    private TextField budgetLimitField;
    private ComboBox<String> budgetMonthSelector;
    private Label budgetStatus;

    private final RecurringTransactionDAO recurringDAO = new RecurringTransactionDAO();
    private TableView<RecurringTransactionDAO.Rule> recurringTable;
    private TextField recurringDescription, recurringCategory, recurringAmount;
    private ComboBox<String> recurringType;
    private Spinner<Integer> recurringDay;
    private Label recurringStatus;

    private Label healthScoreLabel;
    private Label healthRecommendationLabel;
    private Button healthScoreButton;

    @Override
    public void start(Stage stage) {
        Database.initialize();

        Label heading = new Label("Finance Copilot");
        heading.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitle = new Label("Your personal finance dashboard");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        HBox summary = new HBox(
            15,
            createCard("Total Income", incomeLabel),
            createCard("Total Expenses", expenseLabel),
            createCard("Balance", balanceLabel)
        );
        summary.setPadding(new Insets(15, 0, 15, 0));

        monthSelector = new ComboBox<>();
        monthSelector.getItems().addAll(months);

        yearSelector = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();

        for (int year = currentYear - 5; year <= currentYear + 1; year++) {
            yearSelector.getItems().add(year);
        }

        monthSelector.setValue(
            LocalDate.now().getMonth().getDisplayName(
                TextStyle.FULL, Locale.ENGLISH
            )
        );
        yearSelector.setValue(currentYear);

        HBox monthControls = new HBox(
            10,
            new Label("Month:"), monthSelector,
            new Label("Year:"), yearSelector
        );

        monthlyIncomeLabel = new Label("Income: ₹0.00");
        monthlyExpenseLabel = new Label("Expenses: ₹0.00");
        monthlyBalanceLabel = new Label("Balance: ₹0.00");

        HBox monthlySummary = new HBox(
            25,
            monthlyIncomeLabel,
            monthlyExpenseLabel,
            monthlyBalanceLabel
        );

        VBox monthlySection = new VBox(
            12,
            sectionTitle("Monthly Financial Analysis"),
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
        VBox savingsGoalsSection = createSavingsGoalsSection();
        VBox budgetSection = createBudgetSection();

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
                String amountText = amount.getText().trim();

                if (desc.isEmpty() || cat.isEmpty()
                    || amountText.isEmpty() || date.getValue() == null) {
                    showAlert("Please enter all transaction details.");
                    return;
                }

                double value = Double.parseDouble(amountText);

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

        Button exportButton = new Button("Export Transactions to CSV");
        exportButton.setOnAction(e -> exportTransactions());

        VBox recurringSection = createRecurringSection();

        Button pdfReportButton = new Button("Export Monthly PDF Report");
        pdfReportButton.setOnAction(e -> exportMonthlyReport());

        HBox reportControls = new HBox(10, pdfReportButton);

        VBox financialHealthSection = createFinancialHealthSection();

        VBox content = new VBox(
            15,
            heading,
            subtitle,
            summary,
            monthlySection,
            chartsSection,
            reportControls,
            aiSection,
            financialHealthSection,
            insightsSection,
            comparisonSection,
            savingsGoalsSection,
            budgetSection,
            recurringSection,
            sectionTitle("Add a Transaction"),
            form,
            sectionTitle("Transaction History"),
            searchBar,
            table,
            new HBox(10, deleteButton, exportButton)
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

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        return label;
    }

    private void styleSection(VBox section) {
        section.setPadding(new Insets(15));
        section.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10;"
        );
    }

    private VBox createAIAnalysisSection() {
        aiIncomeLabel = new Label("Total Income: —");
        aiExpenseLabel = new Label("Total Expenses: —");
        aiBalanceLabel = new Label("Balance: —");
        aiSavingsLabel = new Label("Savings Rate: —");
        aiCategoryLabel = new Label("Largest Expense Category: —");

        aiStatusLabel = new Label("Refresh to analyze transactions.");
        aiStatusLabel.setWrapText(true);

        analyzeButton = new Button("Refresh Analysis");
        analyzeButton.setOnAction(e -> refreshAIAnalysis());

        VBox section = new VBox(
            10,
            sectionTitle("Python Financial Analysis"),
            aiIncomeLabel,
            aiExpenseLabel,
            aiBalanceLabel,
            aiSavingsLabel,
            aiCategoryLabel,
            new HBox(12, analyzeButton, aiStatusLabel)
        );

        styleSection(section);
        return section;
    }

    private VBox createInsightsSection() {
        insightsContent = new Label(
            "Click the button to load spending insights."
        );
        insightsContent.setWrapText(true);

        anomalyContent = new Label(
            "Unusual expense results will appear here."
        );
        anomalyContent.setWrapText(true);

        insightsStatus = new Label("Python service status: not checked");

        insightsButton = new Button("Refresh Spending Insights");
        insightsButton.setOnAction(e -> refreshInsights());

        VBox section = new VBox(
            12,
            sectionTitle("Spending Insights & Unusual Expenses"),
            insightsButton,
            insightsStatus,
            new Label("Spending insights"),
            insightsContent,
            new Separator(),
            new Label("Unusual expense detection"),
            anomalyContent
        );

        styleSection(section);
        return section;
    }

    private VBox createComparisonSection() {
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

        compareMonth1.setValue(
            previous.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        );
        compareYear1.setValue(previous.getYear());

        compareMonth2.setValue(
            now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        );
        compareYear2.setValue(now.getYear());

        HBox firstRow = new HBox(
            10, new Label("First month:"), compareMonth1, compareYear1
        );
        HBox secondRow = new HBox(
            10, new Label("Second month:"), compareMonth2, compareYear2
        );

        comparisonContent = new Label("Select two months and compare.");
        comparisonContent.setWrapText(true);
        comparisonStatus = new Label();

        compareButton = new Button("Compare Months");
        compareButton.setOnAction(e -> compareMonths());

        VBox section = new VBox(
            12,
            sectionTitle("Month-to-Month Comparison"),
            firstRow,
            secondRow,
            compareButton,
            comparisonStatus,
            comparisonContent
        );

        styleSection(section);
        return section;
    }

    private VBox createSavingsGoalsSection() {
        totalGoalsLabel = new Label("Total Goals: 0");
        totalTargetLabel = new Label("Total Target: ₹0.00");
        totalSavedLabel = new Label("Total Saved: ₹0.00");

        overallProgressLabel = new Label("Overall Progress: 0.0%");
        overallProgressBar = new ProgressBar(0);
        overallProgressBar.setMaxWidth(Double.MAX_VALUE);

        HBox goalSummary = new HBox(
            20, totalGoalsLabel, totalTargetLabel, totalSavedLabel
        );

        goalNameField = new TextField();
        goalNameField.setPromptText("Goal name (e.g. Laptop)");

        goalTargetField = new TextField();
        goalTargetField.setPromptText("Target amount (₹)");

        goalDatePicker = new DatePicker(LocalDate.now().plusMonths(6));

        addGoalButton = new Button("Create Goal");
        addGoalButton.setStyle(
            "-fx-background-color: #2457C5; -fx-text-fill: white;"
        );
        addGoalButton.setOnAction(e -> addSavingsGoal());

        GridPane goalForm = new GridPane();
        goalForm.setHgap(10);
        goalForm.setVgap(10);

        goalForm.add(goalNameField, 0, 0);
        goalForm.add(goalTargetField, 1, 0);
        goalForm.add(goalDatePicker, 2, 0);
        goalForm.add(addGoalButton, 0, 1);

        goalForm.getColumnConstraints().addAll(
            new ColumnConstraints(180),
            new ColumnConstraints(160),
            new ColumnConstraints(160)
        );

        goalsList = new ListView<>();
        goalsList.setPrefHeight(230);

        goalsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SavingsGoal goal, boolean empty) {
                super.updateItem(goal, empty);

                if (empty || goal == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label name = new Label(goal.getName());
                name.setStyle(
                    "-fx-font-size: 15px; -fx-font-weight: bold;"
                );

                Label amounts = new Label(String.format(
                    "Saved: ₹%.2f / ₹%.2f    Remaining: ₹%.2f",
                    goal.getSavedAmount(),
                    goal.getTargetAmount(),
                    goal.getRemainingAmount()
                ));

                ProgressBar progress = new ProgressBar(goal.getProgress());
                progress.setMaxWidth(Double.MAX_VALUE);

                Label percentage = new Label(String.format(
                    "%.1f%% complete", goal.getProgress() * 100
                ));

                Label deadline = new Label(
                    "Target date: " + goal.getTargetDate()
                    + (goal.isCompleted() ? "  |  Completed" : "")
                );

                VBox details = new VBox(
                    5, name, amounts, progress, percentage, deadline
                );
                details.setPadding(new Insets(8));
                details.setStyle(
                    "-fx-background-color: #F5F7FB; -fx-background-radius: 8;"
                );

                setGraphic(details);
            }
        });

        contributionField = new TextField();
        contributionField.setPromptText("Contribution amount (₹)");
        contributionField.setPrefWidth(200);

        contributeButton = new Button("Add Savings");
        contributeButton.setOnAction(e -> addSavingsContribution());

        deleteGoalButton = new Button("Delete Selected Goal");
        deleteGoalButton.setOnAction(e -> deleteSavingsGoal());

        goalsStatus = new Label(
            "Create a goal to start tracking your savings."
        );
        goalsStatus.setWrapText(true);

        HBox contributionRow = new HBox(
            10, contributionField, contributeButton, deleteGoalButton
        );

        VBox section = new VBox(
            12,
            sectionTitle("Financial Goals & Savings Tracker"),
            goalSummary,
            overallProgressLabel,
            overallProgressBar,
            new Label("Create a savings goal"),
            goalForm,
            new Separator(),
            new Label("Your savings goals"),
            goalsList,
            contributionRow,
            goalsStatus
        );

        styleSection(section);
        reloadSavingsGoals();
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
                        aiStatusLabel.setText(
                            "Add transactions before analyzing."
                        );
                        analyzeButton.setDisable(false);
                    });
                    return;
                }

                JSONObject result = new JSONObject(
                    analysisService.analyzeTransactions(transactions)
                );

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
                        insightsContent.setText(
                            "Add transactions before analysis."
                        );
                        anomalyContent.setText("No transactions available.");
                        insightsStatus.setText("No transactions");
                        insightsButton.setDisable(false);
                    });
                    return;
                }

                JSONObject requestBody = transactionRequest(transactions);
                JSONObject insights = postJson("/insights", requestBody);
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
        if (compareMonth1.getValue() == null
            || compareYear1.getValue() == null
            || compareMonth2.getValue() == null
            || compareYear2.getValue() == null) {
            comparisonStatus.setText("Select both months and years.");
            return;
        }

        String first = String.format(
            "%04d-%02d",
            compareYear1.getValue(),
            compareMonth1.getSelectionModel().getSelectedIndex() + 1
        );

        String second = String.format(
            "%04d-%02d",
            compareYear2.getValue(),
            compareMonth2.getSelectionModel().getSelectedIndex() + 1
        );

        if (first.equals(second)) {
            comparisonStatus.setText("Choose two different months.");
            return;
        }

        compareButton.setDisable(true);
        comparisonStatus.setText("Comparing months...");

        executor.submit(() -> {
            try {
                JSONObject body = transactionRequest(dao.getAll());
                body.put("month1", first);
                body.put("month2", second);

                JSONObject result = postJson("/compare", body);
                JSONObject m1 = result.getJSONObject("month1");
                JSONObject m2 = result.getJSONObject("month2");
                JSONObject changes = result.getJSONObject("changes");

                String output = String.format(
                    "%s: Income ₹%.2f | Expenses ₹%.2f | Balance ₹%.2f%n"
                    + "%s: Income ₹%.2f | Expenses ₹%.2f | Balance ₹%.2f%n%n"
                    + "Income change: %s%n"
                    + "Expense change: %s%n"
                    + "Balance change: %s%n"
                    + "Spending trend: %s",
                    first,
                    m1.optDouble("income", 0),
                    m1.optDouble("expenses", 0),
                    m1.optDouble("balance", 0),
                    second,
                    m2.optDouble("income", 0),
                    m2.optDouble("expenses", 0),
                    m2.optDouble("balance", 0),
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

        table.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY
        );
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
                || ("Income".equals(filter)
                    && t.getType().equals("INCOME"))
                || ("Expenses".equals(filter)
                    && t.getType().equals("EXPENSE"))
            )
            .toList();

        table.setItems(FXCollections.observableArrayList(filtered));
    }

    private void deleteTransaction() {
        Transaction selected =
            table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Please select a transaction to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Transaction");
        confirm.setHeaderText(
            "Delete " + selected.getDescription() + "?"
        );
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    dao.delete(selected.getId());
                    loadTransactions();
                } catch (Exception ex) {
                    showAlert(
                        "Could not delete transaction: " + ex.getMessage()
                    );
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

            expensePieChart.setData(
                FXCollections.observableArrayList(pieData)
            );

        } catch (Exception ex) {
            showAlert("Could not load monthly analysis: " + ex.getMessage());
        }
    }

    private void addSavingsGoal() {
        try {
            String name = goalNameField.getText().trim();
            String targetText = goalTargetField.getText().trim();
            LocalDate targetDate = goalDatePicker.getValue();

            if (name.isEmpty() || targetText.isEmpty() || targetDate == null) {
                goalsStatus.setText(
                    "Enter the goal name, target amount and date."
                );
                return;
            }

            double target = Double.parseDouble(targetText);

            if (!Double.isFinite(target) || target <= 0) {
                goalsStatus.setText(
                    "Target amount must be a positive number."
                );
                return;
            }

            if (targetDate.isBefore(LocalDate.now())) {
                goalsStatus.setText("Target date cannot be in the past.");
                return;
            }

            addGoalButton.setDisable(true);
            savingsGoalDAO.addGoal(name, target, targetDate.toString());

            goalNameField.clear();
            goalTargetField.clear();
            goalDatePicker.setValue(LocalDate.now().plusMonths(6));

            reloadSavingsGoals();
            goalsStatus.setText("Savings goal created successfully.");

        } catch (NumberFormatException ex) {
            goalsStatus.setText("Enter a valid target amount.");
        } catch (Exception ex) {
            goalsStatus.setText(
                "Could not create goal: " + ex.getMessage()
            );
        } finally {
            addGoalButton.setDisable(false);
        }
    }

    private void addSavingsContribution() {
        SavingsGoal selected =
            goalsList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            goalsStatus.setText("Select a savings goal first.");
            return;
        }

        try {
            String text = contributionField.getText().trim();

            if (text.isEmpty()) {
                goalsStatus.setText("Enter a contribution amount.");
                return;
            }

            double amount = Double.parseDouble(text);

            if (!Double.isFinite(amount) || amount <= 0) {
                goalsStatus.setText(
                    "Contribution must be a positive number."
                );
                return;
            }

            savingsGoalDAO.addSavings(selected.getId(), amount);
            contributionField.clear();
            reloadSavingsGoals();

            goalsStatus.setText(
                "₹" + String.format("%.2f", amount)
                + " added to " + selected.getName() + "."
            );

        } catch (NumberFormatException ex) {
            goalsStatus.setText("Enter a valid contribution amount.");
        } catch (Exception ex) {
            goalsStatus.setText(
                "Could not add savings: " + ex.getMessage()
            );
        }
    }

    private void deleteSavingsGoal() {
        SavingsGoal selected =
            goalsList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            goalsStatus.setText("Select a goal to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Savings Goal");
        confirm.setHeaderText("Delete " + selected.getName() + "?");
        confirm.setContentText("This will permanently remove this goal.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    savingsGoalDAO.deleteGoal(selected.getId());
                    reloadSavingsGoals();
                    goalsStatus.setText("Savings goal deleted.");
                } catch (Exception ex) {
                    goalsStatus.setText(
                        "Could not delete goal: " + ex.getMessage()
                    );
                }
            }
        });
    }

    private void reloadSavingsGoals() {
        if (goalsList == null) {
            return;
        }

        try {
            List<SavingsGoal> goals = savingsGoalDAO.getAllGoals();
            goalsList.setItems(FXCollections.observableArrayList(goals));

            double totalTarget = 0;
            double totalSaved = 0;

            for (SavingsGoal goal : goals) {
                totalTarget += goal.getTargetAmount();
                totalSaved += goal.getSavedAmount();
            }

            double progress = totalTarget > 0
                ? Math.min(1.0, totalSaved / totalTarget)
                : 0;

            totalGoalsLabel.setText("Total Goals: " + goals.size());
            totalTargetLabel.setText(String.format(
                "Total Target: ₹%.2f", totalTarget
            ));
            totalSavedLabel.setText(String.format(
                "Total Saved: ₹%.2f", totalSaved
            ));

            overallProgressLabel.setText(String.format(
                "Overall Progress: %.1f%%", progress * 100
            ));
            overallProgressBar.setProgress(progress);

            if (goals.isEmpty()) {
                goalsStatus.setText(
                    "No goals yet. Create your first savings goal."
                );
            }

        } catch (Exception ex) {
            goalsStatus.setText(
                "Could not load goals: " + ex.getMessage()
            );
        }
    }

    
    private VBox createBudgetSection() {
        budgetCategoryField = new TextField();
        budgetCategoryField.setPromptText("Category (e.g. Food)");

        budgetLimitField = new TextField();
        budgetLimitField.setPromptText("Monthly limit (₹)");

        budgetMonthSelector = new ComboBox<>();
        budgetMonthSelector.getItems().addAll(months);
        budgetMonthSelector.setValue(
            LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));

        ComboBox<Integer> budgetYearSelector = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 5; y <= currentYear + 1; y++)
            budgetYearSelector.getItems().add(y);
        budgetYearSelector.setValue(currentYear);

        budgetTable = new TableView<>();
        budgetTable.setPrefHeight(250);

        TableColumn<BudgetDAO.BudgetSummary, String> categoryCol =
            new TableColumn<>("Category");
        categoryCol.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getCategory()));

        TableColumn<BudgetDAO.BudgetSummary, String> limitCol =
            new TableColumn<>("Budget");
        limitCol.setCellValueFactory(d ->
            new SimpleStringProperty(String.format("₹%.2f", d.getValue().getLimit())));

        TableColumn<BudgetDAO.BudgetSummary, String> spentCol =
            new TableColumn<>("Spent");
        spentCol.setCellValueFactory(d ->
            new SimpleStringProperty(String.format("₹%.2f", d.getValue().getSpent())));

        TableColumn<BudgetDAO.BudgetSummary, String> remainingCol =
            new TableColumn<>("Remaining");
        remainingCol.setCellValueFactory(d ->
            new SimpleStringProperty(String.format("₹%.2f", d.getValue().getRemaining())));

        TableColumn<BudgetDAO.BudgetSummary, String> progressCol =
            new TableColumn<>("Progress");
        progressCol.setCellValueFactory(d ->
            new SimpleStringProperty(String.format("%.1f%%",
                d.getValue().getProgress() * 100)));

        TableColumn<BudgetDAO.BudgetSummary, String> statusCol =
            new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().getStatus()));

        budgetTable.getColumns().addAll(categoryCol, limitCol, spentCol,
            remainingCol, progressCol, statusCol);
        budgetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        budgetStatus = new Label("Set a monthly spending limit for each category.");
        budgetStatus.setWrapText(true);

        Button saveBudgetButton = new Button("Save Budget");
        saveBudgetButton.setOnAction(e -> {
            try {
                String category = budgetCategoryField.getText().trim();
                if (category.isEmpty() || budgetLimitField.getText().trim().isEmpty()) {
                    budgetStatus.setText("Enter a category and budget amount.");
                    return;
                }

                double limit = Double.parseDouble(budgetLimitField.getText().trim());
                if (!Double.isFinite(limit) || limit <= 0) {
                    budgetStatus.setText("Budget must be a positive number.");
                    return;
                }

                String month = String.format("%04d-%02d",
                    budgetYearSelector.getValue(),
                    budgetMonthSelector.getSelectionModel().getSelectedIndex() + 1);

                budgetDAO.saveBudget(category, month, limit);
                budgetCategoryField.clear();
                budgetLimitField.clear();
                loadBudgets(budgetYearSelector.getValue(),
                    budgetMonthSelector.getSelectionModel().getSelectedIndex() + 1);
                budgetStatus.setText("Budget saved successfully.");
            } catch (NumberFormatException ex) {
                budgetStatus.setText("Enter a valid budget amount.");
            } catch (Exception ex) {
                budgetStatus.setText("Could not save budget: " + ex.getMessage());
            }
        });

        Button deleteBudgetButton = new Button("Delete Selected Budget");
        deleteBudgetButton.setOnAction(e -> {
            BudgetDAO.BudgetSummary selected =
                budgetTable.getSelectionModel().getSelectedItem();

            if (selected == null) {
                budgetStatus.setText("Select a budget to delete.");
                return;
            }

            try {
                budgetDAO.deleteBudget(selected.getId());
                loadBudgets(budgetYearSelector.getValue(),
                    budgetMonthSelector.getSelectionModel().getSelectedIndex() + 1);
                budgetStatus.setText("Budget deleted.");
            } catch (Exception ex) {
                budgetStatus.setText("Could not delete budget: " + ex.getMessage());
            }
        });

        Runnable reload = () -> loadBudgets(
            budgetYearSelector.getValue(),
            budgetMonthSelector.getSelectionModel().getSelectedIndex() + 1);

        budgetMonthSelector.setOnAction(e -> reload.run());
        budgetYearSelector.setOnAction(e -> reload.run());

        HBox form = new HBox(10, budgetCategoryField, budgetLimitField,
            budgetMonthSelector, budgetYearSelector, saveBudgetButton);

        VBox section = new VBox(12,
            sectionTitle("Budget Planning & Alerts"),
            new Label("Set a monthly spending limit for each expense category."),
            form, budgetTable, deleteBudgetButton, budgetStatus);

        styleSection(section);
        loadBudgets(currentYear, LocalDate.now().getMonthValue());
        return section;
    }

    private void loadBudgets(int year, int month) {
        try {
            String selectedMonth = String.format("%04d-%02d", year, month);
            List<BudgetDAO.BudgetSummary> budgets =
                budgetDAO.getBudgets(selectedMonth);

            budgetTable.setItems(FXCollections.observableArrayList(budgets));

            long over = budgets.stream()
                .filter(b -> b.getSpent() > b.getLimit()).count();
            long near = budgets.stream()
                .filter(b -> b.getSpent() >= b.getLimit() * 0.8
                    && b.getSpent() <= b.getLimit()).count();

            budgetStatus.setText("Budgets: " + budgets.size()
                + " | Near limit: " + near
                + " | Over budget: " + over
                + "\nA warning is shown at 80% spending or above.");
        } catch (Exception ex) {
            budgetStatus.setText("Could not load budgets: " + ex.getMessage());
        }
    }


    private void exportTransactions() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Transactions");
        chooser.setInitialFileName("finance_transactions.csv");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("ID,Description,Category,Type,Amount,Date");
            writer.newLine();

            for (Transaction t : allTransactions) {
                writer.write(t.getId() + ","
                    + csvEscape(t.getDescription()) + ","
                    + csvEscape(t.getCategory()) + ","
                    + csvEscape(t.getType()) + ","
                    + t.getAmount() + ","
                    + csvEscape(t.getDate()));
                writer.newLine();
            }

            showInfo("Export successful. Transactions exported: "
                + allTransactions.size());
        } catch (Exception ex) {
            showAlert("Could not export transactions: " + ex.getMessage());
        }
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Finance Copilot");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private VBox createRecurringSection() {
        recurringDescription = new TextField();
        recurringDescription.setPromptText("Description (e.g. Internet Bill)");

        recurringCategory = new TextField();
        recurringCategory.setPromptText("Category (e.g. Bills)");

        recurringType = new ComboBox<>();
        recurringType.getItems().addAll("INCOME", "EXPENSE");
        recurringType.setValue("EXPENSE");

        recurringAmount = new TextField();
        recurringAmount.setPromptText("Amount (₹)");

        recurringDay = new Spinner<>(1, 31, 1);
        recurringDay.setEditable(true);

        recurringTable = new TableView<>();
        recurringTable.setPrefHeight(220);

        TableColumn<RecurringTransactionDAO.Rule, String> descCol =
            new TableColumn<>("Description");
        descCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getDescription()));

        TableColumn<RecurringTransactionDAO.Rule, String> categoryCol =
            new TableColumn<>("Category");
        categoryCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getCategory()));

        TableColumn<RecurringTransactionDAO.Rule, String> typeCol =
            new TableColumn<>("Type");
        typeCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue().getType()));

        TableColumn<RecurringTransactionDAO.Rule, String> amountCol =
            new TableColumn<>("Amount");
        amountCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                String.format("₹%.2f", d.getValue().getAmount())));

        TableColumn<RecurringTransactionDAO.Rule, String> dayCol =
            new TableColumn<>("Day of Month");
        dayCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                String.valueOf(d.getValue().getDay())));

        recurringTable.getColumns().addAll(descCol, categoryCol, typeCol, amountCol, dayCol);
        recurringTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        recurringStatus = new Label("Add recurring rules, then generate entries for a month.");
        recurringStatus.setWrapText(true);

        Button addRuleButton = new Button("Add Recurring Rule");
        addRuleButton.setOnAction(e -> {
            try {
                String description = recurringDescription.getText().trim();
                String category = recurringCategory.getText().trim();
                double amount = Double.parseDouble(recurringAmount.getText().trim());
                int day = recurringDay.getValue();

                if (description.isEmpty() || category.isEmpty()) {
                    recurringStatus.setText("Enter a description and category.");
                    return;
                }
                if (!Double.isFinite(amount) || amount <= 0) {
                    recurringStatus.setText("Amount must be positive.");
                    return;
                }

                recurringDAO.addRule(description, category, recurringType.getValue(), amount, day);
                recurringDescription.clear();
                recurringCategory.clear();
                recurringAmount.clear();
                reloadRecurringRules();
                recurringStatus.setText("Recurring rule added.");
            } catch (NumberFormatException ex) {
                recurringStatus.setText("Enter a valid amount.");
            } catch (Exception ex) {
                recurringStatus.setText("Could not add rule: " + ex.getMessage());
            }
        });

        ComboBox<String> monthBox = new ComboBox<>();
        monthBox.getItems().addAll(months);
        monthBox.setValue(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));

        ComboBox<Integer> yearBox = new ComboBox<>();
        int yearNow = LocalDate.now().getYear();
        for (int y = yearNow - 5; y <= yearNow + 1; y++) yearBox.getItems().add(y);
        yearBox.setValue(yearNow);

        Button generateButton = new Button("Generate for Month");
        generateButton.setOnAction(e -> {
            try {
                String month = String.format("%04d-%02d",
                    yearBox.getValue(), monthBox.getSelectionModel().getSelectedIndex() + 1);

                int count = recurringDAO.generateForMonth(month);
                loadTransactions();
                loadBudgets(yearBox.getValue(),
                    monthBox.getSelectionModel().getSelectedIndex() + 1);

                recurringStatus.setText(count == 0
                    ? "All recurring rules have already been generated for " + month + "."
                    : count + " recurring transaction(s) added for " + month + ".");
            } catch (Exception ex) {
                recurringStatus.setText("Could not generate transactions: " + ex.getMessage());
            }
        });

        Button deleteRuleButton = new Button("Delete Selected Rule");
        deleteRuleButton.setOnAction(e -> {
            RecurringTransactionDAO.Rule selected =
                recurringTable.getSelectionModel().getSelectedItem();

            if (selected == null) {
                recurringStatus.setText("Select a rule first.");
                return;
            }

            try {
                recurringDAO.deleteRule(selected.getId());
                reloadRecurringRules();
                recurringStatus.setText("Recurring rule deleted.");
            } catch (Exception ex) {
                recurringStatus.setText("Could not delete rule: " + ex.getMessage());
            }
        });

        HBox form = new HBox(8, recurringDescription, recurringCategory,
            recurringType, recurringAmount, recurringDay, addRuleButton);

        HBox generationControls = new HBox(10,
            new Label("Month:"), monthBox, new Label("Year:"), yearBox, generateButton);

        VBox section = new VBox(12,
            sectionTitle("Recurring Transactions & Monthly Bills"),
            new Label("Create repeating income or expense rules and generate each month once."),
            form, generationControls, recurringTable, deleteRuleButton, recurringStatus);

        styleSection(section);
        reloadRecurringRules();
        return section;
    }

    private void reloadRecurringRules() {
        if (recurringTable == null) return;
        try {
            recurringTable.setItems(
                FXCollections.observableArrayList(recurringDAO.getRules()));
        } catch (Exception ex) {
            recurringStatus.setText("Could not load recurring rules: " + ex.getMessage());
        }
    }

    private void exportMonthlyReport() {
        if (monthSelector.getValue() == null || yearSelector.getValue() == null) {
            showAlert("Select a month and year first.");
            return;
        }

        String month = String.format("%04d-%02d",
            yearSelector.getValue(),
            monthSelector.getSelectionModel().getSelectedIndex() + 1);

        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Export Monthly Financial Report");
        chooser.setInitialFileName("Finance_Report_" + month + ".pdf");
        chooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        java.io.File file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        try {
            List<Transaction> transactions = dao.getByMonth(month);
            double income = 0, expenses = 0;
            Map<String, Double> categories = new TreeMap<>();

            for (Transaction t : transactions) {
                if ("INCOME".equals(t.getType())) {
                    income += t.getAmount();
                } else {
                    expenses += t.getAmount();
                    categories.merge(t.getCategory(), t.getAmount(), Double::sum);
                }
            }

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(new Paragraph("FINANCE COPILOT"));
            document.add(new Paragraph("Monthly Financial Report: " + month));
            document.add(new Paragraph("Generated on: " + LocalDate.now()));
            document.add(new Paragraph(" "));

            document.add(new Paragraph(String.format("Total Income: Rs. %.2f", income)));
            document.add(new Paragraph(String.format("Total Expenses: Rs. %.2f", expenses)));
            document.add(new Paragraph(String.format("Net Balance: Rs. %.2f", income - expenses)));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Category-wise Expenses"));
            PdfPTable categoryTable = new PdfPTable(2);
            categoryTable.addCell(new Phrase("Category"));
            categoryTable.addCell(new Phrase("Amount"));

            for (Map.Entry<String, Double> entry : categories.entrySet()) {
                categoryTable.addCell(entry.getKey());
                categoryTable.addCell(String.format("Rs. %.2f", entry.getValue()));
            }
            document.add(categoryTable);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Transactions"));
            PdfPTable transactionTable = new PdfPTable(4);
            transactionTable.setWidths(new float[]{3, 2, 2, 2});
            transactionTable.addCell("Description");
            transactionTable.addCell("Category");
            transactionTable.addCell("Type");
            transactionTable.addCell("Amount");

            for (Transaction t : transactions) {
                transactionTable.addCell(t.getDescription());
                transactionTable.addCell(t.getCategory());
                transactionTable.addCell(t.getType());
                transactionTable.addCell(String.format("Rs. %.2f", t.getAmount()));
            }
            document.add(transactionTable);

            document.close();
            showInfo("Monthly report exported successfully.");

        } catch (Exception ex) {
            showAlert("Could not export report: " + ex.getMessage());
        }
    }
    
    private VBox createFinancialHealthSection() {
        healthScoreLabel = new Label("Financial Health Score: Not calculated");
        healthScoreLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        healthRecommendationLabel = new Label(
            "Calculate your score to receive personalized financial recommendations.");
        healthRecommendationLabel.setWrapText(true);

        healthScoreButton = new Button("Calculate Financial Health");
        healthScoreButton.setOnAction(e -> calculateFinancialHealth());

        VBox section = new VBox(12,
            sectionTitle("Financial Health Score & Recommendations"),
            healthScoreLabel,
            healthRecommendationLabel,
            healthScoreButton);

        styleSection(section);
        return section;
    }

    private void calculateFinancialHealth() {
        healthScoreButton.setDisable(true);
        healthScoreLabel.setText("Calculating...");

        try {
            List<Transaction> transactions = dao.getAll();

            double income = 0;
            double expenses = 0;
            Map<String, Double> categoryExpenses = new TreeMap<>();

            for (Transaction t : transactions) {
                if ("INCOME".equals(t.getType())) {
                    income += t.getAmount();
                } else {
                    expenses += t.getAmount();
                    categoryExpenses.merge(t.getCategory(),
                        t.getAmount(), Double::sum);
                }
            }

            if (income == 0 && expenses == 0) {
                healthScoreLabel.setText("Financial Health Score: N/A");
                healthRecommendationLabel.setText(
                    "Add transactions before calculating your score.");
                return;
            }

            double score = 0;
            double savingsRate = income > 0
                ? ((income - expenses) / income) * 100 : -100;

            if (income > 0) {
                score += Math.max(0, Math.min(40, savingsRate * 2));
            }

            if (income > 0) {
                double expenseRatio = expenses / income;
                score += Math.max(0, Math.min(30,
                    (1 - expenseRatio) * 30));
            }

            List<BudgetDAO.BudgetSummary> budgets =
                budgetDAO.getBudgets(
                    String.format("%04d-%02d",
                        LocalDate.now().getYear(),
                        LocalDate.now().getMonthValue()));

            if (!budgets.isEmpty()) {
                long withinBudget = budgets.stream()
                    .filter(b -> b.getSpent() <= b.getLimit())
                    .count();

                score += 30.0 * withinBudget / budgets.size();
            } else {
                score += 15;
            }

            int finalScore = (int) Math.round(Math.max(0, Math.min(100, score)));

            String rating;
            if (finalScore >= 80) rating = "Excellent";
            else if (finalScore >= 60) rating = "Good";
            else if (finalScore >= 40) rating = "Fair";
            else rating = "Needs Improvement";

            StringBuilder advice = new StringBuilder();
            advice.append("Rating: ").append(rating).append("\n");
            advice.append(String.format(
                "Income: ₹%.2f | Expenses: ₹%.2f%n", income, expenses));
            advice.append(String.format(
                "Savings rate: %.1f%%%n", savingsRate));

            if (income <= 0) {
                advice.append(
                    "• Record your income to calculate a meaningful savings rate.\n");
            } else if (savingsRate < 0) {
                advice.append(
                    "• Your expenses exceed your income. Review non-essential spending.\n");
            } else if (savingsRate < 20) {
                advice.append(
                    "• Consider setting a savings target and reducing discretionary expenses.\n");
            } else {
                advice.append(
                    "• Good savings progress. Continue tracking your monthly spending.\n");
            }

            if (budgets.isEmpty()) {
                advice.append(
                    "• Create monthly budgets to monitor category-wise spending.\n");
            } else {
                for (BudgetDAO.BudgetSummary b : budgets) {
                    if (b.getSpent() > b.getLimit()) {
                        advice.append("• ").append(b.getCategory())
                            .append(" is over budget. Review this category.\n");
                    } else if (b.getSpent() >= b.getLimit() * 0.8) {
                        advice.append("• ").append(b.getCategory())
                            .append(" is approaching its budget limit.\n");
                    }
                }
            }

            healthScoreLabel.setText(
                "Financial Health Score: " + finalScore + "/100 — " + rating);
            healthRecommendationLabel.setText(advice.toString());

        } catch (Exception ex) {
            healthScoreLabel.setText("Could not calculate score");
            healthRecommendationLabel.setText(ex.getMessage());
        } finally {
            healthScoreButton.setDisable(false);
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