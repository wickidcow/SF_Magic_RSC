#!/usr/bin/env python3
"""Validate single-quoted scalar syntax in Magic saved-item YAML files.

Bukkit's serialized ItemStack YAML frequently stores JSON components inside
single-quoted YAML scalars. A literal apostrophe inside one of those scalars
must be doubled (Owner''s Love). An unescaped apostrophe makes SnakeYAML stop
parsing the scalar early and causes RSC to skip the saved item and dependent
recipes at runtime.

This check is dependency-free so it can run in every packaging/audit job.
It intentionally targets saveditems/, whose generated format uses one-line
single-quoted scalar values.
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def validate_single_quoted_scalar(path: Path, line_number: int, line: str) -> str | None:
    """Return an error message when a one-line YAML single-quoted scalar is malformed."""
    stripped = line.lstrip()
    if not stripped or stripped.startswith("#") or ":" not in stripped:
        return None

    _key, raw_value = stripped.split(":", 1)
    value = raw_value.lstrip()
    if not value.startswith("'"):
        return None

    index = 1
    while index < len(value):
        if value[index] != "'":
            index += 1
            continue

        # YAML escapes an apostrophe inside a single-quoted scalar by doubling it.
        if index + 1 < len(value) and value[index + 1] == "'":
            index += 2
            continue

        # A non-doubled quote is only valid as the closing quote. Anything other
        # than whitespace or a YAML comment after it means it closed too early.
        tail = value[index + 1 :].strip()
        if not tail or tail.startswith("#"):
            return None

        return (
            f"{path}:{line_number}: unescaped apostrophe/early closing quote in "
            "single-quoted YAML scalar"
        )

    return f"{path}:{line_number}: unterminated single-quoted YAML scalar"


def main() -> int:
    directory = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "saveditems"
    if not directory.is_dir():
        print(f"Saved-item directory not found: {directory}", file=sys.stderr)
        return 2

    files = sorted(directory.rglob("*.yml"))
    errors: list[str] = []

    for path in files:
        for line_number, line in enumerate(path.read_text(encoding="utf-8", errors="strict").splitlines(), 1):
            error = validate_single_quoted_scalar(path, line_number, line)
            if error:
                errors.append(error)

    if errors:
        print("Saved-item YAML validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(f"Validated {len(files)} saved-item YAML files: single-quoted scalars are well formed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
