#!/usr/bin/env python3
"""Audit player-facing English quality in the Magic RSC source pack.

This complements audit_magic.py. It focuses on text that is technically English but
still looks machine-translated, redundant, placeholder-like, or malformed.
"""
from __future__ import annotations

import ast
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "audit"
OUT.mkdir(exist_ok=True)

YAML_FILES = sorted(
    p for p in ROOT.rglob("*.yml")
    if ".git" not in p.parts and "audit" not in p.parts
)

LORE_RE = re.compile(r"^(\s*)lore:\s*(?:#.*)?$")
LIST_RE = re.compile(r"^(\s*)-\s*(.*?)\s*$")
NAME_RE = re.compile(r"^\s*(?:name|display-name):\s*(.+?)\s*$")
LEGACY_COLOR_RE = re.compile(r"(?i)(?:§|&)[0-9A-FK-ORX]")
HEX_COLOR_RE = re.compile(r"(?i)(?:&#[0-9A-F]{6}|\{#[0-9A-F]{6}\})")

PLACEHOLDER_PATTERNS = [
    ("tier-placeholder", re.compile(r"^tier$", re.I)),
    # Bad translations encoded bold as a literal leading/trailing `l`, e.g.
    # `lMagic-Spawnerl` and `lMagic-Power and Energyl`.
    ("malformed-magic-branding", re.compile(r"^lMagic(?:-[A-Za-z0-9 &]*)?l$", re.I)),
]

AWKWARD_NAME_PATTERNS = [
    ("plural-generator-name", re.compile(r"^Magic Generators\b", re.I)),
    ("number-suffix-without-space", re.compile(r"\b(?:Pan|Machine|Generator|Reactor|Factory)\d+$", re.I)),
]


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


def lore_context(lore_indent: int | None, line: str) -> tuple[bool, int | None]:
    if lore_indent is None:
        return False, None
    stripped = line.strip()
    current = indentation(line)
    list_match = LIST_RE.match(line)
    if list_match and current >= lore_indent:
        return True, lore_indent
    if current > lore_indent:
        return True, lore_indent
    if not stripped or stripped.startswith("#"):
        return False, lore_indent
    return False, None


redundant_name_lore: list[dict[str, object]] = []
placeholder_lore: list[dict[str, object]] = []
awkward_names: list[dict[str, object]] = []

for path in YAML_FILES:
    rel = path.relative_to(ROOT).as_posix()
    lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
    lore_indent: int | None = None
    current_name = ""
    current_name_line: int | None = None

    for number, line in enumerate(lines, 1):
        name_match = NAME_RE.match(line)
        if name_match:
            current_name = visible(name_match.group(1))
            current_name_line = number
            for kind, pattern in AWKWARD_NAME_PATTERNS:
                if current_name and pattern.search(current_name):
                    awkward_names.append({
                        "file": rel,
                        "line": number,
                        "kind": kind,
                        "visible_text": current_name,
                    })

        lore_match = LORE_RE.match(line)
        if lore_match:
            lore_indent = len(lore_match.group(1))
            continue

        in_lore, lore_indent = lore_context(lore_indent, line)
        if not in_lore:
            continue

        list_match = LIST_RE.match(line)
        if not list_match:
            continue
        text = visible(list_match.group(2))
        if not text:
            continue

        if current_name and text.casefold() == current_name.casefold():
            redundant_name_lore.append({
                "file": rel,
                "line": number,
                "name_line": current_name_line,
                "visible_text": text,
            })

        for kind, pattern in PLACEHOLDER_PATTERNS:
            if pattern.fullmatch(text):
                placeholder_lore.append({
                    "file": rel,
                    "line": number,
                    "kind": kind,
                    "visible_text": text,
                })
                break

report = {
    "redundant_name_lore_count": len(redundant_name_lore),
    "placeholder_lore_count": len(placeholder_lore),
    "awkward_name_count": len(awkward_names),
    "redundant_name_lore": redundant_name_lore,
    "placeholder_lore": placeholder_lore,
    "awkward_names": awkward_names,
}

(OUT / "magic-quality.json").write_text(
    json.dumps(report, indent=2, ensure_ascii=False) + "\n",
    encoding="utf-8",
)

for filename, rows, columns in [
    ("redundant-name-lore.tsv", redundant_name_lore, ("file", "line", "name_line", "visible_text")),
    ("placeholder-lore.tsv", placeholder_lore, ("file", "line", "kind", "visible_text")),
    ("awkward-names.tsv", awkward_names, ("file", "line", "kind", "visible_text")),
]:
    with (OUT / filename).open("w", encoding="utf-8") as f:
        f.write("\t".join(columns) + "\n")
        for row in rows:
            f.write("\t".join(str(row.get(column, "")).replace("\t", "    ") for column in columns) + "\n")

by_file = Counter(str(row["file"]) for row in redundant_name_lore + placeholder_lore + awkward_names)
md = [
    "# Magic English quality audit",
    "",
    f"- Lore lines repeating their item's display name: **{len(redundant_name_lore)}**",
    f"- Placeholder/malformed lore lines: **{len(placeholder_lore)}**",
    f"- Awkward display names flagged: **{len(awkward_names)}**",
    "",
    "## Hotspots",
    "",
]
if by_file:
    for filename, count in by_file.most_common():
        md.append(f"- `{filename}` — {count} finding(s)")
else:
    md.append("- None")
(OUT / "QUALITY.md").write_text("\n".join(md) + "\n", encoding="utf-8")

print(f"Found {len(redundant_name_lore)} lore lines repeating the display name")
print(f"Found {len(placeholder_lore)} placeholder/malformed lore lines")
print(f"Found {len(awkward_names)} awkward display names")
