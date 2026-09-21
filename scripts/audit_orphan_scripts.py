#!/usr/bin/env python3
"""Report JavaScript files that are not directly referenced by Magic YAML."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "audit"
OUT.mkdir(exist_ok=True)

reference_pattern = re.compile(
    r'^\s*(?:script|scriptListener):\s*["\x27]?([^"\x27#\r\n]+?)["\x27]?\s*(?:#.*)?$',
    re.MULTILINE,
)

referenced: set[str] = set()
for path in sorted(ROOT.glob("*.yml")):
    text = path.read_text(encoding="utf-8", errors="replace")
    for match in reference_pattern.finditer(text):
        referenced.add(match.group(1).strip())

scripts = {
    path.relative_to(ROOT / "scripts").with_suffix("").as_posix(): path.relative_to(ROOT).as_posix()
    for path in sorted((ROOT / "scripts").rglob("*.js"))
}

unreferenced = sorted((name, path) for name, path in scripts.items() if name not in referenced)

lines = [
    "# Magic JavaScript reference audit",
    "",
    "This report lists JavaScript files not directly referenced by top-level Magic YAML script or scriptListener entries.",
    "A listed file is a migration/review candidate, not automatically safe to delete from source history.",
    "",
    f"- Script files: **{len(scripts)}**",
    f"- Directly referenced scripts: **{len(set(scripts) & referenced)}**",
    f"- Not directly referenced: **{len(unreferenced)}**",
    "",
    "## Not directly referenced",
    "",
]

if unreferenced:
    lines.extend(f"- {path}" for _, path in unreferenced)
else:
    lines.append("- None")

(OUT / "ORPHAN_SCRIPTS.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

print(
    f"Script reference audit: {len(scripts)} scripts, "
    f"{len(unreferenced)} not directly referenced by top-level YAML"
)
