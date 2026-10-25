# FinSight — Personal Wealth Journal

<<<<<<< HEAD
FinSight is an AI-powered personal financial analysis and decision-support desktop application crafted for Windows. Designed with an intentional **Personal Wealth Journal** visual identity, it rejects generic AI dashboard templates and fintech clones in favor of editorial typography, warm linen palettes, natural information density, and user-owned offline data privacy.
=======
# FinSight
>>>>>>> f54cb54445f08273fb07368dad8212d7152eaab9

---

## 1. Key Features

- **Personal Wealth Journal Design**:
  - Warm linen (`#F6F0E7`) and cream backgrounds with muted olive (`#7D8661`) and terracotta (`#B96F52`) accents.
  - Spacious two-tier top header:
    - **Tier 1**: Brand banner (**FINSIGHT**), reporting period selector, and global quick actions (`📥 Import CSV` and `+ Record Transaction`).
    - **Tier 2**: Horizontal navigation tabs (`Overview`, `Spending Insights`, `Budgets`, `Savings Goals`, `Recurring Bills`, `Comparison`, `Health Score`, `Transactions`).
  - Single-view dashboard: Clicking any tab refreshes the view container to display only that specific feature, eliminating visual clutter.

- **Bank Statement CSV Import with Live Preview**:
  - Full CSV preview dialog showing line-by-line validation, detected columns, and error diagnostics.
  - Flexible header detection (`date`, `description`, `category`, `type`, `amount`).
  - RFC 4180 parsing with support for quotes, commas inside descriptions, and UTF-8 BOM.
  - Automatic duplicate prevention against existing database records and in-file repeats.
  - Multi-format date parsing (`YYYY-MM-DD`, `DD/MM/YYYY`, `MM/DD/YYYY`).

- **Income, Expense & Cash Flow Analytics**:
  - Real-time monthly financial metrics (Total Income, Total Expenses, Net Surplus/Deficit).
  - Income vs. Expense monthly bar chart and Category-wise distribution pie chart.
  - Dynamic monthly narrative summary based on actual transactions.
  - Clean empty-state onboarding when no transactions have been recorded yet.

- **Automated Python Financial Intelligence**:
  - Background Python Flask microservice automatically spawned and managed by the desktop app.
  - Spending concentration warnings (e.g., single category exceeding 40% of expenses).
  - Statistical outlier & unusual expense detection using Interquartile Range (IQR).
  - Month-over-month comparative analysis with percentage variance and spending trend indicators.
  - Indicative rule-based Financial Health Score (0–100) with actionable personal finance recommendations.

- **Budgets & Long-Term Savings Goals**:
  - Category-based monthly spending limits with real-time status badges (`Within Budget`, `NEAR LIMIT`, `OVER BUDGET`).
  - Milestone-driven savings goals with visual progress bars and contribution tracking.

- **Recurring Bills & Rules**:
  - Repeating monthly rules (salary, rent, subscriptions, utility bills).
  - One-click monthly generation with built-in duplicate prevention.

- **Data Export**:
  - OpenPDF-powered monthly financial PDF statements with itemized tables and category breakdowns.
  - Full ledger export to standard CSV.

---

## 2. Technology Stack

- **Desktop UI**: Java 25, JavaFX 21 (`javafx-controls`, `javafx-graphics`)
- **Data Persistence**: SQLite 3 with JDBC (`sqlite-jdbc` 3.50.3)
- **PDF Generation**: OpenPDF 2.0.3
- **Analysis Microservice**: Python 3.13, Flask 3.1.3, Pandas, NumPy
- **Build & Packaging**: Apache Maven, JDK `jpackage` (Native Windows Self-Contained App Image)

---

## 3. Architecture & Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                       FinSight Desktop UI                   │
│                 (JavaFX / Personal Wealth Journal)          │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
       JDBC / SQLite                  HTTP / REST (JSON)
                │                             │
                ▼                             ▼
┌───────────────────────────────┐  ┌──────────────────────────┐
│      SQLite Database          │  │  Python Flask Service    │
│  %APPDATA%\FinSight\data\     │  │  (localhost:5000)        │
│  finsight.db                  │  │  IQR Outliers / Insights │
└───────────────────────────────┘  └──────────────────────────┘
```

1. **JavaFX Desktop Layer**: Drives the user interface, renders charts, handles local calculations, coordinates DAO transactions, and generates PDF/CSV exports.
2. **Persistence Layer**: All tables (`transactions`, `savings_goals`, `budgets`, `recurring_transactions`, `recurring_generated`) reside in a single per-user SQLite file.
3. **Python Service Manager**: Spawns `analysis-service/app.py` in the background upon launch and cleans it up on shutdown. If Python is unavailable, the application remains fully functional with local rule-based analytics.

---

## 4. Database Storage & Production Paths

- **Production Location**: Writable per-user Windows application directory:
  ```
  %APPDATA%\FinSight\data\finsight.db
  ```
- **Integrity**:
  - Never stores mutable data inside program directories.
  - Automatically migrates existing records from previous versions so no user data is lost.
  - Safe transactions with prepared statements and automatic rollback on failure.
- **Development Override**:
  - Set JVM property `-Dfinsight.dev=true` or `-Dfinsight.db.path=<path>` to redirect database storage.

---

## 5. CSV Import Specification

### Standard Supported Format

```csv
date,description,category,type,amount
2026-10-01,Monthly Salary,Salary,INCOME,52000
2026-10-02,Food & Grocery Supermarket,Food,EXPENSE,2450
2026-10-04,Residential Electricity Bill,Bills,EXPENSE,1380
2026-10-05,Metro Transit Card Recharge,Transport,EXPENSE,650
```

### Required Fields

| Field | Accepted Formats / Normalization | Rules |
|---|---|---|
| `date` | `YYYY-MM-DD`, `DD/MM/YYYY`, `MM/DD/YYYY` | Normalized to standard ISO `YYYY-MM-DD`. Cannot be empty. |
| `description` | Text, quoted strings with commas | Leading/trailing whitespace trimmed. Cannot be blank. |
| `category` | Text | Classification tag (e.g., `Food`, `Salary`, `Bills`). |
| `type` | `INCOME`, `EXPENSE`, `Credit`, `Debit` | Case-insensitive; normalized to `INCOME` or `EXPENSE`. |
| `amount` | Numbers, currency symbols (`₹`, `$`) | Strips currency symbols and commas. Must be positive (`> 0`). |

### Duplicate Prevention Strategy

To protect against accidental double-entry from repeated statement uploads, any record having matching `(date, description, category, type, amount)` is flagged as `Duplicate (Skipped)` in the preview window and omitted from the import batch.

---

## 6. How to Run and Build

### Prerequisites

- Windows 10/11 (64-bit)
- JDK 25 or Temurin OpenJDK 25
- Apache Maven 3.9+
- Python 3.13 with packages in `analysis-service/requirements.txt` (installed in `.venv`)

### A. Run from Source (Development)

1. Open PowerShell in the project directory.
2. Run the application:
   ```powershell
   cd desktop-app
   mvn clean javafx:run
   ```
3. Or launch via the root helper script:
   ```cmd
   run-finsight.bat
   ```

### B. Run Unit Tests

Execute the automated test suite covering CSV parsing, validation, database transactions, and duplicate handling:
```powershell
cd desktop-app
mvn test
```

### C. Build Native Windows Desktop Application (`app-image`)

Build the self-contained Windows executable bundling its own Java runtime:
```powershell
# 1. Package JAR and copy dependencies
cd desktop-app
mvn clean package -DskipTests
mvn dependency:copy-dependencies -DoutputDirectory=target/libs

# 2. Stage files for jpackage
New-Item -ItemType Directory -Force target\app-input
Copy-Item target\finsight-1.0.0.jar target\app-input\
Copy-Item target\libs\*.jar target\app-input\

# 3. Build standalone app image
Remove-Item -Recurse -Force ..\dist -ErrorAction SilentlyContinue
jpackage --type app-image --name "FinSight" --input target\app-input --main-jar finsight-1.0.0.jar --main-class com.financecopilot.Launcher --dest ..\dist

# 4. Bundle analysis service & environment into the distribution
cd ..
Copy-Item -Recurse -Force analysis-service dist\FinSight\analysis-service
Copy-Item -Recurse -Force .venv dist\FinSight\.venv
```

The resulting standalone application will be located at:
```
dist\FinSight\FinSight.exe
```

Double-click `FinSight.exe` to run the application independently without VS Code or Maven.

---

## 7. Python Analysis Service & Limitations

### Endpoints

- `GET /health` — Service heartbeat verification.
- `POST /analyze` — Aggregates income, expenses, balance, savings rate, and category distribution.
- `POST /insights` — Computes top spending categories, average expense, and spending concentration alerts.
- `POST /compare` — Compares two specific `YYYY-MM` months and provides change percentages.
- `POST /detect-anomalies` — Identifies statistical outliers per category using the Interquartile Range ($Q_3 + 1.5 \times \text{IQR}$).

### Analysis Limitations

- **Statistical Anomaly Detection**: Outlier detection relies on classical descriptive statistics (IQR). It requires at least 4 recorded transactions within an expense category to establish quartiles.
- **Rule-Based Financial Health**: The health score (0–100) is an indicative rule-based heuristic measuring savings rates and category budget adherence. It does not constitute certified financial or investment advice.

---

## 8. Optional Developer Seed Script

`seed_transactions.py` is an **optional developer tool** used during development to generate sample demo transactions.
- It is **never** executed automatically during startup.
- A new installation begins with an empty database.
- To seed demo transactions explicitly during development:
  ```powershell
  python seed_transactions.py
  ```
