package com.financecopilot;

import com.financecopilot.database.Database;
import com.financecopilot.models.SavingsGoal;
import com.financecopilot.models.Transaction;
import com.financecopilot.services.*;
import com.financecopilot.ui.CsvImportDialog;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FinSight — AI-Powered Financial Analysis & Decision Support System
 * Desktop UI: Personal Wealth Journal (View-Switching Dashboard)
 */
public class MainApp extends Application {

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AnalysisService analysisService = new AnalysisService();
    private final SavingsGoalDAO savingsGoalDAO = new SavingsGoalDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final RecurringTransactionDAO recurringDAO = new RecurringTransactionDAO();
    private final CsvImportService csvImportService = new CsvImportService(transactionDAO);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String API_URL = "http://127.0.0.1:5000";

    private final String[] months = {
        "January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"
    };

    // Navigation and View Management
    public enum DashboardView {
        OVERVIEW("Overview"),
        INSIGHTS("Spending Insights"),
        BUDGETS("Budgets"),
        SAVINGS_GOALS("Savings Goals"),
        RECURRING("Recurring Bills"),
        COMPARISON("Comparison"),
        HEALTH_SCORE("Health Score"),
        LEDGER("Transactions");

        private final String label;
        DashboardView(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private DashboardView currentView = DashboardView.OVERVIEW;
    private final Map<DashboardView, Button> navButtons = new LinkedHashMap<>();
    private StackPane viewContainer;

    // Header Controls
    private ComboBox<String> navMonthSelector;
    private ComboBox<Integer> navYearSelector;

    // Overview Section Controls
    private Label overviewHeadline;
    private Label overviewNarrative;
    private Label metricIncome;
    private Label metricExpenses;
    private Label metricBalance;
    private VBox emptyStateBanner;
    private BarChart<String, Number> monthlyBarChart;
    private PieChart expensePieChart;

    // Python Analysis & Insights
    private Label insightsSummaryLabel;
    private Label insightsAlertsLabel;
    private Label anomalySummaryLabel;
    private Label anomalyStatusLabel;
    private Button refreshInsightsButton;

    // Comparison Controls
    private ComboBox<String> compareMonth1;
    private ComboBox<Integer> compareYear1;
    private ComboBox<String> compareMonth2;
    private ComboBox<Integer> compareYear2;
    private Label comparisonContent;
    private Label comparisonStatus;
    private Button compareButton;

    // Savings Goals
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

    // Budgets
    private TableView<BudgetDAO.BudgetSummary> budgetTable;
    private TextField budgetCategoryField;
    private TextField budgetLimitField;
    private Label budgetStatus;

    // Recurring Transactions
    private TableView<RecurringTransactionDAO.Rule> recurringTable;
    private TextField recurringDescription, recurringCategory, recurringAmount;
    private ComboBox<String> recurringType;
    private Spinner<Integer> recurringDay;
    private Label recurringStatus;

    // Financial Health
    private Label healthScoreLabel;
    private Label healthRecommendationLabel;
    private Button healthScoreButton;

    // Transaction Ledger
    private final TableView<Transaction> transactionTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> filterType = new ComboBox<>();
    private List<Transaction> allTransactions = new ArrayList<>();

    // Add Transaction Form
    private VBox addTransactionSection;
    private TextField addDescField;
    private TextField addCatField;
    private ComboBox<String> addTypeCombo;
    private TextField addAmountField;
    private DatePicker addDatePicker;

    @Override
    public void start(Stage stage) {
        // 1. Initialize SQLite Database
        Database.initialize();

        // 2. Start Python service asynchronously in background if not already online
        executor.submit(PythonServiceManager::ensureRunning);

        // 3. Build Two-Tier Top Navigation Header
        VBox topHeader = buildTopHeader();

        // 4. View Container (Holds the currently selected view)
        viewContainer = new StackPane();
        viewContainer.setStyle("-fx-background-color: #F6F0E7;");

        ScrollPane scrollPane = new ScrollPane(viewContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #F6F0E7; -fx-background: #F6F0E7;");

        VBox root = new VBox(topHeader, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 1220, 860);

        // Load stylesheet
        try {
            String cssPath = getClass().getResource("/styles/wealth-journal.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception ex) {
            System.err.println("Note: wealth-journal.css loaded via fallback style definitions.");
        }

        stage.setTitle("FinSight — Personal Wealth Journal");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);

        stage.setOnCloseRequest(e -> {
            executor.shutdownNow();
            PythonServiceManager.stop();
        });

        stage.show();

        // 5. Initial Data Loading & Default View Display
        loadTransactions();
        reloadSavingsGoals();
        reloadBudgetsForNavPeriod();
        reloadRecurringRules();

        switchView(DashboardView.OVERVIEW);
    }

    // =========================================================================
    // TWO-TIER TOP HEADER (Spacious, Fully Visible, No Truncation)
    // =========================================================================

    private VBox buildTopHeader() {
        // --- TIER 1: BRAND TITLE & GLOBAL CONTROLS ---
        Label brandTitle = new Label("FINSIGHT");
        brandTitle.setMinWidth(180);
        brandTitle.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #362E28; -fx-letter-spacing: 1px;");

        Label brandSub = new Label("Personal Wealth Journal");
        brandSub.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #4A4036;");

        VBox brandBox = new VBox(2, brandTitle, brandSub);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        // Period Selector
        Label periodLabel = new Label("Period:");
        periodLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4A4036;");

        navMonthSelector = new ComboBox<>();
        navMonthSelector.getItems().addAll(months);
        navMonthSelector.setValue(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        navMonthSelector.setPrefWidth(125);

        navYearSelector = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 5; y <= currentYear + 1; y++) {
            navYearSelector.getItems().add(y);
        }
        navYearSelector.setValue(currentYear);
        navYearSelector.setPrefWidth(90);

        navMonthSelector.setOnAction(e -> onPeriodChanged());
        navYearSelector.setOnAction(e -> onPeriodChanged());

        HBox periodBox = new HBox(8, periodLabel, navMonthSelector, navYearSelector);
        periodBox.setAlignment(Pos.CENTER_LEFT);

        // Global Quick Actions
        Button importBtn = new Button("📥 Import CSV");
        importBtn.getStyleClass().add("btn-outline");
        importBtn.setOnAction(e -> openCsvImportDialog());

        Button addTxnBtn = new Button("+ Record Transaction");
        addTxnBtn.getStyleClass().add("btn-primary");
        addTxnBtn.setOnAction(e -> {
            switchView(DashboardView.LEDGER);
            if (addTransactionSection != null) {
                addTransactionSection.setVisible(true);
                addTransactionSection.setManaged(true);
                if (addDescField != null) addDescField.requestFocus();
            }
        });

        HBox topActions = new HBox(12, periodBox, importBtn, addTxnBtn);
        topActions.setAlignment(Pos.CENTER_RIGHT);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox tier1 = new HBox(20, brandBox, topSpacer, topActions);
        tier1.setPadding(new Insets(14, 32, 10, 32));
        tier1.setAlignment(Pos.CENTER_LEFT);
        tier1.setStyle("-fx-background-color: #FFFCF7;");

        // --- TIER 2: HORIZONTAL NAVIGATION TABS STRIP ---
        HBox tier2 = new HBox(8);
        tier2.setPadding(new Insets(0, 32, 0, 32));
        tier2.setAlignment(Pos.CENTER_LEFT);
        tier2.setStyle("-fx-background-color: #FFFCF7; -fx-border-color: #E6D9C8; -fx-border-width: 0 0 1px 0;");

        for (DashboardView view : DashboardView.values()) {
            Button btn = new Button(view.getLabel());
            btn.getStyleClass().add("nav-tab");
            btn.setOnAction(e -> switchView(view));
            navButtons.put(view, btn);
            tier2.getChildren().add(btn);
        }

        VBox headerContainer = new VBox(tier1, tier2);
        return headerContainer;
    }

    private void switchView(DashboardView view) {
        this.currentView = view;

        // Update Nav Tab Styles
        for (Map.Entry<DashboardView, Button> entry : navButtons.entrySet()) {
            Button btn = entry.getValue();
            if (entry.getKey() == view) {
                btn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #7D8661; "
                    + "-fx-font-size: 13px; -fx-font-weight: bold; "
                    + "-fx-border-color: transparent transparent #7D8661 transparent; "
                    + "-fx-border-width: 0 0 3px 0; -fx-border-radius: 0; -fx-background-radius: 0; "
                    + "-fx-padding: 8 16; -fx-cursor: hand;"
                );
            } else {
                btn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #362E28; "
                    + "-fx-font-size: 13px; -fx-font-weight: 500; "
                    + "-fx-border-color: transparent; -fx-border-width: 0 0 3px 0; "
                    + "-fx-border-radius: 0; -fx-background-radius: 0; "
                    + "-fx-padding: 8 16; -fx-cursor: hand;"
                );
            }
        }

        // Render Only the Selected Feature View
        Node viewContent = switch (view) {
            case OVERVIEW -> buildOverviewView();
            case INSIGHTS -> buildInsightsView();
            case BUDGETS -> buildBudgetsView();
            case SAVINGS_GOALS -> buildSavingsGoalsView();
            case RECURRING -> buildRecurringView();
            case COMPARISON -> buildComparisonView();
            case HEALTH_SCORE -> buildHealthScoreView();
            case LEDGER -> buildLedgerView();
        };

        VBox wrapped = new VBox(viewContent);
        wrapped.setPadding(new Insets(24, 32, 40, 32));
        wrapped.setStyle("-fx-background-color: #F6F0E7;");

        viewContainer.getChildren().setAll(wrapped);
    }

    // =========================================================================
    // VIEW 1: OVERVIEW DASHBOARD
    // =========================================================================

    private VBox buildOverviewView() {
        overviewHeadline = new Label("Your money in " + navMonthSelector.getValue() + " " + navYearSelector.getValue());
        overviewHeadline.getStyleClass().add("journal-title");

        overviewNarrative = new Label("Loading monthly financial overview...");
        overviewNarrative.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #3D352E;");

        // Metrics Row
        metricIncome = new Label("₹0.00");
        metricIncome.getStyleClass().add("metric-positive");

        metricExpenses = new Label("₹0.00");
        metricExpenses.getStyleClass().add("metric-negative");

        metricBalance = new Label("₹0.00");
        metricBalance.getStyleClass().add("metric-amount");

        VBox colIncome = createMetricColumn("TOTAL INCOME", metricIncome);
        VBox colExpenses = createMetricColumn("TOTAL EXPENSES", metricExpenses);
        VBox colBalance = createMetricColumn("NET BALANCE", metricBalance);

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);
        Separator sep2 = new Separator(javafx.geometry.Orientation.VERTICAL);

        HBox metricsRow = new HBox(40, colIncome, sep1, colExpenses, sep2, colBalance);
        metricsRow.setAlignment(Pos.CENTER_LEFT);
        metricsRow.setPadding(new Insets(20, 28, 20, 28));
        metricsRow.setStyle("-fx-background-color: #FFFCF7; -fx-border-color: #E6D9C8; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        // Empty state banner
        emptyStateBanner = buildEmptyStateBanner();
        emptyStateBanner.setVisible(allTransactions.isEmpty());
        emptyStateBanner.setManaged(allTransactions.isEmpty());

        // Charts
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Flow");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount (₹)");
        yAxis.setForceZeroInRange(true);

        monthlyBarChart = new BarChart<>(xAxis, yAxis);
        monthlyBarChart.setTitle("Income vs Expenses");
        monthlyBarChart.setLegendVisible(false);
        monthlyBarChart.setAnimated(false);
        monthlyBarChart.setPrefHeight(340);

        expensePieChart = new PieChart();
        expensePieChart.setTitle("Expense Distribution");
        expensePieChart.setLegendVisible(true);
        expensePieChart.setLabelsVisible(true);
        expensePieChart.setAnimated(false);
        expensePieChart.setPrefHeight(340);

        HBox chartsRow = new HBox(20, monthlyBarChart, expensePieChart);
        HBox.setHgrow(monthlyBarChart, Priority.ALWAYS);
        HBox.setHgrow(expensePieChart, Priority.ALWAYS);

        Button exportPdfBtn = new Button("📄 Export Monthly PDF Statement");
        exportPdfBtn.getStyleClass().add("btn-outline");
        exportPdfBtn.setOnAction(e -> exportMonthlyReport());

        Button exportCsvBtn = new Button("Export Transactions to CSV");
        exportCsvBtn.getStyleClass().add("btn-outline");
        exportCsvBtn.setOnAction(e -> exportTransactions());

        HBox actionRow = new HBox(12, exportPdfBtn, exportCsvBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox chartsCard = new VBox(16, chartsRow, actionRow);
        chartsCard.getStyleClass().add("journal-card");

        loadMonthlyAnalysis();

        return new VBox(20, overviewHeadline, overviewNarrative, metricsRow, emptyStateBanner, chartsCard);
    }

    private VBox createMetricColumn(String labelText, Label valueLabel) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #4A4036; -fx-letter-spacing: 0.5px;");
        VBox box = new VBox(6, lbl, valueLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox buildEmptyStateBanner() {
        Label emptyTitle = new Label("Your Financial Journal is Ready");
        emptyTitle.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #362E28;");

        Label emptyDesc = new Label(
            "You have not recorded any transactions yet. Start building your personal wealth journal "
            + "by importing an existing bank statement (CSV) or recording your first income or expense."
        );
        emptyDesc.setWrapText(true);
        emptyDesc.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #3D352E; -fx-line-spacing: 3px;");

        Button emptyImportBtn = new Button("📥 Import Bank Statement (CSV)");
        emptyImportBtn.getStyleClass().add("btn-primary");
        emptyImportBtn.setOnAction(e -> openCsvImportDialog());

        Button emptyAddBtn = new Button("+ Record First Transaction");
        emptyAddBtn.getStyleClass().add("btn-outline");
        emptyAddBtn.setOnAction(e -> {
            switchView(DashboardView.LEDGER);
            if (addTransactionSection != null) {
                addTransactionSection.setVisible(true);
                addTransactionSection.setManaged(true);
                if (addDescField != null) addDescField.requestFocus();
            }
        });

        HBox btnRow = new HBox(12, emptyImportBtn, emptyAddBtn);

        VBox box = new VBox(12, emptyTitle, emptyDesc, btnRow);
        box.getStyleClass().add("journal-card-muted");
        box.setPadding(new Insets(20));
        return box;
    }

    // =========================================================================
    // VIEW 2: SPENDING INSIGHTS & ANOMALY DETECTION
    // =========================================================================

    private VBox buildInsightsView() {
        Label title = new Label("Spending Insights & Outlier Detection");
        title.getStyleClass().add("journal-title");

        Label subtitle = new Label("Automated pattern analysis, spending concentration warnings, and statistical IQR outlier detection.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        insightsSummaryLabel = new Label("Add transactions to calculate spending insights.");
        insightsSummaryLabel.setWrapText(true);
        insightsSummaryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #362E28; -fx-line-spacing: 3px;");

        insightsAlertsLabel = new Label();
        insightsAlertsLabel.setWrapText(true);
        insightsAlertsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #B96F52; -fx-line-spacing: 2px;");

        anomalySummaryLabel = new Label("Unusual transaction anomalies will appear here.");
        anomalySummaryLabel.setWrapText(true);
        anomalySummaryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #362E28; -fx-line-spacing: 3px;");

        anomalyStatusLabel = new Label("Analysis service: ready");
        anomalyStatusLabel.getStyleClass().add("journal-caption");

        refreshInsightsButton = new Button("Refresh Spending Analysis");
        refreshInsightsButton.getStyleClass().add("btn-primary");
        refreshInsightsButton.setOnAction(e -> refreshInsights());

        HBox statusRow = new HBox(14, refreshInsightsButton, anomalyStatusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        VBox insightsCol = new VBox(12,
            new Label("SPENDING CONCENTRATION & HIGHLIGHTS") {{
                setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4A4036;");
            }},
            insightsSummaryLabel,
            insightsAlertsLabel
        );
        insightsCol.setPadding(new Insets(16));
        insightsCol.setStyle("-fx-background-color: #FAF5ED; -fx-border-color: #E6D9C8; -fx-border-radius: 6px;");

        VBox anomalyCol = new VBox(12,
            new Label("UNUSUAL EXPENSE DETECTION (IQR)") {{
                setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4A4036;");
            }},
            anomalySummaryLabel
        );
        anomalyCol.setPadding(new Insets(16));
        anomalyCol.setStyle("-fx-background-color: #FAF5ED; -fx-border-color: #E6D9C8; -fx-border-radius: 6px;");

        HBox splitCols = new HBox(24, insightsCol, anomalyCol);
        HBox.setHgrow(insightsCol, Priority.ALWAYS);
        HBox.setHgrow(anomalyCol, Priority.ALWAYS);

        VBox card = new VBox(20, statusRow, new Separator(), splitCols);
        card.getStyleClass().add("journal-card");

        refreshInsights();

        return new VBox(16, title, subtitle, card);
    }

    // =========================================================================
    // VIEW 3: MONTHLY BUDGETS
    // =========================================================================

    private VBox buildBudgetsView() {
        Label title = new Label("Monthly Category Budgets");
        title.getStyleClass().add("journal-title");

        Label subtitle = new Label("Set spending ceilings for categories in " + navMonthSelector.getValue() + " " + navYearSelector.getValue() + " to guard against overspending.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        budgetCategoryField = new TextField();
        budgetCategoryField.setPromptText("Category (e.g. Food)");
        budgetCategoryField.setPrefWidth(160);

        budgetLimitField = new TextField();
        budgetLimitField.setPromptText("Monthly Limit (₹)");
        budgetLimitField.setPrefWidth(140);

        Button saveBudgetBtn = new Button("Set Budget");
        saveBudgetBtn.getStyleClass().add("btn-primary");
        saveBudgetBtn.setOnAction(e -> onSaveBudget());

        HBox form = new HBox(12, budgetCategoryField, budgetLimitField, saveBudgetBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        budgetTable = new TableView<>();
        budgetTable.setPrefHeight(340);
        budgetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BudgetDAO.BudgetSummary, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));

        TableColumn<BudgetDAO.BudgetSummary, String> limitCol = new TableColumn<>("Monthly Limit");
        limitCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%.2f", d.getValue().getLimit())));

        TableColumn<BudgetDAO.BudgetSummary, String> spentCol = new TableColumn<>("Spent");
        spentCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%.2f", d.getValue().getSpent())));

        TableColumn<BudgetDAO.BudgetSummary, String> remCol = new TableColumn<>("Remaining");
        remCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("₹%.2f", d.getValue().getRemaining())));

        TableColumn<BudgetDAO.BudgetSummary, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("OVER")) {
                        setStyle("-fx-text-fill: #B96F52; -fx-font-weight: bold;");
                    } else if (item.contains("NEAR")) {
                        setStyle("-fx-text-fill: #C4975D; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #3E5C3B; -fx-font-weight: bold;");
                    }
                }
            }
        });

        budgetTable.getColumns().addAll(catCol, limitCol, spentCol, remCol, statusCol);

        Button deleteBudgetBtn = new Button("Delete Selected Budget");
        deleteBudgetBtn.getStyleClass().add("btn-danger");
        deleteBudgetBtn.setOnAction(e -> onDeleteBudget());

        budgetStatus = new Label("Set a monthly spending limit for each expense category.");
        budgetStatus.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        HBox actionsRow = new HBox(16, deleteBudgetBtn, budgetStatus);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16, form, budgetTable, actionsRow);
        card.getStyleClass().add("journal-card");

        reloadBudgetsForNavPeriod();

        return new VBox(16, title, subtitle, card);
    }

    // =========================================================================
    // VIEW 4: SAVINGS GOALS & MILESTONES
    // =========================================================================

    private VBox buildSavingsGoalsView() {
        Label title = new Label("Savings Goals & Milestones");
        title.getStyleClass().add("journal-title");

        Label subtitle = new Label("Set and track specific financial milestones, from emergency funds to equipment purchases.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        totalGoalsLabel = new Label("Total Goals: 0");
        totalTargetLabel = new Label("Total Target: ₹0.00");
        totalSavedLabel = new Label("Total Saved: ₹0.00");

        HBox summary = new HBox(28, totalGoalsLabel, totalTargetLabel, totalSavedLabel);
        summary.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #362E28;");

        overallProgressLabel = new Label("Overall Progress: 0.0%");
        overallProgressLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #362E28;");
        overallProgressBar = new ProgressBar(0);
        overallProgressBar.setPrefHeight(22);
        overallProgressBar.setMinHeight(22);
        overallProgressBar.setMaxWidth(Double.MAX_VALUE);

        // Goal Create Form
        goalNameField = new TextField();
        goalNameField.setPromptText("Goal name (e.g. Laptop)");
        goalNameField.setPrefWidth(180);

        goalTargetField = new TextField();
        goalTargetField.setPromptText("Target amount (₹)");
        goalTargetField.setPrefWidth(130);

        goalDatePicker = new DatePicker(LocalDate.now().plusMonths(6));

        Button createGoalBtn = new Button("Create Goal");
        createGoalBtn.getStyleClass().add("btn-primary");
        createGoalBtn.setOnAction(e -> addSavingsGoal());

        HBox goalForm = new HBox(10, goalNameField, goalTargetField, goalDatePicker, createGoalBtn);
        goalForm.setAlignment(Pos.CENTER_LEFT);

        // Goals List
        goalsList = new ListView<>();
        goalsList.setPrefHeight(280);
        goalsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SavingsGoal goal, boolean empty) {
                super.updateItem(goal, empty);
                if (empty || goal == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label nameLbl = new Label(goal.getName());
                    nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #362E28;");

                    double pct = Math.min(100.0, Math.max(0.0, goal.getProgress() * 100.0));
                    boolean isDone = goal.isCompleted();

                    Label statusBadge = new Label(isDone ? "✓ COMPLETED" : String.format("%.1f%% SAVED", pct));
                    if (isDone) {
                        statusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #274D1F; -fx-background-color: #D6E8CD; -fx-padding: 3 8; -fx-background-radius: 4;");
                    } else {
                        statusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3E5C3B; -fx-background-color: #E2ECD8; -fx-padding: 3 8; -fx-background-radius: 4;");
                    }

                    Region spacerTop = new Region();
                    HBox.setHgrow(spacerTop, Priority.ALWAYS);
                    HBox headerRow = new HBox(10, nameLbl, spacerTop, statusBadge);
                    headerRow.setAlignment(Pos.CENTER_LEFT);

                    ProgressBar bar = new ProgressBar(goal.getProgress());
                    bar.setPrefHeight(22);
                    bar.setMinHeight(22);
                    bar.setMaxWidth(Double.MAX_VALUE);

                    Label amountsLbl = new Label(String.format("Saved ₹%.2f of ₹%.2f  (Remaining: ₹%.2f)",
                        goal.getSavedAmount(), goal.getTargetAmount(), goal.getRemainingAmount()));
                    amountsLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

                    Label deadlineLbl = new Label("Target date: " + goal.getTargetDate());
                    deadlineLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

                    Region spacerBottom = new Region();
                    HBox.setHgrow(spacerBottom, Priority.ALWAYS);
                    HBox footerRow = new HBox(10, amountsLbl, spacerBottom, deadlineLbl);
                    footerRow.setAlignment(Pos.CENTER_LEFT);

                    VBox content = new VBox(8, headerRow, bar, footerRow);
                    content.setPadding(new Insets(10));
                    setGraphic(content);
                }
            }
        });

        // Contribution form
        contributionField = new TextField();
        contributionField.setPromptText("Contribution (₹)");
        contributionField.setPrefWidth(140);

        Button addContribBtn = new Button("Add Savings");
        addContribBtn.getStyleClass().add("btn-outline");
        addContribBtn.setOnAction(e -> addSavingsContribution());

        Button deleteGoalBtn = new Button("Delete Goal");
        deleteGoalBtn.getStyleClass().add("btn-danger");
        deleteGoalBtn.setOnAction(e -> deleteSavingsGoal());

        HBox contribRow = new HBox(12, contributionField, addContribBtn, deleteGoalBtn);
        contribRow.setAlignment(Pos.CENTER_LEFT);

        goalsStatus = new Label("Create a goal to start tracking progress.");
        goalsStatus.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        VBox card = new VBox(16, summary, overallProgressLabel, overallProgressBar, new Separator(), goalForm, goalsList, contribRow, goalsStatus);
        card.getStyleClass().add("journal-card");

        reloadSavingsGoals();

        return new VBox(16, title, subtitle, card);
    }

    // =========================================================================
    // VIEW 5: RECURRING TRANSACTIONS & BILLS
    // =========================================================================

    private VBox buildRecurringView() {
        Label title = new Label("Recurring Transactions & Monthly Bills");
        title.getStyleClass().add("journal-title");

        Label subtitle = new Label("Define monthly repeating rules (salary, utilities, rent) and generate entries once per month.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        recurringDescription = new TextField();
        recurringDescription.setPromptText("Description (e.g. Internet Bill)");
        recurringDescription.setPrefWidth(160);

        recurringCategory = new TextField();
        recurringCategory.setPromptText("Category (e.g. Bills)");
        recurringCategory.setPrefWidth(130);

        recurringType = new ComboBox<>(FXCollections.observableArrayList("EXPENSE", "INCOME"));
        recurringType.setValue("EXPENSE");
        recurringType.setPrefWidth(100);

        recurringAmount = new TextField();
        recurringAmount.setPromptText("Amount (₹)");
        recurringAmount.setPrefWidth(100);

        recurringDay = new Spinner<>(1, 31, 1);
        recurringDay.setPrefWidth(70);

        Button addRuleBtn = new Button("+ Add Rule");
        addRuleBtn.getStyleClass().add("btn-primary");
        addRuleBtn.setOnAction(e -> onAddRecurringRule());

        HBox form = new HBox(10, recurringDescription, recurringCategory, recurringType, recurringAmount, new Label("Day:"), recurringDay, addRuleBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        recurringTable = new TableView<>();
        recurringTable.setPrefHeight(280);
        recurringTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<RecurringTransactionDAO.Rule, String> dCol = new TableColumn<>("Description");
        dCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        TableColumn<RecurringTransactionDAO.Rule, String> cCol = new TableColumn<>("Category");
        cCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        TableColumn<RecurringTransactionDAO.Rule, String> tCol = new TableColumn<>("Type");
        tCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        TableColumn<RecurringTransactionDAO.Rule, String> aCol = new TableColumn<>("Amount");
        aCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("₹%.2f", c.getValue().getAmount())));
        TableColumn<RecurringTransactionDAO.Rule, String> dayCol = new TableColumn<>("Day of Month");
        dayCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDay())));

        recurringTable.getColumns().addAll(dCol, cCol, tCol, aCol, dayCol);

        Button generateBtn = new Button("⚡ Generate for " + navMonthSelector.getValue() + " " + navYearSelector.getValue());
        generateBtn.getStyleClass().add("btn-primary");
        generateBtn.setOnAction(e -> onGenerateRecurring());

        Button deleteRuleBtn = new Button("Delete Selected Rule");
        deleteRuleBtn.getStyleClass().add("btn-danger");
        deleteRuleBtn.setOnAction(e -> onDeleteRecurringRule());

        recurringStatus = new Label("Add recurring rules, then generate transactions for the active period.");
        recurringStatus.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        HBox actions = new HBox(14, generateBtn, deleteRuleBtn, recurringStatus);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16, form, recurringTable, actions);
        card.getStyleClass().add("journal-card");

        reloadRecurringRules();

        return new VBox(16, title, subtitle, card);
    }

    // =========================================================================
    // VIEW 6: MONTH-TO-MONTH COMPARISON
    // =========================================================================

    private VBox buildComparisonView() {
        Label title = new Label("Month-to-Month Comparison");
        title.getStyleClass().add("journal-title");

        Label subtitle = new Label("Compare income, expense, and balance variances between any two historical months.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        int currentYear = LocalDate.now().getYear();
        compareMonth1 = new ComboBox<>();
        compareMonth2 = new ComboBox<>();
        compareMonth1.getItems().addAll(months);
        compareMonth2.getItems().addAll(months);

        compareYear1 = new ComboBox<>();
        compareYear2 = new ComboBox<>();
        for (int y = currentYear - 5; y <= currentYear + 1; y++) {
            compareYear1.getItems().add(y);
            compareYear2.getItems().add(y);
        }

        LocalDate now = LocalDate.now();
        LocalDate prev = now.minusMonths(1);
        compareMonth1.setValue(prev.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        compareYear1.setValue(prev.getYear());
        compareMonth2.setValue(now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        compareYear2.setValue(now.getYear());

        HBox row1 = new HBox(8, new Label("Month A:"), compareMonth1, compareYear1);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, new Label("Month B:"), compareMonth2, compareYear2);
        row2.setAlignment(Pos.CENTER_LEFT);

        compareButton = new Button("Compare Periods");
        compareButton.getStyleClass().add("btn-primary");
        compareButton.setOnAction(e -> compareMonths());

        comparisonStatus = new Label();
        comparisonStatus.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        HBox selectorBar = new HBox(16, row1, row2, compareButton, comparisonStatus);
        selectorBar.setAlignment(Pos.CENTER_LEFT);

        comparisonContent = new Label("Select two months and click 'Compare Periods' to see spending and savings trends.");
        comparisonContent.setWrapText(true);
        comparisonContent.setStyle("-fx-font-size: 14px; -fx-text-fill: #362E28; -fx-line-spacing: 4px;");

        VBox resultBox = new VBox(12, comparisonContent);
        resultBox.setPadding(new Insets(16));
        resultBox.setStyle("-fx-background-color: #FAF5ED; -fx-border-color: #E6D9C8; -fx-border-radius: 6px;");

        VBox card = new VBox(18, selectorBar, resultBox);
        card.getStyleClass().add("journal-card");

        return new VBox(16, title, subtitle, card);
    }

    // =========================================================================
    // VIEW 7: FINANCIAL HEALTH SCORE
    // =========================================================================

    private VBox buildHealthScoreView() {
        Label title = new Label("Financial Health Score");
        title.getStyleClass().add("journal-title");

        Label subtitle = new Label("A rule-based financial evaluation of your savings rate, budget discipline, and expense coverage.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        healthScoreLabel = new Label("Financial Health Score: Not evaluated");
        healthScoreLabel.setStyle("-fx-font-family: 'Georgia', serif; -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #362E28;");

        healthRecommendationLabel = new Label(
            "Calculate your score to receive indicative recommendations based on savings rate and budget adherence."
        );
        healthRecommendationLabel.setWrapText(true);
        healthRecommendationLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #3D352E; -fx-line-spacing: 4px;");

        healthScoreButton = new Button("Calculate Financial Health");
        healthScoreButton.getStyleClass().add("btn-primary");
        healthScoreButton.setOnAction(e -> calculateFinancialHealth());

        VBox adviceBox = new VBox(14, healthScoreLabel, healthRecommendationLabel, healthScoreButton);
        adviceBox.setPadding(new Insets(20));
        adviceBox.setStyle("-fx-background-color: #FAF5ED; -fx-border-color: #E6D9C8; -fx-border-radius: 6px;");

        VBox card = new VBox(16, adviceBox);
        card.getStyleClass().add("journal-card");

        calculateFinancialHealth();

        return new VBox(16, title, subtitle, card);
    }

    // =========================================================================
    // VIEW 8: TRANSACTIONS LEDGER
    // =========================================================================

    private VBox buildLedgerView() {
        Label title = new Label("Transaction Ledger");
        title.getStyleClass().add("journal-title");

        Label subtitle = new Label("Complete chronological ledger. Filter, search, add, import, or export transactions.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #4A4036;");

        // Search & Filter Bar
        searchField.setPromptText("Search description or category...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, o, n) -> filterTransactions());

        filterType.getItems().setAll("All", "Income", "Expenses");
        filterType.setValue("All");
        filterType.setOnAction(e -> filterTransactions());

        Button resetBtn = new Button("Reset");
        resetBtn.getStyleClass().add("btn-outline");
        resetBtn.setOnAction(e -> {
            searchField.clear();
            filterType.setValue("All");
            filterTransactions();
        });

        Button toggleAddBtn = new Button("+ Record Transaction");
        toggleAddBtn.getStyleClass().add("btn-primary");
        toggleAddBtn.setOnAction(e -> {
            boolean visible = !addTransactionSection.isVisible();
            addTransactionSection.setVisible(visible);
            addTransactionSection.setManaged(visible);
        });

        Button importCsvBtn = new Button("📥 Import CSV");
        importCsvBtn.getStyleClass().add("btn-outline");
        importCsvBtn.setOnAction(e -> openCsvImportDialog());

        Button deleteTxnBtn = new Button("Delete Selected");
        deleteTxnBtn.getStyleClass().add("btn-danger");
        deleteTxnBtn.setOnAction(e -> deleteSelectedTransaction());

        Button exportLedgerCsvBtn = new Button("Export CSV");
        exportLedgerCsvBtn.getStyleClass().add("btn-outline");
        exportLedgerCsvBtn.setOnAction(e -> exportTransactions());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(10, searchField, filterType, resetBtn, spacer, toggleAddBtn, importCsvBtn, deleteTxnBtn, exportLedgerCsvBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Add Transaction Form
        addTransactionSection = buildAddTransactionForm();
        addTransactionSection.setVisible(false);
        addTransactionSection.setManaged(false);

        setupTransactionTable();

        VBox card = new VBox(16, toolbar, addTransactionSection, transactionTable);
        card.getStyleClass().add("journal-card");

        filterTransactions();

        return new VBox(16, title, subtitle, card);
    }

    private VBox buildAddTransactionForm() {
        addDescField = new TextField();
        addDescField.setPromptText("Description (e.g. Salary, Groceries)");
        addCatField = new TextField();
        addCatField.setPromptText("Category (e.g. Food, Salary)");
        addTypeCombo = new ComboBox<>(FXCollections.observableArrayList("EXPENSE", "INCOME"));
        addTypeCombo.setValue("EXPENSE");
        addAmountField = new TextField();
        addAmountField.setPromptText("Amount (₹)");
        addDatePicker = new DatePicker(LocalDate.now());

        Button saveBtn = new Button("Save Transaction");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> onSaveNewTransaction());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-outline");
        cancelBtn.setOnAction(e -> {
            addTransactionSection.setVisible(false);
            addTransactionSection.setManaged(false);
        });

        HBox form = new HBox(10, addDescField, addCatField, addTypeCombo, addAmountField, addDatePicker, saveBtn, cancelBtn);
        form.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, new Label("RECORD A NEW TRANSACTION") {{
            setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #4A4036;");
        }}, form);
        box.getStyleClass().add("journal-card-muted");
        box.setPadding(new Insets(14));
        return box;
    }

    private void setupTransactionTable() {
        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setMinWidth(110);
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDate()));

        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setMinWidth(260);
        descCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));

        TableColumn<Transaction, String> catCol = new TableColumn<>("Category");
        catCol.setMinWidth(140);
        catCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setMinWidth(100);
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("INCOME".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #3E5C3B; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #B96F52; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<Transaction, String> amtCol = new TableColumn<>("Amount");
        amtCol.setMinWidth(120);
        amtCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("₹%.2f", c.getValue().getAmount())));
        amtCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        transactionTable.getColumns().setAll(dateCol, descCol, catCol, typeCol, amtCol);
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transactionTable.setPrefHeight(380);
    }

    // =========================================================================
    // DATA OPERATIONS & EVENT LOGIC
    // =========================================================================

    private void onPeriodChanged() {
        loadMonthlyAnalysis();
        reloadBudgetsForNavPeriod();
        if (currentView == DashboardView.OVERVIEW) {
            overviewHeadline.setText("Your money in " + navMonthSelector.getValue() + " " + navYearSelector.getValue());
        }
    }

    private void loadTransactions() {
        try {
            allTransactions = transactionDAO.getAll();
            filterTransactions();

            if (emptyStateBanner != null) {
                emptyStateBanner.setVisible(allTransactions.isEmpty());
                emptyStateBanner.setManaged(allTransactions.isEmpty());
            }

            loadMonthlyAnalysis();

        } catch (Exception ex) {
            showAlert("Could not load transactions: " + ex.getMessage());
        }
    }

    private void loadMonthlyAnalysis() {
        if (navMonthSelector == null || navYearSelector == null
            || navMonthSelector.getValue() == null || navYearSelector.getValue() == null) {
            return;
        }

        int monthNumber = navMonthSelector.getSelectionModel().getSelectedIndex() + 1;
        String selectedMonth = String.format("%04d-%02d", navYearSelector.getValue(), monthNumber);

        try {
            List<Transaction> monthlyTxns = transactionDAO.getByMonth(selectedMonth);

            double monthlyInc = 0;
            double monthlyExp = 0;
            Map<String, Double> categoryTotals = new TreeMap<>();

            for (Transaction t : monthlyTxns) {
                if ("INCOME".equalsIgnoreCase(t.getType())) {
                    monthlyInc += t.getAmount();
                } else {
                    monthlyExp += t.getAmount();
                    categoryTotals.merge(t.getCategory(), t.getAmount(), Double::sum);
                }
            }

            double netBalance = monthlyInc - monthlyExp;

            if (metricIncome != null) metricIncome.setText(String.format("₹%.2f", monthlyInc));
            if (metricExpenses != null) metricExpenses.setText(String.format("₹%.2f", monthlyExp));
            if (metricBalance != null) {
                metricBalance.setText(String.format("₹%.2f", netBalance));
                if (netBalance >= 0) {
                    metricBalance.getStyleClass().remove("metric-negative");
                    if (!metricBalance.getStyleClass().contains("metric-positive")) {
                        metricBalance.getStyleClass().add("metric-positive");
                    }
                } else {
                    metricBalance.getStyleClass().remove("metric-positive");
                    if (!metricBalance.getStyleClass().contains("metric-negative")) {
                        metricBalance.getStyleClass().add("metric-negative");
                    }
                }
            }

            // Overview Narrative
            if (overviewHeadline != null) {
                overviewHeadline.setText("Your money in " + navMonthSelector.getValue() + " " + navYearSelector.getValue());
            }
            if (overviewNarrative != null) {
                if (monthlyTxns.isEmpty()) {
                    overviewNarrative.setText("No transactions recorded for this period yet. Import a statement or add transactions to begin.");
                } else {
                    double rate = monthlyInc > 0 ? (netBalance / monthlyInc) * 100 : 0;
                    overviewNarrative.setText(String.format(
                        "Across %d recorded transaction(s), net balance is %s at ₹%.2f (savings rate: %.1f%%).",
                        monthlyTxns.size(),
                        netBalance >= 0 ? "positive" : "negative",
                        Math.abs(netBalance),
                        rate
                    ));
                }
            }

            // Update Charts if available
            if (monthlyBarChart != null) {
                XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
                barSeries.getData().add(new XYChart.Data<>("Income", monthlyInc));
                barSeries.getData().add(new XYChart.Data<>("Expenses", monthlyExp));
                monthlyBarChart.getData().setAll(barSeries);
            }

            if (expensePieChart != null) {
                List<PieChart.Data> pieData = new ArrayList<>();
                for (Map.Entry<String, Double> e : categoryTotals.entrySet()) {
                    pieData.add(new PieChart.Data(e.getKey() + String.format(" (₹%.0f)", e.getValue()), e.getValue()));
                }
                expensePieChart.setData(FXCollections.observableArrayList(pieData));
            }

        } catch (Exception ex) {
            System.err.println("Note: " + ex.getMessage());
        }
    }

    private void filterTransactions() {
        String search = searchField.getText().trim().toLowerCase();
        String filter = filterType.getValue();

        List<Transaction> filtered = allTransactions.stream()
            .filter(t -> t.getDescription().toLowerCase().contains(search)
                || t.getCategory().toLowerCase().contains(search))
            .filter(t -> "All".equals(filter)
                || ("Income".equalsIgnoreCase(filter) && "INCOME".equalsIgnoreCase(t.getType()))
                || ("Expenses".equalsIgnoreCase(filter) && "EXPENSE".equalsIgnoreCase(t.getType())))
            .toList();

        transactionTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void openCsvImportDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Bank Statement or Transaction CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = chooser.showOpenDialog(transactionTable.getScene().getWindow());
        if (file == null) return;

        try {
            CsvImportService.CsvImportResult result = csvImportService.parseAndValidate(file);

            if (result.hasHeaderError()) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("CSV Format Error");
                errorAlert.setHeaderText("Invalid or Unrecognized CSV Headers");
                errorAlert.setContentText(result.getHeaderError());
                errorAlert.initOwner(transactionTable.getScene().getWindow());
                errorAlert.showAndWait();
                return;
            }

            CsvImportDialog dialog = new CsvImportDialog(transactionTable.getScene().getWindow(), result, transactionDAO);
            boolean imported = dialog.showAndWait();

            if (imported) {
                loadTransactions();
                reloadBudgetsForNavPeriod();
                switchView(currentView); // Refresh current view
            }

        } catch (Exception ex) {
            showAlert("Failed to parse CSV file: " + ex.getMessage());
        }
    }

    private void onSaveNewTransaction() {
        try {
            String desc = addDescField.getText().trim();
            String cat = addCatField.getText().trim();
            String amountStr = addAmountField.getText().trim();
            LocalDate dateVal = addDatePicker.getValue();

            if (desc.isEmpty() || cat.isEmpty() || amountStr.isEmpty() || dateVal == null) {
                showAlert("Please fill in all transaction fields.");
                return;
            }

            double amt = Double.parseDouble(amountStr);
            if (!Double.isFinite(amt) || amt <= 0) {
                showAlert("Amount must be a positive number greater than 0.");
                return;
            }

            Transaction t = new Transaction(0, desc, cat, addTypeCombo.getValue(), amt, dateVal.toString());
            transactionDAO.add(t);

            addDescField.clear();
            addCatField.clear();
            addAmountField.clear();
            addDatePicker.setValue(LocalDate.now());
            addTransactionSection.setVisible(false);
            addTransactionSection.setManaged(false);

            loadTransactions();
            reloadBudgetsForNavPeriod();

        } catch (NumberFormatException e) {
            showAlert("Amount must be a valid numeric value.");
        } catch (Exception ex) {
            showAlert("Could not save transaction: " + ex.getMessage());
        }
    }

    private void deleteSelectedTransaction() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a transaction from the ledger to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Transaction");
        confirm.setHeaderText("Permanently delete transaction?");
        confirm.setContentText(String.format("Delete '%s' (₹%.2f on %s)? This cannot be undone.",
            selected.getDescription(), selected.getAmount(), selected.getDate()));

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    transactionDAO.delete(selected.getId());
                    loadTransactions();
                    reloadBudgetsForNavPeriod();
                } catch (Exception ex) {
                    showAlert("Could not delete transaction: " + ex.getMessage());
                }
            }
        });
    }

    private void exportTransactions() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Transactions to CSV");
        chooser.setInitialFileName("finsight_transactions.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = chooser.showSaveDialog(transactionTable.getScene().getWindow());
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("date,description,category,type,amount");
            writer.newLine();

            for (Transaction t : allTransactions) {
                writer.write(String.format("%s,%s,%s,%s,%.2f",
                    t.getDate(),
                    csvEscape(t.getDescription()),
                    csvEscape(t.getCategory()),
                    t.getType(),
                    t.getAmount()
                ));
                writer.newLine();
            }

            showInfo("Transactions exported successfully (" + allTransactions.size() + " rows).");
        } catch (Exception ex) {
            showAlert("Could not export transactions: " + ex.getMessage());
        }
    }

    private String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // =========================================================================
    // RECURRING TRANSACTIONS LOGIC
    // =========================================================================

    private void onAddRecurringRule() {
        try {
            String desc = recurringDescription.getText().trim();
            String cat = recurringCategory.getText().trim();
            String amtText = recurringAmount.getText().trim();
            int day = recurringDay.getValue();

            if (desc.isEmpty() || cat.isEmpty() || amtText.isEmpty()) {
                recurringStatus.setText("Please provide description, category, and amount.");
                return;
            }

            double amt = Double.parseDouble(amtText);
            if (!Double.isFinite(amt) || amt <= 0) {
                recurringStatus.setText("Amount must be a positive number.");
                return;
            }

            recurringDAO.addRule(desc, cat, recurringType.getValue(), amt, day);
            recurringDescription.clear();
            recurringCategory.clear();
            recurringAmount.clear();
            reloadRecurringRules();
            recurringStatus.setText("Recurring rule added successfully.");

        } catch (NumberFormatException e) {
            recurringStatus.setText("Invalid amount format.");
        } catch (Exception ex) {
            recurringStatus.setText("Error: " + ex.getMessage());
        }
    }

    private void onGenerateRecurring() {
        if (navMonthSelector.getValue() == null || navYearSelector.getValue() == null) return;
        int mNum = navMonthSelector.getSelectionModel().getSelectedIndex() + 1;
        String month = String.format("%04d-%02d", navYearSelector.getValue(), mNum);

        try {
            int count = recurringDAO.generateForMonth(month);
            loadTransactions();
            reloadBudgetsForNavPeriod();

            recurringStatus.setText(count == 0
                ? "All recurring rules have already been generated for " + month + "."
                : count + " recurring transaction(s) generated for " + month + ".");
        } catch (Exception ex) {
            recurringStatus.setText("Could not generate transactions: " + ex.getMessage());
        }
    }

    private void onDeleteRecurringRule() {
        RecurringTransactionDAO.Rule selected = recurringTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            recurringStatus.setText("Select a rule to delete.");
            return;
        }

        try {
            recurringDAO.deleteRule(selected.getId());
            reloadRecurringRules();
            recurringStatus.setText("Recurring rule deleted.");
        } catch (Exception ex) {
            recurringStatus.setText("Could not delete rule: " + ex.getMessage());
        }
    }

    private void reloadRecurringRules() {
        try {
            if (recurringTable != null) {
                recurringTable.setItems(FXCollections.observableArrayList(recurringDAO.getRules()));
            }
        } catch (Exception ex) {
            if (recurringStatus != null) recurringStatus.setText("Could not load recurring rules: " + ex.getMessage());
        }
    }

    // =========================================================================
    // BUDGETS & SAVINGS GOALS LOGIC
    // =========================================================================

    private void onSaveBudget() {
        try {
            String cat = budgetCategoryField.getText().trim();
            String limitText = budgetLimitField.getText().trim();

            if (cat.isEmpty() || limitText.isEmpty()) {
                budgetStatus.setText("Enter category and monthly limit.");
                return;
            }

            double limit = Double.parseDouble(limitText);
            if (!Double.isFinite(limit) || limit <= 0) {
                budgetStatus.setText("Budget limit must be positive.");
                return;
            }

            int mNum = navMonthSelector.getSelectionModel().getSelectedIndex() + 1;
            String month = String.format("%04d-%02d", navYearSelector.getValue(), mNum);

            budgetDAO.saveBudget(cat, month, limit);
            budgetCategoryField.clear();
            budgetLimitField.clear();
            reloadBudgetsForNavPeriod();
            budgetStatus.setText("Budget for " + cat + " saved.");

        } catch (NumberFormatException e) {
            budgetStatus.setText("Invalid limit amount.");
        } catch (Exception ex) {
            budgetStatus.setText("Could not save budget: " + ex.getMessage());
        }
    }

    private void onDeleteBudget() {
        BudgetDAO.BudgetSummary selected = budgetTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            budgetStatus.setText("Select a budget to delete.");
            return;
        }

        try {
            budgetDAO.deleteBudget(selected.getId());
            reloadBudgetsForNavPeriod();
            budgetStatus.setText("Budget deleted.");
        } catch (Exception ex) {
            budgetStatus.setText("Could not delete budget: " + ex.getMessage());
        }
    }

    private void reloadBudgetsForNavPeriod() {
        if (navMonthSelector == null || navYearSelector == null
            || navMonthSelector.getValue() == null || navYearSelector.getValue() == null) return;

        int mNum = navMonthSelector.getSelectionModel().getSelectedIndex() + 1;
        String month = String.format("%04d-%02d", navYearSelector.getValue(), mNum);

        try {
            List<BudgetDAO.BudgetSummary> budgets = budgetDAO.getBudgets(month);
            if (budgetTable != null) {
                budgetTable.setItems(FXCollections.observableArrayList(budgets));
            }

            long over = budgets.stream().filter(b -> b.getSpent() > b.getLimit()).count();
            long near = budgets.stream().filter(b -> b.getSpent() >= b.getLimit() * 0.8 && b.getSpent() <= b.getLimit()).count();

            if (budgetStatus != null) {
                budgetStatus.setText(String.format("Budgets active: %d | Near limit: %d | Over budget: %d", budgets.size(), near, over));
            }
        } catch (Exception ex) {
            if (budgetStatus != null) budgetStatus.setText("Could not load budgets: " + ex.getMessage());
        }
    }

    private void addSavingsGoal() {
        try {
            String name = goalNameField.getText().trim();
            String targetStr = goalTargetField.getText().trim();
            LocalDate date = goalDatePicker.getValue();

            if (name.isEmpty() || targetStr.isEmpty() || date == null) {
                goalsStatus.setText("Provide goal name, target amount, and target date.");
                return;
            }

            double target = Double.parseDouble(targetStr);
            if (!Double.isFinite(target) || target <= 0) {
                goalsStatus.setText("Target must be a positive number.");
                return;
            }

            savingsGoalDAO.addGoal(name, target, date.toString());
            goalNameField.clear();
            goalTargetField.clear();
            goalDatePicker.setValue(LocalDate.now().plusMonths(6));
            reloadSavingsGoals();
            goalsStatus.setText("Savings goal '" + name + "' created.");

        } catch (NumberFormatException e) {
            goalsStatus.setText("Enter a valid target amount.");
        } catch (Exception ex) {
            goalsStatus.setText("Could not create goal: " + ex.getMessage());
        }
    }

    private void addSavingsContribution() {
        SavingsGoal selected = goalsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            goalsStatus.setText("Select a savings goal from the list first.");
            return;
        }

        try {
            String text = contributionField.getText().trim();
            if (text.isEmpty()) {
                goalsStatus.setText("Enter a contribution amount.");
                return;
            }

            double amt = Double.parseDouble(text);
            savingsGoalDAO.addSavings(selected.getId(), amt);
            contributionField.clear();
            reloadSavingsGoals();
            goalsStatus.setText(String.format("Added ₹%.2f to %s.", amt, selected.getName()));

        } catch (NumberFormatException e) {
            goalsStatus.setText("Enter a valid numeric contribution.");
        } catch (Exception ex) {
            goalsStatus.setText("Could not add savings: " + ex.getMessage());
        }
    }

    private void deleteSavingsGoal() {
        SavingsGoal selected = goalsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            goalsStatus.setText("Select a goal to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Savings Goal");
        confirm.setHeaderText("Delete '" + selected.getName() + "'?");
        confirm.setContentText("This goal will be permanently removed.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    savingsGoalDAO.deleteGoal(selected.getId());
                    reloadSavingsGoals();
                    goalsStatus.setText("Goal deleted.");
                } catch (Exception ex) {
                    goalsStatus.setText("Could not delete goal: " + ex.getMessage());
                }
            }
        });
    }

    private void reloadSavingsGoals() {
        try {
            List<SavingsGoal> goals = savingsGoalDAO.getAllGoals();
            if (goalsList != null) {
                goalsList.setItems(FXCollections.observableArrayList(goals));
            }

            double totalTarget = 0;
            double totalSaved = 0;
            for (SavingsGoal g : goals) {
                totalTarget += g.getTargetAmount();
                totalSaved += g.getSavedAmount();
            }

            double progress = totalTarget > 0 ? Math.min(1.0, totalSaved / totalTarget) : 0;
            if (totalGoalsLabel != null) totalGoalsLabel.setText("Total Goals: " + goals.size());
            if (totalTargetLabel != null) totalTargetLabel.setText(String.format("Total Target: ₹%.2f", totalTarget));
            if (totalSavedLabel != null) totalSavedLabel.setText(String.format("Total Saved: ₹%.2f", totalSaved));
            if (overallProgressLabel != null) overallProgressLabel.setText(String.format("Overall Progress: %.1f%%  (Saved ₹%.2f of ₹%.2f total target)", progress * 100, totalSaved, totalTarget));
            if (overallProgressBar != null) overallProgressBar.setProgress(progress);

        } catch (Exception ex) {
            if (goalsStatus != null) goalsStatus.setText("Could not load goals: " + ex.getMessage());
        }
    }

    // =========================================================================
    // FINANCIAL HEALTH & COMPARISON
    // =========================================================================

    private void calculateFinancialHealth() {
        if (healthScoreButton != null) healthScoreButton.setDisable(true);
        if (healthScoreLabel != null) healthScoreLabel.setText("Evaluating...");

        try {
            List<Transaction> transactions = transactionDAO.getAll();

            double income = 0;
            double expenses = 0;
            for (Transaction t : transactions) {
                if ("INCOME".equalsIgnoreCase(t.getType())) income += t.getAmount();
                else expenses += t.getAmount();
            }

            if (income == 0 && expenses == 0) {
                if (healthScoreLabel != null) healthScoreLabel.setText("Score: N/A");
                if (healthRecommendationLabel != null) healthRecommendationLabel.setText("Record income and expense transactions to evaluate your score.");
                if (healthScoreButton != null) healthScoreButton.setDisable(false);
                return;
            }

            double score = 0;
            double savingsRate = income > 0 ? ((income - expenses) / income) * 100 : -100;

            if (income > 0) {
                score += Math.max(0, Math.min(40, savingsRate * 2));
                double expenseRatio = expenses / income;
                score += Math.max(0, Math.min(30, (1 - expenseRatio) * 30));
            }

            int mNum = navMonthSelector.getSelectionModel().getSelectedIndex() + 1;
            String month = String.format("%04d-%02d", navYearSelector.getValue(), mNum);
            List<BudgetDAO.BudgetSummary> budgets = budgetDAO.getBudgets(month);

            if (!budgets.isEmpty()) {
                long within = budgets.stream().filter(b -> b.getSpent() <= b.getLimit()).count();
                score += 30.0 * within / budgets.size();
            } else {
                score += 15;
            }

            int finalScore = (int) Math.round(Math.max(0, Math.min(100, score)));
            String rating = finalScore >= 80 ? "Excellent" : finalScore >= 60 ? "Good" : finalScore >= 40 ? "Fair" : "Needs Improvement";

            StringBuilder advice = new StringBuilder();
            advice.append("Rating: ").append(rating).append("\n");
            advice.append(String.format("Lifetime Income: ₹%.2f | Lifetime Expenses: ₹%.2f%n", income, expenses));
            advice.append(String.format("Calculated Savings Rate: %.1f%%%n", savingsRate));

            if (savingsRate < 0) {
                advice.append("• Caution: Total expenses exceed income. Review discretionary spending categories.\n");
            } else if (savingsRate < 20) {
                advice.append("• Aim for a 20%+ target savings rate by allocating savings on salary day.\n");
            } else {
                advice.append("• Strong savings discipline maintained. Consider routing surplus into your savings goals.\n");
            }

            if (budgets.isEmpty()) {
                advice.append("• Set category budgets to actively manage monthly spending ceilings.\n");
            }

            if (healthScoreLabel != null) healthScoreLabel.setText(finalScore + " / 100 — " + rating);
            if (healthRecommendationLabel != null) healthRecommendationLabel.setText(advice.toString());

        } catch (Exception ex) {
            if (healthScoreLabel != null) healthScoreLabel.setText("Score: Error");
            if (healthRecommendationLabel != null) healthRecommendationLabel.setText("Evaluation failed: " + ex.getMessage());
        } finally {
            if (healthScoreButton != null) healthScoreButton.setDisable(false);
        }
    }

    private void compareMonths() {
        if (compareMonth1.getValue() == null || compareYear1.getValue() == null
            || compareMonth2.getValue() == null || compareYear2.getValue() == null) {
            comparisonStatus.setText("Select both months to compare.");
            return;
        }

        String m1 = String.format("%04d-%02d", compareYear1.getValue(), compareMonth1.getSelectionModel().getSelectedIndex() + 1);
        String m2 = String.format("%04d-%02d", compareYear2.getValue(), compareMonth2.getSelectionModel().getSelectedIndex() + 1);

        if (m1.equals(m2)) {
            comparisonStatus.setText("Please choose two distinct months to compare.");
            return;
        }

        compareButton.setDisable(true);
        comparisonStatus.setText("Evaluating month-over-month differences...");

        executor.submit(() -> {
            try {
                JSONObject body = transactionRequest(transactionDAO.getAll());
                body.put("month1", m1);
                body.put("month2", m2);

                JSONObject result = postJson("/compare", body);
                JSONObject r1 = result.getJSONObject("month1");
                JSONObject r2 = result.getJSONObject("month2");
                JSONObject changes = result.getJSONObject("changes");

                String out = String.format(
                    "Period A (%s): Income ₹%.2f | Expenses ₹%.2f | Balance ₹%.2f%n"
                    + "Period B (%s): Income ₹%.2f | Expenses ₹%.2f | Balance ₹%.2f%n%n"
                    + "Income Change: %s%n"
                    + "Expense Change: %s%n"
                    + "Balance Change: %s%n"
                    + "Overall Trend: Expenses %s from Period A to Period B.",
                    m1, r1.optDouble("income", 0), r1.optDouble("expenses", 0), r1.optDouble("balance", 0),
                    m2, r2.optDouble("income", 0), r2.optDouble("expenses", 0), r2.optDouble("balance", 0),
                    formatChange(changes.getJSONObject("income")),
                    formatChange(changes.getJSONObject("expenses")),
                    formatChange(changes.getJSONObject("balance")),
                    result.optString("spending_trend", "unchanged")
                );

                Platform.runLater(() -> {
                    comparisonContent.setText(out);
                    comparisonStatus.setText("Comparison complete.");
                    compareButton.setDisable(false);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    comparisonStatus.setText("Comparison service offline or failed: " + ex.getMessage());
                    compareButton.setDisable(false);
                });
            }
        });
    }

    private String formatChange(JSONObject change) {
        double amt = change.optDouble("amount", 0);
        Object pct = change.opt("percentage");
        if (pct == null || pct == JSONObject.NULL) {
            return String.format("%+.2f (prior value 0)", amt);
        } else if (pct instanceof Number) {
            return String.format("%+.2f (%+.1f%%)", amt, ((Number) pct).doubleValue());
        }
        return String.format("%+.2f", amt);
    }

    // =========================================================================
    // PYTHON ANALYSIS INTEGRATION
    // =========================================================================

    private void refreshInsights() {
        if (refreshInsightsButton != null) refreshInsightsButton.setDisable(true);
        if (anomalyStatusLabel != null) anomalyStatusLabel.setText("Connecting to analysis service...");

        executor.submit(() -> {
            try {
                PythonServiceManager.ensureRunning();

                List<Transaction> transactions = transactionDAO.getAll();
                if (transactions.isEmpty()) {
                    Platform.runLater(() -> {
                        if (insightsSummaryLabel != null) insightsSummaryLabel.setText("Record transactions to generate spending insights.");
                        if (insightsAlertsLabel != null) insightsAlertsLabel.setText("");
                        if (anomalySummaryLabel != null) anomalySummaryLabel.setText("No transactions available for outlier detection.");
                        if (anomalyStatusLabel != null) anomalyStatusLabel.setText("Waiting for transactions");
                        if (refreshInsightsButton != null) refreshInsightsButton.setDisable(false);
                    });
                    return;
                }

                JSONObject req = transactionRequest(transactions);
                JSONObject insights = postJson("/insights", req);
                JSONObject anomalies = postJson("/detect-anomalies", req);

                StringBuilder sSummary = new StringBuilder();
                sSummary.append(String.format("Top Spending Category: %s (₹%.2f, %.1f%% of all expenses)%n",
                    insights.optString("top_spending_category", "None"),
                    insights.optDouble("top_category_amount", 0),
                    insights.optDouble("top_category_share_percent", 0)
                ));
                sSummary.append(String.format("Average Expense: ₹%.2f per transaction across %d records.",
                    insights.optDouble("average_expense", 0),
                    insights.optInt("transaction_count", 0)
                ));

                StringBuilder sAlerts = new StringBuilder();
                JSONArray alerts = insights.optJSONArray("alerts");
                if (alerts != null && alerts.length() > 0) {
                    for (int i = 0; i < alerts.length(); i++) {
                        sAlerts.append("• ").append(alerts.getJSONObject(i).optString("message")).append("\n");
                    }
                }

                StringBuilder aSummary = new StringBuilder();
                JSONArray found = anomalies.optJSONArray("anomalies");
                if (found == null || found.length() == 0) {
                    aSummary.append(anomalies.optString("message", "No statistical outliers detected in categories with sufficient transactions (4+)."));
                } else {
                    aSummary.append(String.format("Detected %d unusual expense outlier(s):%n", found.length()));
                    for (int i = 0; i < found.length(); i++) {
                        JSONObject item = found.getJSONObject(i);
                        aSummary.append(String.format("• %s (%s): ₹%.2f (IQR upper bound: ₹%.2f)%n",
                            item.optString("description", "Expense"),
                            item.optString("category", "General"),
                            item.optDouble("amount", 0),
                            item.optDouble("usual_upper_limit", 0)
                        ));
                    }
                }

                Platform.runLater(() -> {
                    if (insightsSummaryLabel != null) insightsSummaryLabel.setText(sSummary.toString());
                    if (insightsAlertsLabel != null) insightsAlertsLabel.setText(sAlerts.toString());
                    if (anomalySummaryLabel != null) anomalySummaryLabel.setText(aSummary.toString());
                    if (anomalyStatusLabel != null) anomalyStatusLabel.setText("Analysis updated via Python service.");
                    if (refreshInsightsButton != null) refreshInsightsButton.setDisable(false);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (anomalyStatusLabel != null) anomalyStatusLabel.setText("Python service is offline: " + ex.getMessage());
                    if (insightsSummaryLabel != null) insightsSummaryLabel.setText("Automated insights unavailable. Rule-based ledger calculations remain active.");
                    if (refreshInsightsButton != null) refreshInsightsButton.setDisable(false);
                });
            }
        });
    }

    private JSONObject transactionRequest(List<Transaction> transactions) {
        JSONObject body = new JSONObject();
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
        body.put("transactions", array);
        return body;
    }

    private JSONObject postJson(String endpoint, JSONObject body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject result;
        try {
            result = new JSONObject(response.body());
        } catch (Exception ex) {
            throw new Exception("Analysis service returned non-JSON response.");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception(result.optString("error", "HTTP " + response.statusCode()));
        }

        return result;
    }

    // =========================================================================
    // PDF REPORT EXPORT (OpenPDF)
    // =========================================================================

    private void exportMonthlyReport() {
        if (navMonthSelector.getValue() == null || navYearSelector.getValue() == null) {
            showAlert("Select a month and year first.");
            return;
        }

        int mNum = navMonthSelector.getSelectionModel().getSelectedIndex() + 1;
        String month = String.format("%04d-%02d", navYearSelector.getValue(), mNum);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Monthly Financial Report");
        chooser.setInitialFileName("FinSight_Report_" + month + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));

        File file = chooser.showSaveDialog(transactionTable.getScene().getWindow());
        if (file == null) return;

        try {
            List<Transaction> transactions = transactionDAO.getByMonth(month);
            double income = 0;
            double expenses = 0;
            Map<String, Double> categories = new TreeMap<>();

            for (Transaction t : transactions) {
                if ("INCOME".equalsIgnoreCase(t.getType())) {
                    income += t.getAmount();
                } else {
                    expenses += t.getAmount();
                    categories.merge(t.getCategory(), t.getAmount(), Double::sum);
                }
            }

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            document.add(new Paragraph("FINSIGHT — PERSONAL WEALTH JOURNAL", titleFont));
            document.add(new Paragraph("Monthly Financial Statement: " + month, headingFont));
            document.add(new Paragraph("Generated on: " + LocalDate.now(), bodyFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph(String.format("Total Income: Rs. %.2f", income), bodyFont));
            document.add(new Paragraph(String.format("Total Expenses: Rs. %.2f", expenses), bodyFont));
            document.add(new Paragraph(String.format("Net Balance: Rs. %.2f", income - expenses), bodyFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Category-wise Expenses", headingFont));
            PdfPTable catTable = new PdfPTable(2);
            catTable.setWidthPercentage(100);
            catTable.addCell(new Phrase("Category", headingFont));
            catTable.addCell(new Phrase("Amount", headingFont));

            for (Map.Entry<String, Double> e : categories.entrySet()) {
                catTable.addCell(new Phrase(e.getKey(), bodyFont));
                catTable.addCell(new Phrase(String.format("Rs. %.2f", e.getValue()), bodyFont));
            }
            document.add(catTable);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Transaction History", headingFont));
            PdfPTable txnTable = new PdfPTable(5);
            txnTable.setWidthPercentage(100);
            txnTable.setWidths(new float[]{2, 4, 3, 2, 2});
            txnTable.addCell(new Phrase("Date", headingFont));
            txnTable.addCell(new Phrase("Description", headingFont));
            txnTable.addCell(new Phrase("Category", headingFont));
            txnTable.addCell(new Phrase("Type", headingFont));
            txnTable.addCell(new Phrase("Amount", headingFont));

            for (Transaction t : transactions) {
                txnTable.addCell(new Phrase(t.getDate(), bodyFont));
                txnTable.addCell(new Phrase(t.getDescription(), bodyFont));
                txnTable.addCell(new Phrase(t.getCategory(), bodyFont));
                txnTable.addCell(new Phrase(t.getType(), bodyFont));
                txnTable.addCell(new Phrase(String.format("Rs. %.2f", t.getAmount()), bodyFont));
            }
            document.add(txnTable);

            document.close();
            showInfo("Monthly PDF statement exported successfully.");

        } catch (Exception ex) {
            showAlert("Could not export PDF statement: " + ex.getMessage());
        }
    }

    // =========================================================================
    // UI HELPERS
    // =========================================================================

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("FinSight");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("FinSight");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
