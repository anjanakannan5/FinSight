
from flask import Flask, jsonify
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

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=False)
