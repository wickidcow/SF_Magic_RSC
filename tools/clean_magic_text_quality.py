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
VERSION_RE = re.compile(r"(?m)^version:\s*Legacy-([0-9]+(?:\.[0-9]+)*)\s*$")
LEGACY_STAMP_RE = re.compile(r"(?:Legacy|Release)-1\.1\.(?:16|17|18)")

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


def is_maintenance_path(root: Path, path: Path) -> bool:
    """Ignore maintenance folders *inside* root, never ancestors of root itself."""
    try:
        relative = path.relative_to(root)
    except ValueError:
        return False
    return any(part in {".git", "audit", "dist"} for part in relative.parts[:-1])


def stamp_runtime_version(root: Path) -> int:
    """Keep packaged lore/version badges aligned with info.yml.

    Older upstream/runtime cleanup intentionally preserved a few historical
    1.1.16-1.1.18 labels. Release packaging should never ship a mixed version,
    so normalize only those known legacy labels to the current info.yml value.
    """
    info = root / "info.yml"
    if not info.is_file():
        return 0
    info_text = info.read_text(encoding="utf-8")
    match = VERSION_RE.search(info_text)
    if not match:
        return 0
    current = f"Legacy-{match.group(1)}"
    changes = 0
    for path in sorted(root.rglob("*.yml")):
        if is_maintenance_path(root, path):
            continue
        text = path.read_text(encoding="utf-8")
        updated, count = LEGACY_STAMP_RE.subn(current, text)
        if count:
            path.write_text(updated, encoding="utf-8")
            changes += count
    return changes


def active_unsafe_polyglot_lines(text: str) -> list[str]:
    """Return executable-looking host calls; comments are harmless."""
    unsafe: list[str] = []
    for raw in text.splitlines():
        stripped = raw.strip()
        if not stripped or stripped.startswith("//"):
            continue
        if any(pattern in raw for pattern in UNSAFE_POLYGLOT_PATTERNS):
            unsafe.append(stripped)
    return unsafe


def apply_replacements(text: str, replacements: tuple[tuple[str, str], ...]) -> tuple[str, int]:
    changes = 0
    updated = text
    for old, new in replacements:
        count = updated.count(old)
        if count:
            updated = updated.replace(old, new)
            changes += count
    return updated, changes


def fix_polyglot_host_reflection(path: Path) -> int:
    """Keep GraalJS from reflecting over arbitrary addon SlimefunItem classes.

    Graal resolves a member call by enumerating the concrete host class' public
    methods. If another addon class has an obsolete method signature (for
    example an IE1 StorageUnit type), even a harmless getId() call can throw a
    NoClassDefFoundError before the intended method is invoked.

    Runtime Magic scripts therefore compare returned SlimefunItem host objects
    to known `getSfItemById(...)` objects, or compare ItemStacks, instead of
    invoking getId()/getOutputSlots() on arbitrary addon objects.
    """
    text = path.read_text(encoding="utf-8")
    updated = text
    changes = 0

    if path.name == POLYGLOT_SCRIPT:
        updated, n = apply_replacements(
            updated,
            (
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
            ),
        )
        changes += n

    elif path.name == "magic_custom_machine.js":
        updated, n = apply_replacements(
            updated,
            (
                ("        let sfitemid = sfitem.getId()\n", ""),
                (
                    'if (!(sfitemid === "MAGIC_FLOWER_MIX_BOX_1")){',
                    'if (!(sfitem === getSfItemById("MAGIC_FLOWER_MIX_BOX_1"))){',
                ),
                (
                    'if (!(sfitemid === "MAGIC_GEOMINER_BOX")){',
                    'if (!(sfitem === getSfItemById("MAGIC_GEOMINER_BOX"))){',
                ),
            ),
        )
        changes += n

    elif path.name == "魔法矩阵-发电机1.js":
        updated, n = apply_replacements(
            updated,
            (
                ("    let sfitemid = sfitem.getId()\n", ""),
                (
                    'if (!(sfitemid === "MAGIC_POWER_MIX_BOX_1")){',
                    'if (!(sfitem === getSfItemById("MAGIC_POWER_MIX_BOX_1"))){',
                ),
                (
                    "function countTargetItemsInMenu(menu, targetId) {\n"
                    "    let count = 0;\n"
                    "    for (let i = 0; i < menu.getSize(); i++) {",
                    "function countTargetItemsInMenu(menu, targetId) {\n"
                    "    let count = 0;\n"
                    "    let targetItem = getSfItemById(targetId);\n"
                    "    if (targetItem == null) {\n"
                    "        return 0;\n"
                    "    }\n"
                    "    let targetStack = targetItem.getItem();\n"
                    "    for (let i = 0; i < menu.getSize(); i++) {",
                ),
                (
                    "            let slimefunItem = getSfItemByItem(itemStack);\n"
                    "            if (slimefunItem && slimefunItem.getId() === targetId) {",
                    "            if (isItemSimilar(itemStack, targetStack, true)) {",
                ),
            ),
        )
        changes += n

    elif path.name == "SPAWNER.js":
        updated, n = apply_replacements(
            updated,
            (
                ("    let sfitemid = sfitem.getId()\n", ""),
                (
                    "SPAWNER_TYPE.find(spawner => spawner.id === sfitemid)",
                    "SPAWNER_TYPE.find(spawner => sfitem === getSfItemById(spawner.id))",
                ),
            ),
        )
        changes += n

    elif path.name == "服务器.js":
        updated, n = apply_replacements(
            updated,
            (
                (
                    "CARGO_STORAGE_UNITS.some(unit => unit === sfitem.getId())",
                    "CARGO_STORAGE_UNITS.some(unit => sfitem === getSfItemById(unit))",
                ),
                (
                    "CARGO_STORAGE_UNITS.some(unit => unit === slimefunItem.getId())",
                    "CARGO_STORAGE_UNITS.some(unit => slimefunItem === getSfItemById(unit))",
                ),
            ),
        )
        changes += n

    elif path.name == "MFCT_FIX.js":
        updated, n = apply_replacements(
            updated,
            (
                ("    let target_sfid_Store = slimefunItem.getId();\n", ""),
                (
                    "    let slimefunItem2 = getSfItemByItem(player.getInventory().getItemInMainHand());\n"
                    "    let target_sfid_Fix = slimefunItem2.getId();",
                    "    let slimefunItem2 = getSfItemByItem(player.getInventory().getItemInMainHand());\n"
                    "    if (slimefunItem == null || slimefunItem2 == null) {\n"
                    "        return;\n"
                    "    }",
                ),
                (
                    "    const found = storageUnits.some(unit => \n"
                    "        unit.sfid_Store === target_sfid_Store && unit.sfid_Fix === target_sfid_Fix\n"
                    "    );",
                    "    const found = storageUnits.some(unit =>\n"
                    "        slimefunItem === getSfItemById(unit.sfid_Store) &&\n"
                    "        slimefunItem2 === getSfItemById(unit.sfid_Fix)\n"
                    "    );",
                ),
            ),
        )
        changes += n

    elif path.name == "魔法植物1.js":
        updated, n = apply_replacements(
            updated,
            (
                (
                    "        let sfplantid = sfitem.getId();",
                    '        let sfplantid = (sfitem === getSfItemById("MAGIC_PLANT_1")) ? "MAGIC_PLANT_1" : null;',
                ),
            ),
        )
        changes += n

    remaining = active_unsafe_polyglot_lines(updated)
    if remaining:
        raise RuntimeError(
            f"Unsafe Graal Slimefun host reflection remained in {path.name}: " + " | ".join(remaining)
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

    version_stamps = stamp_runtime_version(root)

    polyglot_changes = 0
    for path in sorted(root.rglob("*.js")):
        if is_maintenance_path(root, path):
            continue
        polyglot_changes += fix_polyglot_host_reflection(path)

    redundant = placeholders = replacements = duplicates = 0
    for path in sorted(root.rglob("*.yml")):
        if is_maintenance_path(root, path):
            continue
        r, p, n, d = clean_text_file(path)
        redundant += r
        placeholders += p
        replacements += n
        duplicates += d

    print(
        "Cleaned Magic runtime/text quality "
        f"({polyglot_changes} Graal host-reflection fix(es), {version_stamps} version stamp(s), "
        f"{redundant} redundant title lore, {placeholders} placeholder lore, "
        f"{replacements} verified text replacements, {duplicates} duplicate lore lines)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
