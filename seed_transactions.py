"""
FinSight — Developer / Demo Data Seeding Utility
Optional script for local testing and feature demonstration.
Note: This is strictly an optional developer tool and is NEVER executed
automatically during application startup.
"""

import sqlite3
import os
import sys
from pathlib import Path
from datetime import date
from calendar import monthrange

# Determine target database:
# 1. Command-line argument: --db <path>
# 2. Environment variable: FINSIGHT_DB
# 3. User production AppData path if exists: %APPDATA%\FinSight\data\finsight.db
# 4. Project workspace fallback: ./data/finsight.db

db_path = None
if len(sys.argv) > 2 and sys.argv[1] == "--db":
    db_path = Path(sys.argv[2])
elif os.environ.get("FINSIGHT_DB"):
    db_path = Path(os.environ["FINSIGHT_DB"])
elif os.environ.get("FINANCE_COPILOT_DB"):
    db_path = Path(os.environ["FINANCE_COPILOT_DB"])
else:
    app_data = os.environ.get("APPDATA")
    if app_data and (Path(app_data) / "FinSight" / "data" / "finsight.db").exists():
        db_path = Path(app_data) / "FinSight" / "data" / "finsight.db"
    elif app_data and (Path(app_data) / "FinanceCopilot" / "data" / "finance_copilot.db").exists():
        db_path = Path(app_data) / "FinanceCopilot" / "data" / "finance_copilot.db"
    else:
        db_path = Path(__file__).parent / "data" / "finsight.db"

db_path.parent.mkdir(parents=True, exist_ok=True)
print(f"Targeting database: {db_path.resolve()}")

today = date.today()
current_month = today.replace(day=1)
previous_month = (current_month.replace(day=1)
                  .fromordinal(current_month.toordinal() - 1))
previous_month = previous_month.replace(day=1)

def transaction_date(month, day):
    day = min(day, monthrange(month.year, month.month)[1])
    return month.replace(day=day).isoformat()

samples = [
    ("Monthly Salary", "Salary", "INCOME", 45000, 1),
    ("Freelance Website", "Freelance", "INCOME", 6500, 5),
    ("Food Groceries", "Food", "EXPENSE", 1800, 2),
    ("Weekly Groceries", "Food", "EXPENSE", 950, 8),
    ("Restaurant Dinner", "Food", "EXPENSE", 750, 12),
    ("Lunch", "Food", "EXPENSE", 280, 15),
    ("Large Grocery Purchase", "Food", "EXPENSE", 5200, 18),
    ("Bus Pass", "Transport", "EXPENSE", 650, 3),
    ("Cab Ride", "Transport", "EXPENSE", 420, 7),
    ("Fuel", "Transport", "EXPENSE", 1200, 14),
    ("Train Ticket", "Transport", "EXPENSE", 850, 20),
    ("Electricity Bill", "Bills", "EXPENSE", 1400, 4),
    ("Internet Bill", "Bills", "EXPENSE", 799, 6),
    ("Mobile Recharge", "Bills", "EXPENSE", 399, 11),
    ("Streaming Subscription", "Entertainment", "EXPENSE", 299, 9),
    ("Movie Tickets", "Entertainment", "EXPENSE", 650, 16),
    ("Online Shopping", "Shopping", "EXPENSE", 1700, 10),
    ("Clothing", "Shopping", "EXPENSE", 2300, 21),
    ("Pharmacy", "Health", "EXPENSE", 480, 13),
    ("Doctor Visit", "Health", "EXPENSE", 900, 19),
    ("Course Subscription", "Education", "EXPENSE", 1200, 17),
    ("College Supplies", "Education", "EXPENSE", 550, 22),
    ("Coffee", "Food", "EXPENSE", 180, 23),
    ("Snacks", "Food", "EXPENSE", 120, 24),
    ("Gift", "Shopping", "EXPENSE", 1000, 25),
    ("Water Bill", "Bills", "EXPENSE", 350, 26),
]

conn = sqlite3.connect(db_path)
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    description TEXT NOT NULL,
    category TEXT NOT NULL,
    type TEXT NOT NULL,
    amount REAL NOT NULL,
    date TEXT NOT NULL
)
""")

# Remove only rows created by this demo script, so rerunning won't duplicate them.
cur.execute("""
DELETE FROM transactions
WHERE description LIKE 'DEMO-%'
""")

for month in (previous_month, current_month):
    for description, category, kind, amount, day in samples:
        cur.execute("""
            INSERT INTO transactions
            (description, category, type, amount, date)
            VALUES (?, ?, ?, ?, ?)
        """, (
            "DEMO-" + description,
            category,
            kind,
            amount,
            transaction_date(month, day)
        ))

    # One-off bonus for testing income variance
    cur.execute("""
        INSERT INTO transactions
        (description, category, type, amount, date)
        VALUES (?, ?, ?, ?, ?)
    """, (
        "DEMO-Performance Bonus",
        "Salary",
        "INCOME",
        3000,
        transaction_date(month, 26)
    ))

conn.commit()
conn.close()

print("Sample demo transactions added for", previous_month.strftime("%Y-%m"),
      "and", current_month.strftime("%Y-%m"))
print("Open FinSight to view them.")
