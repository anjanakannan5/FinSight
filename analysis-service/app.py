
from flask import Flask, request, jsonify

app = Flask(__name__)


def validate_transactions(data):
    if not isinstance(data, dict) or not isinstance(data.get("transactions"), list):
        return None, "A transactions list is required"

    transactions = data["transactions"]

    for t in transactions:
        if not isinstance(t, dict):
            return None, "Each transaction must be an object"

        if not all(k in t for k in ["type", "amount", "category"]):
            return None, "Each transaction needs type, amount and category"

        if t["type"] not in ["INCOME", "EXPENSE"]:
            return None, "Type must be INCOME or EXPENSE"

        if isinstance(t["amount"], bool):
            return None, "Amount must be a number"

        try:
            amount = float(t["amount"])
        except (ValueError, TypeError):
            return None, "Amount must be a number"

        if amount <= 0:
            return None, "Amount must be positive"

        if not isinstance(t["category"], str) or not t["category"].strip():
            return None, "Category cannot be empty"

        t["amount"] = amount

    return transactions, None


def calculate_totals(transactions):
    income = sum(t["amount"] for t in transactions if t["type"] == "INCOME")
    expenses = sum(t["amount"] for t in transactions if t["type"] == "EXPENSE")
    balance = income - expenses
    savings_rate = (balance / income * 100) if income > 0 else 0

    expense_by_category = {}

    for t in transactions:
        if t["type"] == "EXPENSE":
            category = t["category"].strip()
            expense_by_category[category] = (
                expense_by_category.get(category, 0) + t["amount"]
            )

    return income, expenses, balance, savings_rate, expense_by_category


@app.route("/", methods=["GET"])
def home():
    return jsonify({
        "message": "Finance Copilot Analysis Service is running",
        "status": "success"
    })


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "service": "Finance Copilot",
        "status": "healthy"
    })


@app.route("/analyze", methods=["POST"])
def analyze():
    transactions, error = validate_transactions(request.get_json(silent=True))

    if error:
        return jsonify({"error": error}), 400

    income, expenses, balance, savings_rate, categories = calculate_totals(transactions)

    largest_category = max(categories, key=categories.get) if categories else "None"

    return jsonify({
        "total_income": round(income, 2),
        "total_expenses": round(expenses, 2),
        "balance": round(balance, 2),
        "savings_rate_percent": round(savings_rate, 2),
        "expense_by_category": categories,
        "largest_expense_category": largest_category,
        "transaction_count": len(transactions)
    })


@app.route("/insights", methods=["POST"])
def insights():
    transactions, error = validate_transactions(request.get_json(silent=True))

    if error:
        return jsonify({"error": error}), 400

    income, expenses, balance, savings_rate, categories = calculate_totals(transactions)

    expense_transactions = [
        t for t in transactions if t["type"] == "EXPENSE"
    ]

    largest_expense = max(
        expense_transactions,
        key=lambda t: t["amount"],
        default=None
    )

    top_category = max(categories, key=categories.get) if categories else "None"

    category_shares = {
        category: round(amount / expenses * 100, 2) if expenses > 0 else 0
        for category, amount in categories.items()
    }

    alerts = []

    for category, share in category_shares.items():
        if share >= 40:
            alerts.append({
                "category": category,
                "share_percent": share,
                "message": f"{category} accounts for {share}% of your expenses"
            })

    if expenses > income and income > 0:
        alerts.append({
            "category": "Overall",
            "share_percent": None,
            "message": "Your expenses exceed your income"
        })

    return jsonify({
        "total_income": round(income, 2),
        "total_expenses": round(expenses, 2),
        "balance": round(balance, 2),
        "savings_rate_percent": round(savings_rate, 2),
        "top_spending_category": top_category,
        "top_category_amount": round(categories.get(top_category, 0), 2),
        "top_category_share_percent": category_shares.get(top_category, 0),
        "expense_by_category": categories,
        "category_shares_percent": category_shares,
        "largest_expense": {
            "description": largest_expense.get("description", "Not provided"),
            "category": largest_expense["category"],
            "amount": largest_expense["amount"]
        } if largest_expense else None,
        "average_expense": round(
            expenses / len(expense_transactions), 2
        ) if expense_transactions else 0,
        "alerts": alerts,
        "transaction_count": len(transactions)
    })


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=False)
