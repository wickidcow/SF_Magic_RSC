#!/usr/bin/env python3
"""Apply safe runtime-only compatibility fixes to a staged Magic RSC folder.

The source pack intentionally preserves historical/internal IDs. This script only
normalizes defects that are unsafe or noisy on the maintained Legacy/IE2 stack.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

TOP_LEVEL_KEY = re.compile(r"^([A-Za-z0-9_.-]+):\s*(?:#.*)?$")

OLD_IE1_WORKBENCH = "io.github.mooy1.infinityexpansion.items.blocks.InfinityWorkbench"
IE2_WORKBENCH = "net.guizhanss.infinityexpansion2.implementation.items.machines.InfinityWorkbench"

# Current IDs verified against the maintained addon sources. These replacements are
# deliberately Magic-runtime-only so RSC does not globally claim generic historical IDs.
RUNTIME_ITEM_ID_REPLACEMENTS = {
    # DynaTech now derives Slimefun IDs from its dynatech:* NamespacedKeys.
    "VEX_GEM | DYNATECH_VEX_GEM": "DYNATECH_VEX_GEM",
    "VEX_GEM": "DYNATECH_VEX_GEM",
    "BEE | DT_BEE": "DYNATECH_BEE",
    "DT_BEE | BEE": "DYNATECH_BEE",
    "DT_BEE": "DYNATECH_BEE",
    "GROWTH_CHAMBER_MK2 | DYNATECH_GROWTH_CHAMBER_MARK_2 | DT_GROWTH_CHAMBER_MK2 | MAGIC_GROWTH_CHAMBER_MK2": "DYNATECH_GROWTH_CHAMBER_MK2",
    "GROWTH_CHAMBER_MK2": "DYNATECH_GROWTH_CHAMBER_MK2",
    # Current GeneticChickengineering-Reborn exposes tier 3, not the old tier 10 ID.
    "GCE_EXCITATION_CHAMBER_10 | GCE_EXCITATION_CHAMBER_3": "GCE_EXCITATION_CHAMBER_3",
}

# These machines depend on optional third-party Slimefun items. The IDs below were
# verified against their current owners where an owner could be identified. If the
# provider is not installed/registered, the machine should be intentionally omitted
# rather than entering recipe parsing and producing a chain of unresolved-item errors.
OPTIONAL_MACHINE_CONDITIONS: dict[str, dict[str, list[str]]] = {
    "template_machines.yml": {
        "MAGIC_BEE_HOUSE_1": ["FN_MACHINERY_COMPONENT_PART"],
    },
    "mat_generators.yml": {
        "MAGIC_END_ESSENCE_MACHINE": ["STABLEINGOT"],
        "MAGIC_PLASTIC_SHEET_MACHINE": ["FN_FAL_RECYCLER_3"],
    },
    "recipe_machines.yml": {
        "MAGIC_ORIGIN_BASIC_INGOT_FORMER": ["FN_FAL_CONDENSER_3"],
        "MAGIC_ORIGIN_PRESS": ["FN_FAL_COMPRESSOR_3"],
    },
}


def split_top_level_blocks(lines: list[str]) -> tuple[list[str], list[tuple[str, list[str]]]]:
    starts: list[tuple[int, str]] = []
    for index, line in enumerate(lines):
        match = TOP_LEVEL_KEY.match(line.rstrip("\r\n"))
        if match:
            starts.append((index, match.group(1)))

    if not starts:
        return lines[:], []

    preamble = lines[: starts[0][0]]
    blocks: list[tuple[str, list[str]]] = []
    for pos, (start, key) in enumerate(starts):
        end = starts[pos + 1][0] if pos + 1 < len(starts) else len(lines)
        blocks.append((key, lines[start:end]))
    return preamble, blocks


def dedupe_top_level_yaml(path: Path, drop_keys: set[str] | None = None) -> None:
    drop_keys = drop_keys or set()
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    preamble, blocks = split_top_level_blocks(lines)

    if not blocks:
        return

    # Keep the last occurrence. This matches the effective value historically
    # used by permissive YAML loaders while eliminating Bukkit duplicate-key warnings.
    seen: set[str] = set()
    keep_reversed: list[tuple[str, list[str]]] = []
    for key, block in reversed(blocks):
        if key in drop_keys or key in seen:
            continue
        seen.add(key)
        keep_reversed.append((key, block))

    rebuilt = preamble[:]
    for _, block in reversed(keep_reversed):
        rebuilt.extend(block)

    path.write_text("".join(rebuilt), encoding="utf-8")


def replace_material_id(text: str, old: str, new: str) -> str:
    """Replace a complete YAML material value while preserving indentation/comments."""
    return re.sub(
        rf"(?m)^(\s*material:\s*){re.escape(old)}(\s*(?:#.*)?)$",
        lambda m: f"{m.group(1)}{new}{m.group(2)}",
        text,
    )


def replace_runtime_references(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    updated = text.replace(OLD_IE1_WORKBENCH, IE2_WORKBENCH)

    # IE2 now owns the real powered-bedrock item. Do not keep Magic's old
    # compatibility alias registered under the legacy ID.
    updated = replace_material_id(updated, "POWERED_BEDROCK", "IE_POWERED_BEDROCK")

    for old_id, new_id in RUNTIME_ITEM_ID_REPLACEMENTS.items():
        updated = replace_material_id(updated, old_id, new_id)

    # Keep the in-game version sheet aligned with the actual drop-in release.
    updated = updated.replace("&eRelease-1.1.16", "&eLegacy-1.1.17")

    if updated != text:
        path.write_text(updated, encoding="utf-8")


def indentation(line: str) -> int:
    return len(line) - len(line.lstrip(" "))


def add_itemexist_conditions(block: list[str], item_ids: list[str]) -> list[str]:
    missing = [item_id for item_id in item_ids if f"itemexist {item_id}" not in "".join(block)]
    if not missing:
        return block

    register_index = next(
        (i for i, line in enumerate(block[1:], start=1) if re.match(r"^  register:\s*(?:#.*)?$", line.rstrip("\r\n"))),
        None,
    )

    condition_lines = [f"      - 'itemexist {item_id}'\n" for item_id in missing]

    if register_index is None:
        insertion = [
            "  register:\n",
            "    warn: false\n",
            "    conditions:\n",
            *condition_lines,
        ]
        return block[:1] + insertion + block[1:]

    # Find the end of the register mapping (the next top-level property at two spaces).
    register_end = len(block)
    for i in range(register_index + 1, len(block)):
        stripped = block[i].strip()
        if stripped and not stripped.startswith("#") and indentation(block[i]) <= 2:
            register_end = i
            break

    conditions_index = next(
        (
            i
            for i in range(register_index + 1, register_end)
            if re.match(r"^    conditions:\s*(?:#.*)?$", block[i].rstrip("\r\n"))
        ),
        None,
    )

    if conditions_index is None:
        return block[:register_end] + ["    conditions:\n", *condition_lines] + block[register_end:]

    # Append to the existing conditions list, before the next register property.
    conditions_end = register_end
    for i in range(conditions_index + 1, register_end):
        stripped = block[i].strip()
        if stripped and not stripped.startswith("#") and indentation(block[i]) <= 4:
            conditions_end = i
            break

    return block[:conditions_end] + condition_lines + block[conditions_end:]


def gate_optional_machines(path: Path, requirements: dict[str, list[str]]) -> None:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    preamble, blocks = split_top_level_blocks(lines)
    if not blocks:
        return

    found: set[str] = set()
    rebuilt = preamble[:]
    for key, block in blocks:
        if key in requirements:
            found.add(key)
            block = add_itemexist_conditions(block, requirements[key])
        rebuilt.extend(block)

    missing_keys = set(requirements) - found
    if missing_keys:
        missing = ", ".join(sorted(missing_keys))
        raise RuntimeError(f"Could not find expected optional Magic machine(s) in {path.name}: {missing}")

    path.write_text("".join(rebuilt), encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: apply_runtime_fixes.py <staged Magic folder>")

    root = Path(sys.argv[1]).resolve()
    if not root.is_dir():
        raise SystemExit(f"Not a directory: {root}")

    for yaml_file in sorted(root.glob("*.yml")):
        drops = {"MAGIC_POWERED_BEDROCK_COMPAT"} if yaml_file.name == "items.yml" else set()
        dedupe_top_level_yaml(yaml_file, drops)

    for path in sorted(root.rglob("*")):
        if path.is_file() and path.suffix.lower() in {".yml", ".yaml", ".js"}:
            replace_runtime_references(path)

    for file_name, requirements in OPTIONAL_MACHINE_CONDITIONS.items():
        path = root / file_name
        if not path.is_file():
            raise RuntimeError(f"Missing expected Magic runtime file: {file_name}")
        gate_optional_machines(path, requirements)

    print("Applied Magic Legacy runtime compatibility fixes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
