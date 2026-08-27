#!/usr/bin/env python3
"""Translate active player-facing Chinese JavaScript string literals to English.

Only strings on lines that call player-facing message/display APIs are touched.
Comments, internal lookup keys, entity names and configuration identifiers remain
unchanged to avoid changing gameplay behavior.
"""
from __future__ import annotations

import argparse
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHINESE = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")
STRING = re.compile(r"(?P<q>['\"])(?P<text>(?:\\.|(?!\1).)*)(?P=q)")
PLAYER_HINTS = (
    "sendMessage", "sendActionBar", "sendTitle", "broadcastMessage", "broadcast(",
    "setDisplayName", "setLore", "setCustomName", "showTitle", ".kick(", "kickPlayer",
)
PROTECTED = re.compile(
    r"&[0-9A-FK-ORa-fk-or]|§[0-9A-FK-ORa-fk-or]|%[A-Za-z0-9_.:-]+%|"
    r"\$\{[^}]+\}|\{[0-9]+\}|https?://\S+"
)
GLOSSARY = {
    "主手": "main hand",
    "副手": "off hand",
    "电量": "energy",
    "充电": "charge",
    "生物": "mob",
    "背包": "inventory",
    "冷却": "cooldown",
    "魔法": "Magic",
    "增幅": "Amplification",
    "强化": "Enhancement",
    "装备": "equipment",
    "属性": "attribute",
    "头盔": "helmet",
    "胸甲": "chestplate",
    "护腿": "leggings",
    "鞋子": "boots",
    "生命值": "health",
    "护甲值": "armor",
    "盔甲韧性": "armor toughness",
    "攻击力": "attack damage",
    "攻击速度": "attack speed",
    "移动速度": "movement speed",
    "击退抗性": "knockback resistance",
    "经验": "experience",
    "世界": "world",
    "坐标": "coordinates",
    "传送": "teleport",
    "玩家": "player",
    "效果": "effect",
    "等级": "level",
    "鱼饵": "bait",
    "钓到": "caught",
    "系统": "System",
    "秒": "seconds",
    "坤秒": "seconds",
}

# Hand-written phrases cover frequent UI text and preserve a natural Minecraft tone.
EXACT = {
    "主手请持物品": "Please hold an item in your main hand",
    "§b主手请持物品": "§bPlease hold an item in your main hand",
    "§b请用主手持物品": "§bPlease hold an item in your main hand",
    "副手需要持有物品": "Please hold an item in your off hand",
    "副手没有持有物品。": "There is no item in your off hand.",
    "§b电量不足，请进行充电~": "§bNot enough energy. Please recharge it~",
    "§b要对准§e生物§b哦~": "§bAim at a §emob§b first~",
    "§b不可以抓捕幼年§e生物§b哦~": "§bYou cannot capture baby §emobs§b~",
    "§b不可以捕捉§c已死亡的生物§b哦~": "§bYou cannot capture a §cdead mob§b~",
    "§e早就猜到你会这么想了！": "§eI knew you would try that!",
    "§b这个§e生物§b不在图鉴内哦~": "§bThis §emob§b is not in the collection~",
    "§b不可以贪心哦~": "§bOne at a time~",
    "§b注入成功~": "§bInfusion successful~",
    "§e背包已满，物品已掉落在地面上": "§eYour inventory is full. The item was dropped on the ground.",
    "&b背包已满，物品已掉落在地面上": "&bYour inventory is full. The item was dropped on the ground.",
    "§e成功获得物品 ": "§eReceived item ",
    "§b成功获得物品 ": "§bReceived item ",
    "&b成功获得物品 ": "&bReceived item ",
    "§b物品已经充满电啦~": "§bThe item is fully charged~",
    "§b你手中的物品电量已经满啦！充不进去啦~": "§bThe item in your hand is already fully charged!",
    "§b这个是个不可充电物品": "§bThis item cannot be charged.",
    "§b当前物品不支持电量操作，请检查物品或插件版本。": "§bThis item does not support energy operations. Check the item or plugin version.",
    "§b充电宝电量不足，无法进行充电！": "§bThe power bank does not have enough energy to charge this item!",
    "§7[系统] §f你已破坏方块，但不会有掉落物。": "§7[System] §fYou broke the block, but it will not drop anything.",
    "§b没有击中任何目标~": "§bYou did not hit a target~",
    "§b击中了一个非生物目标~": "§bYou hit something that is not a mob~",
    "§a更改成功": "§aChanged successfully",
    "§c未知的效果类型！": "§cUnknown effect type!",
    "§b设置了新的坐标~": "§bSaved new coordinates~",
    "§c错误：无效的坐标值！": "§cError: invalid coordinate value!",
    "§c传送失败：": "§cTeleport failed: ",
    "§c§l你已经虚脱了！": "§c§lYou are exhausted!",
    "§b请用主手持剑": "§bPlease hold a sword in your main hand",
    "验证执行": "Validation executed",
    "§b未能成功掉落物品，请检查配置。": "§bThe item could not be dropped. Please check the configuration.",
    "§b请仔细刷刷这个沙子，还没清理干净呢！": "§bKeep brushing the suspicious sand. It is not clean yet!",
    "§b请仔细刷刷这个沙砾，还没清理干净呢！": "§bKeep brushing the suspicious gravel. It is not clean yet!",
}


def marker_for(index: int) -> str:
    letters = ""
    n = index
    while True:
        letters = chr(ord("A") + n % 26) + letters
        n = n // 26 - 1
        if n < 0:
            return f"MZFMT{letters}TOKEN"


def protect(text: str) -> tuple[str, list[str]]:
    tokens: list[str] = []
    def repl(match: re.Match[str]) -> str:
        tokens.append(match.group(0))
        return marker_for(len(tokens) - 1)
    return PROTECTED.sub(repl, text), tokens


def restore(text: str, tokens: list[str]) -> str:
    for idx, token in enumerate(tokens):
        marker = marker_for(idx)
        loose = r"\s*".join(map(re.escape, marker))
        text = re.sub(loose, lambda _: token, text, flags=re.I)
    return text


def pre_glossary(text: str) -> str:
    for zh, en in sorted(GLOSSARY.items(), key=lambda kv: len(kv[0]), reverse=True):
        text = text.replace(zh, f" {en} ")
    return re.sub(r"[ \t]+", " ", text).strip()


def normalize(text: str) -> str:
    text = re.sub(r"\s+([,.;:!?])", r"\1", text)
    text = re.sub(r"\s{2,}", " ", text).strip()
    text = text.replace("Magic - Amplification", "Magic Amplification")
    text = text.replace("Magic - Enhancement", "Magic Enhancement")
    return text


def active_player_line(line: str) -> bool:
    stripped = line.lstrip()
    return not stripped.startswith("//") and any(hint in line for hint in PLAYER_HINTS)


def collect() -> tuple[list[tuple[Path, int, str]], list[str]]:
    occurrences: list[tuple[Path, int, str]] = []
    unique: list[str] = []
    seen: set[str] = set()
    for path in sorted((ROOT / "scripts").rglob("*.js")):
        for line_no, line in enumerate(path.read_text(encoding="utf-8", errors="strict").splitlines()):
            if not active_player_line(line):
                continue
            for match in STRING.finditer(line):
                value = match.group("text")
                if CHINESE.search(value):
                    occurrences.append((path, line_no, value))
                    if value not in seen:
                        seen.add(value)
                        unique.append(value)
    return occurrences, unique


def translate(values: list[str], model_name: str, batch_size: int) -> dict[str, str]:
    pending = [v for v in values if v not in EXACT]
    mapping = {v: EXACT[v] for v in values if v in EXACT}
    if not pending:
        return mapping

    import torch
    from transformers import MarianMTModel, MarianTokenizer
    torch.set_num_threads(max(1, os.cpu_count() or 1))
    tokenizer = MarianTokenizer.from_pretrained(model_name)
    model = MarianMTModel.from_pretrained(model_name)
    model.eval()

    prepared: list[str] = []
    token_maps: list[list[str]] = []
    for value in pending:
        protected, tokens = protect(value)
        prepared.append(pre_glossary(protected))
        token_maps.append(tokens)

    translated: list[str] = []
    with torch.inference_mode():
        for start in range(0, len(prepared), batch_size):
            batch = prepared[start:start + batch_size]
            encoded = tokenizer(batch, return_tensors="pt", padding=True, truncation=True, max_length=160)
            generated = model.generate(**encoded, max_new_tokens=160, num_beams=1, do_sample=False)
            translated.extend(tokenizer.batch_decode(generated, skip_special_tokens=True))

    for source, english, tokens in zip(pending, translated, token_maps):
        result = normalize(restore(english, tokens))
        if not result:
            raise RuntimeError(f"Empty JavaScript translation for {source!r}")
        mapping[source] = result
    return mapping


def escape_for_quote(text: str, quote: str) -> str:
    text = text.replace("\\", "\\\\") if False else text
    return text.replace(quote, "\\" + quote)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="Helsinki-NLP/opus-mt-zh-en")
    parser.add_argument("--batch-size", type=int, default=96)
    args = parser.parse_args()

    occurrences, unique = collect()
    print(f"Active player-facing JS Chinese literals: {len(occurrences)}")
    print(f"Unique JS strings to translate: {len(unique)}")
    if not unique:
        print("JavaScript player-facing text is already English")
        return 0

    mapping = translate(unique, args.model, args.batch_size)
    by_path: dict[Path, set[int]] = {}
    for path, line_no, _ in occurrences:
        by_path.setdefault(path, set()).add(line_no)

    for path, line_numbers in by_path.items():
        lines = path.read_text(encoding="utf-8").splitlines()
        for line_no in sorted(line_numbers):
            line = lines[line_no]
            matches = [m for m in STRING.finditer(line) if CHINESE.search(m.group("text"))]
            for match in reversed(matches):
                old = match.group("text")
                new = escape_for_quote(mapping[old], match.group("q"))
                line = line[:match.start("text")] + new + line[match.end("text"):]
            lines[line_no] = line
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    report = ROOT / "audit" / "js-translation-report.tsv"
    report.parent.mkdir(exist_ok=True)
    with report.open("w", encoding="utf-8") as f:
        f.write("source\tenglish\n")
        for source in unique:
            f.write(source.replace("\t", "    ") + "\t" + mapping[source].replace("\t", "    ") + "\n")

    remaining_occurrences, _ = collect()
    print(f"Changed JS files: {len(by_path)}")
    print(f"Remaining active player-facing Chinese JS literals: {len(remaining_occurrences)}")
    return 1 if remaining_occurrences else 0


if __name__ == "__main__":
    raise SystemExit(main())
