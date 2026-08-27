#!/usr/bin/env python3
"""Apply high-confidence English translations to Magic player-facing text only.

This intentionally does NOT translate identifiers, recipe keys, materials, class names,
or other internal configuration. It edits only the same YAML/JS display surfaces audited
by audit_player_facing_english.py.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HAN = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")
FORMAT = re.compile(r"(?:\{#[0-9A-Fa-f]{6}\}|&[0-9A-FK-ORa-fk-or]|§[0-9A-FK-ORa-fk-or])")
SCALAR_KEYS = {"name", "title", "description", "geo_name"}
LIST_KEYS = {"lore", "loadStartTexts", "enabledTexts"}
SKIP_DIRS = {".git", ".github", "audit", "dist"}

# Longest/highest-confidence phrases first. Replacements keep existing surrounding
# formatting where possible and are intentionally limited to player-facing lines.
PHRASES = {
    "采用现实时间算法（独立于粘液刻之外）": "Uses real-time timing (independent of Slimefun ticks)",
    "切记！该魔法为一次性魔法！拆除需要移出所有物品！": "Important! This is a one-time enchantment. Remove all items before dismantling!",
    "不使用网络扳手也能拆除该模型方块": "This model block can be removed without a network wrench",
    "支持网络快速输入/输出": "Supports fast network input/output",
    "使用后此核心将会融入到魔法抽屉中": "This core merges into the Magic Drawer when used",
    "放置后再破坏将失去现有皮肤": "Breaking it after placement removes the current skin",
    "左手持物品，右手持矛盾的结晶体": "Hold the item in your off hand and the Contradictory Crystal in your main hand",
    "经验已成功注入魔法学识之瓶": "Experience was successfully infused into the Bottle of Magical Knowledge",
    "这个是个不可充电物品": "This item cannot be recharged",
    "副手需要持有物品": "You must hold an item in your off hand",
    "请使用主手进行操作": "Use your main hand for this action",
    "无法找到世界 '": "Could not find world '",
    "请用主手持物品": "Please hold an item in your main hand",
    "主手请持物品": "Please hold an item in your main hand",
    "用于修复封印的魔法抽屉": "Used to repair a sealed Magic Drawer",
    "方便放入背包进行运输": "Easy to carry in your inventory",
    "发射出返老还童光线": "Fires a rejuvenation beam",
    "使用电力模拟鸡生产": "Simulates chicken production using electricity",
    "结合无尽锭的力量": "Harnesses the power of Infinite Ingots",
    "用魔法协助蜜蜂": "Uses magic to assist bees",
    "结合电力帮你": "Uses electricity to help you",
    "机器生产物品的合成材料": "Crafting material produced by machines",
    "提高 99% 挖到此矿的几率": "Increases the chance of mining this ore by 99%",
    "放置在魔法矿机中": "Place this in a Magic Geo-Miner",
    "鸡孵蛋的地方": "A place for chickens to hatch eggs",
    "愿国宝生生不息": "May this treasured species thrive forever",
    "正在合成中---": "Crafting in progress---",
    "当前生物数量限制：": "Current mob limit: ",
    "范围内生物数量：": "Mobs in range: ",
    "刷怪笼等级：": "Spawner tier: ",
    "生成间隔：": "Spawn interval: ",
    "每个生物": "per mob",
    "可存储": "Storage",
    "可储存": "Storage",
    "每种物品可容纳": "Capacity per item",
    "实验性机器": "Experimental Machine",
    "实验性道具": "Experimental Item",
    "终极 机器": "Ultimate Machine",
    "至尊 机器": "Supreme Machine",
    "高级 机器": "Advanced Machine",
    "萌新专属机器": "Novice Machine",
    "魔法机器": "Magic Machine",
    "魔法材料": "Magic Material",
    "魔法芯片": "Magic Chip",
    "魔法卡片": "Magic Card",
    "魔法动物": "Magical Creature",
    "自然元素": "Natural Element",
    "起源机器": "Origin Machine",
    "多方块机器": "Multiblock Machine",
    "魔法卡片复制机": "Magic Card Duplicator",
    "生产材料": "Production Material",
    "让蜜蜂来帮你": "Let the bees help you",
    "插入无尽生物模拟室中使用": "Insert into the Infinity Mob Simulation Chamber",
    "需要防化服": "Hazmat suit required",
    "魔法加速 : 生效中": "Magic Acceleration: Active",
    "生产速率 : 20秒": "Production rate: 20 seconds",
    "蜜蜂数量 : 192": "Bee count: 192",
    "右侧为输出槽": "Output slots are on the right",
    "左侧为输入槽": "Input slots are on the left",
    "上方为输入槽": "Input slots are above",
    "这里放食物": "Place food here",
    "这里放魔法鸡": "Place a Magic Chicken here",
    "在此放入物品": "Place item here",
    "放入燃料": "Insert fuel",
    "反应堆废料": "Reactor Waste",
    "右键任意地方": "Right-click anywhere",
    "右键方块": "Right-click a block",
    "点击合成": "Click to Craft",
    "产物": "Output",
    "提示": "Info",
    "信息": "Information",
    "输入/输出": "Input/Output",
    "输入": "Input",
    "输出": "Output",
    "这就是一个占位符而已": "This is only a placeholder",
    "并且给予新的皮肤": "and applies a new skin",
    "更纯粹的力量": "A Purer Power",
    "矩阵台": "Matrix Platform",
    "魔法矩阵": "Magic Matrix",
    "放置在支持的矩阵台下方": "Place below a supported Matrix Platform",
    "放置在支持的魔法矩阵上方": "Place above a supported Magic Matrix",
    "说明": "Description",
    "盔甲韧性": "Armor Toughness",
    "装备属性": "Equipment Attribute",
    "魔法-增幅": "Magic - Amplification",
    "魔法-能源与电力": "Magic - Energy & Power",
    "魔法-生物快捷运输": "Magic - Biological Transport",
    "魔法-抽屉": "Magic - Storage Drawers",
    "魔法-装备强化": "Magic - Equipment Upgrades",
    "魔法-垂钓": "Magic - Fishing",
    "魔法-生物工程": "Magic - Bioengineering",
    "魔法-至尊工艺": "Magic - Supreme Crafting",
    "魔法-考古学": "Magic - Archaeology",
    "魔法-测试": "Magic - Test Projects",
    "魔法-萌新福利机器": "Magic - Novice Machines",
    "魔法工具": "Magic Tools",
    "魔法道具": "Magic Items",
    "电力魔法工厂-I": "Electric Magic Factory I",
    "原神": "Genshin Impact",
    "初始成功率99%": "Initial success rate: 99%",
    "成功率100%": "Success rate: 100%",
    "9个培育仓 MK2": "9 Growth Chambers MK2",
    "每 30 粘液刻生成一次": "Generates once every 30 Slimefun ticks",
    "每 15 粘液刻生成一次": "Generates once every 15 Slimefun ticks",
    "速度": "Speed",
    "每个生物": "per mob",
    "个生物": " mobs",
    "级": "",
    "秒": " seconds",
}

ENTITY_NAMES = {
    "哞菇牛":"Mooshroom", "女巫":"Witch", "尸壳":"Husk", "洞穴蜘蛛":"Cave Spider",
    "潜影贝":"Shulker", "铁傀儡":"Iron Golem", "鱿鱼":"Squid", "兔子":"Rabbit",
    "北极熊":"Polar Bear", "唤魔者":"Evoker", "岩浆怪":"Magma Cube", "溺尸":"Drowned",
    "烈焰人":"Blaze", "蠹虫":"Silverfish", "马":"Horse", "僵尸猪灵":"Zombified Piglin",
    "卫道士":"Vindicator", "幻翼":"Phantom", "末影人":"Enderman", "猪":"Pig", "绵羊":"Sheep",
    "蜘蛛":"Spider", "史莱姆":"Slime", "末影螨":"Endermite", "熊猫":"Panda", "牛":"Cow",
    "狼":"Wolf", "猪灵蛮兵":"Piglin Brute", "猫":"Cat", "监守者":"Warden", "僵尸":"Zombie",
    "守卫者":"Guardian", "恶魂":"Ghast", "村民":"Villager", "流浪者":"Stray", "苦力怕":"Creeper",
    "豹猫":"Ocelot", "远古守卫者":"Elder Guardian", "骷髅":"Skeleton", "鸡":"Chicken",
    "僵尸村民":"Zombie Villager", "凋零骷髅":"Wither Skeleton", "发光鱿鱼":"Glow Squid",
    "末影龙":"Ender Dragon", "凋零":"Wither", "猪灵":"Piglin", "掠夺者":"Pillager",
    "劫掠兽":"Ravager", "狐狸":"Fox", "山羊":"Goat", "海豚":"Dolphin", "蜜蜂":"Bee",
}

# Frequently-used proper names in Genshin-themed player text.
PROPER_NAMES = {
    "雷电将军":"Raiden Shogun", "钟离":"Zhongli", "胡桃":"Hu Tao", "神里绫华":"Kamisato Ayaka",
    "刻晴":"Keqing", "纳西妲":"Nahida", "可莉":"Klee", "甘雨":"Ganyu", "魈":"Xiao",
    "达达利亚":"Tartaglia",
}


def plain_visible(text: str) -> str:
    return FORMAT.sub("", text).replace("&l", "").replace("&k", "")


def translate_spawner_name(text: str) -> str | None:
    visible = plain_visible(text)
    m = re.search(r"魔法刷怪笼[（(]([^）)]+)[）)]", visible)
    if not m:
        return None
    mob = ENTITY_NAMES.get(m.group(1))
    if not mob:
        return None
    # Preserve YAML quoting style while replacing the whole visible display value.
    quote = '"' if text.strip().startswith('"') else "'" if text.strip().startswith("'") else ""
    suffix = quote if quote else ""
    return f'{quote}&d&lMagic Spawner &6({mob}){suffix}'


def translate_line(raw: str) -> str:
    # Special handling for gradient/color-fragmented spawner names.
    special = translate_spawner_name(raw)
    if special is not None:
        prefix = raw[: len(raw) - len(raw.lstrip())]
        # For YAML name/list lines, keep syntax before the display value.
        if ":" in raw and not raw.lstrip().startswith("-"):
            before = raw.split(":", 1)[0] + ": "
            return prefix + before.lstrip() + special
        if raw.lstrip().startswith("-"):
            return prefix + "- " + special

    out = raw
    # Exact proper nouns first, then high-confidence phrase dictionary.
    for source, target in sorted(PROPER_NAMES.items(), key=lambda kv: len(kv[0]), reverse=True):
        out = out.replace(source, target)
    for source, target in sorted(PHRASES.items(), key=lambda kv: len(kv[0]), reverse=True):
        out = out.replace(source, target)
    return out


def yaml_player_lines(lines: list[str]) -> set[int]:
    indexes: set[int] = set()
    active_indent: int | None = None
    for i, raw in enumerate(lines):
        stripped = raw.strip()
        indent = len(raw) - len(raw.lstrip(" "))
        if active_indent is not None:
            if stripped and not stripped.startswith("-") and indent <= active_indent:
                active_indent = None
            elif stripped.startswith("-"):
                indexes.add(i)
                continue
        m = re.match(r"\s*([A-Za-z_][A-Za-z0-9_-]*):\s*(.*)$", raw)
        if not m:
            continue
        key = m.group(1)
        if key in LIST_KEYS:
            active_indent = indent
            indexes.add(i)
        elif key in SCALAR_KEYS:
            indexes.add(i)
    return indexes


def js_is_player_text(raw: str) -> bool:
    hints = ("sendMessage", "sendActionBar", "broadcast", "message", "setDisplayName", "setLore", "sendTitle")
    return any(h in raw for h in hints)


def process(path: Path) -> tuple[int, int]:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    changed = 0
    removed_han = 0
    if path.suffix.lower() in {".yml", ".yaml"}:
        targets = yaml_player_lines(lines)
        for i in targets:
            if not HAN.search(lines[i]):
                continue
            before = lines[i]
            after = translate_line(before)
            if after != before:
                lines[i] = after
                changed += 1
                if HAN.search(before) and not HAN.search(after):
                    removed_han += 1
    elif path.suffix.lower() == ".js":
        for i, before in enumerate(lines):
            if HAN.search(before) and js_is_player_text(before):
                after = translate_line(before)
                if after != before:
                    lines[i] = after
                    changed += 1
                    if not HAN.search(after):
                        removed_han += 1
    if changed:
        newline = "\r\n" if "\r\n" in text else "\n"
        path.write_text(newline.join(lines) + (newline if text.endswith(("\n", "\r\n")) else ""), encoding="utf-8", newline="")
    return changed, removed_han


def main() -> int:
    total = fully = files = 0
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or any(part in SKIP_DIRS for part in path.parts):
            continue
        if path.suffix.lower() not in {".yml", ".yaml", ".js"}:
            continue
        changed, removed = process(path)
        if changed:
            files += 1
            total += changed
            fully += removed
            print(f"{path.relative_to(ROOT)}: {changed} translated lines ({removed} fully English)")
    print(f"Changed {total} player-facing lines across {files} files; {fully} lines now contain no Han text")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
