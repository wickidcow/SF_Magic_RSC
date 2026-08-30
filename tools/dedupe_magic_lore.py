#!/usr/bin/env python3
"""Remove repeated visible lore lines from Magic RSC YAML.

This is intentionally run after translation/text normalization so two formerly
different source strings cannot normalize into duplicate player-visible lore.
Blank/color-only separator lines are preserved. The release text-quality cleaner
uses this as its final pass so normalization can never reintroduce duplicate lore.
"""
from __future__ import annotations

import ast
import json
import re
import sys
from pathlib import Path

LORE_RE = re.compile(r"^(\s*)lore:\s*(?:#.*)?$")
LIST_RE = re.compile(r"^(\s*)-\s*(.*?)\s*$")
LEGACY_COLOR_RE = re.compile(r"(?i)(?:§|&)[0-9A-FK-ORX]")
HEX_COLOR_RE = re.compile(r"(?i)(?:&#[0-9A-F]{6}|\{#[0-9A-F]{6}\})")


def indentation(line: str) -> int:
    return len(line) - len(line.lstrip(" \t"))


def unquote_scalar(raw: str) -> str:
    value = raw.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        try:
            parsed = ast.literal_eval(value)
            if isinstance(parsed, str):
                return parsed
        except (SyntaxError, ValueError):
            return value[1:-1]
    return value


def json_visible_text(value: str) -> str | None:
    candidate = value.strip()
    if not candidate.startswith(("{", "[")):
        return None
    try:
        data = json.loads(candidate)
    except json.JSONDecodeError:
        return None

    pieces: list[str] = []

    def walk(node: object) -> None:
        if isinstance(node, dict):
            text = node.get("text")
            if isinstance(text, str):
                pieces.append(text)
            for key, child in node.items():
                if key != "text":
                    walk(child)
        elif isinstance(node, list):
            for child in node:
                walk(child)

    walk(data)
    return "".join(pieces)


def visible(raw: str) -> str:
    value = unquote_scalar(raw)
    json_text = json_visible_text(value)
    if json_text is not None:
        value = json_text
    value = HEX_COLOR_RE.sub("", value)
    value = LEGACY_COLOR_RE.sub("", value)
    return re.sub(r"\s+", " ", value).strip()


def clean_file(path: Path) -> int:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    lore_indent: int | None = None
    seen_visible: set[str] = set()
    removed = 0
    rebuilt: list[str] = []

    for line in lines:
        no_eol = line.rstrip("\r\n")
        stripped = line.strip()
        current_indent = indentation(line)

        lore_match = LORE_RE.match(no_eol)
        if lore_match:
            lore_indent = len(lore_match.group(1))
            seen_visible.clear()
            rebuilt.append(line)
            continue

        in_lore = False
        if lore_indent is not None:
            list_match = LIST_RE.match(no_eol)
            if list_match and current_indent >= lore_indent:
                # Bukkit serialized ItemStack YAML can put lore entries at the
                # same indentation as the lore key.
                in_lore = True
            elif current_indent > lore_indent:
                in_lore = True
            elif stripped and not stripped.startswith("#"):
                lore_indent = None
                seen_visible.clear()

        if in_lore:
            list_match = LIST_RE.match(no_eol)
            if list_match:
                text = visible(list_match.group(2))
                if text:
                    key = text.casefold()
                    if key in seen_visible:
                        removed += 1
                        continue
                    seen_visible.add(key)

        rebuilt.append(line)

    if removed:
        path.write_text("".join(rebuilt), encoding="utf-8")
    return removed


def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: dedupe_magic_lore.py <Magic folder>")
    root = Path(sys.argv[1]).resolve()
    if not root.is_dir():
        raise SystemExit(f"Not a directory: {root}")

    removed = 0
    for path in sorted(root.rglob("*.yml")):
        if any(part in {".git", "audit", "dist"} for part in path.parts):
            continue
        removed += clean_file(path)

    print(f"Removed {removed} duplicate visible Magic lore line(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
