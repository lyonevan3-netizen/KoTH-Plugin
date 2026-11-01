# KoTH for Robblestone Network — 1.5.1 source

Paper/Purpur 1.21.4, Java 21. Standalone backend plugin; Geyser/Floodgate players use the normal Bukkit player path. PlaceholderAPI is optional but required for placeholders in TAB/FancyHolograms.

## Build
Run `mvn --batch-mode clean package` with Java 21. GitHub Actions workflow is in `.github/workflows/main.yml`; the artifact is created from `target/*.jar`.

## Arena commands
- `/koth wand` — get the golden axe; left click corner 1 and right click corner 2.
- `/koth define <name>` — create an arena or redefine its cuboid.
- `/koth editor <name>` — open the editor for that named arena.
- `/koth list` — list all arenas.
- `/koth delete <name>` — delete an arena.
- `/koth start <name>` — start a timed event.
- `/koth start <name> hourly` — start hourly rounds; unfinished rounds are replaced at the next hour, and a won round stays inactive until that next start.
- `/koth stop <name>` — stop the event and clear hourly mode.
- `/koth schedule <name> <DAY> <HH:mm>` / `/koth unschedule <name>` — weekly schedule.
- `/koth reward <name> add <console command>` — add reward command. Use `%player%` and `%arena%` tokens.
- `/koth reward <name> list` — list rewards.
- `/koth reward <name> remove <number>` — remove a reward by its 1-based list number.
- `/koth reward <name> clear` — clear rewards.

The editor allows capture-duration adjustment, enabled/reset toggles, manual start/stop, and chat prompts for reward and weekly schedule changes. Area changes remain intentionally command/wand based.

## PlaceholderAPI placeholders
Global:
- `%koth_active%`, `%koth_arena%`
- `%koth_next_event%`, `%koth_event_time%`, `%koth_next_start%`, `%koth_time_until_next%` — countdown to the next hourly or weekly scheduled start, e.g. `22m, 22s`.
- `%koth_next_event_seconds%`, `%koth_next_event_minutes%` — numeric countdown values (`-1` means no upcoming event).
- `%koth_next_arena%`, `%koth_next_event_arena%`
- `%koth_capturer%`, `%koth_time%`, `%koth_capture_time%`, `%koth_status%`, `%koth_players%`
- `%koth_wins%`, `%koth_player_wins%` — viewer's wins; `%koth_wins_<player>%` — named player's wins.
- `%koth_top_<rank>_name%`, `%koth_top_<rank>_wins%` — persistent win leaderboard, e.g. `%koth_top_1_name%` and `%koth_top_1_wins%`.

Per arena (replace `<name>` with the arena name):
- `%koth_arena_<name>_active%`
- `%koth_arena_<name>_capturer%`
- `%koth_arena_<name>_time%`
- `%koth_arena_<name>_status%`
- `%koth_arena_<name>_players%`
- `%koth_arena_<name>_next_event%` / `%koth_arena_<name>_next_start%`

No particle effects are included. Stats are stored in `plugins/KoTH/stats.yml`; arena configuration is stored in `arenas.yml`.

## Important validation note
This is an updated source package. It has not yet been compiled or tested on a live Paper/Purpur server in this environment. Treat the GitHub Actions build and an in-game test as required before production use.
