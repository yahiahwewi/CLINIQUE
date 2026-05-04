"""
Generate four sample lab-report PDFs for quick smoke-testing.

The layout intentionally mimics what a real PDF lab report looks like — header
with patient + date, then a table of 'Test | Result | Unit | Reference Range'.
The PDF parser is built against this exact format so it extracts cleanly.

Run as a script:
    python pdf/samples.py
The PDFs land in lab-ai/sample_pdfs/.
"""

from __future__ import annotations

import sys
from datetime import datetime, timedelta
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
)

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
from reference_ranges import RANGES, RANGE_BY_ID  # noqa: E402

OUT_DIR = ROOT / "sample_pdfs"

# Each scenario gives a value (or None to use a normal-ish midpoint) per test.
# Values picked to produce clear, realistic abnormal patterns the model will
# pick up.
SCENARIOS = {
    "normal_panel.pdf": {
        "title": "Routine Annual Check-Up",
        "patient": "John Doe (Age 35, Male)",
        "values": {
            "glucose": 88, "cholesterol_total": 175, "ldl": 90, "hdl": 55,
            "triglycerides": 110, "hemoglobin": 14.5, "hematocrit": 43,
            "wbc": 6.8, "rbc": 5.1, "platelets": 250, "alt": 25,
            "ast": 22, "creatinine": 0.9, "bun": 14,
        },
    },
    "high_cholesterol.pdf": {
        "title": "Lipid Panel — Follow-up",
        "patient": "Sarah Miller (Age 52, Female)",
        "values": {
            "glucose": 95, "cholesterol_total": 274, "ldl": 198, "hdl": 38,
            "triglycerides": 245, "hemoglobin": 13.2, "hematocrit": 40,
            "wbc": 7.4, "rbc": 4.6, "platelets": 280, "alt": 28,
            "ast": 24, "creatinine": 0.8, "bun": 15,
        },
    },
    "anemia.pdf": {
        "title": "Complete Blood Count (CBC)",
        "patient": "Aisha Khan (Age 28, Female)",
        "values": {
            "glucose": 84, "cholesterol_total": 180, "ldl": 95, "hdl": 60,
            "triglycerides": 100, "hemoglobin": 9.8, "hematocrit": 30,
            "wbc": 6.2, "rbc": 3.6, "platelets": 220, "alt": 20,
            "ast": 19, "creatinine": 0.7, "bun": 11,
        },
    },
    "infection_markers.pdf": {
        "title": "Acute Care Workup",
        "patient": "Marcus Lee (Age 41, Male)",
        "values": {
            "glucose": 102, "cholesterol_total": 188, "ldl": 105, "hdl": 50,
            "triglycerides": 130, "hemoglobin": 14.0, "hematocrit": 41,
            "wbc": 16.4, "rbc": 4.9, "platelets": 420, "alt": 33,
            "ast": 30, "creatinine": 1.0, "bun": 18,
        },
    },
}

LUMEN_TEAL  = colors.HexColor("#0F4C5C")
LUMEN_INK   = colors.HexColor("#3A3631")
LUMEN_INK_2 = colors.HexColor("#6B6660")
LUMEN_SAND  = colors.HexColor("#F4EFE6")


def _label_for(value: float, low: float, high: float) -> str:
    if value < low:
        return "LOW"
    if value > high:
        return "HIGH"
    return "Normal"


def _fmt(value: float) -> str:
    if value is None:
        return "—"
    if isinstance(value, float) and value.is_integer():
        return str(int(value))
    return f"{value:g}"


def render(filename: str, scenario: dict, out_dir: Path) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / filename

    doc = SimpleDocTemplate(
        str(out_path),
        pagesize=A4,
        leftMargin=18 * mm, rightMargin=18 * mm,
        topMargin=18 * mm,  bottomMargin=18 * mm,
        title=scenario["title"],
    )

    styles = getSampleStyleSheet()
    h1 = styles["Heading1"]; h1.textColor = LUMEN_TEAL; h1.fontSize = 18
    h2 = styles["Heading2"]; h2.textColor = LUMEN_INK; h2.fontSize = 11
    body = styles["BodyText"]; body.textColor = LUMEN_INK; body.fontSize = 10

    story = []

    # --- Header ---
    story.append(Paragraph("LUMEN HEALTH · LABORATORY REPORT", h1))
    story.append(Paragraph(scenario["title"], h2))
    today = datetime.now().date()
    collected = today - timedelta(days=1)
    meta_rows = [
        ["Patient",        scenario["patient"]],
        ["Collected",      collected.strftime("%B %d, %Y")],
        ["Reported",       today.strftime("%B %d, %Y")],
        ["Specimen",       "Serum / Whole blood"],
        ["Ordering MD",    "Dr. Emily Stone"],
    ]
    meta = Table(meta_rows, colWidths=[35 * mm, 100 * mm])
    meta.setStyle(TableStyle([
        ("FONTSIZE",        (0, 0), (-1, -1), 9),
        ("TEXTCOLOR",       (0, 0), (0, -1),  LUMEN_INK_2),
        ("TEXTCOLOR",       (1, 0), (1, -1),  LUMEN_INK),
        ("BOTTOMPADDING",   (0, 0), (-1, -1), 3),
        ("TOPPADDING",      (0, 0), (-1, -1), 3),
    ]))
    story.append(Spacer(1, 4 * mm))
    story.append(meta)
    story.append(Spacer(1, 6 * mm))

    # --- Results table ---
    header = ["Test", "Result", "Unit", "Reference Range", "Flag"]
    data = [header]
    for r in RANGES:
        v = scenario["values"].get(r.test_id)
        flag = _label_for(v, r.low, r.high) if v is not None else ""
        data.append([
            r.label,
            _fmt(v),
            r.unit,
            f"{_fmt(r.low)} – {_fmt(r.high)}",
            flag,
        ])

    tbl = Table(
        data,
        colWidths=[60 * mm, 22 * mm, 22 * mm, 38 * mm, 22 * mm],
        repeatRows=1,
    )
    style = TableStyle([
        ("BACKGROUND",    (0, 0), (-1, 0),   LUMEN_SAND),
        ("TEXTCOLOR",     (0, 0), (-1, 0),   LUMEN_TEAL),
        ("FONTNAME",      (0, 0), (-1, 0),   "Helvetica-Bold"),
        ("FONTSIZE",      (0, 0), (-1, -1),  9),
        ("BOTTOMPADDING", (0, 0), (-1, -1),  6),
        ("TOPPADDING",    (0, 0), (-1, -1),  6),
        ("LINEBELOW",     (0, 0), (-1, 0),   0.6, LUMEN_TEAL),
        ("LINEBELOW",     (0, 1), (-1, -2),  0.25, colors.HexColor("#E6E0D2")),
        ("VALIGN",        (0, 0), (-1, -1),  "MIDDLE"),
    ])
    # Color flag column: red for HIGH/LOW, green for Normal, grey for blank
    for i, row in enumerate(data[1:], start=1):
        flag = row[4]
        if flag in ("HIGH", "LOW"):
            style.add("TEXTCOLOR", (4, i), (4, i), colors.HexColor("#C85555"))
            style.add("FONTNAME",  (4, i), (4, i), "Helvetica-Bold")
        elif flag == "Normal":
            style.add("TEXTCOLOR", (4, i), (4, i), colors.HexColor("#5E8E62"))
    tbl.setStyle(style)
    story.append(tbl)

    story.append(Spacer(1, 6 * mm))
    story.append(Paragraph(
        "<i>This sample report is generated for the Lumen Health AI lab-analyzer demo. "
        "It is synthetic data — not a real patient.</i>",
        body,
    ))

    doc.build(story)
    return out_path


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for filename, scenario in SCENARIOS.items():
        path = render(filename, scenario, OUT_DIR)
        print(f"  · wrote {path}")
    print(f"\n{len(SCENARIOS)} sample PDFs in {OUT_DIR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
