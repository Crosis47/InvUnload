# Development

For installation and player commands, see the [README](README.md).

## Build

InvUnload compiles against Paper `26.3` while retaining Paper `1.21.11` as its minimum supported API. One JAR is intended for both server generations; the plugin keeps Java `21` bytecode.

Requirements:

- JDK `25` or newer to build against the Paper `26.3` API
- Maven `3.9+`

Paper `1.21.11` runs on Java `21`; Paper `26.1+`, including `26.3`, requires Java `25` or newer. The server's Java requirement still applies even though the plugin emits Java `21` bytecode.

Optional integrations such as ChestSort, CoreProtect, ItemsAdder, Minepacks, PlotSquared, Spartan, and InventoryPages are resolved at runtime, so no manual `lib/*.jar` setup is required anymore.

Build from the repository root with `mvn verify`. Dependencies use the repo-local `.mvn/repository` cache.

The API dependency is pinned to `26.3.build.222-stable`. To check source compatibility with the minimum API, run `mvn -Dpaper.api.version=1.21.11-R0.1-SNAPSHOT clean verify`. Rebuild normally afterward to produce the release artifact against `26.3`.

Release `5.3.0` passed the same-JAR server self-tests on Paper `1.21.11` using Java `21`, and `26.1.2`/`26.3` using Java `25`, both with and without ProtocolLib. See [COMPATABILITY.md](COMPATABILITY.md) for the support boundary, exact results, and remaining client checks.

## Debug Self-Test

For local verification on a running Paper server:

1. Set `debug: true` in `plugins/InvUnload/config.yml`
2. Grant yourself `invunload.selftest`
3. Run `/unload selftest`

The harness creates a small isolated test arena in a world named `invunload-selftest`. It restores the player's inventory, ender chest, cursor, held slot, location, blacklist, hotbar settings, and locked slots when finished. It checks:

- `/searchitem` finds a matching chest
- `/searchitem` uses the configured default search radius
- `/searchitem` reports matching chest slot numbers
- clickable `/searchitem` take links keep item tooltip data and move the reported stack to the player
- take links enforce distance, world, permission, configuration, and chest access checks
- stale or used take links cannot take a replacement stack or be replayed
- full inventories and partial transfers leave the correct stack in the chest
- `/searchitem` finds items and enchanted books with a specific enchantment
- `/searchitem` combines material and enchantment filters
- `/unload` only moves matching items
- `/dump` unloads matches and dumps leftovers
- blacklist entries prevent dumping
- locked hotbar and inventory slots are skipped by both `/unload` and `/dump`
- locked slots survive save/reload, and preview metadata does not change real items

The `Paper compatibility` workflow builds the plugin once against `26.3`, then runs that same JAR on Paper `1.21.11`, `26.1.2`, and `26.3`, each with and without ProtocolLib `5.4.0`. A test-only probe supplies a player adapter while Paper provides the real worlds, containers, item metadata, enchantment registry, and protection events. It also checks that self-test player state is restored. Connected-client checks run separately.

After `mvn verify`, run a server check locally in PowerShell 7:

```powershell
./scripts/verify-paper.ps1 -PaperVersion '26.3'
```

The script downloads a stable server, verifies its SHA256, and uses an isolated directory under `target/compatibility`. To use a cached server without downloading it:

```powershell
./scripts/verify-paper.ps1 -PaperVersion '1.21.11' -ServerJar './test-server/paper-1.21.11-130.jar' -CacheDirectory './test-server'
```

Pass `-AdditionalPluginJar '/path/to/ProtocolLib.jar'` to include ProtocolLib in a server check.

For connected protocol-client menu checks, install Node.js dependencies under `target` and select a cached Paper server and suitable JDK:

```powershell
npm install --prefix target/client-dependencies minecraft-protocol@1.68.0
$env:NODE_PATH = (Resolve-Path 'target/client-dependencies/node_modules').Path
$env:JAVA_HOME = '/path/to/jdk-21'
$env:PAPER_SERVER_JAR = (Resolve-Path 'test-server/paper-1.21.11-130.jar').Path
$env:PAPER_CACHE_DIRECTORY = (Resolve-Path 'test-server').Path
$env:PROTOCOLLIB_JAR = '/path/to/ProtocolLib.jar'
node scripts/verify-client.cjs '1.21.11'
```

Omit `PROTOCOLLIB_JAR` to test the fallback menu. For Paper `26.1.2`, use Java `25`, its cached server, and client version `26.1`. For Paper `26.3`, use Java `25`, its cached server, and client version `26.3`. The script starts a fresh loopback-only server, connects a test player, verifies menu packets and persisted locks, then stops the server. It retains logs under `target/client-checks/`. These checks also run in the compatibility workflow; they do not inspect graphical rendering.

The `26.3` test client uses the included inventory protocol adapter, which speaks native protocol `777` directly to Paper. It handles the login, configuration, and inventory packets needed by these tests; unrelated game packets are treated as opaque data. Its packet/component IDs come from the server's `net.minecraft.data.Main --reports` output. To regenerate the ID data, generate reports from Paper `26.1.2-74` and `26.3-227`, then run:

```powershell
node scripts/generate-inventory-protocol.cjs /path/to/26.1.2/reports /path/to/26.3/reports
```
