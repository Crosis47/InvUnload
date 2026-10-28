# Release verification

- Completed locally: both API builds, Java `21`/`25` server matrix on three Paper versions, with and without ProtocolLib, artifact checks, and connected protocol-client menu/overlay checks on `1.21.11`, `26.1.2`, and native `26.3`. See `COMPATABILITY.md`.
- Remaining: graphical Minecraft rendering inspection. The automated client verifies packet contents and server inventory state.
- Run the GitHub-hosted compatibility workflow after publishing the changes; local results do not establish hosted CI status.

Particle visualization is implemented. bStats was deliberately removed from this branch; extending its metrics is no longer pending work.
