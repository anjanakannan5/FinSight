import os
import sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable, KeepTogether
)
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        self.setFont("Helvetica", 9)
        self.setFillColor(colors.HexColor("#7A6E65"))
        
        # Header (pages 2+)
        if self._pageNumber > 1:
            self.drawString(54, letter[1] - 36, "FinSight — Personal Wealth Journal | Project Documentation")
            self.setStrokeColor(colors.HexColor("#E6D9C8"))
            self.setLineWidth(0.75)
            self.line(54, letter[1] - 42, letter[0] - 54, letter[1] - 42)
        
        # Footer
        page_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(letter[0] - 54, 34, page_text)
        self.drawString(54, 34, "Confidential — FinSight Architecture & System Reference")
        self.setStrokeColor(colors.HexColor("#E6D9C8"))
        self.setLineWidth(0.75)
        self.line(54, 46, letter[0] - 54, 46)
        
        self.restoreState()

def build_pdf(filename="FinSight_Project_Documentation.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()
    
    # Custom Palette
    c_primary = colors.HexColor("#362E28")     # Deep Espresso / Text
    c_secondary = colors.HexColor("#4A4036")   # Dark Brown
    c_olive = colors.HexColor("#556038")       # Muted Olive
    c_terra = colors.HexColor("#B96F52")       # Terracotta
    c_bg_light = colors.HexColor("#FAF5ED")    # Warm linen cream
    c_border = colors.HexColor("#E6D9C8")      # Warm subtle border

    # Styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=28,
        textColor=c_primary,
        spaceAfter=4
    )

    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=12,
        leading=16,
        textColor=c_olive,
        fontStyle='italic',
        spaceAfter=15
    )

    h1_style = ParagraphStyle(
        'H1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=15,
        leading=19,
        textColor=c_primary,
        spaceBefore=14,
        spaceAfter=6,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'H2',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=16,
        textColor=c_olive,
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True
    )

    h3_style = ParagraphStyle(
        'H3',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=10,
        leading=14,
        textColor=c_secondary,
        spaceBefore=8,
        spaceAfter=3,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'Body',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=14,
        textColor=c_primary,
        spaceAfter=6
    )

    bullet_style = ParagraphStyle(
        'Bullet',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=c_primary,
        leftIndent=14,
        firstLineIndent=-10,
        spaceAfter=3
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=12,
        textColor=colors.white
    )

    table_cell_style = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=12,
        textColor=c_primary
    )

    table_cell_bold = ParagraphStyle(
        'TableCellBold',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=12,
        textColor=c_primary
    )

    story = []

    # Title Banner
    story.append(Paragraph("FinSight — Personal Wealth Journal", title_style))
    story.append(Paragraph("Comprehensive Technical Abstract, Feature Specification, Tech Stack & Dashboard Guide", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=c_olive, spaceAfter=14))

    # --- 1. ABSTRACT ---
    story.append(Paragraph("1. Project Abstract", h1_style))
    abstract_p1 = (
        "<b>FinSight</b> is an AI-powered personal financial analysis and decision-support desktop application "
        "crafted for Windows. Designed with an intentional <b>Personal Wealth Journal</b> visual identity, it rejects "
        "cluttered, generic fintech templates in favor of editorial typography, warm linen palettes, and natural information "
        "density. FinSight operates under a <b>local-first, privacy-respecting architecture</b>: all user transactions, "
        "budgets, savings goals, and recurring rules are stored offline in an embedded SQLite database residing entirely "
        "on the user's local machine, requiring no mandatory cloud registration or third-party bank credential sharing."
    )
    story.append(Paragraph(abstract_p1, body_style))

    abstract_p2 = (
        "The application integrates a fluid JavaFX desktop user interface with an embedded Python Flask analytical microservice. "
        "Through this hybrid pipeline, FinSight offers robust automated intelligence—including spending concentration alerts, "
        "statistical outlier detection using the Interquartile Range (IQR), month-over-month variance analysis, and a rule-based "
        "Financial Health Score (0–100). By blending the deliberate mindfulness of a personal journal with modern data science "
        "and offline security, FinSight delivers comprehensive financial clarity and actionable decision support."
    )
    story.append(Paragraph(abstract_p2, body_style))
    story.append(Spacer(1, 8))

    # --- 2. KEY FEATURES LIST ---
    story.append(Paragraph("2. Comprehensive Feature List", h1_style))
    
    features = [
        ("Personal Wealth Journal Design", 
         "Features a bespoke aesthetic with warm linen (#F6F0E7) and cream backgrounds, muted olive (#7D8661) and terracotta (#B96F52) accents, and a clean single-view switching architecture that eliminates dashboard clutter."),
        
        ("Bank Statement CSV Import with Pre-Validation", 
         "RFC 4180-compliant CSV parser with real-time modal preview. Validates dates (YYYY-MM-DD, DD/MM/YYYY, MM/DD/YYYY), descriptions, categories, types, and amounts. Automatically detects and skips duplicate entries against both database records and in-file repeats."),
        
        ("Income, Expense & Cash Flow Analytics", 
         "Real-time monthly financial metrics (Total Income, Total Expenses, Net Surplus/Deficit), dynamic narrative financial summary, categorical Income vs. Expense bar charts, and category-wise distribution pie charts."),
        
        ("Automated Python Financial Intelligence", 
         "Autonomous background microservice providing spending concentration alerts (e.g., single category >= 40% of expenses), statistical IQR outlier detection for unusual expenses, and month-over-month comparative analysis."),
        
        ("Monthly Category Budgeting", 
         "Set monthly spending limits per category with instant feedback badges: 'Within Budget', 'NEAR LIMIT' (>= 80%), and 'OVER BUDGET' (> 100%), alongside exact remaining balance calculations."),
        
        ("Milestone-Driven Savings Goals", 
         "Set and track target savings amounts and deadlines for specific milestones (e.g., Emergency Fund, Laptop, Travel) with individual progress bars, percentage indicators, completion badges, and an aggregate portfolio progress tracker."),
        
        ("Recurring Bills & Inflows Engine", 
         "Configure repeating monthly rules (salary, rent, utilities, subscriptions) and batch-generate them into the ledger once per month with built-in duplicate prevention."),
        
        ("Chronological Ledger & Search", 
         "Comprehensive transaction table with live full-text search across descriptions and categories, instant type filtering (All / Income / Expenses), manual entry drawer, and record deletion."),
        
        ("Document & Data Export", 
         "Generate professional monthly PDF statements with itemized tables and summary charts using OpenPDF, or export full ledger transaction records to CSV.")
    ]

    for title, desc in features:
        story.append(Paragraph(f"• <b>{title}:</b> {desc}", bullet_style))

    story.append(Spacer(1, 10))

    # --- 3. TECHNOLOGY STACK ---
    story.append(Paragraph("3. Technology Stack & Component Responsibilities", h1_style))
    story.append(Paragraph(
        "FinSight implements a modular multi-tier desktop architecture combining a native JavaFX presentation layer, "
        "an embedded relational SQLite persistence layer, and a Python Flask microservice for statistical computations.",
        body_style
    ))

    tech_data = [
        [
            Paragraph("Technology / Library", table_header_style),
            Paragraph("Version", table_header_style),
            Paragraph("Role & Architectural Responsibility", table_header_style)
        ],
        [
            Paragraph("Java (JDK)", table_cell_bold),
            Paragraph("25", table_cell_style),
            Paragraph("Core desktop application runtime, multithreading executor management, DAO transactions, and process orchestration.", table_cell_style)
        ],
        [
            Paragraph("JavaFX Controls & Graphics", table_cell_bold),
            Paragraph("21.0.2", table_cell_style),
            Paragraph("Powers the graphical user interface, Wealth Journal stylesheet styling, view-switching container, and charts.", table_cell_style)
        ],
        [
            Paragraph("SQLite 3 (sqlite-jdbc)", table_cell_bold),
            Paragraph("3.50.3.0", table_cell_style),
            Paragraph("Embedded relational database engine storing all transactions, budgets, goals, and recurring rules locally in %APPDATA%.", table_cell_style)
        ],
        [
            Paragraph("OpenPDF", table_cell_bold),
            Paragraph("2.0.3", table_cell_style),
            Paragraph("Generates structured, publication-quality monthly PDF financial statements and summary reports.", table_cell_style)
        ],
        [
            Paragraph("org.json", table_cell_bold),
            Paragraph("20250517", table_cell_style),
            Paragraph("Handles robust JSON payload serialization and deserialization for Java-Python REST microservice communication.", table_cell_style)
        ],
        [
            Paragraph("Python", table_cell_bold),
            Paragraph("3.13", table_cell_style),
            Paragraph("Backend data science runtime powering advanced statistical computations and financial intelligence.", table_cell_style)
        ],
        [
            Paragraph("Flask", table_cell_bold),
            Paragraph("3.1.3", table_cell_style),
            Paragraph("Microservice framework exposing internal REST API endpoints (/analyze, /insights, /compare, /detect-anomalies) on localhost.", table_cell_style)
        ],
        [
            Paragraph("Pandas & NumPy", table_cell_bold),
            Paragraph("Latest", table_cell_style),
            Paragraph("Data aggregation, category grouping, variance calculations, and Interquartile Range (IQR) percentile distributions.", table_cell_style)
        ],
        [
            Paragraph("Apache Maven", table_cell_bold),
            Paragraph("3.9+", table_cell_style),
            Paragraph("Build lifecycle management, dependency resolution, compilation, and automated test execution.", table_cell_style)
        ],
        [
            Paragraph("jpackage (JDK)", table_cell_bold),
            Paragraph("Built-in", table_cell_style),
            Paragraph("Packages the entire system into a standalone native Windows distribution (FinSight.exe) bundling its own runtime.", table_cell_style)
        ]
    ]

    col_widths = [115, 55, 334]
    tech_table = Table(tech_data, colWidths=col_widths, repeatRows=1)
    tech_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_olive),
        ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border),
        ('BOX', (0, 0), (-1, -1), 1, c_olive),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('LEFTPADDING', (0, 0), (-1, -1), 6),
        ('RIGHTPADDING', (0, 0), (-1, -1), 6),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, c_bg_light]),
    ]))
    story.append(tech_table)
    story.append(Spacer(1, 12))

    # --- 4. NAVBAR & DASHBOARD FEATURE BREAKDOWN ---
    story.append(Paragraph("4. Navbar & Feature Dashboard Architecture", h1_style))
    story.append(Paragraph(
        "FinSight features a spacious, two-tier navigation header designed to maximize usability and prevent visual clutter. "
        "Clicking any tab immediately swaps the active view in the central container, ensuring a focused, single-tasking experience.",
        body_style
    ))

    # Header Tiers
    story.append(Paragraph("A. Top Header Structure (Two-Tier)", h2_style))
    story.append(Paragraph(
        "• <b>Tier 1 — Brand Title & Global Controls:</b> Displays the <b>FINSIGHT</b> brand header and subtitle. "
        "Houses the global <b>Period Selector</b> (Month and Year dropdowns) which filters all metrics across the application, "
        "along with quick action buttons: <b>'📥 Import CSV'</b> (opens the statement import dialog) and <b>'+ Record Transaction'</b> "
        "(switches to the ledger view and expands the new record drawer).",
        bullet_style
    ))
    story.append(Paragraph(
        "• <b>Tier 2 — Horizontal Navigation Strip:</b> An uncrowded tab strip containing the 8 dedicated dashboard features. "
        "The active tab is highlighted with an olive accent underline, signaling the active context.",
        bullet_style
    ))
    story.append(Spacer(1, 6))

    # Detailed 8 Tabs
    story.append(Paragraph("B. Deep Dive: The 8 Navbar Feature Dashboards", h2_style))

    tab_details = [
        ("1. Overview (DashboardView.OVERVIEW)", [
            "<b>Monthly Summary Metrics:</b> Three distinct cards display Total Income (positive green), Total Expenses (terracotta), and Net Balance (surplus/deficit) for the selected period.",
            "<b>Dynamic Narrative Editorial:</b> Real-time prose explaining financial performance, savings rate, and top category expenditures.",
            "<b>Dual Synchronized Charts:</b> An Income vs. Expense monthly bar chart alongside a categorical expense breakdown pie chart.",
            "<b>Export Utilities:</b> Direct buttons to export a formal OpenPDF statement or a complete CSV file.",
            "<b>Empty-State Guidance:</b> If no transactions exist for the selected month, a dedicated onboarding banner directs the user to import a statement or add their first record."
        ]),
        
        ("2. Spending Insights & Outlier Detection (DashboardView.INSIGHTS)", [
            "<b>Spending Concentration & Highlights:</b> Identifies top expense categories, percentage of budget absorbed, and average transaction size.",
            "<b>Concentration Warnings:</b> Automated alerts triggering whenever any single category consumes >= 40% of monthly spending or when overall expenses surpass income.",
            "<b>IQR Anomaly Detection:</b> Leverages Interquartile Range statistical filtering across categories with >= 4 transactions to detect and explain unusual, statistically outlying purchases.",
            "<b>Microservice Health & Sync:</b> Asynchronously communicates with Python Flask with status monitoring and manual refresh triggers."
        ]),

        ("3. Monthly Budgets (DashboardView.BUDGETS)", [
            "<b>Category Spending Ceilings:</b> Set targeted monthly caps per category for the active month/year.",
            "<b>Live Tracking Table:</b> Displays Category, Monthly Limit, Spent Amount, and Remaining Balance.",
            "<b>Dynamic Status Badges:</b> Categorizes spending health into 'Within Budget' (green), 'NEAR LIMIT' (amber for >= 80%), and 'OVER BUDGET' (bold red for > 100%).",
            "<b>Budget Management:</b> Intuitive controls to adjust limits or remove stale budget rules."
        ]),

        ("4. Savings Goals & Milestones (DashboardView.SAVINGS_GOALS)", [
            "<b>Portfolio Summary:</b> Tracks overall metrics including Total Goals, Total Target Capital, Total Saved, and Aggregate Portfolio Progress.",
            "<b>Goal Definition:</b> Create milestone targets with a designated name, target amount (₹), and target completion date.",
            "<b>Progress Cards:</b> Individual milestone cards rendering visual progress bars, percentage indicators, remaining amounts, and '✓ COMPLETED' status badges.",
            "<b>Deposit Contributions:</b> Simple inline contribution form to deposit incremental savings directly into selected goals."
        ]),

        ("5. Recurring Bills & Inflows (DashboardView.RECURRING)", [
            "<b>Rule Definition Engine:</b> Configure fixed repeating transactions (e.g., Salary, Apartment Rent, Subscriptions, Utilities) with category, type (INCOME/EXPENSE), amount, and day of the month.",
            "<b>Batch Generation:</b> One-click '⚡ Generate for Active Period' button that automatically writes all active recurring entries into the ledger.",
            "<b>Duplicate Prevention:</b> Dedicated SQLite tracking table ('recurring_generated') prevents accidental double-generation within the same calendar month."
        ]),

        ("6. Month-to-Month Comparison (DashboardView.COMPARISON)", [
            "<b>Dual-Period Selection:</b> Allows independent selection of Month A and Month B for comparative benchmarking.",
            "<b>Variance Analytics:</b> Calculates absolute currency changes and percentage variances for income, expenses, and net balance.",
            "<b>Spending Trend Indicator:</b> Evaluates whether overall spending trend has increased, decreased, or held steady between periods."
        ]),

        ("7. Financial Health Score (DashboardView.HEALTH_SCORE)", [
            "<b>Rule-Based Evaluation:</b> Computes a comprehensive 0–100 financial health index based on savings rate, debt/expense ratio, and budget discipline.",
            "<b>Categorical Tiers:</b> Classifies results into actionable tiers (Needs Attention, Fair, Good, Excellent).",
            "<b>Tailored Financial Advice:</b> Delivers customized guidance on improving savings cushions, rebalancing expenses, and sustaining budgetary compliance."
        ]),

        ("8. Transaction Ledger (DashboardView.LEDGER)", [
            "<b>Complete Historical Ledger:</b> Chronological, sortable table detailing Date, Description, Category, Type, and Amount (₹).",
            "<b>Real-Time Search & Filtering:</b> Live keyword filtering across descriptions/categories, and dropdown filtering by type (All, Income, Expenses).",
            "<b>Manual Transaction Drawer:</b> Collapsible form to quickly record one-off income or expense items.",
            "<b>Batch Actions:</b> Quick access to CSV statement import, deletion of selected rows, and full CSV export."
        ])
    ]

    for tab_title, bullet_points in tab_details:
        tab_box = []
        tab_box.append(Paragraph(tab_title, h3_style))
        for point in bullet_points:
            tab_box.append(Paragraph(f"• {point}", bullet_style))
        tab_box.append(Spacer(1, 4))
        story.append(KeepTogether(tab_box))

    # Build Document
    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Successfully generated PDF: {filename}")

if __name__ == "__main__":
    out_pdf = "FinSight_Project_Documentation.pdf"
    build_pdf(out_pdf)
