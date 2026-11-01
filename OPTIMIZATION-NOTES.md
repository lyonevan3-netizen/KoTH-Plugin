# KoTH 1.1.0 optimization notes

- Removed the unconditional repeating hologram refresh task. Holograms are refreshed when an event starts/stops and while an event is active.
- Hologram text is only sent to ArmorStands when the rendered lines actually change.
- Built-in hologram ArmorStands are non-persistent so stale copies are not saved into world data across restarts; invalid/unloaded stand references are recreated when refreshed.
- The event ticker now skips arenas with no active state instead of allocating state records for every configured arena on every tick.
- Kept all Bukkit world/entity operations on the server thread.

This is a source-level optimization pass, not a performance benchmark. Runtime testing on the target Paper/Purpur version is still required.
