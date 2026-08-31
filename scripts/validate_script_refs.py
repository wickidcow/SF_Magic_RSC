#!/usr/bin/env python3
"""Verify every YAML `script:` reference has a matching JavaScript file.

RSC resolves a configured script name relative to the addon's scripts folder
and appends `.js`. A missing file does not stop the item from registering, so
without this check a feature can silently load without its scripted behavior.

This validator intentionally has no third-party dependencies so it can run in
both repository audit and drop-in packaging jobs.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT_LINE = re.compile(r"^\s*script\s*:\s*(.*?)\s*$")


def parse_scalar(raw: str) -> str:
    value = raw.strip()
    if not value:
        return ""

    # Remove a simple trailing YAML comment. Script names in Magic do not use #.
    if " #" in value:
        value = value.split(" #", 1)[0].rstrip()

    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        value = value[1:-1]

    return value.strip()


def main() -> int:
    addon_root = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT
    scripts_dir = addon_root / "scripts"

    if not scripts_dir.is_dir():
        print(f"Scripts directory not found: {scripts_dir}", file=sys.stderr)
        return 2

    references: list[tuple[Path, int, str]] = []
    missing: list[tuple[Path, int, str, Path]] = []

    for yaml_file in sorted(addon_root.glob("*.yml")):
        for line_number, line in enumerate(
            yaml_file.read_text(encoding="utf-8", errors="strict").splitlines(), 1
        ):
            match = SCRIPT_LINE.match(line)
            if not match:
                continue

            script_name = parse_scalar(match.group(1))
            if not script_name or script_name.lower() in {"null", "~"}:
                continue

            references.append((yaml_file, line_number, script_name))
            target = scripts_dir / f"{script_name}.js"
            if not target.is_file():
                missing.append((yaml_file, line_number, script_name, target))

    if missing:
        print("Magic script-reference validation failed:", file=sys.stderr)
        for yaml_file, line_number, script_name, target in missing:
            print(
                f"- {yaml_file}:{line_number}: script '{script_name}' -> missing {target}",
                file=sys.stderr,
            )
        return 1

    unique_scripts = len({script for _, _, script in references})
    print(
        f"Validated {len(references)} script references "
        f"({unique_scripts} unique): all JavaScript files exist"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
