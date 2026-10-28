# Compatibility

| Setting | Value |
| --- | --- |
| Compile API | Paper `26.3.build.222-stable` |
| Minimum supported server API | Paper `1.21.11` |
| Plugin bytecode | Java `21` (class version `65`) |
| Build JDK | Java `25` or newer |
| Paper `1.21.11` runtime | Java `21` or newer |
| Paper `26.1+` runtime | Java `25` or newer |

`paper.minimum-api.version` in `pom.xml` supplies the filtered `plugin.yml` `api-version`. Paper rejects loading on servers older than that version. This is the supported lower boundary; no support claim is made for earlier APIs. It is deliberately independent of the compile API so newer servers and the minimum version can share one JAR.

The compatibility workflow first compiles against the minimum API, then rebuilds against `26.3` and runs the same artifact on `1.21.11` (Java 21), `26.1.2` (Java 25), and `26.3` (Java 25). The test-only server probe runs the feature self-test without a connected Minecraft client.

Optional integrations are detected at runtime so the plugin can start when companion plugins are absent. The server matrix runs both without companions and with ProtocolLib `5.4.0`. The ignored-slot menu has a fallback when ProtocolLib is absent. Other companion integrations are outside this test configuration.

## Verification in the 2026-10-28 update

- Clean Maven builds passed against both the minimum `1.21.11-R0.1-SNAPSHOT` API and the pinned `26.3.build.222-stable` API, including compilation of the server probe.
- Release `5.3.0` was built on Temurin `25.0.5+8`. The same final `26.3`-compiled JAR passed all 23 feature self-test checks and the player-state restoration check in all six combinations below, without plugin task exceptions. These are local results; the GitHub-hosted workflow has not been run for the uncommitted changes.

| Paper server | Java runtime | Without companions | With ProtocolLib 5.4.0 |
| --- | --- | --- | --- |
| `1.21.11-130-c5a2736` | `21.0.12.1+1` | PASS | PASS |
| `26.1.2-74-e4e17fc` | `25.0.5+8` | PASS | PASS |
| `26.3-227-7f9c797` | `25.0.5+8` | PASS | PASS |

The verified JAR SHA256 is `3d377a362dbd7e1c3361521f6068ed45b76cece2b762faa992a095d19f6d3b78`. Its descriptor declares version `5.3.0` and `api-version: '1.21.11'`, every plugin class uses Java `21` bytecode (class version `65`), and the test probe is excluded from the production JAR. Logs and result markers are retained under `target/compatibility/` until the next Maven clean.

Connected protocol-client checks (`scripts/verify-client.cjs`, minecraft-protocol `1.68.0`) passed on Paper `1.21.11`, `26.1.2`, and native `26.3`, both with ProtocolLib and with the fallback menu. They verify menu contents, slot clicks, persisted locks, received glint metadata, unchanged real items, and closing/reopening. Logs and result markers are under `target/client-checks/`.

The native `26.3` checks use a limited inventory-test adapter (`scripts/inventory-protocol-26.3.cjs`) with protocol ID `777`, packet IDs and component IDs generated from server reports, and updated login/teleport codecs. It connects directly to an unmodified `26.3` server, without a protocol translation plugin. Unrelated world/entity packets are consumed as opaque payloads. This is support for the inventory-test client, not a complete port of minecraft-protocol. The adapter data records SHA256 hashes of its source reports.

The connected `26.3` test exposed ProtocolLib `5.4.0` attempting to call the removed CraftBukkit `asCraftMirror` method during full-window updates. InvUnload now preserves the packet's native stacks and converts only replacement previews, avoiding that read converter. The connected overlay checks require that no preview-disable warnings occur.

Paper `1.21.11` is the verified supported minimum; older servers were not tested. These packet-level tests do not inspect graphical Minecraft rendering. ProtocolLib itself warns that the newer server versions are untested upstream; the runtime and connected-client results above describe the tested integration scope.

Paper's [project setup documentation](https://docs.papermc.io/paper/dev/project-setup/), [Java requirements](https://docs.papermc.io/paper/getting-started/), and [plugin descriptor documentation](https://docs.papermc.io/paper/dev/plugin-yml/) describe the dependency scheme, runtime requirements, and API loading boundary.
