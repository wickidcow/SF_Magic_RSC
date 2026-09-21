#!/usr/bin/env python3
"""Validate Magic saved-item YAML files.

Checks:
- one-line single-quoted YAML scalars use doubled apostrophes correctly;
- serialized Bukkit TILE_ENTITY ItemStacks carry a valid BlockEntityTag id.

The validator is dependency-free so it can run in packaging, release, audit,
and local builds.
"""
from __future__ import annotations

import base64
import binascii
import gzip
import re
import struct
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


def _read_u16(data: bytes, pos: int) -> tuple[int, int]:
    if pos + 2 > len(data):
        raise ValueError("truncated NBT u16")
    return struct.unpack(">H", data[pos : pos + 2])[0], pos + 2


def _read_i32(data: bytes, pos: int) -> tuple[int, int]:
    if pos + 4 > len(data):
        raise ValueError("truncated NBT i32")
    return struct.unpack(">i", data[pos : pos + 4])[0], pos + 4


def _read_string(data: bytes, pos: int) -> tuple[str, int]:
    length, pos = _read_u16(data, pos)
    end = pos + length
    if end > len(data):
        raise ValueError("truncated NBT string")
    return data[pos:end].decode("utf-8"), end


def _skip_payload(data: bytes, pos: int, tag_type: int) -> int:
    if tag_type == 1:
        return pos + 1
    if tag_type == 2:
        return pos + 2
    if tag_type in {3, 5}:
        return pos + 4
    if tag_type in {4, 6}:
        return pos + 8
    if tag_type == 7:
        length, pos = _read_i32(data, pos)
        return pos + length
    if tag_type == 8:
        _value, pos = _read_string(data, pos)
        return pos
    if tag_type == 9:
        if pos >= len(data):
            raise ValueError("truncated NBT list")
        element_type = data[pos]
        pos += 1
        length, pos = _read_i32(data, pos)
        if length < 0:
            raise ValueError("negative NBT list length")
        for _ in range(length):
            pos = _skip_payload(data, pos, element_type)
        return pos
    if tag_type == 10:
        while True:
            if pos >= len(data):
                raise ValueError("unterminated NBT compound")
            child_type = data[pos]
            pos += 1
            if child_type == 0:
                return pos
            _name, pos = _read_string(data, pos)
            pos = _skip_payload(data, pos, child_type)
    if tag_type == 11:
        length, pos = _read_i32(data, pos)
        return pos + (4 * length)
    if tag_type == 12:
        length, pos = _read_i32(data, pos)
        return pos + (8 * length)
    raise ValueError(f"unsupported NBT tag type {tag_type}")


def _compound_entries(data: bytes, pos: int) -> dict[str, tuple[int, int]]:
    entries: dict[str, tuple[int, int]] = {}
    while True:
        if pos >= len(data):
            raise ValueError("unterminated NBT compound")
        tag_type = data[pos]
        pos += 1
        if tag_type == 0:
            return entries

        name, pos = _read_string(data, pos)
        payload_start = pos
        entries[name] = (tag_type, payload_start)
        pos = _skip_payload(data, payload_start, tag_type)


def _block_entity_id(raw_nbt: bytes) -> str | None:
    if not raw_nbt or raw_nbt[0] != 10:
        raise ValueError("NBT root is not a compound")

    _root_name, root_payload = _read_string(raw_nbt, 1)
    root = _compound_entries(raw_nbt, root_payload)
    block_entity = root.get("BlockEntityTag")
    if block_entity is None or block_entity[0] != 10:
        raise ValueError("missing BlockEntityTag compound")

    block_entries = _compound_entries(raw_nbt, block_entity[1])
    id_entry = block_entries.get("id")
    if id_entry is None:
        return None
    if id_entry[0] != 8:
        raise ValueError("BlockEntityTag id is not a string")

    value, _ = _read_string(raw_nbt, id_entry[1])
    return value


def validate_tile_entity_payload(path: Path, text: str) -> str | None:
    if not re.search(r"(?m)^\s*meta-type:\s*TILE_ENTITY\s*$", text):
        return None

    internal = re.search(r"(?m)^\s*internal:\s*(\S+)\s*$", text)
    if internal is None:
        return f"{path}: TILE_ENTITY ItemStack is missing its serialized internal payload"

    try:
        raw = gzip.decompress(base64.b64decode(internal.group(1), validate=True))
        block_entity_id = _block_entity_id(raw)
    except (binascii.Error, OSError, UnicodeDecodeError, ValueError) as ex:
        return f"{path}: invalid TILE_ENTITY internal payload ({ex})"

    if not block_entity_id:
        return (
            f"{path}: serialized BlockEntityTag is missing its id; modern Paper "
            "may log 'Skipping block entity with invalid type: null'"
        )

    if ":" not in block_entity_id:
        return f"{path}: invalid BlockEntityTag id {block_entity_id!r}"

    return None


def main() -> int:
    directory = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "saveditems"
    if not directory.is_dir():
        print(f"Saved-item directory not found: {directory}", file=sys.stderr)
        return 2

    files = sorted(directory.rglob("*.yml"))
    errors: list[str] = []
    tile_entities = 0

    for path in files:
        text = path.read_text(encoding="utf-8", errors="strict")
        for line_number, line in enumerate(text.splitlines(), 1):
            error = validate_single_quoted_scalar(path, line_number, line)
            if error:
                errors.append(error)

        if re.search(r"(?m)^\s*meta-type:\s*TILE_ENTITY\s*$", text):
            tile_entities += 1
        tile_error = validate_tile_entity_payload(path, text)
        if tile_error:
            errors.append(tile_error)

    if errors:
        print("Saved-item YAML validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        f"Validated {len(files)} saved-item YAML files: quoted scalars are well formed; "
        f"{tile_entities} TILE_ENTITY payload(s) have valid BlockEntityTag ids"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
