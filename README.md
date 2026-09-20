<div align="center">

# Magic Legacy

### Managed Magic addon for Slimefun Legacy

Magic Legacy preserves the original Magic RSC content while moving its fragile runtime pieces into a maintained Java plugin.

**Current plugin line: 2.0.x**

</div>

## Installation

Magic Legacy 2.0 is installed like a normal plugin.

1. Stop the server normally.
2. Put `SF_MagicLegacy2.0.15.jar` in the server's `plugins/` folder.
3. Keep the required Slimefun addons installed.
4. Start the server normally.

Do not manually extract a Magic folder from the release JAR.

On load, Magic Legacy deploys its verified embedded runtime to:

```text
plugins/RykenSlimefunCustomizer/addons/Magic/
```

This happens before RykenSlimefunCustomizer enables, so RSC can load the pack during the same normal startup.

If an older, manually installed `addons/Magic` folder is found, Magic Legacy preserves it under:

```text
plugins/MagicLegacy/backups/
```

The plugin then installs its managed copy. Future Magic Legacy updates replace only the folder marked as managed by Magic Legacy.

## Why 2.0 uses a managed runtime

Magic is a very large content pack. Rewriting every item, recipe and machine into Java at once would create unnecessary migration risk for existing worlds.

The 2.0 line therefore uses a staged conversion:

- the Java plugin owns deployment, validation and diagnostics;
- existing `MAGIC_*` identifiers remain intact;
- the verified YAML content is embedded in the JAR;
- release-time compatibility fixes are applied before the content is embedded;
- JavaScript-backed systems can be migrated to native Java incrementally without forcing item-ID changes.

RykenSlimefunCustomizer is still required for the content runtime during this stage. The goal is to remove that runtime dependency only after each major machine/script family has a native replacement and has been tested against existing worlds.

## Requirements

Required for the current 2.0 bridge:

- Slimefun Legacy
- RykenSlimefunCustomizer Legacy
- InfinityExpansion2
- DynaTech
- GeneticChickengineering
- Supreme
- FoxyMachines

Networks, FNAmplifications, and Magic Expansion are optional integrations. When they are installed, Magic Legacy soft-depends on them so their Slimefun items register before RSC loads the Magic runtime. `/magiclegacy doctor` checks representative registry IDs from each integration.

The primary server baseline remains the maintained Slimefun Legacy Paper/Purpur stack. The public JAR is compiled as Java 21 bytecode and is also checked against the current Paper 26.3 candidate API in the Slimefun Legacy addon bundle workflow.

## Diagnostics

Magic Legacy adds:

```text
/magiclegacy status
/magiclegacy doctor
/magiclegacy deploy
```

`status` shows deployment and plugin dependency state.

`doctor` also checks representative Slimefun registry IDs from Magic Legacy, InfinityExpansion2, DynaTech, Networks, FNAmplifications and Magic Expansion. This catches cases where a dependency JAR is present but the item Magic expects did not actually register.

`deploy` re-extracts the managed runtime. Use a normal full restart afterward; do not use Bukkit `/reload` for Slimefun/RSC updates.

## Compatibility work retained from 1.x

The embedded runtime is produced from the maintained source using the existing Magic Legacy repair pipeline. This includes the IE1-to-InfinityExpansion2 migration, saved-item YAML validation, missing-script checks, optional-recipe guards, text cleanup and the runtime fixes already developed for RSC Legacy.

The 2.0 builder also declares DynaTech explicitly because the maintained Magic recipes use DynaTech IDs such as the Vex Gem, Bee and Growth Chamber replacements.

## Development direction

The Java plugin is now the stable owner of Magic Legacy. Native migrations move into it in small testable groups, starting with the JavaScript systems that are most likely to fail across Graal/Paper changes.

### Native migrations in 2.0.15

- `MAGIC_GUN_1` no longer executes `scripts/基础枪.js` at runtime.
- The same 20-block hitscan, 5 damage, 8-tick cooldown, END_ROD beam, and firework sound are handled by `MagicGunListener` in Java.
- The established `MAGIC_GUN_1` Slimefun ID and RSC item/recipe definition are unchanged.
- `MAGIC_CHRISTMAS_SNOWBALL` no longer executes `scripts/CHRISTMAS_SNOWBALL.js`.
- Its native Java effect keeps the same one-block END_ROD beam while reducing the old roughly 5,000-particle click effect to about 21 particles.
- `MAGIC_EXP_COLLECTOR` and `MAGIC_EXP_BOTTLE` are native Java. The collector safely converts 100 player levels into the existing Magic EXP Bottle, and the bottle releases its stored XP without the old global placeholder-message spam.
- `MAGIC_UNBREAKABLE_RUNE` is native Java. It keeps the same purpose and item ID but uses a harmless visual lightning effect instead of spawning ten real lightning entities at the player.
- `MAGIC_INFINITE_STICK` is native Java. Its PvP-only instant-kill behavior is preserved, but it now respects cancelled damage events and no longer needs Graal scripting.
- `MAGIC_INFINITY_BLADE_1` is native Java. Its 1% lifesteal now uses `Attribute.MAX_HEALTH` instead of the deprecated `getMaxHealth()` API.
- `MAGIC_FOODS_RANDOMFOOD` is native Java. It preserves the 1–3 random-effect concept, uses the current Bukkit potion registry, avoids zero-second effects, and replaces broken placeholder messages with one concise result message.
- `planecup.js` is retained in source history for reference but excluded from the managed runtime because no Magic YAML or central listener references it.
- `MAGIC_SOUND` is native Java. It uses Paper's sound registry instead of the old `Sound.values()` assumption and plays each online player a random sound at that player's own location.
- `MAGIC_STICK_JIGUANG_1` is native Java. It preserves the 50 J cost, 25-block ray trace, 1,000 damage and dual-spiral visual while reducing particle calls and fixing the old vertical-vector edge case.
- `MAGIC_ZHENFA_FIRE_1` is now the player-facing **Magic Flame Formation** and runs in native Java. It preserves the actual 1-second cooldown, 5-block radius, 100 damage, and hunger/saturation cost while reducing the old roughly 10,800-particle activation to about 216 particles and removing Chinese/placeholder runtime messages.
- The entire 12-item snow-food family is native Java. The foods now use the current potion registry, never roll zero-second effects, and have names/lore that identify their effect instead of placeholders such as `Magic Foods A 4`. Effect strength remains randomized from level 1-50 and duration from 1-100 seconds.

### Lore quality in 2.0.15

The migrated items now have behavior-matched English lore instead of blank color lines or fragments such as `tier: 100`, `100 tier`, `Materials`, and `seconds`. The Infinity Blade wording was also corrected from incoming damage to damage dealt, matching its 1% lifesteal behavior. The 20 Magic mob data cards now use clean names such as `Magic Zombie Data Card`, identify themselves as converted mob-data cards, and no longer carry the misleading `800 J/s` item lore. The guide recipe and machine are consistently named **Magic Data Card Inserter**, with the machine showing its actual `20,000 J` energy-per-craft value. CI runs a lore wording audit that flags blank/color-only lore, low-information fragments, CJK text, `bug`, merged/duplicated placeholder wording, generic legacy power text, and known placeholder failure messages. The Magic Supreme Workbench now reports its configured 100 J per craft and 1,000 J capacity instead of the old incorrect 10,000/100,000 values; Storage Upgrade Table, EXP Bottle usage, and Grinding Stone guide entries also have descriptive lore. The Magic Geominer now explains its 5,200 J capacity, 1,314 J-per-machine-tick runtime draw, required Geominer Box placement, and local GEO-resource purpose, while its in-machine status text is English instead of Chinese. `MAGIC_INFINITY_MIX_1` is now player-facing as **Magic Infinity Processor** with its 2,147,483,647 J capacity, per-module energy costs, required mix-box placement, and dynamic-draw behavior explained; its live machine menu is also English and the old garbled/`??? J/s` lore is gone. Resource wording has also been cleaned for Magic Ingot, Infinite Water, Magic Bucket, Magic Lava Bucket, compressed coolant cells, Blizzard Core, and Magic Chicken Relic. The Blizzard Core recipe now explicitly records the Extreme Freezer ingredient amount as 1, matching RSC's previous defaulting behavior while removing ambiguous YAML.

The intended end state is a fully native Slimefun addon with the same established `MAGIC_*` identities and no RSC runtime dependency.

## Credits

Original Magic project and content: **magicsolo / Yomicer**

Legacy compatibility maintenance: **wickidcow**

Magic Legacy is an unofficial community compatibility project. It is not an official Minecraft product and is not approved by or associated with Mojang or Microsoft.
