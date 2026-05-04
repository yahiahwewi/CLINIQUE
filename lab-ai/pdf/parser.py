"""
Extract a {test_id: value} dict from a lab-report PDF.

Strategy:
  1. Use pdfplumber to pull all text from every page.
  2. Walk the lines looking for "<test name> <number> <unit?>" patterns.
  3. Match the test name against the alias list in reference_ranges.RANGES
     (case-insensitive, longest match wins).

The parser is forgiving — if a test isn't found, it's simply omitted from the
output (and the inference layer will impute the midpoint).
"""

from __future__ import annotations

import re
import sys
from io import BytesIO
from pathlib import Path
from typing import IO, Union

import pdfplumber

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))
from reference_ranges import RANGES  # noqa: E402

# A "value token" looks like: 12, 12.3, .9, 0.42, 1,200 (commas tolerated)
_NUMBER_RE = re.compile(r"\d{1,3}(?:[,]\d{3})*(?:\.\d+)?|\.\d+")

# Prefer longest aliases so e.g. "white blood cell" matches before "wbc" in
# the same line and we don't grab a stray "WBC" out of a header.
_ALIAS_TABLE = sorted(
    [
        (alias.lower(), r.test_id)
        for r in RANGES
        for alias in (r.aliases or (r.label.lower(),))
    ],
    key=lambda pair: -len(pair[0]),
)


def _extract_text(source: Union[str, Path, IO[bytes], bytes]) -> str:
    """Return all page text concatenated with single newlines."""
    if isinstance(source, (bytes, bytearray)):
        source = BytesIO(source)

    chunks: list[str] = []
    with pdfplumber.open(source) as pdf:
        for page in pdf.pages:
            text = page.extract_text() or ""
            chunks.append(text)
    return "\n".join(chunks)


def _first_number(line: str, after_index: int) -> float | None:
    sub = line[after_index:]
    m = _NUMBER_RE.search(sub)
    if not m:
        return None
    raw = m.group(0).replace(",", "")
    try:
        return float(raw)
    except ValueError:
        return None


def parse_text(text: str) -> dict[str, float]:
    """Walk each line, attempt to recognise a test name + extract its value."""
    out: dict[str, float] = {}
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        lower = line.lower()
        for alias, test_id in _ALIAS_TABLE:
            if test_id in out:
                continue
            idx = lower.find(alias)
            if idx == -1:
                continue
            value = _first_number(line, idx + len(alias))
            if value is not None:
                out[test_id] = value
            break
    return out


def parse_pdf(source: Union[str, Path, IO[bytes], bytes]) -> tuple[dict[str, float], str]:
    """Return (extracted_values, raw_text). raw_text is handy for the UI to
    show 'what we read from the PDF'."""
    text = _extract_text(source)
    values = parse_text(text)
    return values, text


# CLI helper — parse a path from the command line, print JSON result
def _main() -> int:
    import argparse, json
    p = argparse.ArgumentParser()
    p.add_argument("pdf_path")
    args = p.parse_args()
    values, _text = parse_pdf(args.pdf_path)
    print(json.dumps(values, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(_main())
