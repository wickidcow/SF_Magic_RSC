#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
CJK = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")
COLOR = re.compile(r"(?:&[0-9A-FK-ORa-fk-or]|§[0-9A-FK-ORa-fk-or]|\{#[0-9A-Fa-f]{3,6}(?:[<>])?\})")
TOP_KEY = re.compile(r"^([A-Za-z0-9_.-]+):\s*(?:#.*)?$")
SCALAR = re.compile(r"^(\s*)([A-Za-z0-9_.-]+):\s*(['\"])(.*)\3\s*(?:#.*)?$")
LIST_STRING = re.compile(r"^(\s*)-\s*(['\"])(.*)\2\s*(?:#.*)?$")
QUOTED_MAP_KEY = re.compile(r"^\s*(['\"]).*\1:\s*(?:#.*)?$")

INTERNAL_KEYS = {
    "script", "scriptlistener", "function", "event", "condition", "conditions",
    "action", "actions", "material", "material_type", "type", "recipe_type",
    "item_group", "id", "id_alias", "repo", "depends", "plugindepends", "authors",
    "key", "permission", "entity", "entity_type"
}
PLAYER_KEYS = {"name", "title", "description", "displayname", "display_name", "message", "prefix", "suffix"}

WORD_MAP = {
    "NEWPLAYER": "New Player", "SPAWNER": "Spawner", "WITHER": "Wither",
    "SKELETON": "Skeleton", "ZOMBIE": "Zombie", "CREEPER": "Creeper",
    "ENDERMAN": "Enderman", "BLAZE": "Blaze", "CAVE": "Cave", "SPIDER": "Spider",
    "RESOURCE": "Resources", "RESOURCES": "Resources", "MACHINE": "Machine",
    "MACHINES": "Machines", "GENERATOR": "Generator", "GENERATORS": "Generators",
    "AUTHOR": "Credits", "INFO": "Information", "GUIDE": "Guide", "POWER": "Power",
    "BEEHIVE": "Beehive", "INFINITE": "Infinite", "INTERESTING": "Interesting",
    "ORIGIN": "Origin", "COSMIC": "Cosmic", "DUST": "Dust", "REDSTONE": "Redstone",
    "BLISTERING": "Blistering", "INGOT": "Ingot", "EXCITATION": "Excitation",
    "CHAMBER": "Chamber", "QUARTZ": "Quartz", "EXTRACTOR": "Extractor",
    "COBBLESTONE": "Cobblestone", "MAKER": "Maker", "FORMER": "Former",
    "TEST": "Test", "COLOR": "Color", "VERSION": "Version", "GEC": "GEC", "GCE": "GCE"
}

PHRASES = [
    ("需要防化服", "Hazmat Suit required"),
    ("非可再生资源", "Non-renewable resource"),
    ("自然资源", "Natural resource"),
    ("实验性机器", "Experimental machine"),
    ("实验性工具", "Experimental tool"),
    ("挖掘不掉落", "Does not drop when mined"),
    ("刷怪笼等级", "Spawner tier"),
    ("采用现实时间算法", "Uses real-time scheduling"),
    ("独立于粘液刻之外", "independent of Slimefun ticks"),
    ("生成间隔", "Spawn interval"),
    ("范围内生物数量", "Nearby mob count"),
    ("当前生物数量限制", "Current mob limit"),
    ("可存储", "capacity"),
    ("每个生物", "per mob"),
    ("左侧为输入槽", "Input slots are on the left"),
    ("右侧为输出槽", "Output slots are on the right"),
    ("输入槽", "input slot"), ("输出槽", "output slot"),
    ("魔法", "Magic"), ("版本号", "Version"), ("当前", "Current"),
    ("作者", "Author"), ("说明书", "Guide"), ("材料", "Materials"),
    ("能源与电力", "Power and Energy"), ("蜂箱", "Beehive"), ("刷怪笼", "Spawner"),
    ("秒", " seconds"), ("级", " tier")
]


def humanize(identifier: str | None) -> str:
    if not identifier:
        return "Magic Legacy"
    value = identifier.replace("-", "_")
    if value.upper().startswith("MAGIC_"):
        value = value[6:]
    words = []
    for token in filter(None, value.split("_")):
        up = token.upper()
        words.append(WORD_MAP.get(up, token if token.isdigit() else token.title()))
    result = " ".join(words).strip() or "Legacy"
    return result if result.lower().startswith("magic") else "Magic " + result


def clean_visible(raw: str, top: str | None) -> str:
    plain = COLOR.sub("", raw)
    exact = {"输出": "Output", "输入": "Input", "提示": "Information", "说明": "Information", "返回": "Back", "关闭": "Close"}
    for zh, en in exact.items():
        if zh in plain:
            return en
    return humanize(top)


def clean_lore(raw: str, top: str | None) -> str:
    value = COLOR.sub("", raw)
    for zh, en in PHRASES:
        value = value.replace(zh, en)
    value = value.replace("：", ": ").replace("，", ", ").replace("。", ". ")
    value = value.replace("（", " (").replace("）", ") ").replace("→", " ")
    value = CJK.sub("", value)
    value = re.sub(r"\s+", " ", value).strip(" -~")
    if not re.search(r"[A-Za-z]", value):
        value = humanize(top)
    return "&7" + value


def process(path: pathlib.Path) -> int:
    lines = path.read_text(encoding="utf-8").splitlines()
    out: list[str] = []
    top: str | None = None
    parents: dict[int, str] = {}
    changes = 0

    for line in lines:
        mtop = TOP_KEY.match(line)
        if mtop:
            top = mtop.group(1)
        stripped = line.lstrip()
        indent = len(line) - len(stripped)
        mkey = re.match(r"^([A-Za-z0-9_.-]+):", stripped)
        if mkey:
            parents[indent] = mkey.group(1)
            for old in list(parents):
                if old > indent:
                    parents.pop(old, None)

        if re.match(r"^\s*-\s*GuizhanLibPlugin\s*$", line):
            changes += 1
            continue
        if not CJK.search(line) or stripped.startswith("#"):
            out.append(line)
            continue

        # Quoted map keys in this addon are internal recipe/object lookup names.
        # Translating them can break references, so preserve them exactly.
        if QUOTED_MAP_KEY.match(line):
            out.append(line)
            continue

        scalar = SCALAR.match(line)
        if scalar:
            spaces, key, quote, raw = scalar.groups()
            lk = key.lower()
            if lk == "scriptlistener":
                out.append(line)
                continue
            if lk in PLAYER_KEYS:
                value = clean_visible(raw, top)
                out.append(f"{spaces}{key}: {quote}&d{value}{quote}")
                changes += 1
                continue
            if lk not in INTERNAL_KEYS:
                out.append(f"{spaces}{key}: {quote}{humanize(top)}{quote}")
                changes += 1
                continue

        ml = LIST_STRING.match(line)
        if ml:
            spaces, quote, raw = ml.groups()
            parent = ""
            for level in sorted(parents, reverse=True):
                if level < len(spaces):
                    parent = parents[level].lower()
                    break
            if parent == "lore":
                out.append(f"{spaces}- {quote}{clean_lore(raw, top)}{quote}")
                changes += 1
                continue
            if parent not in INTERNAL_KEYS:
                out.append(f"{spaces}- {quote}{humanize(top)}{quote}")
                changes += 1
                continue

        out.append(line)

    new = "\n".join(out) + "\n"
    old = path.read_text(encoding="utf-8")
    if new != old:
        path.write_text(new, encoding="utf-8")
    return changes


total = 0
for path in sorted(ROOT.glob("*.yml")):
    count = process(path)
    total += count
    if count:
        print(f"updated {path.name}: {count} changes")

remaining: list[str] = []
for path in sorted(ROOT.glob("*.yml")):
    for n, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        stripped = line.lstrip()
        if not CJK.search(line) or stripped.startswith("#"):
            continue
        if stripped.lower().startswith("scriptlistener:"):
            continue
        if QUOTED_MAP_KEY.match(line):
            continue
        # Any remaining quoted CJK value is potentially player-facing.
        if "\"" in line or "'" in line:
            remaining.append(f"{path.name}:{n}: {line.strip()}")

report = ROOT / "tools/yaml-cjk-report.txt"
report.write_text("\n".join(remaining) + ("\n" if remaining else ""), encoding="utf-8")
print(f"Magic YAML changes: {total}; player-facing quoted CJK remaining: {len(remaining)}")
for row in remaining[:200]:
    print(row)
if remaining:
    raise SystemExit(3)
print("Magic YAML English-only player-facing scan passed.")
