# KoTH 1.2.0 integrations

## Hourly event
Use `/koth start <arena> hourly`. The arena stays active without an event-expiration timer and forcibly resets/restarts at the next server-local hour boundary. A capture in progress is reset at that boundary.

## TAB sidebar and FancyHolograms
Install PlaceholderAPI alongside KoTH. TAB and FancyHolograms can then read these PlaceholderAPI values:

- `%koth_active%` / `%koth_arena%` — first active arena name
- `%koth_status%` — Inactive, Waiting, or Capturing
- `%koth_capturer%` — current capturer or `---`
- `%koth_time%` — remaining capture time or `---`
- `%koth_next_event%` — time until the next hourly boundary for an hourly arena
- `%koth_players%` — online players currently inside the first active arena
- `%koth_arena_<name>_active%` — per-arena active state
- `%koth_arena_<name>_capturer%` — per-arena capturer
- `%koth_arena_<name>_time%` — per-arena capture time

FancyHolograms owns the hologram entities. KoTH no longer spawns ArmorStand holograms. Use these placeholders in FancyHolograms text.

## Bedrock players
No Bedrock-specific dependency is required. Geyser/Floodgate players are handled through the normal Paper player API.

## Compatibility and testing
Build target: Paper API 1.21.4, Java 21. Purpur is a Paper-derived server and is intended to work. This source has not yet been compiled in this environment (Maven is unavailable) or runtime-tested; use the included GitHub Actions workflow to compile it, then test on a staging server before production.
