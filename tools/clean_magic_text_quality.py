#!/usr/bin/env python3
"""Clean safe player-facing English/lore quality issues in a Magic RSC tree.

Order matters: runtime compatibility is normalized first, then text is cleaned,
redundant title/placeholder lore is removed, and finally visible lore is
deduplicated. This prevents two previously different strings from becoming
duplicates after translation/format cleanup.

IDs, script references, recipe keys, and serialized data keys are never renamed.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

from dedupe_magic_lore import LIST_RE, LORE_RE, clean_file as dedupe_lore_file, indentation, visible

NAME_RE = re.compile(r"^(\s*)(?:name|display-name):\s*(.+?)\s*$")

TEXT_REPLACEMENTS = {
    "&7lMagicl": "&8Magic Legacy",
    "&7lMagic-Spawnerl": "&8Magic • Spawners",
    "&7lMagic-l": "&8Magic Legacy",
    "&7lMagic-Power and Energyl": "&8Magic • Power & Energy",
    "&7lMagic-Materialsl": "&8Magic • Materials",
    "Magic Generators Quartz": "Magic Quartz Generator",
    "Magic Generators Magma Block": "Magic Magma Block Generator",
    "Magic Generators Wood": "Magic Wood Generator",
    "Magic Generators Redstone 1": "Magic Redstone Generator I",
    "Magic New Player Gold Pan2": "Magic New Player Gold Pan II",
    "Magic Stoneworks Factory9": "Magic Stoneworks Factory 9",
    "Magic Stoneworks Factory81": "Magic Stoneworks Factory 81",
    "Magic Stoneworks Factory729": "Magic Stoneworks Factory 729",
}

POLYGLOT_SCRIPT = "魔法无尽一体化.js"
UNSAFE_POLYGLOT_PATTERNS = (
    "sfitem.getId()",
    "sfitem.getOutputSlots()",
    "slimefunItem.getId()",
)


def fix_polyglot_host_reflection(path: Path) -> int:
    """Keep GraalJS from reflecting over arbitrary addon SlimefunItem classes.

    Graal resolves a member call by enumerating the concrete host class' public
    methods. If another addon class has an obsolete method signature (for
    example an IE1 StorageUnit type), even a harmless getId() call can throw a
    NoClassDefFoundError before the intended method is invoked.

    The Magic/Infinity integration only needs identity checks for foreign
    Slimefun items. Compare the returned host object with the known Magic item,
    and resolve known Magic item stacks/slots instead of invoking members on an
    arbitrary addon object.
    """
    if path.name != POLYGLOT_SCRIPT:
        return 0

    text = path.read_text(encoding="utf-8")
    updated = text
    changes = 0

    replacements = (
        (
            "let sfitemid = sfitem.getId()",
            'let sfitemid = (sfitem === getSfItemById("MAGIC_INFINITY_MIX_BOX_1")) ? "MAGIC_INFINITY_MIX_BOX_1" : null',
        ),
        (
            "let sfitem = StorageCacheUtils.getSfItem(containerLocation);\n        let outslots = sfitem.getOutputSlots();",
            'let mixBoxItem = getSfItemById("MAGIC_INFINITY_MIX_BOX_1");\n'
            "        if (mixBoxItem == null) {\n"
            "            return;\n"
            "        }\n"
            "        let outslots = mixBoxItem.getOutputSlots();",
        ),
        (
            "function countTargetItemsInMenu(menu, targetId) {\n"
            "    let count = 0;\n"
            "    for (let i = 45; i < 52; i++) {",
            "function countTargetItemsInMenu(menu, targetId) {\n"
            "    let count = 0;\n"
            "    let targetItem = getSfItemById(targetId);\n"
            "    if (targetItem == null) {\n"
            "        return 0;\n"
            "    }\n"
            "    let targetStack = targetItem.getItem();\n"
            "    for (let i = 45; i < 52; i++) {",
        ),
        (
            "            let slimefunItem = getSfItemByItem(itemStack);\n"
            "            if (slimefunItem && slimefunItem.getId() === targetId) {",
            "            if (isItemSimilar(itemStack, targetStack, true)) {",
        ),
    )

    for old, new in replacements:
        count = updated.count(old)
        if count:
            updated = updated.replace(old, new)
            changes += count

    remaining = [pattern for pattern in UNSAFE_POLYGLOT_PATTERNS if pattern in updated]
    if remaining:
        raise RuntimeError(
            f"Unsafe Graal Slimefun host reflection remained in {path.name}: {', '.join(remaining)}"
        )

    if updated != text:
        path.write_text(updated, encoding="utf-8")
    return changes


def in_lore_block(lore_indent: int | None, line: str) -> tuple[bool, int | None]:
    if lore_indent is None:
        return False, None
    stripped = line.strip()
    current = indentation(line)
    list_match = LIST_RE.match(line.rstrip("\r\n"))
    # Bukkit serialized ItemStack YAML can put lore list entries at the same
    # indentation as the `lore:` key; normal RSC YAML nests them deeper.
    if list_match and current >= lore_indent:
        return True, lore_indent
    if current > lore_indent:
        return True, lore_indent
    if not stripped or stripped.startswith("#"):
        return False, lore_indent
    return False, None


def clean_text_file(path: Path) -> tuple[int, int, int, int]:
    text = path.read_text(encoding="utf-8")
    replaced = text
    replacement_count = 0
    for old, new in TEXT_REPLACEMENTS.items():
        count = replaced.count(old)
        if count:
            replacement_count += count
            replaced = replaced.replace(old, new)

    lines = replaced.splitlines(keepends=True)
    rebuilt: list[str] = []
    lore_indent: int | None = None
    lore_name = ""
    current_name = ""
    current_name_indent: int | None = None
    redundant_removed = 0
    placeholders_removed = 0

    for line in lines:
        no_eol = line.rstrip("\r\n")
        name_match = NAME_RE.match(no_eol)
        if name_match:
            current_name = visible(name_match.group(2))
            current_name_indent = len(name_match.group(1))

        lore_match = LORE_RE.match(no_eol)
        if lore_match:
            lore_indent = len(lore_match.group(1))
            lore_name = current_name if current_name_indent == lore_indent else ""
            rebuilt.append(line)
            continue

        in_lore, lore_indent = in_lore_block(lore_indent, line)
        if not in_lore:
            if lore_indent is None:
                lore_name = ""
            rebuilt.append(line)
            continue

        list_match = LIST_RE.match(no_eol)
        if not list_match:
            rebuilt.append(line)
            continue

        lore_text = visible(list_match.group(2))
        if lore_text.casefold() == "tier":
            placeholders_removed += 1
            continue
        if lore_name and lore_text and lore_text.casefold() == lore_name.casefold():
            redundant_removed += 1
            continue

        rebuilt.append(line)

    updated = "".join(rebuilt)
    if updated != text:
        path.write_text(updated, encoding="utf-8")

    # Must run last: formatting/translation replacements can make formerly
    # different source lines identical to players.
    duplicate_removed = dedupe_lore_file(path)
    return redundant_removed, placeholders_removed, replacement_count, duplicate_removed


def main() -> int:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: clean_magic_text_quality.py <Magic folder>")
    root = Path(sys.argv[1]).resolve()
    if not root.is_dir():
        raise SystemExit(f"Not a directory: {root}")

    polyglot_changes = 0
    for path in sorted(root.rglob("*.js")):
        if any(part in {".git", "audit", "dist"} for part in path.parts):
            continue
        polyglot_changes += fix_polyglot_host_reflection(path)

    redundant = placeholders = replacements = duplicates = 0
    for path in sorted(root.rglob("*.yml")):
        if any(part in {".git", "audit", "dist"} for part in path.parts):
            continue
        r, p, n, d = clean_text_file(path)
        redundant += r
        placeholders += p
        replacements += n
        duplicates += d

    print(
        "Cleaned Magic runtime/text quality "
        f"({polyglot_changes} Graal host-reflection fix(es), {redundant} redundant title lore, "
        f"{placeholders} placeholder lore, {replacements} verified text replacements, "
        f"{duplicates} duplicate lore lines)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
