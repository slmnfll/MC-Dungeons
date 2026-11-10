# Dungeon Structure Pipeline

## Decision

Dungeon geometry will use a **hybrid Structure Block and code pipeline**.

- Builders create and decorate reusable room, corridor, entrance, boss, and terrain modules.
- Native Minecraft structure templates store the blocks and optional entities.
- Code plans the dungeon graph, selects compatible modules, rotates or mirrors them, joins their connectors, and owns all gameplay state.
- The current graybox builder remains the fast fallback for mechanic tests.

This keeps room construction builder-friendly without giving up seeded procedural layouts.

## Template Contract

Every room template has a small metadata entry beside its `.nbt` structure:

| Field | Purpose |
|---|---|
| `id` | Stable template name |
| `theme` | Forest, ice ruin, drowned archive, and so on |
| `room-type` | Start, combat, puzzle, NPC, treasure, secret, or boss |
| `footprint` | Width, height, and depth |
| `origin` | Placement anchor relative to the saved structure |
| `connectors` | Local door positions and directions |
| `markers` | Candidate pots, chests, encounters, lecterns, archaeology, NPCs, and exits |
| `weight` | Relative selection chance |

Templates should use fixed connector heights and widths within a theme. A room may expose four sockets; code opens connected sockets and caps unused ones. The generator must validate every selected room and connection before players enter.

## Runtime Assembly

1. Plan and validate the logical `DungeonLayout`.
2. Choose a template for each node using theme, room type, required connectors, and seed.
3. Rotate or mirror the template so its connectors match the graph.
4. Place it through Paper's native `StructureManager` and `Structure` API.
5. Join exposed connectors with corridor templates and cap unused sockets.
6. Resolve marker positions into plugin-owned pots, loot sources, NPCs, gates, encounters, and objectives.
7. Remove or replace all visible development markers.
8. Validate reachability, then teleport the party.

Survey Site Zero maps its six fixed room IDs to six fixed templates. Adventure dungeons choose from weighted pools. The Community Dungeon uses the same modules but persists each generated floor in its shared world.

## Map Boundary

Expedition maps do not sample the world's topmost blocks. A custom renderer draws the logical room graph and converts the player's real X/Z position into a live map cursor. Consequently:

- roofs never hide the layout;
- rooms may sit at different heights;
- open-air themes do not need a duplicate map model above or beside them;
- the visual dungeon can change without rewriting map logic.

Exploration fog and hidden-room revelation can be added later using per-player discovered-room state.

## Open-Air Themes

A forest dungeon does not need to look like a floating island. Its theme can choose one of two world envelopes:

- a generated overworld terrain base with structure rooms blended into it; or
- a void instance filled by broad terrain modules with deep dirt and stone foundations, dense trees, cliffs, mist, and blocked sightlines around its playable boundary.

The second option is more deterministic. The first supports wide outdoor vistas. Both use the same logical layout and custom expedition map.

## First Builder Tooling Milestone

Add admin-only capture, validate, preview, and register commands for a staging world. The first production conversion should be Survey Site Zero because its fixed six-room route makes connector and marker mistakes easy to detect before the random generator uses the templates.
