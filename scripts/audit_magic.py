#!/usr/bin/env python3
"""Audit the Magic RSC pack for Legacy/IE2 porting.

This script is intentionally dependency-free so it can run in GitHub Actions.
It scans YAML as text because RSC supports syntax/extensions that are not always
accepted by generic YAML parsers.
"""
from __future__ import annotations

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

# IE1 ids whose IE2 targets are not simply IE_<old id>.
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

# Prefixes/names strongly associated with InfinityExpansion v1. These are only
# audit hints. Automatic replacement is performed separately and only after
# verification against IE2.
IE1_HINTS = (
    "INFINITY_", "INFINITE_", "VOID_", "SINGULARITY", "QUARRY_",
    "BASIC_STORAGE", "ADVANCED_STORAGE", "REINFORCED_STORAGE",
    "BASIC_STRAINER", "ADVANCED_STRAINER", "REINFORCED_STRAINER",
    "DATA_INFUSER", "EMPTY_DATA_CARD", "END_ESSENCE",
)

external_refs: dict[str, list[dict[str, object]]] = defaultdict(list)
recipe_types: dict[str, list[dict[str, object]]] = defaultdict(list)
chinese_lines: list[dict[str, object]] = []
all_tokens = Counter()

for path in YAML_FILES:
    rel = path.relative_to(ROOT).as_posix()
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    pending_slimefun_line = None

    for number, line in enumerate(lines, 1):
        if CHINESE_RE.search(line):
            chinese_lines.append({"file": rel, "line": number, "text": line})

        all_tokens.update(ID_TOKEN_RE.findall(line))

        mt = MATERIAL_TYPE_RE.match(line)
        if mt:
            pending_slimefun_line = number if mt.group(1).strip().lower() == "slimefun" else None
            continue

        if pending_slimefun_line is not None:
            m = MATERIAL_RE.match(line)
            if m:
                raw = m.group(1).strip()
                # RSC allows alternatives separated by |.
                for item_id in (part.strip() for part in raw.split("|")):
                    if item_id and not item_id.startswith("MAGIC_"):
                        external_refs[item_id].append({"file": rel, "line": number, "raw": raw})
                pending_slimefun_line = None
            elif line.strip() and not line.lstrip().startswith("#"):
                # Keep waiting across amount/metadata lines only if material did not occur yet.
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

report = {
    "yaml_file_count": len(YAML_FILES),
    "external_slimefun_id_count": len(external_refs),
    "external_slimefun_ids": dict(sorted(external_refs.items())),
    "external_recipe_types": dict(sorted(recipe_types.items())),
    "ie2_candidates": ie_candidates,
    "chinese_line_count": len(chinese_lines),
    "chinese_lines": chinese_lines,
}

(OUT / "magic-audit.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

with (OUT / "external-slimefun-ids.txt").open("w", encoding="utf-8") as f:
    for item_id, refs in sorted(external_refs.items()):
        f.write(f"{item_id}\t{len(refs)}\n")

with (OUT / "chinese-lines.tsv").open("w", encoding="utf-8") as f:
    f.write("file\tline\ttext\n")
    for row in chinese_lines:
        text = str(row["text"]).replace("\t", "    ")
        f.write(f'{row["file"]}\t{row["line"]}\t{text}\n')

md = []
md.append("# Magic Legacy compatibility audit")
md.append("")
md.append(f"- YAML files scanned: **{len(YAML_FILES)}**")
md.append(f"- Unique external Slimefun IDs: **{len(external_refs)}**")
md.append(f"- IE2 migration candidates: **{len(ie_candidates)}**")
md.append(f"- Lines containing Chinese text: **{len(chinese_lines)}**")
md.append("")
md.append("## IE2 migration candidates")
md.append("")
md.append("| IE1/reference ID | Proposed IE2 ID | Uses | Basis |")
md.append("|---|---|---:|---|")
for old, data in sorted(ie_candidates.items()):
    md.append(f'| `{old}` | `{data["target"]}` | {len(data["references"])} | {data["reason"]} |')
md.append("")
md.append("## External Slimefun IDs")
md.append("")
for item_id, refs in sorted(external_refs.items()):
    md.append(f"- `{item_id}` — {len(refs)} reference(s)")

(OUT / "AUDIT.md").write_text("\n".join(md) + "\n", encoding="utf-8")

print(f"Scanned {len(YAML_FILES)} YAML files")
print(f"Found {len(external_refs)} unique external Slimefun IDs")
print(f"Found {len(ie_candidates)} IE2 migration candidates")
print(f"Found {len(chinese_lines)} lines containing Chinese text")
