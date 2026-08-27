#!/usr/bin/env python3
"""Apply the source-verified IE2 migration and Magic-only compatibility shims."""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

POWERED_BEDROCK_COMPAT = '''# Legacy compatibility: InfinityExpansion2 removed IE1's POWERED_BEDROCK.
# Magic still uses this ID as a progression component, so retain the historical
# Slimefun ID locally rather than substituting an unrelated IE2 machine.
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
      - "&8The original IE1 powered-block behavior is no longer provided by IE2"
  recipe_type: MAGIC_NONE

'''


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ie1", required=True, type=Path)
    parser.add_argument("--ie2", required=True, type=Path)
    args = parser.parse_args()

    report = ROOT / "audit" / "ie2-migration.json"
    cmd = [
        sys.executable,
        str(ROOT / "scripts" / "migrate_ie2.py"),
        "--ie1", str(args.ie1),
        "--ie2", str(args.ie2),
        "--write",
        "--report", str(report),
    ]
    result = subprocess.run(cmd, cwd=ROOT, check=False)

    data = json.loads(report.read_text(encoding="utf-8"))
    unresolved = data.get("unresolved_confirmed_ie1_ids", [])
    if unresolved not in ([], ["POWERED_BEDROCK"]):
        print(f"Unexpected unresolved IE1 IDs: {unresolved}", file=sys.stderr)
        return result.returncode or 2

    items = ROOT / "items.yml"
    text = items.read_text(encoding="utf-8", errors="strict")
    if "id_alias: POWERED_BEDROCK" not in text:
        items.write_text(POWERED_BEDROCK_COMPAT + text, encoding="utf-8", newline="\n")
        print("Added Magic-owned POWERED_BEDROCK compatibility item")
    else:
        print("POWERED_BEDROCK compatibility item already present")

    # Keep the in-game version card aligned with the Legacy fork metadata.
    text = items.read_text(encoding="utf-8")
    text = text.replace("&eRelease-1.1.16", "&eLegacy-1.1.16", 1)
    items.write_text(text, encoding="utf-8", newline="\n")

    print("Legacy port changes applied")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
