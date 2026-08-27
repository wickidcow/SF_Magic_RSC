#!/usr/bin/env python3
"""Translate player-facing Magic text from Chinese to English.

This intentionally leaves YAML keys, IDs, comments, scripts and saved serialized
items untouched. Only known display fields and list entries inside display-text
blocks are translated.
"""
from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHINESE = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")
SCALAR = re.compile(r"^(?P<prefix>\s*)(?P<key>[A-Za-z_][A-Za-z0-9_-]*)(?P<sep>\s*:\s*)(?P<quote>['\"])(?P<value>.*)(?P=quote)(?P<tail>\s*(?:#.*)?)$")
BLOCK = re.compile(r"^(?P<prefix>\s*)(?P<key>[A-Za-z_][A-Za-z0-9_-]*)\s*:\s*(?:#.*)?$")
LIST_QUOTED = re.compile(r"^(?P<prefix>\s*-\s*)(?P<quote>['\"])(?P<value>.*)(?P=quote)(?P<tail>\s*(?:#.*)?)$")

TRANSLATABLE_SCALARS = {
    "name", "description", "title", "subtitle", "message", "display_name",
    "display-name", "label", "text",
}
TRANSLATABLE_BLOCKS = {
    "lore", "loadStartTexts", "enabledTexts", "texts", "messages",
}
SKIP_PARTS = {".git", ".github", "scripts", "saveditems", "audit", "_refs"}

# Formatting/placeholders that must survive model translation.
PROTECTED = re.compile(
    r"\{#[0-9A-Fa-f]{6}\}|&[0-9A-FK-ORa-fk-or]|%[A-Za-z0-9_.:-]+%|"
    r"\$\{[^}]+\}|\{[0-9]+\}|https?://\S+"
)

# Deterministic glossary for common Magic/Slimefun vocabulary. The translation
# model still handles grammar and less-common phrases.
GLOSSARY = {
    "魔法": "Magic",
    "机器": "Machine",
    "发电机": "Generator",
    "无尽": "Infinity",
    "无限": "Infinite",
    "终极": "Ultimate",
    "高级": "Advanced",
    "基础": "Basic",
    "核心": "Core",
    "方块": "Block",
    "农场": "Farm",
    "林场": "Tree Farm",
    "资源": "Resource",
    "速度": "Speed",
    "可存储": "Capacity",
    "自然资源": "Natural Resource",
    "不可再生资源": "Non-renewable Resource",
    "实验性机器": "Experimental Machine",
}


def protect(text: str) -> tuple[str, list[str]]:
    tokens: list[str] = []

    def repl(match: re.Match[str]) -> str:
        idx = len(tokens)
        tokens.append(match.group(0))
        # Alphabetic sentinel is copied more reliably by Marian than punctuation-heavy text.
        letters = ""
        n = idx
        while True:
            letters = chr(ord("A") + n % 26) + letters
            n = n // 26 - 1
            if n < 0:
                break
        return f"MZFMT{letters}TOKEN"

    return PROTECTED.sub(repl, text), tokens


def restore(text: str, tokens: list[str]) -> str:
    for idx, token in enumerate(tokens):
        letters = ""
        n = idx
        while True:
            letters = chr(ord("A") + n % 26) + letters
            n = n // 26 - 1
            if n < 0:
                break
        marker = f"MZFMT{letters}TOKEN"
        # Models sometimes insert spaces inside copied uppercase markers.
        loose = r"\s*".join(map(re.escape, marker))
        text = re.sub(loose, lambda _: token, text, flags=re.I)
    return text


def pre_glossary(text: str) -> str:
    # Longer phrases first.
    for zh, en in sorted(GLOSSARY.items(), key=lambda kv: len(kv[0]), reverse=True):
        text = text.replace(zh, f" {en} ")
    return re.sub(r"[ \t]+", " ", text).strip()


def normalize_english(text: str) -> str:
    text = re.sub(r"\s+([,.;:!?])", r"\1", text)
    text = re.sub(r"\s{2,}", " ", text).strip()
    # Common raw-MT artifacts for Minecraft terminology.
    replacements = {
        "slime ticks": "Slimefun ticks",
        "viscous ticks": "Slimefun ticks",
        "electric quantity": "energy",
        "electricity storage": "energy capacity",
        "magic machine": "Magic Machine",
    }
    lower = text.lower()
    for old, new in replacements.items():
        if old in lower:
            text = re.sub(re.escape(old), new, text, flags=re.I)
            lower = text.lower()
    return text


def collect_targets(paths: list[Path]) -> list[tuple[Path, int, str]]:
    targets: list[tuple[Path, int, str]] = []
    for path in paths:
        lines = path.read_text(encoding="utf-8", errors="strict").splitlines()
        active_block_indent: int | None = None
        for idx, line in enumerate(lines):
            indent = len(line) - len(line.lstrip(" "))
            block_match = BLOCK.match(line)
            if block_match:
                key = block_match.group("key")
                if key in TRANSLATABLE_BLOCKS:
                    active_block_indent = indent
                elif active_block_indent is not None and indent <= active_block_indent:
                    active_block_indent = None
            elif active_block_indent is not None and line.strip() and indent <= active_block_indent:
                active_block_indent = None

            scalar = SCALAR.match(line)
            if scalar and scalar.group("key") in TRANSLATABLE_SCALARS and CHINESE.search(scalar.group("value")):
                targets.append((path, idx, scalar.group("value")))
                continue

            if active_block_indent is not None and indent > active_block_indent:
                item = LIST_QUOTED.match(line)
                if item and CHINESE.search(item.group("value")):
                    targets.append((path, idx, item.group("value")))
    return targets


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="Helsinki-NLP/opus-mt-zh-en")
    parser.add_argument("--batch-size", type=int, default=32)
    args = parser.parse_args()

    from transformers import MarianMTModel, MarianTokenizer

    paths = sorted(
        p for p in ROOT.rglob("*.yml")
        if not any(part in SKIP_PARTS for part in p.relative_to(ROOT).parts)
    )
    targets = collect_targets(paths)
    unique: list[str] = []
    seen: set[str] = set()
    for _, _, value in targets:
        if value not in seen:
            seen.add(value)
            unique.append(value)

    print(f"Player-facing Chinese lines: {len(targets)}")
    print(f"Unique strings to translate: {len(unique)}")

    tokenizer = MarianTokenizer.from_pretrained(args.model)
    model = MarianMTModel.from_pretrained(args.model)

    prepared: list[str] = []
    token_maps: list[list[str]] = []
    for value in unique:
        protected, tokens = protect(value)
        prepared.append(pre_glossary(protected))
        token_maps.append(tokens)

    translated: list[str] = []
    for start in range(0, len(prepared), args.batch_size):
        batch = prepared[start:start + args.batch_size]
        encoded = tokenizer(batch, return_tensors="pt", padding=True, truncation=True, max_length=256)
        generated = model.generate(**encoded, max_new_tokens=256, num_beams=2)
        translated.extend(tokenizer.batch_decode(generated, skip_special_tokens=True))
        print(f"Translated {min(start + args.batch_size, len(prepared))}/{len(prepared)}")

    mapping: dict[str, str] = {}
    for original, english, tokens in zip(unique, translated, token_maps):
        english = restore(english, tokens)
        english = normalize_english(english)
        if not english:
            raise RuntimeError(f"Empty translation for: {original!r}")
        mapping[original] = english

    by_path: dict[Path, dict[int, str]] = {}
    for path, idx, original in targets:
        by_path.setdefault(path, {})[idx] = mapping[original]

    changed = 0
    for path, line_map in by_path.items():
        lines = path.read_text(encoding="utf-8").splitlines()
        for idx, english in line_map.items():
            line = lines[idx]
            scalar = SCALAR.match(line)
            if scalar and scalar.group("key") in TRANSLATABLE_SCALARS:
                quote = scalar.group("quote")
                escaped = english.replace(quote, "\\" + quote) if quote == '"' else english.replace("'", "''")
                lines[idx] = f'{scalar.group("prefix")}{scalar.group("key")}{scalar.group("sep")}{quote}{escaped}{quote}{scalar.group("tail")}'
                continue
            item = LIST_QUOTED.match(line)
            if item:
                quote = item.group("quote")
                escaped = english.replace(quote, "\\" + quote) if quote == '"' else english.replace("'", "''")
                lines[idx] = f'{item.group("prefix")}{quote}{escaped}{quote}{item.group("tail")}'
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        changed += 1

    # Emit a compact report for review.
    report = ROOT / "audit" / "translation-report.tsv"
    report.parent.mkdir(exist_ok=True)
    with report.open("w", encoding="utf-8") as f:
        f.write("source\tenglish\n")
        for source in unique:
            f.write(source.replace("\t", "    ") + "\t" + mapping[source].replace("\t", "    ") + "\n")

    remaining = 0
    for path in paths:
        for _, _, value in collect_targets([path]):
            if CHINESE.search(value):
                remaining += 1
    print(f"Changed YAML files: {changed}")
    print(f"Remaining player-facing Chinese lines: {remaining}")
    return 1 if remaining else 0


if __name__ == "__main__":
    raise SystemExit(main())
