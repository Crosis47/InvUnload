# InvUnload

**Put your inventory away with one command.**

InvUnload is a Minecraft server plugin that moves items from your inventory into nearby storage. Stand in your storage room and run `/unload` to put items into chests that already contain the same item type, or `/dump` to store everything you can. You can also search for items without opening every chest.

<p align="center">
  <img src="https://api.jeff-media.de/invunload/spigotmc/img/invunload128.png" alt="InvUnload logo" width="128" height="128" />
</p>

- **Keep storage organized:** unload matching items, or dump the rest into the nearest storage with room.
- **Keep essentials with you:** your hotbar is skipped by default. Ignore individual slots or blacklist entire item types.
- **Find what you need:** search by item type, enchantment, or both, then click a result to retrieve a stack if your server allows it.
- **See where items went:** chat summaries and optional particle beams help you locate affected containers.

InvUnload works with chests, trapped chests, barrels, and placed shulker boxes.

## Installation

InvUnload runs on the **server**. Players do not need to install it on their clients.

### Server requirements

| Paper version | Java required |
| --- | --- |
| `1.21.11` | Java `21` or newer |
| `26.1+`, including `26.3` | Java `25` or newer |

Paper `1.21.11` is the minimum supported version. The same plugin JAR has been checked on Paper `1.21.11`, `26.1.2`, and `26.3`. See [compatibility details](COMPATABILITY.md) for the tested versions and limitations.

### Install the plugin

1. Open a successful run of the [Paper compatibility workflow](https://github.com/Crosis47/InvUnload/actions/workflows/compatibility.yml), download the **compatibility-build** artifact, and extract `InvUnload.jar`. GitHub requires you to sign in to download workflow artifacts. To build the JAR yourself, see [Development](DEVELOPMENT.md).
2. Stop your server and place `InvUnload.jar` in its `plugins` folder.
3. Start the server. InvUnload creates its settings in `plugins/InvUnload/`.
4. Use your permissions plugin to grant players `invunload.use`. Grant `invunload.search` to players who should also be able to search storage.
5. Join the server, stand near a chest containing some cobblestone, and run `/unload` with more cobblestone in your main inventory.

ProtocolLib and the other integrations listed below are optional.

## Getting started

Put at least one item of each type into the chest where you want that type stored. For example, put cobblestone in one chest and oak logs in another. Run `/unload` nearby, and InvUnload will move matching items from your main inventory into those chests. Items without a matching chest stay with you.

Use `/dump` when you also want to put away items that have no matching chest. It fills matching storage first, then puts the remaining items into the nearest storage with room. Items that do not fit stay in your inventory.

Both commands use a **10-block radius** by default. You can request another radius, such as `/unload 15`, up to your server's limit, which defaults to **20 blocks**.

## Commands

Run these commands in game. Square brackets indicate optional arguments; angle brackets indicate values you supply.

| Command | What it does |
| --- | --- |
| `/unload [radius]` | Moves items into nearby storage that already contains matching items. |
| `/dump [radius]` | Moves matching items first, then puts the rest into the nearest storage with room. |
| `/unload hotbar` | Toggles whether `/unload` includes your hotbar. |
| `/dump hotbar` | Toggles whether `/dump` includes your hotbar, independently of `/unload`. |
| `/unload slots` or `/dump slots` | Opens the ignored-slot editor. Both commands use the same ignored slots. |
| `/unloadinfo [duration]` | Shows the last saved storage summary and highlights affected containers. Duration is in seconds. |
| `/searchitem [item or enchantment] [radius]` | Searches nearby storage. With no query, searches for the item type you are holding. |
| `/searchitem <item> <enchantment> [radius]` | Searches for a particular item type with a particular enchantment. |
| `/blacklist` | Shows the item types you have chosen to keep. |

Shortcuts include `/ul` for `/unload`, `/dumpinfo` for `/unloadinfo`, and `/search` for `/searchitem`.

### Keep items in your inventory

**Ignore a slot:** run `/unload slots` and click the slots you want to keep untouched. With ProtocolLib, click directly in your inventory; without it, click the slot previews in the menu. The menu also has buttons to lock or unlock all hotbar slots, lock or unlock all main inventory slots, or clear your choices. Changes are saved automatically and apply to both `/unload` and `/dump`, even when hotbar unloading is enabled.

**Keep an item type:** hold an item and run `/blacklist add`. Every item of that type will be skipped by both `/unload` and `/dump`, regardless of its slot. For example, blacklisting `DIAMOND_PICKAXE` keeps all your diamond pickaxes, not just the one you are holding.

| Command | What it does |
| --- | --- |
| `/blacklist add` | Blacklists the item type you are holding. |
| `/blacklist add DIAMOND_PICKAXE TORCH` | Blacklists the specified item types. |
| `/blacklist add inventory` | Blacklists all item types in your main inventory, excluding your hotbar. |
| `/blacklist add hotbar` | Blacklists all item types in your hotbar. |
| `/blacklist remove` | Removes the held item type from your blacklist. |
| `/blacklist remove <items...>` | Removes the specified item types. You can also use `inventory` or `hotbar`. |
| `/blacklist reset` | Clears your blacklist. |

### Find and retrieve items

Use item names such as `DIAMOND_PICKAXE` and enchantment names such as `MENDING`. Tab completion helps you find the names.

| Example | What it finds |
| --- | --- |
| `/searchitem` | The item type you are holding. |
| `/searchitem DIAMOND` | Diamonds in nearby storage. |
| `/searchitem MENDING` | Items and enchanted books with Mending. |
| `/searchitem DIAMOND_PICKAXE MENDING 15` | Diamond pickaxes with Mending within 15 blocks. |

Results show matching containers and their slot numbers. When retrieval is enabled and you have permission, hover over a result's slot number to preview the item, or click it to move that stack into your inventory. You must still be able to access the container and be within the allowed retrieval distance, which defaults to **10 blocks**. Search links expire after two minutes; run the search again if a result is no longer available.

## Permissions

Server owners can grant these permissions through their permissions plugin.

| Permission | Allows |
| --- | --- |
| `invunload.use` | `/unload`, `/dump`, `/unloadinfo`, `/blacklist`, hotbar toggles, and the ignored-slot editor. |
| `invunload.search` | `/searchitem`. Also grants `invunload.search.take` as a child permission. |
| `invunload.search.take` | Retrieving items through clickable search results. Deny this permission explicitly if players should only search. |
| `invunload.reload` | Reloading configuration with `/unload reload`; in game, `invunload.use` is also required. |
| `invunload.group.<name>` | Applying the radius settings for a group defined in `groups.yml`. |

## Configuration

Edit `plugins/InvUnload/config.yml`, then run `/unload reload` to apply your changes. From the server console, use `unload reload` without the slash. The generated file includes comments explaining all available options, including messages, sounds, particles, and integrations.

Common settings and their defaults:

| Setting | Default | Purpose |
| --- | --- | --- |
| `default-chest-radius` | `10` | Radius used by `/unload` and `/dump` when none is specified. |
| `max-chest-radius` | `20` | Largest radius players may request for unloading or dumping. |
| `default-search-radius` | `10` | Radius used by `/searchitem` when none is specified. |
| `max-search-radius` | `20` | Largest radius players may request for searches. |
| `search-take-links` | `true` | Enables clickable retrieval from search results. |
| `search-take-radius` | `10` | Maximum retrieval distance in blocks. Use `-1` for any distance within the same world. |
| `cooldown` | `0` | Seconds between unload/dump attempts; `0` disables the cooldown. |
| `match-enchantments` | `false` | Requires matching enchantments and levels when choosing storage for `/unload`. |
| `match-enchantments-on-books` | `false` | Requires matching enchantments for enchanted books even when the option above is off. |
| `ignore-blocked-chests` | `true` | Skips regular chests blocked by a solid, opaque block above them. |
| `always-show-summary` | `true` | Shows a chat summary after unloading or dumping. |
| `laser-animation` | `false` | Shows particle beams automatically after unloading or dumping. |

To give certain players different radii, copy `plugins/InvUnload/groups.example.yml` to `groups.yml` in the same folder, edit the group's values, and grant `invunload.group.<name>`. For example, the `donor` group uses `invunload.group.donor`. Reload the configuration afterward.

## Optional integrations

- **ProtocolLib:** shows ignored-slot previews directly in the player's inventory. The editor also works without it using previews in the menu.
- **ChestSort:** sorts affected containers when the player has automatic sorting enabled, or when `force-chestsort` is enabled.
- **CoreProtect:** logs items deposited by `/unload` and `/dump`.
- **PlotSquared:** checks plot access using the configured trusted-player and outside-plot settings.
- **ItemsAdder:** distinguishes custom items when matching them to storage.
- **Minepacks and InventoryPages:** skips backpack items and inventory navigation buttons when unloading or dumping.
- **Spartan:** includes a compatibility hook for InvUnload's chest access checks.

By default, InvUnload also asks protection plugins whether a player can interact with each container. Keep `use-playerinteractevent: true` to retain these checks. The [compatibility report](COMPATABILITY.md) lists which integrations have been tested.

## Troubleshooting

- **Nothing moves with `/unload`:** make sure the destination already contains that item type, has space, and is within range. Check your blacklist, ignored slots, and hotbar toggle. Use `/dump` if you want to fill storage without matching items.
- **A container is skipped:** check that you can open it, that it is within range, and that a protection plugin is not denying access. Blocked regular chests are skipped by default, and containers in unloaded chunks are not searched.
- **You cannot run a command:** ask the server owner to check the permissions above. Searching requires `invunload.search` separately from `invunload.use`.
- **A search result cannot be retrieved:** move closer, make room in your inventory, and search again. Retrieval also requires `invunload.search.take`, enabled take links, and continued access to the container.
- **The plugin does not load:** check the server console and confirm that your Paper and Java versions meet the requirements above.

For version changes, see the [changelog](CHANGELOG.md). For building, self-tests, and compatibility checks, see [Development](DEVELOPMENT.md).
