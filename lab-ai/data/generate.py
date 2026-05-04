"""
Synthetic lab-panel generator.

Each row has the 14 features defined in reference_ranges.py plus a label drawn
from one of 6 conditions. For each condition we shift specific values into the
abnormal range with some noise — that gives the classifier a learnable pattern.

Run as a script to produce data/synthetic_lab_data.csv.
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

import numpy as np
import pandas as pd

# Make the parent dir importable when run as a script
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from reference_ranges import RANGES, FEATURE_COLUMNS  # noqa: E402

CONDITIONS = (
    "HEALTHY",
    "HYPERLIPIDEMIA",
    "ANEMIA",
    "INFECTION",
    "DIABETES",
    "LIVER_DYSFUNCTION",
)


def _normal_panel(rng: np.random.Generator) -> dict[str, float]:
    """All values within the normal range (slightly biased toward the centre)."""
    panel = {}
    for r in RANGES:
        # Sample around the midpoint, clamp to range
        mid = (r.low + r.high) / 2
        spread = (r.high - r.low) / 5
        v = rng.normal(mid, spread)
        panel[r.test_id] = float(np.clip(v, r.low, r.high))
    return panel


def _push_high(rng: np.random.Generator, panel: dict, test_id: str, factor: float):
    """Move a single value above the upper bound by `factor` (multiplicative)."""
    from reference_ranges import RANGE_BY_ID
    r = RANGE_BY_ID[test_id]
    panel[test_id] = float(r.high * (1.0 + factor + rng.normal(0, 0.05)))


def _push_low(rng: np.random.Generator, panel: dict, test_id: str, factor: float):
    """Move a single value below the lower bound by `factor`."""
    from reference_ranges import RANGE_BY_ID
    r = RANGE_BY_ID[test_id]
    panel[test_id] = float(max(r.low * (1.0 - factor + rng.normal(0, 0.05)), 0.1))


def generate_panel(condition: str, rng: np.random.Generator) -> dict:
    panel = _normal_panel(rng)

    if condition == "HEALTHY":
        pass

    elif condition == "HYPERLIPIDEMIA":
        # Elevated total cholesterol, LDL, often triglycerides; HDL may be low
        _push_high(rng, panel, "cholesterol_total", rng.uniform(0.20, 0.60))
        _push_high(rng, panel, "ldl",               rng.uniform(0.30, 0.80))
        if rng.random() < 0.6:
            _push_high(rng, panel, "triglycerides", rng.uniform(0.20, 0.70))
        if rng.random() < 0.4:
            _push_low (rng, panel, "hdl",            rng.uniform(0.10, 0.30))

    elif condition == "ANEMIA":
        _push_low(rng, panel, "hemoglobin", rng.uniform(0.10, 0.35))
        _push_low(rng, panel, "hematocrit", rng.uniform(0.10, 0.30))
        if rng.random() < 0.5:
            _push_low(rng, panel, "rbc",     rng.uniform(0.05, 0.25))

    elif condition == "INFECTION":
        _push_high(rng, panel, "wbc", rng.uniform(0.20, 1.20))
        if rng.random() < 0.4:
            _push_high(rng, panel, "platelets", rng.uniform(0.05, 0.20))

    elif condition == "DIABETES":
        _push_high(rng, panel, "glucose", rng.uniform(0.15, 0.80))
        if rng.random() < 0.4:
            _push_high(rng, panel, "triglycerides", rng.uniform(0.10, 0.40))

    elif condition == "LIVER_DYSFUNCTION":
        _push_high(rng, panel, "alt", rng.uniform(0.30, 1.50))
        _push_high(rng, panel, "ast", rng.uniform(0.30, 1.50))
        if rng.random() < 0.3:
            _push_high(rng, panel, "bun", rng.uniform(0.10, 0.40))

    panel["condition"] = condition
    return panel


def make_dataset(n_per_class: int = 800, seed: int = 42) -> pd.DataFrame:
    rng = np.random.default_rng(seed)
    rows = []
    for cond in CONDITIONS:
        for _ in range(n_per_class):
            rows.append(generate_panel(cond, rng))
    df = pd.DataFrame(rows)
    df = df.sample(frac=1.0, random_state=seed).reset_index(drop=True)
    # Round for readability
    for col in FEATURE_COLUMNS:
        df[col] = df[col].round(1)
    return df


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--n-per-class", type=int, default=800,
                   help="rows per class (default 800 -> 4800 rows total)")
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--out", default=str(Path(__file__).parent / "synthetic_lab_data.csv"))
    args = p.parse_args()

    df = make_dataset(args.n_per_class, args.seed)
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(args.out, index=False)
    print(f"Wrote {len(df)} rows to {args.out}")
    print("\nClass balance:")
    print(df["condition"].value_counts())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
