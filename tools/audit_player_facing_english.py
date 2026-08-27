#!/usr/bin/env python3
"""Report non-English Han text that can be shown to Magic players.

The audit is intentionally conservative: it scans display names/titles/descriptions,
geo names, lore/list text, startup text, and JavaScript lines likely to send text to
players. Internal IDs, recipe keys and material names are not treated as failures.
"""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HAN = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")
SCALAR_KEYS = {"name", "title", "description", "geo_name"}
LIST_KEYS = {"lore", "loadStartTexts", "enabledTexts"}
SKIP_DIRS = {".git", ".github", "audit", "dist"}


def indent_of(line: str) -> int:
    return len(line) - len(line.lstrip(" "))


def audit_yaml(path: Path) -> list[tuple[int, str]]:
    findings: list[tuple[int, str]] = []
    active_list_indent: int | None = None

    for lineno, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        stripped = raw.strip()
        indent = indent_of(raw)

        if active_list_indent is not None:
            if stripped and not stripped.startswith("-") and indent <= active_list_indent:
                active_list_indent = None
            elif stripped.startswith("-") and HAN.search(stripped):
                findings.append((lineno, raw))
                continue

        m = re.match(r"\s*([A-Za-z_][A-Za-z0-9_-]*):\s*(.*)$", raw)
        if not m:
            continue

        key, value = m.groups()
        if key in LIST_KEYS:
            active_list_indent = indent
            if HAN.search(value):
                findings.append((lineno, raw))
            continue

        if key in SCALAR_KEYS and HAN.search(value):
            findings.append((lineno, raw))

    return findings


def audit_js(path: Path) -> list[tuple[int, str]]:
    findings: list[tuple[int, str]] = []
    player_text_hints = (
        "sendMessage", "sendActionBar", "broadcast", "message", "setDisplayName",
        "setLore", "title", "subtitle", "sendTitle", "Component", "ChatColor",
    )
    for lineno, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if HAN.search(raw) and any(hint in raw for hint in player_text_hints):
            findings.append((lineno, raw))
    return findings


def main() -> int:
    findings: dict[Path, list[tuple[int, str]]] = {}

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

    total = sum(len(v) for v in findings.values())
    print(f"Player-facing Han-text findings: {total}")
    for path, hits in findings.items():
        print(f"\n{path}: {len(hits)}")
        for lineno, line in hits:
            print(f"  {lineno}: {line.strip()}")

    # During the translation branch this is a reporting tool, not a hard CI gate.
    # When the count reaches zero it can be promoted to a required check.
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
