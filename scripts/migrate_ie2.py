#!/usr/bin/env python3
"""Migrate confirmed InfinityExpansion v1 IDs in Magic to InfinityExpansion2 IDs.

Only IDs proven against the IE1 and IE2 source trees are rewritten. Optional
third-party addon IDs are intentionally left alone rather than substituted.
"""
from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOKEN_CHARS = r"A-Z0-9_"

EXPLICIT = {
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
    "INFINITY_CROWN": "IE_INFINITY_HELMET",
    "INFINITY_BLADE": "IE_INFINITY_SWORD",
    "ENDERMEN_DATA_CARD": "IE_MOB_DATA_CARD_ENDERMAN",
}

IE1_STACK_ID = re.compile(r'new\s+SlimefunItemStack\s*\(\s*"([A-Z][A-Z0-9_]+)"')
IE1_STACK_ID_ALT = re.compile(r'SlimefunItemStack\s*\(\s*\n?\s*"([A-Z][A-Z0-9_]+)"')
IE2_PROP = re.compile(r'\bval\s+([A-Z][A-Z0-9_]+)\s+by\s+buildSlimefunItem')
MATERIAL_TYPE = re.compile(r'^\s*material_type:\s*["\']?slimefun["\']?\s*(?:#.*)?$', re.I)
MATERIAL = re.compile(r'^(\s*material:\s*)(["\']?)([^"\'#\r\n]+?)(\2)(\s*(?:#.*)?)$')


def tree_files(root: Path, suffix: str) -> list[Path]:
    return [p for p in root.rglob(f"*{suffix}") if ".git" not in p.parts]


def extract_ie1_ids(root: Path) -> set[str]:
    ids: set[str] = set()
    for path in tree_files(root, ".java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        ids.update(IE1_STACK_ID.findall(text))
        ids.update(IE1_STACK_ID_ALT.findall(text))
    return ids


def extract_ie2_ids(root: Path) -> set[str]:
    path = root / "src/main/kotlin/net/guizhanss/infinityexpansion2/implementation/IEItems.kt"
    text = path.read_text(encoding="utf-8", errors="replace")
    ids = {f"IE_{name}" for name in IE2_PROP.findall(text)}

    mob_cfg = root / "src/main/resources/mob-simulation.yml"
    for line in mob_cfg.read_text(encoding="utf-8", errors="replace").splitlines():
        if line and not line[0].isspace() and line.rstrip().endswith(":") and not line.lstrip().startswith("#"):
            key = line.rstrip()[:-1]
            if re.fullmatch(r"[a-z0-9_]+", key):
                ids.add(f"IE_MOB_DATA_CARD_{key.upper()}")
    return ids


def build_mapping(ie1_root: Path, ie2_root: Path) -> tuple[dict[str, str], set[str], set[str]]:
    ie1_ids = extract_ie1_ids(ie1_root)
    ie2_ids = extract_ie2_ids(ie2_root)
    mapping: dict[str, str] = {}

    for old in ie1_ids:
        direct = f"IE_{old}"
        if direct in ie2_ids:
            mapping[old] = direct

    for old, new in EXPLICIT.items():
        if old in ie1_ids or old.endswith("_DATA_CARD") or old in {"INFINITY_CROWN", "INFINITY_BLADE"}:
            if new in ie2_ids:
                mapping[old] = new

    for new in ie2_ids:
        prefix = "IE_MOB_DATA_CARD_"
        if new.startswith(prefix) and new != "IE_MOB_DATA_CARD_EMPTY":
            mob = new.removeprefix(prefix)
            mapping.setdefault(f"{mob}_DATA_CARD", new)

    if "IE_MOB_DATA_CARD_ENDERMAN" in ie2_ids:
        mapping["ENDERMEN_DATA_CARD"] = "IE_MOB_DATA_CARD_ENDERMAN"

    return mapping, ie1_ids, ie2_ids


def scan_magic_external_ids() -> set[str]:
    ids: set[str] = set()
    for path in ROOT.rglob("*.yml"):
        if any(part in {".git", "_refs", "audit"} for part in path.parts):
            continue
        pending = False
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            if MATERIAL_TYPE.match(line):
                pending = True
                continue
            if pending:
                match = MATERIAL.match(line)
                if match:
                    for value in match.group(3).split("|"):
                        token = value.strip()
                        if token and not token.startswith("MAGIC_"):
                            ids.add(token)
                    pending = False
                elif line.strip() and not line.startswith((" ", "\t")):
                    pending = False
    return ids


def replace_tokens(text: str, mapping: dict[str, str], counts: Counter[str]) -> str:
    for old in sorted(mapping, key=len, reverse=True):
        new = mapping[old]
        pattern = re.compile(rf"(?<![{TOKEN_CHARS}]){re.escape(old)}(?![{TOKEN_CHARS}])")
        text, n = pattern.subn(new, text)
        if n:
            counts[old] += n
    return text


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ie1", required=True, type=Path)
    parser.add_argument("--ie2", required=True, type=Path)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--report", type=Path, default=ROOT / "audit" / "ie2-migration.json")
    args = parser.parse_args()

    mapping, ie1_ids, ie2_ids = build_mapping(args.ie1, args.ie2)
    external = scan_magic_external_ids()
    applicable = {old: new for old, new in mapping.items() if old in external}

    for old in sorted(external):
        if old.startswith("QUARRY_OSCILLATOR_"):
            applicable[old] = f"IE_OSCILLATOR_{old.removeprefix('QUARRY_OSCILLATOR_')}"

    counts: Counter[str] = Counter()
    changed_files: list[str] = []
    for path in sorted(ROOT.rglob("*.yml")):
        if any(part in {".git", "_refs", "audit"} for part in path.parts):
            continue
        original = path.read_text(encoding="utf-8", errors="replace")
        updated = replace_tokens(original, applicable, counts)
        if updated != original:
            changed_files.append(path.relative_to(ROOT).as_posix())
            if args.write:
                path.write_text(updated, encoding="utf-8", newline="\n")

    unresolved = sorted(item for item in external if item in ie1_ids and item not in applicable)
    report_data = {
        "ie1_static_id_count": len(ie1_ids),
        "ie2_known_id_count": len(ie2_ids),
        "magic_external_id_count": len(external),
        "applicable_mapping": dict(sorted(applicable.items())),
        "replacement_counts": dict(sorted(counts.items())),
        "total_replacements": sum(counts.values()),
        "changed_files": changed_files,
        "unresolved_confirmed_ie1_ids": unresolved,
        "write_mode": args.write,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report_data, indent=2) + "\n", encoding="utf-8")

    print(f"Confirmed mappings used: {len(applicable)}")
    print(f"Token replacements: {sum(counts.values())}")
    print(f"Changed YAML files: {len(changed_files)}")
    if unresolved:
        print("Unresolved confirmed IE1 IDs:")
        for item in unresolved:
            print(f"  - {item}")
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
