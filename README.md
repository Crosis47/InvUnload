# InvUnload

Automatically puts your stuff into the right chests

You don't have time to open every chest in your storage room to tidy up your inventory?
Fear no more! You have no more excuses for having chests full of random garbage!

<p align="center">
  <img src="https://api.jeff-media.de/invunload/spigotmc/img/invunload128.png"/>
</p>

InvUnload does two brilliant things:

When you enter /unload, it checks if there are chests nearby. For each chest, the player's inventory (except hotbar) will be searched for matching items. If there are any, they will be put into the chest.

When you enter /dump, it will put all items from the player's inventory (except hotbar) into nearby chests. If possible, they will be put into chests already containing matching items.

Players can also open an ignored-slot editor with `/unload slots` or `/dump slots` to mark specific inventory slots that should always be skipped by both commands.

`/searchitem` can search by material, enchantment, or both. For example, `/searchitem MENDING` finds enchanted items and enchanted books with Mending, while `/searchitem DIAMOND_PICKAXE MENDING 10` narrows the search to nearby Mending diamond pickaxes. Search uses configurable `default-search-radius` and `max-search-radius` settings, both with group overrides. Search summaries include the matching chest slot numbers, and players with `invunload.search.take` can hover a result slot to preview the item tooltip or click it to move that stack into their inventory while they are within the configured `search-take-radius`.

## Build

InvUnload now targets Paper `1.21.11` and Java `21`.

Requirements:

- JDK `21` or newer
- Maven `3.9+`

Optional integrations such as ChestSort, CoreProtect, ItemsAdder, Minepacks, PlotSquared, Spartan, and InventoryPages are resolved at runtime, so no manual `lib/*.jar` setup is required anymore.

Build with `mvn -DskipTests package`

## Debug Self-Test

For local verification on a running Paper server:

1. Set `debug: true` in `plugins/InvUnload/config.yml`
2. Grant yourself `invunload.selftest`
3. Run `/unload selftest`

The harness creates a small isolated test arena in a world named `invunload-selftest` and checks:

- `/searchitem` finds a matching chest
- `/searchitem` uses the configured default search radius
- `/searchitem` reports matching chest slot numbers
- clickable `/searchitem` take links keep item tooltip data and move the reported stack to the player
- `/searchitem` finds items and enchanted books with a specific enchantment
- `/unload` only moves matching items
- `/dump` unloads matches and dumps leftovers
- blacklist entries prevent dumping
