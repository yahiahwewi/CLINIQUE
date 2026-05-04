"""
Train a RandomForest classifier on the synthetic lab dataset.

Usage:
    python model/train.py [--data data/synthetic_lab_data.csv] [--out model/lab_model.pkl]

If the dataset doesn't exist yet, this script invokes the generator first.
The trained pipeline (scaler + classifier) is persisted with joblib so the
Flask app can load it on startup without retraining.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

# Make project root importable when run as a script
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
from reference_ranges import FEATURE_COLUMNS  # noqa: E402


def ensure_dataset(path: Path) -> pd.DataFrame:
    if not path.exists():
        print(f"[train] dataset missing — generating it now")
        from data.generate import make_dataset
        df = make_dataset()
        path.parent.mkdir(parents=True, exist_ok=True)
        df.to_csv(path, index=False)
        print(f"[train] wrote {len(df)} rows to {path}")
    else:
        df = pd.read_csv(path)
    return df


def train(df: pd.DataFrame, seed: int = 42) -> tuple[Pipeline, dict]:
    X = df[FEATURE_COLUMNS].values
    y = df["condition"].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, stratify=y, random_state=seed
    )

    pipe = Pipeline([
        ("scaler", StandardScaler()),
        ("clf",    RandomForestClassifier(
            n_estimators=200,
            max_depth=None,
            min_samples_leaf=2,
            class_weight="balanced",
            random_state=seed,
            n_jobs=-1,
        )),
    ])
    pipe.fit(X_train, y_train)

    y_pred = pipe.predict(X_test)
    accuracy = float(np.mean(y_pred == y_test))
    report = classification_report(y_test, y_pred, output_dict=True)
    matrix = confusion_matrix(y_test, y_pred, labels=sorted(set(y))).tolist()

    print(f"[train] accuracy on hold-out: {accuracy:.4f}")
    print("\n" + classification_report(y_test, y_pred))

    metrics = {
        "accuracy": accuracy,
        "feature_columns": FEATURE_COLUMNS,
        "labels": sorted(set(y)),
        "confusion_matrix": matrix,
        "classification_report": report,
        "n_train": len(X_train),
        "n_test": len(X_test),
    }
    return pipe, metrics


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--data", default=str(ROOT / "data" / "synthetic_lab_data.csv"))
    p.add_argument("--out",  default=str(ROOT / "model" / "lab_model.pkl"))
    p.add_argument("--metrics-out", default=str(ROOT / "model" / "training_metrics.json"))
    args = p.parse_args()

    df = ensure_dataset(Path(args.data))
    pipe, metrics = train(df)

    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipe, args.out)
    print(f"[train] model saved to {args.out}")

    with open(args.metrics_out, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)
    print(f"[train] metrics saved to {args.metrics_out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
