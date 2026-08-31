#!/usr/bin/env python3
"""Verify the packaged Magic runtime removes unsafe Graal host reflection.

This intentionally exercises the same `dist/Magic` staging layout used by the
GitHub package/release workflows. That catches path-filter regressions which a
direct helper-unit test would miss.
"""
from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent
sys.path.insert(0, str(TOOLS))

from clean_magic_text_quality import POLYGLOT_SCRIPT, UNSAFE_POLYGLOT_PATTERNS  # noqa: E402


def main() -> int:
    source = ROOT / "scripts" / POLYGLOT_SCRIPT
    cleaner = TOOLS / "clean_magic_text_quality.py"
    if not source.is_file():
        raise SystemExit(f"Missing expected Magic integration script: {source}")
    if not cleaner.is_file():
        raise SystemExit(f"Missing runtime cleaner: {cleaner}")

    with tempfile.TemporaryDirectory() as tmp:
        staged_root = Path(tmp) / "dist" / "Magic"
        staged_scripts = staged_root / "scripts"
        staged_scripts.mkdir(parents=True)
        staged = staged_scripts / POLYGLOT_SCRIPT
        shutil.copy2(source, staged)

        result = subprocess.run(
            [sys.executable, str(cleaner), str(staged_root)],
            text=True,
            capture_output=True,
            check=False,
        )
        if result.returncode != 0:
            sys.stderr.write(result.stdout)
            sys.stderr.write(result.stderr)
            raise SystemExit("Runtime cleaner failed against dist/Magic staging layout")

        text = staged.read_text(encoding="utf-8")
        remaining = [pattern for pattern in UNSAFE_POLYGLOT_PATTERNS if pattern in text]
        if remaining:
            raise SystemExit(
                "Unsafe Graal host reflection remained in dist/Magic staging output: "
                + ", ".join(remaining)
            )

        required = (
            'sfitem === getSfItemById("MAGIC_INFINITY_MIX_BOX_1")',
            'let mixBoxItem = getSfItemById("MAGIC_INFINITY_MIX_BOX_1");',
            "let targetStack = targetItem.getItem();",
            "isItemSimilar(itemStack, targetStack, true)",
        )
        missing = [snippet for snippet in required if snippet not in text]
        if missing:
            raise SystemExit(
                "Expected safe Magic Graal bridge logic was not produced: " + ", ".join(missing)
            )

        if "4 Graal host-reflection fix(es)" not in result.stdout:
            raise SystemExit(
                "Runtime cleaner did not report the expected four Magic Infinity transformations:\n"
                + result.stdout
            )

    print(
        "Verified dist/Magic runtime packaging removes arbitrary SlimefunItem "
        "getId()/getOutputSlots() host reflection"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
