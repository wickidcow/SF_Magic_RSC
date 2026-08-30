#!/usr/bin/env python3
"""Clean safe player-facing English/lore quality issues in a Magic RSC tree.

The cleaner is intentionally conservative:
- removes lore that exactly repeats the item's display name,
- removes the literal placeholder lore line "tier",
- fixes a few malformed Legacy formatting boilerplate strings,
- normalizes a small verified set of awkward display names.

It does not rename IDs, script references, recipe keys, or serialized data keys.
"""
from __future__ import annotations

import ast
import json
import re
import sys
from pathlib import Path

LORE_RE = re.compile(r"^(\s*)lore:\s*(?:#.*)?$")
LIST_RE = re.compile(r"^(\s*)-\s*(.*?)\s*$")
NAME_RE = re.compile(r"^(\s*)(?:name|display-name):\s*(.+?)\s*$")
LEGACY_COLOR_RE = re.compile(r"(?i)(?:§|&)[0-9A-FK-ORX]")
HEX_COLOR_RE = re.compile(r"(?i)(?:&#[0-9A-F]{6}|\{#[0-9A-F]{6}\})")

TEXT_REPLACEMENTS = {
    "&7lMagicl": "&8Magic Legacy",
    "&7lMagic-Spawnerl": "&8Magic • Spawners",
    "&7lMagic-l": "&8Magic Legacy",
    "&7lMagic-Power and Energyl": "&8Magic • Power & Energy",
    "&7lMagic-Materialsl": "&8Magic • Materials",
    "Magic Generators Quartz": "Magic Quartz Generator",
    "Magic Generators Magma Block": "Magic Magma Block Generator",
    "Magic Generators Wood": "Magic Wood Generator",
    "Magic Generators Redstone 1": "Magic Redstone Generator I",
    "Magic New Player Gold Pan2": "Magic New Player Gold Pan II",
    "Magic Stoneworks Factory9": "Magic Stoneworks Factory 9",
    "Magic Stoneworks Factory81": "Magic Stoneworks Factory 81",
    "Magic Stoneworks Factory729": "Magic Stoneworks Factory 729",
}


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


def in_lore_block(lore_indent: int | None, line: str) -> tuple[bool, int | None]:
    if lore_indent is None:
        return False, None
    stripped = line.strip()
    current = indentation(line)
    list_match = LIST_RE.match(line.rstrip("\r\n"))
    # Bukkit serialized ItemStack YAML places list entries at the same indentation
    # as the `lore:` key, while ordinary RSC YAML nests them deeper.
    if list_match and current >= lore_indent:
        return True, lore_indent
    if current > lore_indent:
        return True, lore_indent
    if not stripped or stripped.startswith("#"):
        return False, lore_indent
    return False, None


def clean_file(path: Path) -> tuple[int, int, int]:
    text = path.read_text(encoding="utf-8")
    replaced = text
    rename_count = 0
    for old, new in TEXT_REPLACEMENTS.items():
        count = replaced.count(old)
        if count:
            rename_count += count
            replaced = replaced.replace(old, new)

    lines = replaced.splitlines(keepends=True)
    rebuilt: list[str] = []
    lore_indent: int | None = None
    lore_name = ""
    current_name = ""
    current_name_indent: int | None = None
    redundant_removed = 0
    placeholders_removed = 0

    for line in lines:
        line_noeol = line.rstrip("\r\n")
        name_match = NAME_RE.match(line_noeol)
        if name_match:
            current_name = visible(name_match.group(2))
            current_name_indent = len(name_match.group(1))

        lore_match = LORE_RE.match(line_noeol)
        if lore_match:
            lore_indent = len(lore_match.group(1))
            lore_name = current_name if current_name_indent == lore_indent else ""
            rebuilt.append(line)
            continue

        in_lore, lore_indent = in_lore_block(lore_indent, line)
        if not in_lore:
            if lore_indent is None:
                lore_name = ""
            rebuilt.append(line)
            continue

        list_match = LIST_RE.match(line_noeol)
        if not list_match:
            rebuilt.append(line)
            continue

        lore_text = visible(list_match.group(2))
        if lore_text.casefold() == "tier":
            placeholders_removed += 1
            continue
        if lore_name and lore_text and lore_text.casefold() == lore_name.casefold():
            redundant_removed += 1
            continue

        rebuilt.append(line)

    updated = "".join(rebuilt)
    if updated != text:
        path.write_text(updated, encoding="utf-8")
    return redundant_removed, placeholders_removed, rename_count


def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: clean_magic_text_quality.py <Magic folder>")
    root = Path(sys.argv[1]).resolve()
    if not root.is_dir():
        raise SystemExit(f"Not a directory: {root}")

    redundant = placeholders = replacements = 0
    for path in sorted(root.rglob("*.yml")):
        if any(part in {".git", "audit", "dist"} for part in path.parts):
            continue
        r, p, n = clean_file(path)
        redundant += r
        placeholders += p
        replacements += n

    print(
        "Cleaned Magic text quality "
        f"({redundant} redundant title lore, {placeholders} placeholder lore, "
        f"{replacements} verified text replacements)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
