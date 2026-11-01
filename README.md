# KoTH (Paper 1.21.4)

A GUI-first King of the Hill plugin for Paper, built for Java 21.

## Build

```bash
mvn clean package
```

The plugin jar is created in `target/KoTH-1.0.0.jar`. Put it in your server's `plugins` folder and restart.

## Quick setup

1. Run `/koth wand`.
2. Left-click the first corner with the golden axe.
3. Right-click the opposite corner.
4. Run `/koth define koth1`.
5. Run `/koth` and click the arena to configure it.
6. Run `/koth start koth1` to force-start the event.

Running `/koth define koth1` again after selecting a new region updates the existing arena area.

## Commands

- `/koth` — open the management GUI
- `/koth wand` — receive the golden axe region selector
- `/koth define <name>` — create or redefine an arena from the selected corners
- `/koth start <name>` — force-start an event
- `/koth stop <name>` — stop an event
- `/koth list` — list arenas
- `/koth delete <name>` — delete an arena
- `/koth schedule <name> <DAY> <HH:mm>` — set one weekly start time (server local timezone)
- `/koth unschedule <name>` — remove its schedule
- `/koth reward <name> <console command>` — add a winner reward command; `%player%` and `%arena%` are supported
- `/koth hologram <name>` — set hologram location to your current position
- `/koth hologramtext <name> <line1>|<line2>` — change hologram lines; `%arena%`, `%capturer%`, `%time%` are supported
- `/koth reload` — reload config and arena data

## Capture rules

A player entering an active arena starts a full uninterrupted capture countdown (90 seconds by default). Only one player owns the capture attempt at a time. Other players cannot steal it while the current capturer remains in the defined cuboid. Leaving the cuboid resets the attempt to the full duration. A new player can then start a fresh attempt. The first player to finish receives the configured console-command rewards and ends the event.

## GUI

The main `/koth` menu lists arenas. Click an arena to edit capture duration, event duration, cooldown value, enabled state and reset-on-leave behaviour, force-start/stop, and view setup hints. Left/right click the timer entries to decrease/increase their values. Changes are saved to `plugins/KoTH/arenas.yml` immediately.

## Configuration

`config.yml` contains global defaults and messages. `arenas.yml` stores each arena's bounds and per-arena settings. The plugin saves arena settings automatically. Schedules currently support one recurring weekly start time per arena.

## Notes / current version scope

- Requires Paper 1.21.4 and Java 21.
- Holograms use native invisible marker ArmorStands; no hologram dependency is required.
- Reward commands run as console.
- Vault economy payouts, PlaceholderAPI expansion registration, MySQL, team mode, multiple schedules per arena, and fully in-GUI text editing are not included in this initial build.
