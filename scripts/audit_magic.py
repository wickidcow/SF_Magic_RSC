#!/usr/bin/env python3
"""Audit the Magic RSC pack for Legacy/IE2 porting and player-facing text quality.

This script is intentionally dependency-free so it can run in GitHub Actions.
It scans YAML as text because RSC supports syntax/extensions that are not always
accepted by generic YAML parsers.
"""
from __future__ import annotations

import ast
import json
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "audit"
OUT.mkdir(exist_ok=True)

YAML_FILES = sorted(
    p for p in ROOT.rglob("*.yml")
    if ".git" not in p.parts and "audit" not in p.parts
)

CHINESE_RE = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")
MATERIAL_TYPE_RE = re.compile(r"^\s*material_type:\s*['\"]?([^'\"#]+?)['\"]?\s*(?:#.*)?$")
MATERIAL_RE = re.compile(r"^\s*material:\s*['\"]?([^'\"#]+?)['\"]?\s*(?:#.*)?$")
RECIPE_TYPE_RE = re.compile(r"^\s*recipe_type:\s*['\"]?([^'\"#]+?)['\"]?\s*(?:#.*)?$")
ID_TOKEN_RE = re.compile(r"\b[A-Z][A-Z0-9_]{2,}\b")
LORE_RE = re.compile(r"^(\s*)lore:\s*(?:#.*)?$")
LIST_RE = re.compile(r"^(\s*)-\s*(.*?)\s*$")
NAME_RE = re.compile(r"^\s*(?:name|display-name):\s*(.+?)\s*$")
LEGACY_COLOR_RE = re.compile(r"(?i)(?:§|&)[0-9A-FK-ORX]")
HEX_COLOR_RE = re.compile(r"(?i)(?:&#[0-9A-F]{6}|\{#[0-9A-F]{6}\})")

EXPLICIT_IE2 = {
    "INFINITE_INGOT": "IE_INFINITY_INGOT",
    "INFINITE_MACHINE_CIRCUIT": "IE_INFINITY_MACHINE_CIRCUIT",
    "INFINITE_MACHINE_CORE": "IE_INFINITY_MACHINE_CORE",
    "END_ESSENCE": "IE_ENDER_ESSENCE",
    "INFINITY_FORGE": "IE_INFINITY_WORKBENCH",
    "BASIC_STRAINER": "IE_STRAINER_1",
    "ADVANCED_STRAINER": "IE_STRAINER_2",
    "REINFORCED_STRAINER": "IE_STRAINER_3",
    "BASIC_COBBLE_GEN": "IE_COBBLESTONE_GENERATOR",
    "ADVANCED_COBBLE_GEN": "IE_COBBLESTONE_GENERATOR_2",
    "INFINITY_COBBLE_GEN": "IE_COBBLESTONE_GENERATOR_4",
    "BASIC_VIRTUAL_FARM": "IE_VIRTUAL_FARM",
    "ADVANCED_VIRTUAL_FARM": "IE_VIRTUAL_FARM_2",
    "INFINITY_VIRTUAL_FARM": "IE_VIRTUAL_FARM_4",
    "BASIC_TREE_GROWER": "IE_TREE_GROWER",
    "ADVANCED_TREE_GROWER": "IE_TREE_GROWER_2",
    "INFINITY_TREE_GROWER": "IE_TREE_GROWER_4",
    "BASIC_QUARRY": "IE_QUARRY",
    "ADVANCED_QUARRY": "IE_QUARRY_2",
    "VOID_QUARRY": "IE_QUARRY_3",
    "INFINITY_QUARRY": "IE_QUARRY_4",
    "INFINITE_VOID_HARVESTER": "IE_VOID_HARVESTER_3",
    "INFINITY_CONSTRUCTOR": "IE_SINGULARITY_CONSTRUCTOR_2",
    "INFINITY_DUST_EXTRACTOR": "IE_DUST_EXTRACTOR_4",
    "INFINITY_INGOT_FORMER": "IE_INGOT_FORMER_4",
    "BASIC_OBSIDIAN_GEN": "IE_OBSIDIAN_GENERATOR",
    "HYDRO_GENERATOR": "IE_HYDRO_GENERATOR",
    "REINFORCED_HYDRO_GENERATOR": "IE_HYDRO_GENERATOR_2",
    "GEOTHERMAL_GENERATOR": "IE_GEOTHERMAL_GENERATOR",
    "REINFORCED_GEOTHERMAL_GENERATOR": "IE_GEOTHERMAL_GENERATOR_2",
    "BASIC_PANEL": "IE_SOLAR_PANEL",
    "ADVANCED_PANEL": "IE_SOLAR_PANEL_2",
    "CELESTIAL_PANEL": "IE_SOLAR_PANEL_3",
    "VOID_PANEL": "IE_VOID_PANEL",
    "INFINITE_PANEL": "IE_INFINITY_PANEL",
    "EMPTY_DATA_CARD": "IE_MOB_DATA_CARD_EMPTY",
    "DATA_INFUSER": "IE_MOB_DATA_INFUSER",
    "BASIC_STORAGE": "IE_STORAGE_UNIT_2",
    "ADVANCED_STORAGE": "IE_STORAGE_UNIT_3",
    "REINFORCED_STORAGE": "IE_STORAGE_UNIT_4",
    "VOID_STORAGE": "IE_STORAGE_UNIT_5",
    "INFINITY_STORAGE": "IE_STORAGE_UNIT_6",
}

IE1_HINTS = (
    "INFINITY_", "INFINITE_", "VOID_", "SINGULARITY", "QUARRY_",
    "BASIC_STORAGE", "ADVANCED_STORAGE", "REINFORCED_STORAGE",
    "BASIC_STRAINER", "ADVANCED_STRAINER", "REINFORCED_STRAINER",
    "DATA_INFUSER", "EMPTY_DATA_CARD", "END_ESSENCE",
)


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


def normalize_visible_text(raw: str) -> str:
    value = unquote_scalar(raw)
    json_text = json_visible_text(value)
    if json_text is not None:
        value = json_text
    value = HEX_COLOR_RE.sub("", value)
    value = LEGACY_COLOR_RE.sub("", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value


def classify_chinese_line(line: str, in_lore: bool) -> str:
    stripped = line.lstrip()
    if stripped.startswith("#"):
        return "comment"
    if in_lore or NAME_RE.match(line):
        return "player-facing"
    if re.match(r"^\s*['\"].*['\"]\s*:\s*(?:#.*)?$", line):
        return "data-key"
    return "other"


external_refs: dict[str, list[dict[str, object]]] = defaultdict(list)
recipe_types: dict[str, list[dict[str, object]]] = defaultdict(list)
chinese_lines: list[dict[str, object]] = []
player_facing_chinese_lines: list[dict[str, object]] = []
duplicate_lore_lines: list[dict[str, object]] = []
all_tokens = Counter()

for path in YAML_FILES:
    rel = path.relative_to(ROOT).as_posix()
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    pending_slimefun_line = None
    lore_indent: int | None = None
    lore_start_line: int | None = None
    lore_seen: dict[str, int] = {}

    for number, line in enumerate(lines, 1):
        stripped = line.strip()
        current_indent = indentation(line)

        lore_match = LORE_RE.match(line)
        if lore_match:
            lore_indent = len(lore_match.group(1))
            lore_start_line = number
            lore_seen = {}
            in_lore = False
        else:
            in_lore = lore_indent is not None and current_indent > lore_indent
            if lore_indent is not None and stripped and not stripped.startswith("#") and current_indent <= lore_indent:
                lore_indent = None
                lore_start_line = None
                lore_seen = {}
                in_lore = False

        if CHINESE_RE.search(line):
            classification = classify_chinese_line(line, in_lore)
            row = {"file": rel, "line": number, "classification": classification, "text": line}
            chinese_lines.append(row)
            if classification == "player-facing":
                player_facing_chinese_lines.append(row)

        if in_lore:
            list_match = LIST_RE.match(line)
            if list_match:
                visible = normalize_visible_text(list_match.group(2))
                # Formatting-only entries such as "", &a, and pure color/reset lines are
                # intentional separators and should never be reported as duplicates.
                if visible:
                    key = visible.casefold()
                    first_line = lore_seen.get(key)
                    if first_line is None:
                        lore_seen[key] = number
                    else:
                        duplicate_lore_lines.append(
                            {
                                "file": rel,
                                "line": number,
                                "first_line": first_line,
                                "lore_start_line": lore_start_line,
                                "visible_text": visible,
                            }
                        )

        all_tokens.update(ID_TOKEN_RE.findall(line))

        mt = MATERIAL_TYPE_RE.match(line)
        if mt:
            pending_slimefun_line = number if mt.group(1).strip().lower() == "slimefun" else None
            continue

        if pending_slimefun_line is not None:
            m = MATERIAL_RE.match(line)
            if m:
                raw = m.group(1).strip()
                for item_id in (part.strip() for part in raw.split("|")):
                    if item_id and not item_id.startswith("MAGIC_"):
                        external_refs[item_id].append({"file": rel, "line": number, "raw": raw})
                pending_slimefun_line = None
            elif stripped and not stripped.startswith("#"):
                if not line.startswith((" ", "\t")):
                    pending_slimefun_line = None

        rt = RECIPE_TYPE_RE.match(line)
        if rt:
            recipe_type = rt.group(1).strip()
            if recipe_type and not recipe_type.startswith("MAGIC_") and recipe_type not in {"NULL"}:
                recipe_types[recipe_type].append({"file": rel, "line": number})

ie_candidates = {}
for item_id, refs in sorted(external_refs.items()):
    target = EXPLICIT_IE2.get(item_id)
    reason = None
    if target:
        reason = "explicit IE1 -> IE2 mapping"
    elif item_id.endswith("_DATA_CARD") and item_id != "EMPTY_DATA_CARD":
        target = f"IE_MOB_DATA_CARD_{item_id.removesuffix('_DATA_CARD')}"
        reason = "IE1 dynamic mob data card pattern"
    elif item_id.startswith("QUARRY_OSCILLATOR_"):
        target = f"IE_OSCILLATOR_{item_id.removeprefix('QUARRY_OSCILLATOR_')}"
        reason = "IE1 oscillator pattern"
    elif item_id.startswith(IE1_HINTS):
        target = f"IE_{item_id}"
        reason = "probable IE1 id; verify target exists in IE2"

    if target:
        ie_candidates[item_id] = {"target": target, "reason": reason, "references": refs}

chinese_class_counts = Counter(str(row["classification"]) for row in chinese_lines)
duplicate_lore_files = Counter(str(row["file"]) for row in duplicate_lore_lines)

report = {
    "yaml_file_count": len(YAML_FILES),
    "external_slimefun_id_count": len(external_refs),
    "external_slimefun_ids": dict(sorted(external_refs.items())),
    "external_recipe_types": dict(sorted(recipe_types.items())),
    "ie2_candidates": ie_candidates,
    "chinese_line_count": len(chinese_lines),
    "chinese_line_classification": dict(sorted(chinese_class_counts.items())),
    "player_facing_chinese_line_count": len(player_facing_chinese_lines),
    "chinese_lines": chinese_lines,
    "player_facing_chinese_lines": player_facing_chinese_lines,
    "duplicate_lore_line_count": len(duplicate_lore_lines),
    "duplicate_lore_lines": duplicate_lore_lines,
}

(OUT / "magic-audit.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
with (OUT / "external-slimefun-ids.txt").open("w", encoding="utf-8") as f:
    for item_id, refs in sorted(external_refs.items()):
        f.write(f"{item_id}\t{len(refs)}\n")
with (OUT / "chinese-lines.tsv").open("w", encoding="utf-8") as f:
    f.write("file\tline\tclassification\ttext\n")
    for row in chinese_lines:
        text = str(row["text"]).replace("\t", "    ")
        f.write(f'{row["file"]}\t{row["line"]}\t{row["classification"]}\t{text}\n')
with (OUT / "player-facing-chinese-lines.tsv").open("w", encoding="utf-8") as f:
    f.write("file\tline\ttext\n")
    for row in player_facing_chinese_lines:
        text = str(row["text"]).replace("\t", "    ")
        f.write(f'{row["file"]}\t{row["line"]}\t{text}\n')
with (OUT / "duplicate-lore-lines.tsv").open("w", encoding="utf-8") as f:
    f.write("file\tline\tfirst_line\tlore_start_line\tvisible_text\n")
    for row in duplicate_lore_lines:
        text = str(row["visible_text"]).replace("\t", "    ")
        f.write(
            f'{row["file"]}\t{row["line"]}\t{row["first_line"]}\t'
            f'{row["lore_start_line"]}\t{text}\n'
        )

md = [
    "# Magic Legacy compatibility audit", "",
    f"- YAML files scanned: **{len(YAML_FILES)}**",
    f"- Unique external Slimefun IDs: **{len(external_refs)}**",
    f"- IE2 migration candidates: **{len(ie_candidates)}**",
    f"- Lines containing Chinese text: **{len(chinese_lines)}**",
    f"- Player-facing Chinese candidates: **{len(player_facing_chinese_lines)}**",
    f"- Duplicate visible lore lines: **{len(duplicate_lore_lines)}**", "",
    "## Chinese text classification", "",
]
for classification, count in sorted(chinese_class_counts.items()):
    md.append(f"- **{classification}**: {count}")

md += ["", "## IE2 migration candidates", "",
    "| IE1/reference ID | Proposed IE2 ID | Uses | Basis |",
    "|---|---|---:|---|",
]
for old, data in sorted(ie_candidates.items()):
    md.append(f'| `{old}` | `{data["target"]}` | {len(data["references"])} | {data["reason"]} |')

md += ["", "## Duplicate lore hotspots", ""]
if duplicate_lore_files:
    for filename, count in duplicate_lore_files.most_common():
        md.append(f"- `{filename}` — {count} duplicate visible lore line(s)")
else:
    md.append("- None")

md += ["", "## External Slimefun IDs", ""]
for item_id, refs in sorted(external_refs.items()):
    md.append(f"- `{item_id}` — {len(refs)} reference(s)")
(OUT / "AUDIT.md").write_text("\n".join(md) + "\n", encoding="utf-8")

print(f"Scanned {len(YAML_FILES)} YAML files")
print(f"Found {len(external_refs)} unique external Slimefun IDs")
print(f"Found {len(ie_candidates)} IE2 migration candidates")
print(f"Found {len(chinese_lines)} lines containing Chinese text")
print(f"Found {len(player_facing_chinese_lines)} player-facing Chinese candidates")
print(f"Found {len(duplicate_lore_lines)} duplicate visible lore lines")
