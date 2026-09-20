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
        "MAGIC_NEW_PLANECUP": "planecup",
        "MAGIC_BANNER_LIANHUN": "BANNER_LIST",
        "MAGIC_BANNER_SOUL": "BANNER_INSERT",
        "MAGIC_POWER_BANK_DESCRIPTION": "CDB_1",
        "MAGIC_POWER_BANK_ALPHA": "CDB_1",
        "MAGIC_POWER_BANK_BETA": "CDB_2",
        "MAGIC_TP_PAPER": "specialItem/定点传送卷轴",
        "MAGIC_TP_STICK": "specialItem/定点传送法杖",
        "MAGIC_RANDOM_TP_PAPER": "specialItem/破损的传送卷轴",
        "MAGIC_ENCHANT_UP_1": "enchant_up_1",
        "MAGIC_ENCHANT_UP_1_MAX": "enchant_up_1_max",
        "MAGIC_ENCHANT_UPUP_1": "enchant_upup_1",
        "MAGIC_ENCHANT_UPUP_1_MAX": "enchant_upup_1_max",
        "MAGIC_ATTRIBUTE_UPUP_1": "attribute_upup_1",
        "MAGIC_ATTRIBUTE_UPUP_1_MAX": "attribute_upup_1_max",
        "MAGIC_GENSHIN_IMPACT_RADDOM": "Genshin_box",
        "MAGIC_MUSIC": "LQ",
        "MAGIC_DUST_PICKAXE": "MOFENGAO",
        "MAGIC_STORE_FIX_1": "MFCT_FIX",
        "MAGIC_STORE_FIX_2": "MFCT_FIX",
        "MAGIC_STORE_FIX_3": "MFCT_FIX",
        "MAGIC_STORE_FIX_4": "MFCT_FIX",
        "MAGIC_STORE_FIX_5": "MFCT_FIX",
        "MAGIC_STORE_FIX_6": "MFCT_FIX",
        "MAGIC_STORE_FIX_7": "MFCT_FIX",
        "MAGIC_STORE_FIX_8": "MFCT_FIX",
        "MAGIC_STORE_FIX_9": "MFCT_FIX",
        "MAGIC_STORE_FIX_10": "MFCT_FIX",
        "MAGIC_STORE_FIX_11": "MFCT_FIX",
        "MAGIC_STORE_FIX_12": "MFCT_FIX",
        "MAGIC_STORE_FIX_13": "MFCT_FIX",
        "MAGIC_BEE_CATCH": "SOME_CATCH",
        "MAGIC_PIG_CATCH": "SOME_CATCH",
        "MAGIC_SHEEP_CATCH": "SOME_CATCH",
        "MAGIC_CHICKEN_CATCH": "SOME_CATCH",
        "MAGIC_COW_CATCH": "SOME_CATCH",
        "MAGIC_OCELOT_CATCH": "SOME_CATCH",
        "MAGIC_CAT_CATCH": "SOME_CATCH",
        "MAGIC_DONKEY_CATCH": "SOME_CATCH",
        "MAGIC_FOX_CATCH": "SOME_CATCH",
        "MAGIC_FROG_CATCH": "SOME_CATCH",
        "MAGIC_GOAT_CATCH": "SOME_CATCH",
        "MAGIC_HOGLIN_CATCH": "SOME_CATCH",
        "MAGIC_HORSE_CATCH": "SOME_CATCH",
        "MAGIC_LLAMA_CATCH": "SOME_CATCH",
        "MAGIC_TRADER_LLAMA_CATCH": "SOME_CATCH",
        "MAGIC_MOOSHROOM_CATCH": "SOME_CATCH",
        "MAGIC_MULE_CATCH": "SOME_CATCH",
        "MAGIC_PANDA_CATCH": "SOME_CATCH",
        "MAGIC_RABBIT_CATCH": "SOME_CATCH",
        "MAGIC_STRIDER_CATCH": "SOME_CATCH",
        "MAGIC_TURTLE_CATCH": "SOME_CATCH",
        "MAGIC_WOLF_CATCH": "SOME_CATCH",
        "MAGIC_ALLAY_CATCH": "SOME_CATCH",
        "MAGIC_ALL_CATCH": "ALL_CATCH",
        "MAGIC_EGG_BEE": "ANIMALS_SPAWN",
        "MAGIC_PIG_1": "ANIMALS_SPAWN",
        "MAGIC_SHEEP_1": "ANIMALS_SPAWN",
        "MAGIC_CHICKEN_1": "ANIMALS_SPAWN",
        "MAGIC_COW_1": "ANIMALS_SPAWN",
        "MAGIC_OCELOT_1": "ANIMALS_SPAWN",
        "MAGIC_CAT_1": "ANIMALS_SPAWN",
        "MAGIC_DONKEY_1": "ANIMALS_SPAWN",
        "MAGIC_FOX_1": "ANIMALS_SPAWN",
        "MAGIC_FROG_1": "ANIMALS_SPAWN",
        "MAGIC_GOAT_1": "ANIMALS_SPAWN",
        "MAGIC_HOGLIN_1": "ANIMALS_SPAWN",
        "MAGIC_HORSE_1": "ANIMALS_SPAWN",
        "MAGIC_LLAMA_1": "ANIMALS_SPAWN",
        "MAGIC_TRADER_LLAMA_1": "ANIMALS_SPAWN",
        "MAGIC_MOOSHROOM_1": "ANIMALS_SPAWN",
        "MAGIC_MULE_1": "ANIMALS_SPAWN",
        "MAGIC_PANDA_1": "ANIMALS_SPAWN",
        "MAGIC_RABBIT_1": "ANIMALS_SPAWN",
        "MAGIC_STRIDER_1": "ANIMALS_SPAWN",
        "MAGIC_TURTLE_1": "ANIMALS_SPAWN",
        "MAGIC_WOLF_1": "ANIMALS_SPAWN",
        "MAGIC_EGG_IRON_GOLEM": "ARTIFICIAL_SPAWN",
        "MAGIC_ARTIFICIAL_WITHER_SKELETON": "ARTIFICIAL_SPAWN",
        "MAGIC_ARTIFICIAL_SKELETON": "ARTIFICIAL_SPAWN",
        "MAGIC_ARTIFICIAL_CREEPER": "ARTIFICIAL_SPAWN",
        "MAGIC_ARTIFICIAL_ZOMBIE": "ARTIFICIAL_SPAWN",
        "MAGIC_ARTIFICIAL_GIANT": "ARTIFICIAL_SPAWN",
        "MAGIC_ALLAY_1": "ARTIFICIAL_SPAWN",
    },
    "machines.yml": {
        "MAGIC_TEST_WHITE_WOOL": "NO_DROP",
        "MAGIC_KAOGU_SAND_1": "可疑的沙子",
        "MAGIC_KAOGU_GRAVEL_1": "可疑的沙砾",
        "MAGIC_B_SOUND_MACHINE_1": "B动静制造机附近",
        "MAGIC_B_SOUND_MACHINE_2": "B动静制造机全体",
        "MAGIC_BLACK_ROLE": "黑洞电容",
    },
    "foods.yml": {
        "MAGIC_FOODS_RANDOMFOOD": "randomfood",
        "MAGIC_FOODS_JIAOZI": "snow_food",
        "MAGIC_FOODS_APPLE": "snow_food",
        "MAGIC_FOODS_A_1": "snow_food",
        "MAGIC_FOODS_A_2": "snow_food",
        "MAGIC_FOODS_A_3": "snow_food",
        "MAGIC_FOODS_A_4": "snow_food",
        "MAGIC_FOODS_A_5": "snow_food",
        "MAGIC_FOODS_A_6": "snow_food",
        "MAGIC_FOODS_A_7": "snow_food",
        "MAGIC_FOODS_A_8": "snow_food",
        "MAGIC_FOODS_A_9": "snow_food",
        "MAGIC_FOODS_A_10": "snow_food",
    },
}

MIGRATED_SCRIPT_FAMILIES: dict[str, dict[str, int]] = {
    "machines.yml": {
        "SPAWNER": 132,
    },
}

ORPHANED_RUNTIME_SCRIPTS: set[str] = set()


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


def strip_script_family_hooks(path: Path, scripts: dict[str, int]) -> None:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    pattern = re.compile(r'^\s+script:\s*["\x27]?(.+?)["\x27]?\s*(?:#.*)?$')
    removed = {name: 0 for name in scripts}
    out: list[str] = []

    for line in lines:
        raw = line.rstrip("\r\n")
        match = pattern.match(raw)
        if match and match.group(1) in scripts:
            removed[match.group(1)] += 1
            continue
        out.append(line)

    failures = {
        name: {"expected": scripts[name], "actual": count}
        for name, count in removed.items()
        if count != scripts[name]
    }
    if failures:
        raise RuntimeError(f"Native script family hook mismatch in {path.name}: {failures}")

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

    for yaml_name, scripts in MIGRATED_SCRIPT_FAMILIES.items():
        path = destination / yaml_name
        if not path.is_file():
            raise RuntimeError(f"Missing staged runtime file: {path}")
        strip_script_family_hooks(path, scripts)
        migrated_script_names.update(scripts)

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
