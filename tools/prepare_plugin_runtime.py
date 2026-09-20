#!/usr/bin/env python3
"""Prepare the fixed Magic runtime that is embedded inside MagicLegacy.jar."""
from __future__ import annotations

import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


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


def run_checks(destination: Path) -> None:
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


def main() -> int:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: prepare_plugin_runtime.py <destination> <plugin-version>")

    destination = Path(sys.argv[1]).resolve()
    version = sys.argv[2].strip()
    if not re.fullmatch(r"\d+(?:\.\d+)+", version):
        raise SystemExit(f"Invalid plugin version: {version!r}")

    copy_runtime(destination)
    run_checks(destination)
    stamp_runtime(destination, version)

    required = [
        destination / "info.yml",
        destination / "items.yml",
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
