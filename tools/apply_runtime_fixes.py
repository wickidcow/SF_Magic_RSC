#!/usr/bin/env python3
"""Apply safe runtime-only compatibility fixes to a staged Magic RSC folder.

The source pack intentionally preserves historical/internal IDs. This script only
normalizes defects that are unsafe or noisy on the maintained Legacy/IE2 stack.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

TOP_LEVEL_KEY = re.compile(r"^([A-Za-z0-9_.-]+):\s*(?:#.*)?$")

OLD_IE1_WORKBENCH = "io.github.mooy1.infinityexpansion.items.blocks.InfinityWorkbench"
IE2_WORKBENCH = "net.guizhanss.infinityexpansion2.implementation.items.machines.InfinityWorkbench"


def dedupe_top_level_yaml(path: Path, drop_keys: set[str] | None = None) -> None:
    drop_keys = drop_keys or set()
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)

    starts: list[tuple[int, str]] = []
    for index, line in enumerate(lines):
        match = TOP_LEVEL_KEY.match(line.rstrip("\r\n"))
        if match:
            starts.append((index, match.group(1)))

    if not starts:
        return

    preamble = lines[: starts[0][0]]
    blocks: list[tuple[str, list[str]]] = []
    for pos, (start, key) in enumerate(starts):
        end = starts[pos + 1][0] if pos + 1 < len(starts) else len(lines)
        blocks.append((key, lines[start:end]))

    # Keep the last occurrence. This matches the effective value historically
    # used by permissive YAML loaders while eliminating Bukkit duplicate-key warnings.
    seen: set[str] = set()
    keep_reversed: list[tuple[str, list[str]]] = []
    for key, block in reversed(blocks):
        if key in drop_keys or key in seen:
            continue
        seen.add(key)
        keep_reversed.append((key, block))

    rebuilt = preamble[:]
    for _, block in reversed(keep_reversed):
        rebuilt.extend(block)

    path.write_text("".join(rebuilt), encoding="utf-8")


def replace_runtime_references(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    updated = text.replace(OLD_IE1_WORKBENCH, IE2_WORKBENCH)

    # IE2 now owns the real powered-bedrock item. Do not keep Magic's old
    # compatibility alias registered under the legacy ID.
    updated = re.sub(
        r"(?m)^(\s*material:\s*)POWERED_BEDROCK(\s*(?:#.*)?)$",
        r"\1IE_POWERED_BEDROCK\2",
        updated,
    )

    # Keep the in-game version sheet aligned with the actual drop-in release.
    updated = updated.replace("&eRelease-1.1.16", "&eLegacy-1.1.17")

    if updated != text:
        path.write_text(updated, encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: apply_runtime_fixes.py <staged Magic folder>")

    root = Path(sys.argv[1]).resolve()
    if not root.is_dir():
        raise SystemExit(f"Not a directory: {root}")

    for yaml_file in sorted(root.glob("*.yml")):
        drops = {"MAGIC_POWERED_BEDROCK_COMPAT"} if yaml_file.name == "items.yml" else set()
        dedupe_top_level_yaml(yaml_file, drops)

    for path in sorted(root.rglob("*")):
        if path.is_file() and path.suffix.lower() in {".yml", ".yaml", ".js"}:
            replace_runtime_references(path)

    print("Applied Magic Legacy runtime compatibility fixes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
