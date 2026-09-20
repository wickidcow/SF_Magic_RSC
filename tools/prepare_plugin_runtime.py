#!/usr/bin/env python3
"""Prepare the fixed Magic runtime that is embedded inside MagicLegacy.jar."""
from __future__ import annotations

import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

MIGRATED_SCRIPTS: dict[str, dict[str, str]] = {
    "items.yml": {
        "MAGIC_GUN_1": "基础枪",
        "MAGIC_CHRISTMAS_SNOWBALL": "CHRISTMAS_SNOWBALL",
        "MAGIC_EXP_COLLECTOR": "MFEXPBOTTLE",
        "MAGIC_EXP_BOTTLE": "MFEXPBOTTLE100",
        "MAGIC_UNBREAKABLE_RUNE": "UNBREAKABLE_RUNE",
        "MAGIC_INFINITE_STICK": "INFINITE_STICK",
        "MAGIC_INFINITY_BLADE_1": "QYZJ_1",
        "MAGIC_SOUND": "sound",
        "MAGIC_STICK_JIGUANG_1": "magic_stick_jiguang_1",
        "MAGIC_ZHENFA_FIRE_1": "阵法_小火苗_1",
    },
    "foods.yml": {
        "MAGIC_FOODS_RANDOMFOOD": "randomfood",
    },
}

ORPHANED_RUNTIME_SCRIPTS = {
    "planecup",
}


def copy_runtime(destination: Path) -> None:
    if destination.exists():
        shutil.rmtree(destination)
    destination.mkdir(parents=True, exist_ok=True)

    for path in sorted(ROOT.glob("*.yml")):
        shutil.copy2(path, destination / path.name)

    saved = ROOT / "saveditems"
    if saved.is_dir():
        shutil.copytree(saved, destination / "saveditems", dirs_exist_ok=True)

    scripts = ROOT / "scripts"
    for path in sorted(scripts.rglob("*.js")):
        relative = path.relative_to(scripts)
        target = destination / "scripts" / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)


def stamp_runtime(destination: Path, version: str) -> None:
    label = f"Legacy-{version}"

    for path in sorted(destination.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in {".yml", ".yaml", ".js"}:
            continue
        text = path.read_text(encoding="utf-8")
        updated = re.sub(
            r"(?i)(?:Release|Legacy)-\d+\.\d+\.\d+",
            label,
            text,
        )
        if updated != text:
            path.write_text(updated, encoding="utf-8")

    info = destination / "info.yml"
    text = info.read_text(encoding="utf-8")
    text = re.sub(r"(?m)^version:\s*.*$", f"version: {label}", text)
    text = re.sub(
        r'(?m)^- " Legacy version: [^"]*"$',
        f'- " Legacy version: {version}"',
        text,
    )

    if "- DynaTech" not in text:
        needle = "- InfinityExpansion2\n"
        if needle not in text:
            raise RuntimeError("Could not locate InfinityExpansion2 dependency in staged info.yml")
        text = text.replace(needle, needle + "- DynaTech\n", 1)

    text = re.sub(
        r"(?m)^description:\s*.*$",
        "description: Magic Legacy managed runtime for Slimefun Legacy, deployed by the MagicLegacy plugin.",
        text,
    )
    info.write_text(text, encoding="utf-8")


def strip_script_hooks(path: Path, migrated: dict[str, str]) -> None:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    top = re.compile(r"^([A-Za-z0-9_.-]+):\s*(?:#.*)?$")
    script = re.compile(r'^\s+script:\s*["\x27]?(.+?)["\x27]?\s*(?:#.*)?$')

    current: str | None = None
    removed = {item_id: 0 for item_id in migrated}
    out: list[str] = []

    for line in lines:
        raw = line.rstrip("\r\n")
        match = top.match(raw)
        if match:
            current = match.group(1)

        expected = migrated.get(current or "")
        if expected is not None:
            script_match = script.match(raw)
            if script_match and script_match.group(1) == expected:
                removed[current] += 1
                continue

        out.append(line)

    failures = {item_id: count for item_id, count in removed.items() if count != 1}
    if failures:
        raise RuntimeError(f"Native script migration hook mismatch in {path.name}: {failures}")

    path.write_text("".join(out), encoding="utf-8")


def disable_migrated_scripts(destination: Path) -> None:
    """Remove RSC hooks and runtime files that now have native Java replacements."""
    migrated_script_names: set[str] = set()

    for yaml_name, mappings in MIGRATED_SCRIPTS.items():
        path = destination / yaml_name
        if not path.is_file():
            raise RuntimeError(f"Missing staged runtime file: {path}")
        strip_script_hooks(path, mappings)
        migrated_script_names.update(mappings.values())

    for script_name in sorted(migrated_script_names | ORPHANED_RUNTIME_SCRIPTS):
        script_path = destination / "scripts" / f"{script_name}.js"
        if not script_path.is_file():
            raise RuntimeError(f"Expected runtime script file was not staged: {script_path}")
        script_path.unlink()


def run_source_checks(destination: Path) -> None:
    subprocess.run(
        [sys.executable, str(ROOT / "tools" / "apply_runtime_fixes.py"), str(destination)],
        check=True,
        cwd=ROOT,
    )
    subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "validate_saveditems_yaml.py"), str(destination / "saveditems")],
        check=True,
        cwd=ROOT,
    )
    subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "validate_script_refs.py"), str(destination)],
        check=True,
        cwd=ROOT,
    )


def validate_final_runtime(destination: Path) -> None:
    subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "validate_script_refs.py"), str(destination)],
        check=True,
        cwd=ROOT,
    )


def main() -> int:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: prepare_plugin_runtime.py <destination> <plugin-version>")

    destination = Path(sys.argv[1]).resolve()
    version = sys.argv[2].strip()
    if not re.fullmatch(r"\d+(?:\.\d+)+", version):
        raise SystemExit(f"Invalid plugin version: {version!r}")

    copy_runtime(destination)
    run_source_checks(destination)
    stamp_runtime(destination, version)
    disable_migrated_scripts(destination)
    validate_final_runtime(destination)

    required = [
        destination / "info.yml",
        destination / "items.yml",
        destination / "foods.yml",
        destination / "recipe_machines.yml",
        destination / "scripts" / "服务器.js",
    ]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise RuntimeError(f"Prepared Magic runtime is incomplete: {missing}")

    count = sum(1 for path in destination.rglob("*") if path.is_file())
    print(f"Prepared Magic Legacy {version} runtime: {count} files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
