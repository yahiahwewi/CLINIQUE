"""
Reference ranges for the 14 lab tests we model.

Used both:
  - as ground truth when generating synthetic patient data, and
  - at inference time to flag specific values as low / normal / high.

Sources are textbook adult ranges (US units). They are illustrative only —
this whole project is for educational/demo purposes, not clinical use.
"""

from dataclasses import dataclass


@dataclass(frozen=True)
class Range:
    test_id: str           # snake_case key used everywhere internally
    label: str             # human-friendly name shown in PDFs and UI
    unit: str
    low: float             # below this is "low"
    high: float            # above this is "high"
    # Aliases the PDF parser will look for (case-insensitive substring match)
    aliases: tuple = ()


# Order matters: this is also the feature order the model expects.
RANGES: tuple[Range, ...] = (
    Range("glucose",            "Fasting Glucose",       "mg/dL",  70,   99,  ("glucose", "fbg")),
    Range("cholesterol_total",  "Total Cholesterol",     "mg/dL", 125,  200,  ("total cholesterol", "cholesterol total")),
    Range("ldl",                "LDL Cholesterol",       "mg/dL",   0,  100,  ("ldl",)),
    Range("hdl",                "HDL Cholesterol",       "mg/dL",  40,   90,  ("hdl",)),
    Range("triglycerides",      "Triglycerides",         "mg/dL",   0,  150,  ("triglycerides", "tg")),
    Range("hemoglobin",         "Hemoglobin",            "g/dL",   12,   17,  ("hemoglobin", "hgb", "hb")),
    Range("hematocrit",         "Hematocrit",            "%",      36,   50,  ("hematocrit", "hct")),
    Range("wbc",                "White Blood Cell Count", "10^3/uL", 4,   11,  ("white blood cell", "wbc", "leukocytes")),
    Range("rbc",                "Red Blood Cell Count",   "10^6/uL", 4.2, 5.9, ("red blood cell", "rbc", "erythrocytes")),
    Range("platelets",          "Platelet Count",        "10^3/uL", 150, 400, ("platelet", "plt")),
    Range("alt",                "ALT (SGPT)",            "U/L",     7,   56,  ("alt", "sgpt")),
    Range("ast",                "AST (SGOT)",            "U/L",    10,   40,  ("ast", "sgot")),
    Range("creatinine",         "Creatinine",            "mg/dL",  0.6,  1.3, ("creatinine", "creat")),
    Range("bun",                "Blood Urea Nitrogen",   "mg/dL",   7,   20,  ("bun", "blood urea nitrogen", "urea")),
)

FEATURE_COLUMNS = [r.test_id for r in RANGES]
RANGE_BY_ID = {r.test_id: r for r in RANGES}


def classify_value(test_id: str, value: float) -> str:
    """Return one of: 'low', 'normal', 'high', 'unknown'."""
    r = RANGE_BY_ID.get(test_id)
    if r is None or value is None:
        return "unknown"
    if value < r.low:
        return "low"
    if value > r.high:
        return "high"
    return "normal"


def severity(test_id: str, value: float) -> str:
    """How far out of range is the value? -> 'mild', 'moderate', 'severe'."""
    r = RANGE_BY_ID.get(test_id)
    if r is None or value is None:
        return "unknown"
    if r.low <= value <= r.high:
        return "normal"
    # Compute fractional distance from the nearer bound
    if value < r.low:
        dist = (r.low - value) / max(r.low, 1e-9)
    else:
        dist = (value - r.high) / max(r.high, 1e-9)
    if dist < 0.10:
        return "mild"
    if dist < 0.30:
        return "moderate"
    return "severe"
