#!/usr/bin/env python3
"""Apply the source-verified IE2 migration and the one intentional IE1 shim."""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

POWERED_BEDROCK_COMPAT = '''# Legacy compatibility: InfinityExpansion2 removed IE1's POWERED_BEDROCK.
# Magic still uses this ID as a progression component, so preserve the historical
# item ID locally instead of replacing it with an unrelated IE2 machine.
MAGIC_POWERED_BEDROCK_COMPAT:
  id_alias: POWERED_BEDROCK
  item_group: magic_resource
  placeable: false
  item:
    name: "&4Powered Bedrock"
    material: NETHERITE_BLOCK
    glow: true
    lore:
      - "&7Legacy InfinityExpansion compatibility component"
      - "&7Preserved for Magic progression on InfinityExpansion2"
      - "&8The original IE1 powered-block behavior is not provided by IE2"
  recipe_type: MAGIC_NONE

'''


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ie1", required=True, type=Path)
    parser.add_argument("--ie2", required=True, type=Path)
    args = parser.parse_args()

    report = ROOT / "audit" / "ie2-migration.json"
    result = subprocess.run(
        [
            sys.executable,
            str(ROOT / "scripts" / "migrate_ie2.py"),
            "--ie1", str(args.ie1),
            "--ie2", str(args.ie2),
            "--write",
            "--report", str(report),
        ],
        cwd=ROOT,
        check=False,
    )

    data = json.loads(report.read_text(encoding="utf-8"))
    unresolved = data.get("unresolved_confirmed_ie1_ids", [])
    unexpected = [item for item in unresolved if item != "POWERED_BEDROCK"]
    if unexpected:
        print(f"Unexpected unresolved IE1 IDs: {unexpected}", file=sys.stderr)
        return result.returncode or 2

    if "POWERED_BEDROCK" in unresolved:
        items = ROOT / "items.yml"
        text = items.read_text(encoding="utf-8", errors="strict")
        if "id_alias: POWERED_BEDROCK" not in text:
            items.write_text(POWERED_BEDROCK_COMPAT + text, encoding="utf-8", newline="\n")
            print("Added Magic-owned POWERED_BEDROCK compatibility item")

    print("Magic IE2 compatibility migration applied")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
