"""
Lumen Health · Lab Analyzer (standalone Flask service)

Endpoints
---------
  GET  /                        Built-in upload UI (open in a browser to test).
  GET  /health                  { ok, modelLoaded, modelMetrics }
  POST /api/analyze             Upload a lab-report PDF (multipart 'file') →
                                JSON analysis (extracted values + ML
                                prediction + per-value flags + summary).
  POST /api/analyze-values      Send raw {test_id: value} JSON (skip PDF
                                parsing) — handy for tests.
  GET  /api/samples             List sample PDFs bundled with the service.
  GET  /api/samples/<name>      Download a sample PDF.

Run
---
  python -m venv .venv && .venv/Scripts/activate    (Windows)
  pip install -r requirements.txt
  python setup.py                                   # train + generate samples
  python app.py                                     # serve on :5001
"""

from __future__ import annotations

import json
import os
from pathlib import Path

from flask import (
    Flask, jsonify, render_template, request, send_from_directory,
)
from flask_cors import CORS

from model.predict import analyze, analysis_to_dict, MODEL_PATH
from pdf.parser import parse_pdf

ROOT = Path(__file__).resolve().parent
SAMPLES_DIR = ROOT / "sample_pdfs"
METRICS_PATH = ROOT / "model" / "training_metrics.json"

app = Flask(__name__, template_folder=str(ROOT / "templates"),
            static_folder=str(ROOT / "static"))
CORS(app, resources={r"/*": {"origins": "*"}})  # demo: allow any origin

app.config["MAX_CONTENT_LENGTH"] = 10 * 1024 * 1024  # 10 MB upload limit


# -------------------------------------------------------------------- routes --

@app.route("/")
def index():
    samples = _samples_listing()
    return render_template("index.html", samples=samples)


@app.route("/health")
def health():
    return jsonify({
        "ok": True,
        "service": "lumen-lab-analyzer",
        "modelLoaded": MODEL_PATH.exists(),
        "modelMetrics": _load_metrics(),
    })


@app.route("/api/analyze", methods=["POST"])
def api_analyze():
    file = request.files.get("file")
    if file is None or file.filename == "":
        return jsonify({"error": "No file uploaded. POST a multipart form with field 'file'."}), 400
    if not file.filename.lower().endswith(".pdf"):
        return jsonify({"error": "Only PDF files are supported."}), 415

    try:
        raw_bytes = file.read()
        values, raw_text = parse_pdf(raw_bytes)
    except Exception as e:
        return jsonify({"error": f"Failed to parse PDF: {e}"}), 422

    if not values:
        return jsonify({
            "error": "Could not recognise any lab values in this PDF.",
            "rawText": raw_text[:2000],
        }), 422

    try:
        analysis = analyze(values)
    except FileNotFoundError as e:
        return jsonify({"error": str(e)}), 503

    payload = analysis_to_dict(analysis)
    payload["filename"] = file.filename
    payload["rawText"] = raw_text[:4000]
    return jsonify(payload)


@app.route("/api/analyze-values", methods=["POST"])
def api_analyze_values():
    body = request.get_json(silent=True) or {}
    values = body.get("values") or body
    if not isinstance(values, dict):
        return jsonify({"error": "Body must be a JSON object {test_id: value}."}), 400

    try:
        cleaned = {k: float(v) for k, v in values.items() if v is not None}
    except (TypeError, ValueError) as e:
        return jsonify({"error": f"Non-numeric value in input: {e}"}), 400

    try:
        analysis = analyze(cleaned)
    except FileNotFoundError as e:
        return jsonify({"error": str(e)}), 503
    return jsonify(analysis_to_dict(analysis))


@app.route("/api/samples")
def api_samples():
    return jsonify(_samples_listing())


@app.route("/api/samples/<path:name>")
def api_sample_download(name):
    if not name.endswith(".pdf"):
        return jsonify({"error": "samples are PDFs"}), 404
    return send_from_directory(SAMPLES_DIR, name, as_attachment=False)


# -------------------------------------------------------------------- helpers --

def _samples_listing() -> list[dict]:
    if not SAMPLES_DIR.exists():
        return []
    items = []
    for path in sorted(SAMPLES_DIR.glob("*.pdf")):
        items.append({
            "name":  path.name,
            "title": path.stem.replace("_", " ").title(),
            "url":   f"/api/samples/{path.name}",
            "size":  path.stat().st_size,
        })
    return items


def _load_metrics() -> dict | None:
    if not METRICS_PATH.exists():
        return None
    try:
        with open(METRICS_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)
        return {
            "accuracy": data.get("accuracy"),
            "labels": data.get("labels"),
            "nTrain": data.get("n_train"),
            "nTest": data.get("n_test"),
        }
    except Exception:
        return None


# ------------------------------------------------------------------- bootstrap

if __name__ == "__main__":
    port = int(os.environ.get("PORT", "5001"))
    app.run(host="127.0.0.1", port=port, debug=False)
