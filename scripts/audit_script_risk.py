#!/usr/bin/env python3
"""Produce an informational risk inventory for Magic's remaining RSC JavaScript runtime."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"
OUT = ROOT / "audit"
OUT.mkdir(exist_ok=True)

RULES = [
    ("instant_kill", re.compile(r"\.setHealth\(\s*0(?:\.0)?\s*\)"), 6, "Directly sets an entity/player health to zero"),
    ("lightning", re.compile(r"LightningStrike"), 2, "Uses spawned lightning entities"),
    ("broadcast", re.compile(r"broadcastMessage\s*\("), 2, "Broadcasts globally to the server"),
    ("placeholder_message", re.compile(r"Magic Legacy action could not be completed"), 2, "Still contains generic placeholder player text"),
    ("nearby_entity_scan", re.compile(r"getNearbyEntities\s*\("), 3, "Scans nearby entities"),
    ("all_online_players", re.compile(r"getOnlinePlayers\s*\("), 2, "Iterates over all online players"),
    ("legacy_max_health", re.compile(r"getMaxHealth\s*\("), 2, "Uses legacy max-health access"),
    ("potion_enum_fields", re.compile(r"PotionEffectType\.[A-Z0-9_]+"), 2, "Uses static potion-effect fields that are brittle across API changes"),
    ("java_type", re.compile(r"Java\.type\s*\("), 1, "Uses direct Graal Java interop"),
]

PARTICLE_CALL = re.compile(r"spawnParticle\s*\(")
TIGHT_STEP = re.compile(r"(?:\+=\s*0\.0[0-9]+|PARTICLE_INTERVAL\s*=\s*0\.0[0-9]+)")
SPAWN_CALL = re.compile(r"\.spawn\s*\(")


def severity(score: int) -> str:
    if score >= 10:
        return "high"
    if score >= 5:
        return "medium"
    if score:
        return "low"
    return "none"


rows = []
for path in sorted(SCRIPTS.rglob("*.js")):
    text = path.read_text(encoding="utf-8", errors="replace")
    findings = []
    score = 0

    for key, pattern, weight, description in RULES:
        count = len(pattern.findall(text))
        if count:
            findings.append({"key": key, "count": count, "description": description})
            score += weight * min(count, 3)

    particle_calls = len(PARTICLE_CALL.findall(text))
    if particle_calls:
        findings.append({
            "key": "particle_calls",
            "count": particle_calls,
            "description": "Spawns particles",
        })
        score += min(particle_calls, 3)

    if particle_calls and TIGHT_STEP.search(text):
        findings.append({
            "key": "dense_particle_loop",
            "count": 1,
            "description": "Combines particle spawning with a very tight distance step",
        })
        score += 6

    spawn_calls = len(SPAWN_CALL.findall(text))
    if spawn_calls >= 5:
        findings.append({
            "key": "many_spawn_calls",
            "count": spawn_calls,
            "description": "Contains many direct entity spawn calls",
        })
        score += 4

    rows.append({
        "file": path.relative_to(ROOT).as_posix(),
        "bytes": path.stat().st_size,
        "score": score,
        "severity": severity(score),
        "findings": findings,
    })

rows.sort(key=lambda row: (-row["score"], -row["bytes"], row["file"]))
summary = {
    "script_count": len(rows),
    "high": sum(row["severity"] == "high" for row in rows),
    "medium": sum(row["severity"] == "medium" for row in rows),
    "low": sum(row["severity"] == "low" for row in rows),
    "none": sum(row["severity"] == "none" for row in rows),
}

(OUT / "script-risk.json").write_text(
    json.dumps({"summary": summary, "scripts": rows}, indent=2, ensure_ascii=False) + "\n",
    encoding="utf-8",
)

md = [
    "# Magic JavaScript risk inventory",
    "",
    "This report is informational. It prioritizes scripts for native Java migration; it does not mark a script as broken solely because it is listed.",
    "",
    f"- Scripts scanned: **{summary['script_count']}**",
    f"- High priority: **{summary['high']}**",
    f"- Medium priority: **{summary['medium']}**",
    f"- Low priority: **{summary['low']}**",
    f"- No flagged pattern: **{summary['none']}**",
    "",
    "## Highest-priority scripts",
    "",
    "| Script | Severity | Score | Signals |",
    "| --- | --- | ---: | --- |",
]
for row in rows[:20]:
    signals = ", ".join(f"{item['key']} ({item['count']})" for item in row["findings"]) or "none"
    md.append(f"| `{row['file']}` | {row['severity']} | {row['score']} | {signals} |")

(OUT / "SCRIPT_RISK.md").write_text("\n".join(md) + "\n", encoding="utf-8")

print(
    "Script risk audit: "
    f"{summary['script_count']} scanned; "
    f"{summary['high']} high, {summary['medium']} medium, "
    f"{summary['low']} low"
)
