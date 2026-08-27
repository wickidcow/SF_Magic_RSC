#!/usr/bin/env python3
"""Translate player-facing Magic YAML text from Chinese to English safely."""
from __future__ import annotations

import argparse
import os
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
TRANSLATABLE_BLOCKS = {"lore", "loadStartTexts", "enabledTexts", "texts", "messages"}
SKIP_PARTS = {".git", ".github", "scripts", "saveditems", "audit", "_refs", "tools"}
PROTECTED = re.compile(
    r"\{#[0-9A-Fa-f]{6}\}|&[0-9A-FK-ORa-fk-or]|§[0-9A-FK-ORa-fk-or]|"
    r"%[A-Za-z0-9_.:-]+%|\$\{[^}]+\}|\{[0-9]+\}|https?://\S+"
)
GLOSSARY = {
    "魔法": "Magic", "机器": "Machine", "发电机": "Generator",
    "无尽": "Infinity", "无限": "Infinite", "终极": "Ultimate",
    "高级": "Advanced", "基础": "Basic", "核心": "Core",
    "方块": "Block", "农场": "Farm", "林场": "Tree Farm",
    "资源": "Resource", "速度": "Speed", "可存储": "Capacity",
    "自然资源": "Natural Resource", "不可再生资源": "Non-renewable Resource",
    "实验性机器": "Experimental Machine", "电量": "Energy",
    "粘液刻": "Slimefun tick", "工作": "Operation",
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


def normalize_english(text: str) -> str:
    text = re.sub(r"\s+([,.;:!?])", r"\1", text)
    text = re.sub(r"\s{2,}", " ", text).strip()
    replacements = {
        "slime ticks": "Slimefun ticks",
        "viscous ticks": "Slimefun ticks",
        "electric quantity": "energy",
        "electricity storage": "energy capacity",
        "magic machine": "Magic Machine",
        "magic machines": "Magic Machines",
    }
    for old, new in replacements.items():
        text = re.sub(re.escape(old), new, text, flags=re.I)
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


def load_model(model_name: str):
    import torch
    from transformers import MarianMTModel, MarianTokenizer
    torch.set_num_threads(max(1, os.cpu_count() or 1))
    tokenizer = MarianTokenizer.from_pretrained(model_name)
    model = MarianMTModel.from_pretrained(model_name)
    model.eval()
    return torch, tokenizer, model


def translate_many(values: list[str], model_name: str, batch_size: int) -> dict[str, str]:
    torch, tokenizer, model = load_model(model_name)
    prepared: list[str] = []
    token_maps: list[list[str]] = []
    for value in values:
        protected, tokens = protect(value)
        prepared.append(pre_glossary(protected))
        token_maps.append(tokens)

    translated: list[str] = []
    with torch.inference_mode():
        for start in range(0, len(prepared), batch_size):
            batch = prepared[start:start + batch_size]
            encoded = tokenizer(batch, return_tensors="pt", padding=True, truncation=True, max_length=192)
            generated = model.generate(**encoded, max_new_tokens=192, num_beams=1, do_sample=False)
            translated.extend(tokenizer.batch_decode(generated, skip_special_tokens=True))
            print(f"Translated {min(start + batch_size, len(prepared))}/{len(prepared)}", flush=True)

    mapping: dict[str, str] = {}
    for original, english, tokens in zip(values, translated, token_maps):
        english = normalize_english(restore(english, tokens))
        if not english:
            raise RuntimeError(f"Empty translation for: {original!r}")
        mapping[original] = english
    return mapping


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="Helsinki-NLP/opus-mt-zh-en")
    parser.add_argument("--batch-size", type=int, default=96)
    args = parser.parse_args()

    paths = sorted(
        p for p in ROOT.rglob("*.yml")
        if not any(part in SKIP_PARTS for part in p.relative_to(ROOT).parts)
    )
    targets = collect_targets(paths)
    unique = list(dict.fromkeys(value for _, _, value in targets))
    print(f"Player-facing Chinese lines: {len(targets)}")
    print(f"Unique strings to translate: {len(unique)}")
    if not unique:
        print("YAML player-facing text is already English")
        return 0

    mapping = translate_many(unique, args.model, args.batch_size)
    by_path: dict[Path, dict[int, str]] = {}
    for path, idx, original in targets:
        by_path.setdefault(path, {})[idx] = mapping[original]

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

    report = ROOT / "audit" / "translation-report.tsv"
    report.parent.mkdir(exist_ok=True)
    with report.open("w", encoding="utf-8") as f:
        f.write("source\tenglish\n")
        for source in unique:
            f.write(source.replace("\t", "    ") + "\t" + mapping[source].replace("\t", "    ") + "\n")

    remaining = sum(len(collect_targets([path])) for path in paths)
    print(f"Changed YAML files: {len(by_path)}")
    print(f"Remaining player-facing Chinese lines: {remaining}")
    return 1 if remaining else 0


if __name__ == "__main__":
    raise SystemExit(main())
