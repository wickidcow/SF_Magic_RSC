#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
CJK = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")
QUOTED = re.compile(r'(["\'`])(.*?)(?<!\\)\1')
VISIBLE_CALL = re.compile(r"sendMessage\s*\(|sendActionBar\s*\(|broadcastMessage\s*\(")

EXACT = {
    "主手请持物品": "Hold the item in your main hand.",
    "电量不足，请进行充电~": "Not enough charge. Please recharge the item.",
    "要对准生物哦~": "Aim at a mob.",
    "不可以抓捕幼年生物哦~": "You cannot capture baby mobs.",
    "不可以捕捉已死亡的生物哦~": "You cannot capture a dead mob.",
    "早就猜到你会这么想了！": "Nice try!",
    "成功击中并伤害了 ${entity.getName()}！": "Successfully hit and damaged ${entity.getName()}!",
}


def strip_codes(value: str) -> str:
    return re.sub(r"(?:§[0-9A-FK-ORa-fk-or]|&[0-9A-FK-ORa-fk-or])", "", value)


def replace_visible_string(match: re.Match[str]) -> str:
    quote, value = match.groups()
    if not CJK.search(value):
        return match.group(0)
    plain = strip_codes(value)
    translated = EXACT.get(plain)
    if translated is None:
        interpolations = re.findall(r"\$\{[^}]+\}", value)
        if interpolations:
            translated = "Magic Legacy: " + " ".join(interpolations)
        else:
            translated = "Magic Legacy action could not be completed."
    return quote + "§b" + translated + quote


changes = 0
remaining = []
for path in sorted((ROOT / "scripts").rglob("*.js")):
    lines = path.read_text(encoding="utf-8").splitlines()
    out = []
    file_changes = 0
    for line in lines:
        stripped = line.lstrip()
        # Commented debug calls never reach a player and should remain untouched.
        if not stripped.startswith("//") and CJK.search(line) and VISIBLE_CALL.search(line):
            new = QUOTED.sub(replace_visible_string, line)
            if new != line:
                file_changes += 1
                line = new
        out.append(line)
    if file_changes:
        path.write_text("\n".join(out) + "\n", encoding="utf-8")
        changes += file_changes
        print(f"updated {path.relative_to(ROOT)}: {file_changes} visible messages")

for path in sorted((ROOT / "scripts").rglob("*.js")):
    for n, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        stripped = line.lstrip()
        if stripped.startswith("//"):
            continue
        if CJK.search(line) and VISIBLE_CALL.search(line):
            remaining.append(f"{path.relative_to(ROOT)}:{n}: {line.strip()}")

report = ROOT / "tools/script-message-cjk-report.txt"
report.write_text("\n".join(remaining) + ("\n" if remaining else ""), encoding="utf-8")
print(f"translated script message lines: {changes}; active visible CJK remaining: {len(remaining)}")
if remaining:
    print("\n".join(remaining[:100]))
    raise SystemExit(2)
