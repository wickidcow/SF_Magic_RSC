#!/usr/bin/env python3
"""Audit Magic item lore for placeholder, untranslated and low-information wording."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "audit"
OUT.mkdir(exist_ok=True)

COLOR = re.compile(r"&[0-9A-FK-ORa-fk-or]")
CJK = re.compile(r"[\u3400-\u9fff]")
YAML_LORE = re.compile(r'^\s*-\s*["\x27](.*?)["\x27]\s*(?:#.*)?$')
SUSPICIOUS_FRAGMENTS = {
    "tier",
    "seconds",
    "materials",
    "magic",
    "bug",
}

rows: list[dict[str, object]] = []

for path in sorted(ROOT.glob("*.yml")):
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    in_lore = False
    for lineno, line in enumerate(lines, start=1):
        if re.match(r"^\s+lore:\s*$", line):
            in_lore = True
            continue
        if in_lore and line and not line.startswith((" ", "\t")):
            in_lore = False
        if not in_lore:
            continue

        match = YAML_LORE.match(line)
        if not match:
            continue

        raw = match.group(1).strip()
        plain = COLOR.sub("", raw).strip()
        issues: list[str] = []

        if not plain:
            issues.append("blank/color-only")
        if CJK.search(plain):
            issues.append("contains CJK text")
        if plain.lower() in SUSPICIOUS_FRAGMENTS:
            issues.append("low-information fragment")
        if "action could not be completed" in plain.lower():
            issues.append("placeholder failure text")
        if "tiermagic" in plain.lower():
            issues.append("merged placeholder wording")
        if "magicmagic" in plain.lower():
            issues.append("duplicated placeholder wording")
        if "magic-power and energy" in plain.lower():
            issues.append("generic legacy power wording")
        if "???" in plain or "？？？" in plain:
            issues.append("unknown numeric placeholder")

        if issues:
            rows.append({
                "file": path.name,
                "line": lineno,
                "text": raw,
                "issues": issues,
            })

for path in sorted((ROOT / "saveditems").rglob("*.yml")):
    text = path.read_text(encoding="utf-8", errors="replace")
    for lineno, line in enumerate(text.splitlines(), start=1):
        if "lore" not in text:
            break
        if "Converts incoming damage into health" in line:
            rows.append({
                "file": path.relative_to(ROOT).as_posix(),
                "line": lineno,
                "text": "Converts incoming damage into health",
                "issues": ["known misleading lifesteal wording"],
            })
        if CJK.search(line):
            rows.append({
                "file": path.relative_to(ROOT).as_posix(),
                "line": lineno,
                "text": line.strip()[:180],
                "issues": ["contains CJK text"],
            })

rows.sort(key=lambda row: (str(row["file"]), int(row["line"])))
summary = {
    "flagged_lines": len(rows),
    "files": len({str(row["file"]) for row in rows}),
}

(OUT / "lore-wording.json").write_text(
    json.dumps({"summary": summary, "findings": rows}, indent=2, ensure_ascii=False) + "\n",
    encoding="utf-8",
)

md = [
    "# Magic lore wording audit",
    "",
    "This report is informational. It highlights lore that deserves an English wording review.",
    "",
    f"- Flagged lore lines: **{summary['flagged_lines']}**",
    f"- Files with findings: **{summary['files']}**",
    "",
    "| File | Line | Text | Issue |",
    "| --- | ---: | --- | --- |",
]
for row in rows[:150]:
    text_value = str(row["text"]).replace("|", "\\|")
    issue = ", ".join(str(v) for v in row["issues"])
    md.append(f"| `{row['file']}` | {row['line']} | {text_value} | {issue} |")

(OUT / "LORE_WORDING.md").write_text("\n".join(md) + "\n", encoding="utf-8")
print(f"Lore wording audit: {summary['flagged_lines']} flagged lines across {summary['files']} files")
