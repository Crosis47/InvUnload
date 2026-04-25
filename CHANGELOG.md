# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Added a debug-only `/unload selftest` harness for local Paper verification.
- Added a reusable self-test arena that validates `/searchitem`, `/unload`, `/dump`, and blacklist behavior.
- Added an ignored-slot GUI that lets players choose which inventory slots `/unload` and `/dump` should skip.

### Changed
- Updated the plugin to target Paper `1.21.11` and Java `21`.
- Updated the build to package cleanly on modern Java and Paper toolchains.
- Switched optional integrations such as ChestSort, CoreProtect, ItemsAdder, Minepacks, PlotSquared, Spartan, and InventoryPages to runtime-safe detection so the plugin no longer needs manual companion API jars just to build.
- Replaced the previous external Jeff-Media helper dependencies with local implementations of the required functionality.
- Added persistent per-player locked slot handling to the unload and dump flow.
- Removed bundled `bStats` metrics and the now-unneeded shading step from the build.

### Fixed
- Fixed startup compatibility on Paper `1.21.11`.
- Fixed modern Paper sound and particle validation so current defaults no longer warn on startup.
- Fixed plugin loading when optional integrations are absent, outdated, or unavailable at compile time.

## [5.0.1]

### Changed
- Improved performance when using a huge radius and having `use-playerinteractevent` enabled.

## [5.0.0]

### Added
- Added full `1.19` support.
- Added the `ignore-blocked-chests` config option.

### Changed
- Improved performance enough to support very large unload radii with much less lag.

## [4.17.1]

### Added
- Added full `1.18.2` support.

### Changed
- Improved the update checker.

## [4.17.0]

### Added
- Added a cooldown for `/unload` and `/dump`.

### Fixed
- Fixed `/dump` putting shulker boxes inside shulker boxes.
- Fixed the "all your items are blacklisted" message being shown when there were no remaining chests for non-blacklisted items.

## [4.16.0]

### Changed
- Improved unloading for enchanted books with the `match-enchantments-on-books` option.

## [4.15.5]

### Changed
- Changed unload summaries to show chest names instead of generic container names when available.

## [4.15.4]

### Added
- Added a Polish translation.

## [4.15.1]

### Added
- Added a Dutch translation.

### Changed
- Updated the Spanish translation.

## [4.15.0]

### Changed
- Updated ChestSortAPI to version `11.0.0`.

## [4.14.0]

### Added
- Added support for PlotSquared 6.

## [4.13.5]

### Fixed
- Fixed InvUnload not enabling when ChestSort was not installed.

## [4.13.4]

### Changed
- Improved performance and memory consumption.
- Updated the Turkish translation.

## [4.13.3]

### Fixed
- Fixed ChestSort compatibility.

## [4.13.2]

### Fixed
- Fixed ChestSort compatibility.

## [4.13.1]

### Changed
- Updated to a newer ChestSort API version. If you have ChestSort installed, you must use at least ChestSort version `10.0.0`.

## [4.12.1]

### Fixed
- Removed forgotten debug messages.

## [4.12.0]

### Added
- Added ItemsAdder support.
- Added the `match-enchantments` config option so only items with the exact same enchantments and levels are matched.
- Added the `match-enchantments-on-books` config option so enchanted books can still require exact matches when `match-enchantments` is disabled.

### Fixed
- Fixed the warning about CoreProtect being too old when CoreProtect was not installed.

## [4.11.0]

### Added
- Added `force-chestsort` to sort chests even when the player has ChestSort disabled.

## [4.10.3]

### Fixed
- Fixed `/search` showing `%s` instead of the material name.

## [4.10.1]

### Fixed
- Fixed the blacklist message.

## [4.10.0]

### Added
- Added a new message for the case where all items in the inventory are blacklisted.

## [4.9.5]

### Fixed
- Fixed users being able to add duplicate entries to their blacklist.

## [4.9.2]

### Added
- Added InventoryPages support.

## [4.9.1]

### Added
- Added `/chestsearch` as an alias for `/searchitem`.

### Fixed
- Fixed InvUnload trying to unload into droppers and dispensers.
- Fixed compatibility with some older versions.

## [4.9.0]

### Added
- Added `/blacklist add inventory`.
- Added `/blacklist remove inventory`.
- Added `/blacklist add hotbar`.
- Added `/blacklist remove hotbar`.

## [4.8.2]

### Fixed
- Fixed an exception on startup.

## [4.8.1]

### Changed
- Updated the Chinese (Simplified) translation.

### Fixed
- Fixed the config updating every time the server starts.

## [4.8.0]

### Changed
- Switched the chest access check to a custom event instead of a plain `PlayerInteractEvent` so third-party plugins such as OpenInv can handle it properly.

## [4.7.0]

### Added
- Added a per-player setting for whether hotbar contents should also be unloaded or dumped.
- Added `/unload hotbar` to toggle whether `/unload` includes the hotbar.
- Added `/dump hotbar` to toggle whether `/dump` includes the hotbar.

## [4.6.0]

### Added
- Added a per-player blacklist for items that should not be unloaded.
- Added `/blacklist` to show the blacklist, including clickable links to delete items.
- Added `/blacklist add` to add the item currently held in hand.
- Added `/blacklist add <items...>` to add specified materials.
- Added `/blacklist remove` to remove the item currently held in hand.
- Added `/blacklist remove <items...>` to remove specified materials.
- Added a config option to completely disable CoreProtect logging.

### Changed
- Changed the laser animation so it is always shown after `/unloadinfo` or `/search`.

### Fixed
- Fixed an exception when using ancient versions of CoreProtect.

## [4.5.0]

### Added
- Added configurable laser particles.

### Fixed
- Fixed the sound effect so it only plays once, regardless of how many chests were affected.

## [4.4.2]

### Changed
- Updated ChestSortAPI to the latest version. If you use ChestSort, use version `8.14.0` or higher.
- Updated the Turkish translation.
- Updated the Spigot API target to `1.16.1`.

## [4.4.1]

### Fixed
- Fixed players being able to use a higher radius for `/search` than the configured maximum radius.

## [4.4.0]

### Changed
- Updated the Spanish translation.

### Fixed
- Fixed items being counted twice in double chests when using `/search`.
- Fixed `/search` not working when `always-show-summary` was set to `false`.

## [4.3.0]

### Added
- Added CoreProtect logging.

## [4.2.2]

### Added
- Added `1.16` support.

### Changed
- Improved the PlotSquared hook so protected chests are ignored instead of showing players a message that they cannot use the chest.

## [4.2.1]

### Fixed
- Fixed the update checker again.

## [4.2.0]

### Added
- Added the `/searchitem` command.
- Added `/search` as an alias for `/searchitem`.
- Added Material tab completion for `/searchitem`.

### Changed
- Made `stuffPlayerInventoryIntoAnother()` public for API access.
- Improved the update checker.

### Fixed
- Fixed a config update problem related to UTF-8 handling.

> Note: This update includes a new message in `config.yml`, so please send updated translations.

## [4.1.0]

### Added
- Added PlotSquared support so players can only unload into their own plots, with configurable trust and outside-plot behavior.
- Added `groups.yml` so custom max-radius and default-radius values can be assigned to player groups.
- Added support for Spartan Anti-Cheat.

## [4.0.0]

### Added
- Added a text summary that shows which chests received the unloaded items.
- Added a laser beam that points to affected chests for a few seconds.
- Added support for overriding the default laser duration with `/unloadinfo [duration]` or `/dumpinfo [duration]`.
- Added the `laser-moves-with-player` option so lasers can move with the player.
- Added the `/unload reload` command, protected by the `invunload.reload` permission.

### Fixed
- Fixed `/unload` so it is properly executed before `/dump`.

## [3.0.1]

### Added
- Added an almost complete Spanish translation.

### Fixed
- Fixed `/unload` so it properly ignores the hotbar.
- Fixed Minepacks backpacks being put into chests.

## [3.0.0]

### Added
- Added support for chests, double chests, shulker boxes, and barrels.
- Added compatibility with protection plugins like WorldGuard and GriefPrevention by checking whether the container open event is cancelled.
- Added ChestSort integration for sorting affected chests when the player has automatic chest sorting enabled.
- Added configurable particle types and sound effects in `config.yml`.
- Added a configurable update-check interval in `config.yml`.

### Changed
- Rewrote the plugin from scratch for cleaner and faster code.
- Changed `/dump` so it no longer takes items from the player's hotbar.
- Changed `/dump` so it always runs `/unload` first.
- Renamed the `invunload.unload` and `invunload.dump` permissions to `invunload.use`.

### Removed
- Removed the old config option that controlled whether `/unload` ran before `/dump`.
