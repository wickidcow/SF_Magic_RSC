# Magic RSC Legacy

Magic is a large RykenSlimefunCustomizer content pack originally created by **magicsolo (Yomicer)**. This fork is the Slimefun Legacy compatibility port maintained by **wickidcow**.

The goal of this fork is to preserve the original Magic content while updating it for a modern Slimefun Legacy server stack, including InfinityExpansion2.

## Current port target

- Slimefun Legacy
- Minecraft 1.21.11+
- Paper 26.2 primary runtime
- Java 21+ compatibility, with Java 25 as the primary server runtime
- InfinityExpansion2 instead of the original InfinityExpansion dependency
- English player-facing text

> **Status:** work in progress. The dependency layer has been switched to InfinityExpansion2, but the gameplay configuration is still being audited and migrated item-by-item. Do not treat this branch as a finished release yet.

## What Magic is

Magic is not a standalone Java plugin. It is a configuration-driven Slimefun addon loaded by **RykenSlimefunCustomizer**.

RykenSlimefunCustomizer and SlimeCustomizer can coexist. Magic should be installed as its own addon folder rather than merged into another customizer configuration.

## Required plugins

- Slimefun Legacy
- RykenSlimefunCustomizer
- GuizhanLibPlugin
- FoxyMachines
- InfinityExpansion2
- Supreme
- GeneticChickengineering

Optional integration:

- FNAmplifications

## Installation

1. Install RykenSlimefunCustomizer and all required plugins.
2. Start the server once so RykenSlimefunCustomizer creates its data folders.
3. Place the complete Magic addon folder in:

   `plugins/RykenSlimefunCustomizer/addons/Magic`

4. Start the server normally. Do not use a hot reload for Slimefun or this addon pack.
5. Review the generated Magic addon configuration if the loader reports an error.

## InfinityExpansion2 migration

The original Magic configuration was written against InfinityExpansion v1 item IDs. InfinityExpansion2 uses `IE_` IDs and also contains a compatibility mapper for legacy IE1 items.

This fork is being updated to reference InfinityExpansion2 IDs directly instead of depending on temporary IE1 aliases. Important renamed mappings include:

- `INFINITE_INGOT` -> `IE_INFINITY_INGOT`
- `INFINITE_MACHINE_CIRCUIT` -> `IE_INFINITY_MACHINE_CIRCUIT`
- `INFINITE_MACHINE_CORE` -> `IE_INFINITY_MACHINE_CORE`
- `END_ESSENCE` -> `IE_ENDER_ESSENCE`
- `INFINITY_FORGE` -> `IE_INFINITY_WORKBENCH`
- `INFINITY_CONSTRUCTOR` -> `IE_SINGULARITY_CONSTRUCTOR_2`
- `INFINITY_VIRTUAL_FARM` -> `IE_VIRTUAL_FARM_4`
- `INFINITY_TREE_GROWER` -> `IE_TREE_GROWER_4`

Most unchanged IE1 IDs map to the corresponding IE2 ID with the `IE_` prefix, but every reference is being checked before replacement.

## Porting rules

The Legacy port follows these rules:

- Preserve existing `MAGIC_*` Slimefun IDs wherever possible so existing Magic items and machines do not unnecessarily break.
- Do not rebalance recipes, energy values, or production rates as part of compatibility work unless a value is invalid on the modern runtime.
- Replace obsolete dependency IDs with verified modern equivalents.
- Translate player-facing Chinese text to natural English while preserving gameplay meaning.
- Keep original creator credit intact.
- Avoid relying on compatibility aliases when a stable native ID exists.

## Disabling individual items

If a specific Magic item causes a problem, disable or ban its Slimefun ID rather than deleting unrelated sections of the pack. Direct edits to the large configuration files can make future updates harder to merge.

## Credits

Original Magic project and content: **magicsolo / Yomicer**  
Slimefun Legacy compatibility fork: **wickidcow**

Upstream project: `Yomicer/Magic_RSC`

## Disclaimer

This fork is provided as a compatibility and maintenance port of the original free Magic configuration pack. Original authorship and upstream credit are preserved. Server owners are responsible for complying with the licenses and terms of the plugins and addons used alongside it.
