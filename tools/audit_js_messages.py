#!/usr/bin/env python3
"""Find likely player-facing Chinese string literals in Magic JavaScript.

This is an audit only. It does not alter scripts because arbitrary translation of
internal keys, names, or identifiers could break gameplay behavior.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHINESE = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")
STRING = re.compile(r"(?P<q>['\"])(?P<text>(?:\\.|(?!\1).)*)(?P=q)")
PLAYER_HINTS = (
    "sendMessage", "sendActionBar", "sendTitle", "broadcast", "message(",
    "setDisplayName", "setLore", "setCustomName", "showTitle", "kick",
)

rows = []
for path in sorted((ROOT / "scripts").rglob("*.js")):
    text = path.read_text(encoding="utf-8", errors="replace")
    for line_no, line in enumerate(text.splitlines(), 1):
        literals = [m.group("text") for m in STRING.finditer(line) if CHINESE.search(m.group("text"))]
        if not literals:
            continue
        likely_player = any(hint in line for hint in PLAYER_HINTS)
        rows.append({
            "file": path.relative_to(ROOT).as_posix(),
            "line": line_no,
            "likely_player_facing": likely_player,
            "literals": literals,
            "source": line.strip(),
        })

out = ROOT / "audit"
out.mkdir(exist_ok=True)
(out / "js-chinese-strings.json").write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
with (out / "JS_MESSAGES.md").open("w", encoding="utf-8") as f:
    f.write("# JavaScript Chinese string audit\n\n")
    f.write(f"Total lines with Chinese string literals: **{len(rows)}**\n\n")
    likely = [row for row in rows if row["likely_player_facing"]]
    f.write(f"Likely player-facing lines: **{len(likely)}**\n\n")
    f.write("## Likely player-facing\n\n")
    for row in likely:
        f.write(f'- `{row["file"]}:{row["line"]}` — `{row["source"]}`\n')

print(f"Chinese JS string-literal lines: {len(rows)}")
print(f"Likely player-facing: {sum(1 for row in rows if row['likely_player_facing'])}")
