#!/usr/bin/env python3
"""Verify packaged Magic scripts avoid unsafe Graal host reflection.

This exercises the same `dist/Magic` staging layout used by GitHub packaging,
including every JavaScript file. It catches path-filter regressions and new
scripts that directly invoke getId()/getOutputSlots() on arbitrary SlimefunItem
host objects.
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

from clean_magic_text_quality import POLYGLOT_SCRIPT, active_unsafe_polyglot_lines  # noqa: E402


def main() -> int:
    source_scripts = ROOT / "scripts"
    source_infinity = source_scripts / POLYGLOT_SCRIPT
    cleaner = TOOLS / "clean_magic_text_quality.py"
    if not source_infinity.is_file():
        raise SystemExit(f"Missing expected Magic integration script: {source_infinity}")
    if not cleaner.is_file():
        raise SystemExit(f"Missing runtime cleaner: {cleaner}")

    with tempfile.TemporaryDirectory() as tmp:
        staged_root = Path(tmp) / "dist" / "Magic"
        staged_scripts = staged_root / "scripts"
        shutil.copytree(source_scripts, staged_scripts)

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

        unsafe: list[str] = []
        for path in sorted(staged_scripts.rglob("*.js")):
            for line in active_unsafe_polyglot_lines(path.read_text(encoding="utf-8")):
                unsafe.append(f"{path.relative_to(staged_root)}: {line}")
        if unsafe:
            raise SystemExit(
                "Unsafe Graal Slimefun host reflection remained in staged Magic scripts:\n"
                + "\n".join(unsafe)
            )

        infinity_text = (staged_scripts / POLYGLOT_SCRIPT).read_text(encoding="utf-8")
        required = (
            'sfitem === getSfItemById("MAGIC_INFINITY_MIX_BOX_1")',
            'let mixBoxItem = getSfItemById("MAGIC_INFINITY_MIX_BOX_1");',
            "let targetStack = targetItem.getItem();",
            "isItemSimilar(itemStack, targetStack, true)",
        )
        missing = [snippet for snippet in required if snippet not in infinity_text]
        if missing:
            raise SystemExit(
                "Expected safe Magic Infinity bridge logic was not produced: " + ", ".join(missing)
            )

        if "Graal host-reflection fix(es)" not in result.stdout:
            raise SystemExit("Runtime cleaner did not report its Graal safety pass:\n" + result.stdout)

    print("Verified all dist/Magic JavaScript files avoid unsafe SlimefunItem host reflection")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
