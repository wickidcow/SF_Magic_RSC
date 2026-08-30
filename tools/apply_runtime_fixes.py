#!/usr/bin/env python3
"""Build-time cleanup for the Magic RSC Legacy drop-in."""
from __future__ import annotations

import ast
import json
import re
import sys
from pathlib import Path

TOP = re.compile(r"^([A-Za-z0-9_.-]+):\s*(?:#.*)?$")
LORE = re.compile(r"^(\s*)lore:\s*(?:#.*)?$")
LIST = re.compile(r"^(\s*)-\s*(.*?)\s*$")
NAME = re.compile(r"^\s*(?:name|display-name):\s*(.+?)\s*$")
LEGACY_COLOR = re.compile(r"(?i)(?:§|&)[0-9A-FK-ORX]")
HEX_COLOR = re.compile(r"(?i)(?:&#[0-9A-F]{6}|\{#[0-9A-F]{6}\})")

OLD_IE1_WORKBENCH = "io.github.mooy1.infinityexpansion.items.blocks.InfinityWorkbench"
IE2_WORKBENCH = "net.guizhanss.infinityexpansion2.implementation.items.machines.InfinityWorkbench"

ITEM_ID_REPLACEMENTS = {
    "VEX_DATA_CARD": "IE_MOB_DATA_CARD_VEX",
    "PHANTOM_DATA_CARD": "IE_MOB_DATA_CARD_PHANTOM",
    "VEX_GEM | DYNATECH_VEX_GEM": "DYNATECH_VEX_GEM",
    "VEX_GEM": "DYNATECH_VEX_GEM",
    "BEE | DT_BEE": "DYNATECH_BEE",
    "DT_BEE | BEE": "DYNATECH_BEE",
    "DT_BEE": "DYNATECH_BEE",
    "GROWTH_CHAMBER_MK2 | DYNATECH_GROWTH_CHAMBER_MARK_2 | DT_GROWTH_CHAMBER_MK2 | MAGIC_GROWTH_CHAMBER_MK2": "DYNATECH_GROWTH_CHAMBER_MK2",
    "GROWTH_CHAMBER_MK2": "DYNATECH_GROWTH_CHAMBER_MK2",
    "GCE_EXCITATION_CHAMBER_10 | GCE_EXCITATION_CHAMBER_3": "GCE_EXCITATION_CHAMBER_3",
}

# Only names/display names/lore are translated. Internal recipe keys and script names
# remain untouched so localization cannot change RSC behavior.
PLAYER_TEXT = {
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

OPTIONAL = {
    "template_machines.yml": {"MAGIC_BEE_HOUSE_1": ["FN_MACHINERY_COMPONENT_PART"]},
    "mat_generators.yml": {
        "MAGIC_END_ESSENCE_MACHINE": ["STABLEINGOT"],
        "MAGIC_PLASTIC_SHEET_MACHINE": ["FN_FAL_RECYCLER_3"],
    },
    "recipe_machines.yml": {
        "MAGIC_ORIGIN_BASIC_INGOT_FORMER": ["FN_FAL_CONDENSER_3"],
        "MAGIC_ORIGIN_PRESS": ["FN_FAL_COMPRESSOR_3"],
    },
}


def indent(line: str) -> int:
    return len(line) - len(line.lstrip(" \t"))


def blocks(lines: list[str]) -> tuple[list[str], list[tuple[str, list[str]]]]:
    starts = [(i, m.group(1)) for i, line in enumerate(lines) if (m := TOP.match(line.rstrip("\r\n")))]
    if not starts:
        return lines[:], []
    out = []
    for pos, (start, key) in enumerate(starts):
        end = starts[pos + 1][0] if pos + 1 < len(starts) else len(lines)
        out.append((key, lines[start:end]))
    return lines[: starts[0][0]], out


def dedupe_top(path: Path, drops: set[str]) -> None:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    pre, found = blocks(lines)
    if not found:
        return
    seen: set[str] = set()
    kept = []
    for key, block in reversed(found):
        if key in drops or key in seen:
            continue
        seen.add(key)
        kept.append((key, block))
    path.write_text("".join(pre + [line for _, block in reversed(kept) for line in block]), encoding="utf-8")


def replace_material(text: str, old: str, new: str) -> str:
    return re.sub(
        rf"(?m)^(\s*material:\s*){re.escape(old)}(\s*(?:#.*)?)$",
        lambda m: f"{m.group(1)}{new}{m.group(2)}",
        text,
    )


def compatibility(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    updated = text.replace(OLD_IE1_WORKBENCH, IE2_WORKBENCH)
    updated = replace_material(updated, "POWERED_BEDROCK", "IE_POWERED_BEDROCK")
    for old, new in ITEM_ID_REPLACEMENTS.items():
        updated = replace_material(updated, old, new)
    updated = updated.replace("&eRelease-1.1.16", "&eLegacy-1.1.17")
    if updated != text:
        path.write_text(updated, encoding="utf-8")


def unquote(raw: str) -> str:
    value = raw.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        try:
            parsed = ast.literal_eval(value)
            if isinstance(parsed, str):
                return parsed
        except (SyntaxError, ValueError):
            return value[1:-1]
    return value


def json_text(value: str) -> str | None:
    value = value.strip()
    if not value.startswith(("{", "[")):
        return None
    try:
        data = json.loads(value)
    except json.JSONDecodeError:
        return None
    parts: list[str] = []

    def walk(node: object) -> None:
        if isinstance(node, dict):
            if isinstance(node.get("text"), str):
                parts.append(node["text"])
            for key, child in node.items():
                if key != "text":
                    walk(child)
        elif isinstance(node, list):
            for child in node:
                walk(child)

    walk(data)
    return "".join(parts)


def visible(raw: str) -> str:
    value = unquote(raw)
    value = json_text(value) or value
    value = HEX_COLOR.sub("", value)
    value = LEGACY_COLOR.sub("", value)
    return re.sub(r"\s+", " ", value).strip()


def translate_line(line: str) -> str:
    for source, target in sorted(PLAYER_TEXT.items(), key=lambda item: len(item[0]), reverse=True):
        line = line.replace(source, target)
    return line


def localize(path: Path) -> int:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    lore_indent: int | None = None
    changed = 0
    out = []
    for line in lines:
        stripped = line.strip()
        current = indent(line)
        if m := LORE.match(line.rstrip("\r\n")):
            lore_indent = len(m.group(1))
            out.append(line)
            continue
        in_lore = lore_indent is not None and current > lore_indent
        if lore_indent is not None and stripped and not stripped.startswith("#") and current <= lore_indent:
            lore_indent = None
            in_lore = False
        if NAME.match(line.rstrip("\r\n")) or (in_lore and LIST.match(line.rstrip("\r\n"))):
            newer = translate_line(line)
            if newer != line:
                changed += 1
                line = newer
        out.append(line)
    if changed:
        path.write_text("".join(out), encoding="utf-8")
    return changed


def dedupe_lore(path: Path) -> int:
    """Remove repeated visible text anywhere inside the same lore block."""
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    lore_indent: int | None = None
    seen: set[str] = set()
    removed = 0
    out = []
    for line in lines:
        stripped = line.strip()
        current = indent(line)
        if m := LORE.match(line.rstrip("\r\n")):
            lore_indent = len(m.group(1))
            seen = set()
            out.append(line)
            continue
        in_lore = lore_indent is not None and current > lore_indent
        if lore_indent is not None and stripped and not stripped.startswith("#") and current <= lore_indent:
            lore_indent = None
            seen = set()
            in_lore = False
        if in_lore and (m := LIST.match(line.rstrip("\r\n"))):
            text = visible(m.group(2))
            if text:
                key = text.casefold()
                if key in seen:
                    removed += 1
                    continue
                seen.add(key)
        out.append(line)
    if removed:
        path.write_text("".join(out), encoding="utf-8")
    return removed


def add_conditions(block: list[str], item_ids: list[str]) -> list[str]:
    missing = [item for item in item_ids if f"itemexist {item}" not in "".join(block)]
    if not missing:
        return block
    reg = next((i for i, line in enumerate(block[1:], 1) if re.match(r"^  register:\s*(?:#.*)?$", line.rstrip("\r\n"))), None)
    cond_lines = [f"      - 'itemexist {item}'\n" for item in missing]
    if reg is None:
        return block[:1] + ["  register:\n", "    warn: false\n", "    conditions:\n", *cond_lines] + block[1:]
    end = len(block)
    for i in range(reg + 1, len(block)):
        s = block[i].strip()
        if s and not s.startswith("#") and indent(block[i]) <= 2:
            end = i
            break
    cond = next((i for i in range(reg + 1, end) if re.match(r"^    conditions:\s*(?:#.*)?$", block[i].rstrip("\r\n"))), None)
    if cond is None:
        return block[:end] + ["    conditions:\n", *cond_lines] + block[end:]
    cond_end = end
    for i in range(cond + 1, end):
        s = block[i].strip()
        if s and not s.startswith("#") and indent(block[i]) <= 4:
            cond_end = i
            break
    return block[:cond_end] + cond_lines + block[cond_end:]


def gate(path: Path, required: dict[str, list[str]]) -> None:
    pre, found = blocks(path.read_text(encoding="utf-8").splitlines(keepends=True))
    if not found:
        return
    seen = set()
    out = pre[:]
    for key, block in found:
        if key in required:
            seen.add(key)
            block = add_conditions(block, required[key])
        out.extend(block)
    missing = set(required) - seen
    if missing:
        raise RuntimeError(f"Missing expected Magic object(s) in {path.name}: {', '.join(sorted(missing))}")
    path.write_text("".join(out), encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: apply_runtime_fixes.py <staged Magic folder>")
    root = Path(sys.argv[1]).resolve()
    if not root.is_dir():
        raise SystemExit(f"Not a directory: {root}")

    for path in sorted(root.glob("*.yml")):
        dedupe_top(path, {"MAGIC_POWERED_BEDROCK_COMPAT"} if path.name == "items.yml" else set())

    translated = 0
    removed = 0
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        if path.suffix.lower() in {".yml", ".yaml", ".js"}:
            compatibility(path)
        if path.suffix.lower() in {".yml", ".yaml"}:
            translated += localize(path)
            removed += dedupe_lore(path)

    for filename, required in OPTIONAL.items():
        path = root / filename
        if not path.is_file():
            raise RuntimeError(f"Missing expected Magic runtime file: {filename}")
        gate(path, required)

    print(f"Applied Magic Legacy runtime fixes ({translated} player-facing line(s) translated, {removed} duplicate lore line(s) removed)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
