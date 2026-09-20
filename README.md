<div align="center">

# Magic Legacy

### Managed Magic addon for Slimefun Legacy

Magic Legacy preserves the original Magic RSC content while moving its fragile runtime pieces into a maintained Java plugin.

**Current plugin line: 2.0.x**

</div>

## Installation

Magic Legacy 2.0 is installed like a normal plugin.

1. Stop the server normally.
2. Put `SF_MagicLegacy2.0.5.jar` in the server's `plugins/` folder.
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

### Native migrations in 2.0.5

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

The intended end state is a fully native Slimefun addon with the same established `MAGIC_*` identities and no RSC runtime dependency.

## Credits

Original Magic project and content: **magicsolo / Yomicer**

Legacy compatibility maintenance: **wickidcow**

Magic Legacy is an unofficial community compatibility project. It is not an official Minecraft product and is not approved by or associated with Mojang or Microsoft.
