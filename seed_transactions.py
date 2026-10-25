
import sqlite3
from pathlib import Path
from datetime import date
from calendar import monthrange

db_path = Path(__file__).parent / "data" / "finance_copilot.db"
db_path.parent.mkdir(parents=True, exist_ok=True)

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

# Remove only rows created by this script, so rerunning won't duplicate them.
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

    # A second salary is not added; this is a one-off bonus to test income.
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

print("Sample transactions added for", previous_month.strftime("%Y-%m"),
      "and", current_month.strftime("%Y-%m"))
print("Open Finance Copilot and refresh the dashboard to view them.")
