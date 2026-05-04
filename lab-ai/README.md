# Lumen Health · Lab Analyzer (`lab-ai/`)

A **standalone** ML service that takes a lab-report PDF and returns a structured
analysis: per-value flags, a panel-level prediction (random-forest classifier
trained on synthetic data), and a plain-language summary.

It is independent of the Spring backend — runs on its own port, talks to its
own files, and ships a built-in browser UI for testing.

```
lab-ai/
├── app.py                # Flask service
├── setup.py              # one-shot bootstrap (data → train → samples)
├── reference_ranges.py   # adult reference ranges per test
├── requirements.txt
├── data/
│   ├── generate.py       # synthetic-dataset generator
│   └── synthetic_lab_data.csv  (generated)
├── model/
│   ├── train.py          # RandomForest training
│   ├── predict.py        # inference wrapper combining ML + ref-range checks
│   ├── lab_model.pkl              (generated)
│   └── training_metrics.json      (generated)
├── pdf/
│   ├── samples.py        # reportlab sample-PDF generator
│   └── parser.py         # pdfplumber text → {test_id: value}
├── sample_pdfs/          # generated lab reports for testing
└── templates/index.html  # built-in upload UI
```

## Quickstart

```bash
cd lab-ai

# 1. virtual env (recommended)
python -m venv .venv
# Windows:
.venv\Scripts\activate
# Linux/macOS:
source .venv/bin/activate

# 2. install
pip install -r requirements.txt

# 3. bootstrap (synthetic data + train + sample PDFs) — takes ~10s
python setup.py

# 4. run
python app.py
```

Open <http://127.0.0.1:5001> in a browser. Drop one of the sample PDFs (or
upload your own) and you'll see the analysis.

## What the model does

1. **Extracts values** from the uploaded PDF (`pdfplumber` + alias-based regex).
2. **Classifies the panel** with a `RandomForestClassifier` trained on ~4800
   synthetic patients, six labels:
   `HEALTHY`, `HYPERLIPIDEMIA`, `ANEMIA`, `INFECTION`, `DIABETES`,
   `LIVER_DYSFUNCTION`.
3. **Per-value flagging** against textbook reference ranges, with a severity
   classification (`mild`, `moderate`, `severe`).
4. **Urgency overrides** for dangerous values (e.g. glucose > 250, hemoglobin
   < 8) → forces `URGENT` regardless of model prediction.

## API

| Method | Path | Body | Returns |
|---|---|---|---|
| `GET`  | `/`                         | — | Built-in upload UI |
| `GET`  | `/health`                   | — | `{ ok, modelLoaded, modelMetrics }` |
| `POST` | `/api/analyze`              | multipart `file=<PDF>` | full analysis JSON |
| `POST` | `/api/analyze-values`       | JSON `{ "values": { glucose: 145, ... } }` | analysis JSON (no PDF parsing) |
| `GET`  | `/api/samples`              | — | list of bundled sample PDFs |
| `GET`  | `/api/samples/<name>`       | — | download a sample PDF |

### Example with cURL

```bash
# Health check
curl http://127.0.0.1:5001/health | jq

# Analyze a sample PDF
curl -F "file=@sample_pdfs/high_cholesterol.pdf" \
     http://127.0.0.1:5001/api/analyze | jq

# Analyze raw values (skip PDF parsing)
curl -X POST -H "Content-Type: application/json" \
     -d '{"values":{"glucose":145,"cholesterol_total":274,"ldl":198,"hdl":38}}' \
     http://127.0.0.1:5001/api/analyze-values | jq
```

### Response shape

```jsonc
{
  "extractedValues":   { "glucose": 95, "cholesterol_total": 274, ... },
  "abnormalFindings":  [ { "test": "Total Cholesterol", "value": 274, "unit": "mg/dL",
                           "expectedLow": 125, "expectedHigh": 200,
                           "status": "high", "severity": "moderate" }, ... ],
  "normalFindings":    [ ... ],
  "missingTests":      [],
  "predictedCondition": "HYPERLIPIDEMIA",
  "predictedConditionLabel": "Pattern consistent with high cholesterol",
  "confidence": 0.91,
  "classProbabilities": {
    "HEALTHY": 0.02, "HYPERLIPIDEMIA": 0.91, "ANEMIA": 0.01, ...
  },
  "summary":     "Pattern consistent with high cholesterol (model confidence 91%)…",
  "explanation": "Cholesterol-related markers are higher than recommended…",
  "nextSteps":   "Discuss lifestyle changes with your doctor; recheck lipid panel in 3 months.",
  "urgency":     "PROMPT",
  "disclaimer":  "AI-generated educational summary…",
  "filename":    "high_cholesterol.pdf",
  "rawText":     "LUMEN HEALTH · LABORATORY REPORT …"
}
```

## Sample reports

`setup.py` produces four PDFs under `sample_pdfs/`:

| File | Expected prediction |
|---|---|
| `normal_panel.pdf`        | `HEALTHY` |
| `high_cholesterol.pdf`    | `HYPERLIPIDEMIA` |
| `anemia.pdf`              | `ANEMIA` |
| `infection_markers.pdf`   | `INFECTION` |

## Limitations / educational notes

- The dataset is **synthetic** (~800 patients per class). Reference ranges are
  textbook adult values. Real clinical models need real data + ethics review +
  regulatory clearance.
- The PDF parser is pattern-based — it works on the bundled samples and most
  table-style reports, but odd layouts (scanned images, multi-column) may need
  OCR or custom parsing.
- This service intentionally does **not** call any external LLM. It's a
  classical-ML companion to the Groq-powered triage/summary that lives in the
  main hospital backend.
