#!/usr/bin/env python3
"""Report non-English Han text that can be shown to Magic players.

The audit is intentionally conservative: it scans display names/titles/descriptions,
geo names, lore/list text, startup text, and JavaScript lines likely to send text to
players. Internal IDs, recipe keys and material names are not treated as failures.

Besides console output, this writes deterministic audit reports under ``audit/`` so
translation work can operate on unique display strings instead of editing repeated
lore lines one at a time.
"""

from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
AUDIT_DIR = ROOT / "audit"
HAN = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")
SCALAR_KEYS = {"name", "title", "description", "geo_name"}
LIST_KEYS = {"lore", "loadStartTexts", "enabledTexts"}
SKIP_DIRS = {".git", ".github", "audit", "dist"}


def indent_of(line: str) -> int:
    return len(line) - len(line.lstrip(" "))


def display_value(raw: str) -> str:
    """Return the YAML list/scalar display value without indentation/key syntax."""
    stripped = raw.strip()
    if stripped.startswith("-"):
        return stripped[1:].strip()
    m = re.match(r"[A-Za-z_][A-Za-z0-9_-]*:\s*(.*)$", stripped)
    return m.group(1).strip() if m else stripped


def audit_yaml(path: Path) -> list[tuple[int, str, str]]:
    findings: list[tuple[int, str, str]] = []
    active_list_indent: int | None = None
    active_list_key: str | None = None

    for lineno, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        stripped = raw.strip()
        indent = indent_of(raw)

        if active_list_indent is not None:
            if stripped and not stripped.startswith("-") and indent <= active_list_indent:
                active_list_indent = None
                active_list_key = None
            elif stripped.startswith("-") and HAN.search(stripped):
                findings.append((lineno, active_list_key or "list", raw))
                continue

        m = re.match(r"\s*([A-Za-z_][A-Za-z0-9_-]*):\s*(.*)$", raw)
        if not m:
            continue

        key, value = m.groups()
        if key in LIST_KEYS:
            active_list_indent = indent
            active_list_key = key
            if HAN.search(value):
                findings.append((lineno, key, raw))
            continue

        if key in SCALAR_KEYS and HAN.search(value):
            findings.append((lineno, key, raw))

    return findings


def audit_js(path: Path) -> list[tuple[int, str, str]]:
    findings: list[tuple[int, str, str]] = []
    player_text_hints = (
        "sendMessage", "sendActionBar", "broadcast", "message", "setDisplayName",
        "setLore", "title", "subtitle", "sendTitle", "Component", "ChatColor",
    )
    for lineno, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if HAN.search(raw) and any(hint in raw for hint in player_text_hints):
            findings.append((lineno, "javascript", raw))
    return findings


def write_reports(findings: dict[Path, list[tuple[int, str, str]]]) -> None:
    AUDIT_DIR.mkdir(parents=True, exist_ok=True)

    occurrences: list[dict[str, object]] = []
    by_value: dict[str, list[dict[str, object]]] = defaultdict(list)
    for path, hits in findings.items():
        for lineno, field, raw in hits:
            value = display_value(raw)
            entry = {
                "file": path.as_posix(),
                "line": lineno,
                "field": field,
                "value": value,
            }
            occurrences.append(entry)
            by_value[value].append(entry)

    unique = [
        {
            "value": value,
            "occurrences": len(entries),
            "locations": [f"{entry['file']}:{entry['line']}" for entry in entries],
        }
        for value, entries in sorted(by_value.items(), key=lambda item: (-len(item[1]), item[0]))
    ]

    (AUDIT_DIR / "player_facing_han_occurrences.json").write_text(
        json.dumps(occurrences, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    (AUDIT_DIR / "player_facing_han_unique.json").write_text(
        json.dumps(unique, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    with (AUDIT_DIR / "player_facing_han_unique.txt").open("w", encoding="utf-8", newline="\n") as fp:
        fp.write(f"Unique player-facing Han strings: {len(unique)}\n")
        fp.write(f"Total occurrences: {len(occurrences)}\n\n")
        for index, entry in enumerate(unique, 1):
            fp.write(f"[{index:04d}] x{entry['occurrences']} {entry['value']}\n")
            fp.write("       " + ", ".join(entry["locations"]) + "\n")


def main() -> int:
    findings: dict[Path, list[tuple[int, str, str]]] = {}

    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or any(part in SKIP_DIRS for part in path.parts):
            continue
        if path.suffix.lower() in {".yml", ".yaml"}:
            hits = audit_yaml(path)
        elif path.suffix.lower() == ".js":
            hits = audit_js(path)
        else:
            continue
        if hits:
            findings[path.relative_to(ROOT)] = hits

    write_reports(findings)

    total = sum(len(v) for v in findings.values())
    unique_total = len({display_value(raw) for hits in findings.values() for _, _, raw in hits})
    print(f"Player-facing Han-text findings: {total}")
    print(f"Unique player-facing Han strings: {unique_total}")
    for path, hits in findings.items():
        print(f"\n{path}: {len(hits)}")
        for lineno, field, line in hits:
            print(f"  {lineno} [{field}]: {line.strip()}")

    # During the translation branch this is a reporting tool, not a hard CI gate.
    # When the count reaches zero it can be promoted to a required check.
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
