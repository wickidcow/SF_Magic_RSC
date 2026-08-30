#!/usr/bin/env python3
"""Apply safe compatibility and player-facing cleanup to a staged Magic RSC folder.

Historical/internal IDs remain source-compatible where needed, while the packaged
Legacy runtime is normalized for the maintained Slimefun Legacy/IE2 stack. The
runtime pass also removes accidental consecutive duplicate lore and translates a
verified set of recurring player-facing Chinese strings without touching data keys.
"""

from __future__ import annotations

import ast
import json
import re
import sys
from pathlib import Path

TOP_LEVEL_KEY = re.compile(r"^([A-Za-z0-9_.-]+):\s*(?:#.*)?$")
LORE_RE = re.compile(r"^(\s*)lore:\s*(?:#.*)?$")
LIST_RE = re.compile(r"^(\s*)-\s*(.*?)\s*$")
NAME_RE = re.compile(r"^\s*(?:name|display-name):\s*(.+?)\s*$")
LEGACY_COLOR_RE = re.compile(r"(?i)(?:§|&)[0-9A-FK-ORX]")
HEX_COLOR_RE = re.compile(r"(?i)(?:&#[0-9A-F]{6}|\{#[0-9A-F]{6}\})")

OLD_IE1_WORKBENCH = "io.github.mooy1.infinityexpansion.items.blocks.InfinityWorkbench"
IE2_WORKBENCH = "net.guizhanss.infinityexpansion2.implementation.items.machines.InfinityWorkbench"

# Current IDs verified against the maintained addon sources. These replacements are
# deliberately Magic-runtime-only so RSC does not globally claim generic historical IDs.
RUNTIME_ITEM_ID_REPLACEMENTS = {
    # InfinityExpansion2 mob simulation cards use IE_MOB_DATA_CARD_<MOB> IDs.
    "VEX_DATA_CARD": "IE_MOB_DATA_CARD_VEX",
    "PHANTOM_DATA_CARD": "IE_MOB_DATA_CARD_PHANTOM",
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

# First verified localization batch. Replacements are only applied to item names,
# display names, and lore lines. Recipe labels, script names, object IDs, comments,
# and other internal YAML keys are intentionally left untouched.
PLAYER_TEXT_REPLACEMENTS = {
    "切记！该魔法为一次性魔法！拆除需要移出所有物品！": "Warning! This magic is single-use. Remove all items before breaking it!",
    "网络单元拥有54格空间(相当于一个大箱子)": "Network Cell has 54 slots (the size of a double chest)",
    "这是一把专为萌新法师打造的匕首，拥有神秘的力量。": "A dagger made for novice mages, imbued with mysterious power.",
    "它能帮助你在冒险旅程中更加自信和强大。": "It helps new adventurers grow stronger and more confident.",
    "不使用网络扳手也能拆除该模型方块": "This model block can be broken without a network wrench",
    "网桥用于连接不同的网络物品": "Network Bridges connect different network items",
    "可以通过网络访问其中的物品": "Items inside can be accessed through the network",
    "放置后再破坏将失去现有皮肤": "Breaking this after placement removes its current skin",
    "支持网络快速输入/输出": "Supports fast network input/output",
    "来形成一个完整的网络": "to form one complete network",
    "更加清晰的布局网络": "for a cleaner network layout",
    "可以将伤害转化为自己的生命": "Converts incoming damage into health",
    "萌新法师匕首": "Novice Mage Dagger",
    "网络单元(箱子)": "Network Cell (Chest)",
    "网络单元（箱子）": "Network Cell (Chest)",
    "每种物品可容纳": "Capacity per item: ",
    "鞘翅动能免疫": "Elytra Kinetic Immunity",
    "蜜蜂毒针保护": "Bee Sting Protection",
    "更纯粹的力量": "Purer Power",
    "潮涌能量": "Conduit Power",
    "生命恢复": "Regeneration",
    "生命提升": "Health Boost",
    "火焰抗性": "Fire Resistance",
    "海豚的恩惠": "Dolphin's Grace",
    "抗辐射": "Radiation Resistance",
    "灵魂绑定": "Soulbound",
    "腐竹的爱": "Owner's Love",
    "可储存": "Storage: ",
    "也可以直接打开": "Can also be opened directly",
    "电力魔法工厂": "Electric Magic Factory",
    "网络单元": "Network Cell",
    "网桥": "Network Bridge",
    "起源": "Origin",
    "护腿": "Leggings",
    "头盔": "Helmet",
    "胸甲": "Chestplate",
    "靴子": "Boots",
    "机器": "Machine",
    "模型": "Model",
    "急迫": "Haste",
    "饱和": "Saturation",
    "夜视": "Night Vision",
    "力量": "Strength",
    "速度": "Speed",
    "（透明）": " (Transparent)",
    "（铁栅栏）": " (Iron Bars)",
    "（遮光）": " (Opaque)",
    "之剑": " Sword",
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


def replace_player_text(line: str) -> str:
    updated = line
    for source, target in sorted(PLAYER_TEXT_REPLACEMENTS.items(), key=lambda pair: len(pair[0]), reverse=True):
        updated = updated.replace(source, target)
    return updated


def translate_player_facing_text(path: Path) -> int:
    """Translate only names/display names/lore, never recipe/data keys."""
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    lore_indent: int | None = None
    changed = 0
    rebuilt: list[str] = []

    for line in lines:
        stripped = line.strip()
        current_indent = indentation(line)
        lore_match = LORE_RE.match(line.rstrip("\r\n"))
        if lore_match:
            lore_indent = len(lore_match.group(1))
            rebuilt.append(line)
            continue

        in_lore = lore_indent is not None and current_indent > lore_indent
        if lore_indent is not None and stripped and not stripped.startswith("#") and current_indent <= lore_indent:
            lore_indent = None
            in_lore = False

        eligible = NAME_RE.match(line.rstrip("\r\n")) is not None or (in_lore and LIST_RE.match(line.rstrip("\r\n")) is not None)
        if eligible:
            translated = replace_player_text(line)
            if translated != line:
                changed += 1
                line = translated
        rebuilt.append(line)

    if changed:
        path.write_text("".join(rebuilt), encoding="utf-8")
    return changed


def dedupe_consecutive_lore(path: Path) -> int:
    """Remove accidental repeated visible lore lines while preserving separators."""
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    lore_indent: int | None = None
    previous_visible: str | None = None
    removed = 0
    rebuilt: list[str] = []

    for line in lines:
        stripped = line.strip()
        current_indent = indentation(line)
        lore_match = LORE_RE.match(line.rstrip("\r\n"))
        if lore_match:
            lore_indent = len(lore_match.group(1))
            previous_visible = None
            rebuilt.append(line)
            continue

        in_lore = lore_indent is not None and current_indent > lore_indent
        if lore_indent is not None and stripped and not stripped.startswith("#") and current_indent <= lore_indent:
            lore_indent = None
            previous_visible = None
            in_lore = False

        if in_lore:
            list_match = LIST_RE.match(line.rstrip("\r\n"))
            if list_match:
                visible = normalize_visible_text(list_match.group(2))
                if visible:
                    key = visible.casefold()
                    if previous_visible == key:
                        removed += 1
                        continue
                    previous_visible = key
                else:
                    # Blank/color-only entries are intentional visual separators.
                    previous_visible = None
            elif stripped and not stripped.startswith("#"):
                previous_visible = None

        rebuilt.append(line)

    if removed:
        path.write_text("".join(rebuilt), encoding="utf-8")
    return removed


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

    translated_lines = 0
    duplicate_lore_removed = 0
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        if path.suffix.lower() in {".yml", ".yaml", ".js"}:
            replace_runtime_references(path)
        if path.suffix.lower() in {".yml", ".yaml"}:
            translated_lines += translate_player_facing_text(path)
            duplicate_lore_removed += dedupe_consecutive_lore(path)

    for file_name, requirements in OPTIONAL_MACHINE_CONDITIONS.items():
        path = root / file_name
        if not path.is_file():
            raise RuntimeError(f"Missing expected Magic runtime file: {file_name}")
        gate_optional_machines(path, requirements)

    print(
        "Applied Magic Legacy runtime fixes "
        f"({translated_lines} player-facing line(s) translated, "
        f"{duplicate_lore_removed} duplicate lore line(s) removed)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
