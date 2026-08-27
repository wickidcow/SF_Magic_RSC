#!/usr/bin/env python3
"""Fail CI when Magic retains known IE1 references or targets unknown IE2 ids."""
from __future__ import annotations

import argparse
import re
from pathlib import Path

import migrate_ie2


def configured_oscillator_ids(ie2_root: Path) -> set[str]:
    config = ie2_root / "src/main/resources/config.yml"
    lines = config.read_text(encoding="utf-8", errors="replace").splitlines()
    in_oscillators = False
    base_indent = None
    result: set[str] = set()

    for line in lines:
        if re.match(r"^\s{2}oscillators:\s*$", line):
            in_oscillators = True
            base_indent = len(line) - len(line.lstrip())
            continue
        if not in_oscillators:
            continue
        if line.strip() and not line.lstrip().startswith("#"):
            indent = len(line) - len(line.lstrip())
            if indent <= (base_indent or 0):
                break
            match = re.match(r"^\s+([A-Z0-9_]+):\s*", line)
            if match:
                result.add(f"IE_OSCILLATOR_{match.group(1)}")
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ie1", required=True, type=Path)
    parser.add_argument("--ie2", required=True, type=Path)
    args = parser.parse_args()

    mapping, _ie1_ids, ie2_ids = migrate_ie2.build_mapping(args.ie1, args.ie2)
    external = migrate_ie2.scan_magic_external_ids()

    remaining = sorted(
        item for item in external
        if item in mapping or item.startswith("QUARRY_OSCILLATOR_")
    )
    if remaining:
        print("Known IE1 references remain after migration:")
        for item in remaining:
            print(f"  - {item}")
        return 2

    valid_ie2 = set(ie2_ids) | configured_oscillator_ids(args.ie2)
    unknown_ie2 = sorted(item for item in external if item.startswith("IE_") and item not in valid_ie2)
    if unknown_ie2:
        print("Magic references IE2 ids that current IE2 cannot provide:")
        for item in unknown_ie2:
            print(f"  - {item}")
        return 3

    print(f"Verified {sum(1 for item in external if item.startswith('IE_'))} IE2 references")
    print("No known IE1 references remain")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
