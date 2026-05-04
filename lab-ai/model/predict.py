"""
Inference wrapper: takes a dict of {test_id: value} and returns the structured
analysis the API responds with.

The "interpretation" combines two signals:
  1. Per-test reference-range classification (low / normal / high + severity)
  2. ML classifier prediction over the full panel (condition + probability)
"""

from __future__ import annotations

import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Optional

import joblib
import numpy as np

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
from reference_ranges import (   # noqa: E402
    FEATURE_COLUMNS, RANGE_BY_ID, classify_value, severity,
)

MODEL_PATH = ROOT / "model" / "lab_model.pkl"

CONDITION_LABELS = {
    "HEALTHY":           "Healthy panel",
    "HYPERLIPIDEMIA":    "Pattern consistent with high cholesterol",
    "ANEMIA":            "Pattern consistent with anemia",
    "INFECTION":         "Pattern consistent with infection / inflammation",
    "DIABETES":          "Pattern consistent with elevated blood sugar",
    "LIVER_DYSFUNCTION": "Pattern consistent with liver-enzyme elevation",
}

CONDITION_PLAIN_EXPLANATION = {
    "HEALTHY":           "All measured values are within their expected reference ranges.",
    "HYPERLIPIDEMIA":    "Cholesterol-related markers are higher than recommended. Diet, exercise and follow-up testing are usually the next step.",
    "ANEMIA":            "Red-cell markers (hemoglobin / hematocrit) are below the expected range, which can cause fatigue.",
    "INFECTION":         "White blood cell count is elevated, which often happens during an active infection or inflammation.",
    "DIABETES":          "Fasting glucose is higher than expected. A follow-up test (HbA1c) is usually recommended.",
    "LIVER_DYSFUNCTION": "Liver enzymes (ALT/AST) are elevated. Causes range from medication side effects to viral hepatitis — a clinician should review.",
}

CONDITION_NEXT_STEPS = {
    "HEALTHY":           "Continue routine annual check-ups.",
    "HYPERLIPIDEMIA":    "Discuss lifestyle changes with your doctor; recheck lipid panel in 3 months.",
    "ANEMIA":            "Schedule a doctor visit to investigate the cause (iron, B12, blood loss).",
    "INFECTION":         "If you have fever or feel unwell, see a doctor promptly.",
    "DIABETES":          "Schedule an HbA1c test and a follow-up with a primary-care doctor.",
    "LIVER_DYSFUNCTION": "Avoid alcohol and unnecessary medications until reviewed by a doctor.",
}

URGENCY_OVERRIDE_RULES = (
    # (test_id, threshold_kind, threshold, urgency)
    ("glucose",  "high", 250,   "URGENT"),     # severe hyperglycemia
    ("glucose",  "low",   55,   "URGENT"),     # severe hypoglycemia
    ("hemoglobin", "low",  8,   "URGENT"),     # severe anemia
    ("wbc",      "high", 20,    "URGENT"),     # severe leukocytosis
    ("wbc",      "low",   2,    "URGENT"),     # severe leukopenia
    ("alt",      "high", 200,   "URGENT"),
    ("ast",      "high", 200,   "URGENT"),
    ("creatinine", "high", 3,   "URGENT"),
)


@dataclass
class Finding:
    test: str
    test_id: str
    value: Optional[float]
    unit: str
    expected_low: float
    expected_high: float
    status: str          # 'low' | 'normal' | 'high' | 'unknown'
    severity: str        # 'normal' | 'mild' | 'moderate' | 'severe' | 'unknown'


@dataclass
class Analysis:
    extractedValues: dict
    abnormalFindings: list[dict]
    normalFindings: list[dict]
    missingTests: list[str]
    predictedCondition: str
    predictedConditionLabel: str
    confidence: float
    classProbabilities: dict
    summary: str
    explanation: str
    nextSteps: str
    urgency: str               # 'ROUTINE' | 'PROMPT' | 'URGENT'
    disclaimer: str


_DISCLAIMER = (
    "AI-generated educational summary based on a small synthetic-data model. "
    "It is not a medical diagnosis. Please discuss results with a clinician."
)


def _load_model():
    if not MODEL_PATH.exists():
        raise FileNotFoundError(
            f"Model not found at {MODEL_PATH}. Run `python model/train.py` first."
        )
    return joblib.load(MODEL_PATH)


def _build_finding(test_id: str, value: Optional[float]) -> Finding:
    r = RANGE_BY_ID[test_id]
    return Finding(
        test=r.label,
        test_id=test_id,
        value=value,
        unit=r.unit,
        expected_low=r.low,
        expected_high=r.high,
        status=classify_value(test_id, value) if value is not None else "unknown",
        severity=severity(test_id, value) if value is not None else "unknown",
    )


def _decide_urgency(findings: list[Finding]) -> str:
    """Combine severity + dangerous-threshold rules into one urgency level."""
    for test_id, kind, threshold, urgency in URGENCY_OVERRIDE_RULES:
        for f in findings:
            if f.test_id != test_id or f.value is None:
                continue
            if kind == "high" and f.value >= threshold:
                return urgency
            if kind == "low" and f.value <= threshold:
                return urgency
    if any(f.severity == "severe" for f in findings):
        return "URGENT"
    if any(f.severity == "moderate" for f in findings):
        return "PROMPT"
    return "ROUTINE"


def analyze(values: dict[str, float]) -> Analysis:
    """
    `values` is {test_id: numeric value}. Missing keys are accepted; the
    classifier still runs by imputing the midpoint of the normal range, but
    we mention the missing tests back to the caller so they can flag them.
    """
    pipe = _load_model()

    findings = [_build_finding(t, values.get(t)) for t in FEATURE_COLUMNS]
    abnormal = [f for f in findings if f.status in ("low", "high")]
    normal   = [f for f in findings if f.status == "normal"]
    missing  = [RANGE_BY_ID[f.test_id].label for f in findings if f.value is None]

    # For prediction, impute missing values with the midpoint
    feature_vec = []
    for f in findings:
        if f.value is None:
            r = RANGE_BY_ID[f.test_id]
            feature_vec.append((r.low + r.high) / 2)
        else:
            feature_vec.append(f.value)
    X = np.asarray(feature_vec, dtype=float).reshape(1, -1)

    proba = pipe.predict_proba(X)[0]
    classes = list(pipe.classes_)
    pred_idx = int(np.argmax(proba))
    predicted = classes[pred_idx]
    confidence = float(proba[pred_idx])
    class_probs = {cls: float(p) for cls, p in zip(classes, proba)}

    urgency = _decide_urgency(findings)

    summary = (
        f"{CONDITION_LABELS.get(predicted, predicted)} "
        f"(model confidence {confidence*100:.0f}%)."
    )
    if abnormal:
        # Summarise top 3 abnormal values inline
        top = sorted(abnormal, key=lambda f: {"severe": 0, "moderate": 1, "mild": 2}.get(f.severity, 3))[:3]
        summary += " Notable findings: " + ", ".join(
            f"{f.test} {f.value} {f.unit} ({f.status})" for f in top
        ) + "."

    return Analysis(
        extractedValues={f.test_id: f.value for f in findings if f.value is not None},
        abnormalFindings=[_finding_dict(f) for f in abnormal],
        normalFindings=[_finding_dict(f) for f in normal],
        missingTests=missing,
        predictedCondition=predicted,
        predictedConditionLabel=CONDITION_LABELS.get(predicted, predicted),
        confidence=round(confidence, 4),
        classProbabilities={k: round(v, 4) for k, v in class_probs.items()},
        summary=summary,
        explanation=CONDITION_PLAIN_EXPLANATION.get(predicted, ""),
        nextSteps=CONDITION_NEXT_STEPS.get(predicted, ""),
        urgency=urgency,
        disclaimer=_DISCLAIMER,
    )


def _finding_dict(f: Finding) -> dict:
    return {
        "test":         f.test,
        "testId":       f.test_id,
        "value":        f.value,
        "unit":         f.unit,
        "expectedLow":  f.expected_low,
        "expectedHigh": f.expected_high,
        "status":       f.status,
        "severity":     f.severity,
    }


def analysis_to_dict(a: Analysis) -> dict:
    return asdict(a)
