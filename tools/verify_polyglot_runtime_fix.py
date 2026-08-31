#!/usr/bin/env python3
"""Verify the packaged Magic runtime removes unsafe Graal host reflection.

This intentionally tests the shipping transformation against the source script.
The source stays close to upstream, while release packaging must remove member
calls on arbitrary SlimefunItem host objects that can force Graal to resolve
obsolete addon method signatures.
"""
from __future__ import annotations

import shutil
import sys
import tempfile
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent
sys.path.insert(0, str(TOOLS))

from clean_magic_text_quality import (  # noqa: E402
    POLYGLOT_SCRIPT,
    UNSAFE_POLYGLOT_PATTERNS,
    fix_polyglot_host_reflection,
)


def main() -> int:
    source = ROOT / "scripts" / POLYGLOT_SCRIPT
    if not source.is_file():
        raise SystemExit(f"Missing expected Magic integration script: {source}")

    with tempfile.TemporaryDirectory() as tmp:
        staged = Path(tmp) / POLYGLOT_SCRIPT
        shutil.copy2(source, staged)

        changes = fix_polyglot_host_reflection(staged)
        text = staged.read_text(encoding="utf-8")

        remaining = [pattern for pattern in UNSAFE_POLYGLOT_PATTERNS if pattern in text]
        if remaining:
            raise SystemExit(
                "Unsafe Graal host reflection remained after runtime fix: " + ", ".join(remaining)
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

        if changes < 4:
            raise SystemExit(
                f"Expected at least four source transformations in {POLYGLOT_SCRIPT}, got {changes}"
            )

    print(
        "Verified Magic Graal host-reflection runtime fix: arbitrary SlimefunItem "
        "objects are no longer queried for getId()/getOutputSlots()"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
