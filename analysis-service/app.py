
from flask import Flask, jsonify, request
import pandas as pd

app = Flask(__name__)

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
    data = request.get_json(silent=True)

    if not isinstance(data, dict) or not isinstance(data.get("transactions"), list):
        return jsonify({"error": "Provide a transactions list"}), 400

    transactions = data["transactions"]

    if not transactions:
        return jsonify({"error": "Add at least one transaction"}), 400

    try:
        df = pd.DataFrame(transactions)
        required = {"type", "amount", "category"}

        if not required.issubset(df.columns):
            return jsonify({
                "error": "Each transaction needs type, amount and category"
            }), 400

        df["type"] = df["type"].astype(str).str.upper()
        df["amount"] = pd.to_numeric(df["amount"], errors="raise")
        df["category"] = df["category"].astype(str).str.strip()

        if (
            not df["type"].isin(["INCOME", "EXPENSE"]).all()
            or not df["amount"].map(
                lambda amount: pd.notna(amount) and amount > 0
            ).all()
            or (df["category"] == "").any()
        ):
            return jsonify({
                "error": "Use INCOME or EXPENSE, positive amounts and valid categories"
            }), 400

        income = float(df.loc[df["type"] == "INCOME", "amount"].sum())
        expenses = float(df.loc[df["type"] == "EXPENSE", "amount"].sum())
        balance = income - expenses
        savings_rate = (balance / income * 100) if income > 0 else 0.0

        expense_df = df[df["type"] == "EXPENSE"]
        category_totals = (
            expense_df.groupby("category")["amount"]
            .sum()
            .sort_values(ascending=False)
        )

        category_breakdown = {
            category: round(float(amount), 2)
            for category, amount in category_totals.items()
        }

        largest_category = (
            category_totals.index[0] if not category_totals.empty else None
        )

        return jsonify({
            "total_income": round(income, 2),
            "total_expenses": round(expenses, 2),
            "balance": round(balance, 2),
            "savings_rate_percent": round(savings_rate, 2),
            "expense_by_category": category_breakdown,
            "largest_expense_category": largest_category,
            "transaction_count": len(df)
        })

    except (ValueError, TypeError):
        return jsonify({
            "error": "Invalid transaction data. Amounts must be valid numbers."
        }), 400

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=False)
