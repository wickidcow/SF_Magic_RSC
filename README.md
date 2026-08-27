<div align="center">

# Magic Legacy
### Magic RSC content pack for Slimefun Legacy

A maintained compatibility port of **Magic** for modern Slimefun Legacy servers, packaged as a drop-in RykenSlimefunCustomizer addon folder.

[![Magic Legacy Audit](https://github.com/wickidcow/SF_Magic_RSC/actions/workflows/audit.yml/badge.svg)](https://github.com/wickidcow/SF_Magic_RSC/actions/workflows/audit.yml)
[![Package Drop-In](https://github.com/wickidcow/SF_Magic_RSC/actions/workflows/package-dropin.yml/badge.svg)](https://github.com/wickidcow/SF_Magic_RSC/actions/workflows/package-dropin.yml)
[![Release](https://github.com/wickidcow/SF_Magic_RSC/actions/workflows/release.yml/badge.svg)](https://github.com/wickidcow/SF_Magic_RSC/actions/workflows/release.yml)

[Releases](https://github.com/wickidcow/SF_Magic_RSC/releases) ·
[Actions](https://github.com/wickidcow/SF_Magic_RSC/actions) ·
[Report a Bug](https://github.com/wickidcow/SF_Magic_RSC/issues) ·
[Slimefun Legacy](https://github.com/wickidcow/Slimefun-Legacy) ·
[RykenSlimefunCustomizer Legacy](https://github.com/wickidcow/SF_RykenSlimeCustomizer)

</div>

> [!IMPORTANT]
> **Magic Legacy is an unofficial community compatibility port.**
> The original Magic project and content were created by **magicsolo / Yomicer**. This repository preserves that work while adapting the pack for the Slimefun Legacy ecosystem and current server software.
>
> **NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

---

## ✨ What is Magic Legacy?

Magic is a large RykenSlimefunCustomizer content pack that expands Slimefun with additional materials, machines, generators, recipes, equipment, progression, and integrations with other Slimefun addons.

This Legacy fork focuses on keeping the original pack usable on the modern Slimefun Legacy stack without requiring server owners to manually merge hundreds of configuration entries.

| Focus | What it means |
| --- | --- |
| **Drop-in installation** | The release ZIP contains a complete `Magic` folder ready for `plugins/RykenSlimefunCustomizer/addons/`. |
| **Slimefun Legacy first** | Maintained against the current Slimefun Legacy server stack. |
| **InfinityExpansion2 compatibility** | Historical InfinityExpansion references have been migrated to verified InfinityExpansion2 IDs where valid equivalents exist. |
| **Safer missing dependencies** | Current RykenSlimefunCustomizer Legacy skips unresolved optional recipes instead of silently replacing missing addon items with stone. |
| **Original IDs preserved** | Existing `MAGIC_*` identifiers are retained wherever practical to reduce unnecessary world and item breakage. |
| **Reproducible packages** | GitHub Actions builds and verifies the same runtime-only folder server owners install. |

---

## 📦 Download

There are two supported ways to get the drop-in package:

### GitHub Releases

Download the latest `Magic-Legacy-*.zip` from the repository's [Releases](https://github.com/wickidcow/SF_Magic_RSC/releases) page.

Release archives contain the runtime `Magic/` folder only. Repository maintenance files such as `.github`, Python migration tools, audit output, and documentation are excluded.

### GitHub Actions

Open [Actions](https://github.com/wickidcow/SF_Magic_RSC/actions), select **Package Magic Drop-In**, and run the workflow manually or download the artifact from the latest successful run.

The Actions artifact contains a versioned ZIP plus its SHA-256 checksum.

---

## 🔧 Requirements

Magic Legacy is a **configuration addon**, not a standalone plugin JAR. It requires RykenSlimefunCustomizer to load the pack.

| Requirement | Legacy setup |
| --- | --- |
| **Slimefun core** | [Slimefun Legacy](https://github.com/wickidcow/Slimefun-Legacy) |
| **Customizer loader** | [RykenSlimefunCustomizer Legacy](https://github.com/wickidcow/SF_RykenSlimeCustomizer) |
| **Infinity addon** | [InfinityExpansion2 Legacy fork](https://github.com/wickidcow/SF_InfinityExpansion2) |
| **Other pack dependencies** | GeneticChickengineering, GuizhanLibPlugin, Supreme, FoxyMachines |
| **Primary server target** | Paper 26.2 stack |
| **Java** | Use the Java version required by your Slimefun Legacy / Paper build |

Other optional addon references may exist in individual recipes. With the current RykenSlimefunCustomizer Legacy compatibility handling, unresolved optional ingredients are skipped rather than converted into fake stone recipes.

---

## 🚀 Installation

1. Stop the server normally and make a backup.
2. Install and start the required Slimefun Legacy stack, including RykenSlimefunCustomizer.
3. Download the latest Magic Legacy drop-in ZIP from **Releases** or **Actions**.
4. Extract the archive into:

```text
plugins/RykenSlimefunCustomizer/addons/
```

The resulting path must be:

```text
plugins/RykenSlimefunCustomizer/addons/Magic/info.yml
```

5. Start the server normally and review the console for dependency or recipe warnings.

> [!WARNING]
> Do not use `/reload` to install or update Magic, RykenSlimefunCustomizer, Slimefun Legacy, or its dependencies. Perform a full server restart.

---

## 🔄 Updating an existing installation

For a normal Magic Legacy update:

1. Stop the server.
2. Back up `plugins/RykenSlimefunCustomizer/` and Slimefun data.
3. Replace the old `addons/Magic` folder with the new packaged `Magic` folder.
4. Preserve your generated server-side addon configuration under the RykenSlimefunCustomizer config directories unless a release note specifically says otherwise.
5. Start the server and review startup output before reopening the server to players.

Do not merge old and new Magic runtime YAML files by hand unless you intentionally maintain a customized fork. Replacing the runtime folder as a unit avoids stale definitions being left behind.

---

## ♾️ InfinityExpansion2 migration

The Legacy compatibility pass verifies historical InfinityExpansion item references against both the original InfinityExpansion source and the current InfinityExpansion2 Legacy source.

The migration includes the IE2 mob data-card family, quarry oscillators, machines, materials, generators, storage, and other confirmed equivalents used by Magic. References are migrated only when a valid target can be verified; unrelated items are not used as placeholders.

InfinityExpansion2 also contains compatibility work so dynamically generated mob-card and oscillator items are registered early enough for configuration addons such as Magic to resolve them during startup.

---

## 🧪 GitHub package verification

Every **Package Magic Drop-In** build checks that:

- `Magic/info.yml` exists exactly once
- `Magic/items.yml` exists exactly once
- the archive contains a top-level `Magic/` folder
- `.github/` is excluded
- Python maintenance scripts are excluded
- audit and tool directories are excluded
- repository documentation is not copied into the runtime addon folder

A SHA-256 checksum is generated beside each Actions package.

---

## 🐛 Reporting problems

When reporting a Magic Legacy issue, include:

- exact Slimefun Legacy version
- exact RykenSlimefunCustomizer Legacy version
- exact InfinityExpansion2 version
- Paper/server version and Java version
- the full startup warning or exception
- the Magic item or recipe ID involved
- whether the issue also occurs on a clean test server

Please use the repository [issue tracker](https://github.com/wickidcow/SF_Magic_RSC/issues).

---

## ❤️ Credits and project history

**Original Magic project:** magicsolo / Yomicer  
**Original repository lineage:** `Yomicer/Magic_RSC`  
**Legacy compatibility maintenance:** wickidcow  
**Slimefun Legacy:** maintained separately at `wickidcow/Slimefun-Legacy`

This fork is intended to preserve and maintain the original work for modern Slimefun servers, not to erase its authorship or history.

---

## ⚖️ Usage

Magic and the projects it integrates with are community-created Minecraft server software/content. Review the licenses and usage terms of this repository and each dependency before redistribution or modification.
