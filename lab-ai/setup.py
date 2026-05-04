"""
One-shot bootstrap script.

Runs all three offline steps the Flask app needs before it can serve traffic:
  1. Generate the synthetic CSV (data/synthetic_lab_data.csv)
  2. Train the RandomForest model (model/lab_model.pkl + training_metrics.json)
  3. Generate the sample PDFs (sample_pdfs/*.pdf)

Idempotent — re-running skips work that's already done unless --force is passed.
Run before `python app.py` on a fresh checkout.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))


def step_dataset(force: bool):
    csv = ROOT / "data" / "synthetic_lab_data.csv"
    if csv.exists() and not force:
        print(f"[setup] dataset already exists at {csv} — skip (use --force to rebuild)")
        return
    print("[setup] generating synthetic dataset…")
    from data.generate import make_dataset
    df = make_dataset()
    csv.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(csv, index=False)
    print(f"[setup] wrote {len(df)} rows to {csv}")


def step_train(force: bool):
    model_path = ROOT / "model" / "lab_model.pkl"
    if model_path.exists() and not force:
        print(f"[setup] trained model already at {model_path} — skip (use --force to retrain)")
        return
    print("[setup] training model…")
    from model.train import train, ensure_dataset
    import joblib, json
    df = ensure_dataset(ROOT / "data" / "synthetic_lab_data.csv")
    pipe, metrics = train(df)
    model_path.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipe, model_path)
    with open(ROOT / "model" / "training_metrics.json", "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)
    print(f"[setup] saved model to {model_path}")


def step_samples(force: bool):
    out_dir = ROOT / "sample_pdfs"
    if out_dir.exists() and any(out_dir.glob("*.pdf")) and not force:
        print(f"[setup] sample PDFs already exist in {out_dir} — skip (use --force)")
        return
    print("[setup] generating sample PDFs…")
    from pdf.samples import SCENARIOS, render
    out_dir.mkdir(parents=True, exist_ok=True)
    for filename, scenario in SCENARIOS.items():
        path = render(filename, scenario, out_dir)
        print(f"  · {path.name}")


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--force", action="store_true", help="rebuild even if outputs exist")
    p.add_argument("--skip-dataset",  action="store_true")
    p.add_argument("--skip-train",    action="store_true")
    p.add_argument("--skip-samples",  action="store_true")
    args = p.parse_args()

    if not args.skip_dataset:  step_dataset(args.force)
    if not args.skip_train:    step_train(args.force)
    if not args.skip_samples:  step_samples(args.force)

    print("\n[setup] done. Now run:  python app.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
