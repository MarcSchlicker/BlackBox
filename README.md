# BlackBox

BlackBox is an experimental NeoForge mod for moving automated farms out of permanently loaded server areas. A farm is built and observed inside an isolated farm cell. Its measured net resource balance is then replayed by a compact Blackbox while the original farm remains unloaded.

> **Status:** Work in progress and playable prototype. The complete build-measure-simulate loop is implemented, but large modpacks and multiplayer servers still need broader compatibility and performance testing.

## Compatibility

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Mod loader | NeoForge 21.1.190 or newer |
| Java | 21 |
| Sides | Client and dedicated server |

## How It Works

1. Place a Dimensional Workbench and insert a Dimension Core.
2. Use the free 1x1 chunk cell or apply an affordable 2x2/3x3 Cell Upgrade before the core's first visit.
3. Build the farm in the isolated cell and connect its resources through the Farm Input and Farm Output.
4. Leave the cell when the farm is ready. Building time is never measured.
5. After a hidden warmup period, the configured measurement window records the cell's net item, fluid, energy and persistent-mob balance.
6. The programmed core replays that profile inside the workbench or a separate Blackbox without loading the original farm.

Moving a pre-filled chest through hoppers does not create free resources: BlackBox compares the complete cell at the start and end and subtracts imported resources while adding exported resources.

## Features

### Isolated Farm Cells

- Every assigned Dimension Core receives a persistent UUID and a separate farm cell.
- Every core starts with a 1x1 chunk cell and needs no size item.
- The 2x2 upgrade costs only four copper ingots and one redstone; the 3x3 upgrade is crafted from two 2x2 upgrades.
- A cell has one Dimensional Bedrock layer at Y=0. Unselected surrounding chunks have no floor.
- Placement and breaking are restricted to the core's exact cell.
- Farm Input and Farm Output ports are created automatically. Additional Output ports are supported and collected together.
- Right-clicking Dimensional Bedrock opens the safe return interface in every farm environment.
- Registered cells can be inspected and permanently removed through the operator-only Admin Book.

### Farm Environments

| Environment | Core color | Dimension behavior |
| --- | --- | --- |
| Standard | Yellow | Neutral BlackBox environment |
| Overworld | Green | Overworld dimension properties and plains biome |
| Nether | Red | Nether properties and nether-wastes biome |
| End | Purple | End properties and End biome |

All environments stay flat and intentionally omit normal terrain, ores and structures. Natural mob spawning is disabled per core unless the Mob Spawn Upgrade is applied.

### Persistent Measurement

- Measurement begins only after the builder leaves the cell.
- Warmup activity is ignored; only the following configured window becomes the farm profile.
- Vanilla containers and NeoForge item handlers are included in the inventory balance.
- NeoForge fluid and energy capabilities are included in the resource balance.
- Persistent consumed mobs are recorded as inputs. Natural mobs and spawner mobs with a remaining spawner are excluded.
- The Farm Input imports items, fluids and FE from the workbench; every Farm Output exports all three resource types back to it.
- Measurement output is compressed into a persistent workbench buffer whenever visible item slots, fluid tanks or the FE store are full, so a farm never stops being recorded because of output capacity.
- Item production timestamps are stored so a normal Blackbox can replay peaks and quiet periods.
- Active measurements and their snapshots survive server restarts.
- Farm and workbench chunks use persistent, fully ticking NeoForge tickets only while a measurement is active.
- Removing the core, destroying the workbench or losing a required dimension aborts safely without generating output.
- Re-entering a measuring farm cancels the measurement and clears its profile so it can be edited and measured again.

### Dimension Cores

- Store the farm ID, name, owner, access mode, cell size, environment, mob-spawn setting and measured profile.
- Support private, scoreboard-team and public access. Operators retain administrative access.
- Change color for Standard, Overworld, Nether and End environments.
- Gain an enchantment glint after a valid profile is stored.
- Show item, fluid and energy rates plus required mob inputs while Shift is held.
- Include a pre-programmed example iron farm core in the creative tab.

### Workbench and Blackbox

- Both machines provide one core slot, nine item input slots, eighteen item output slots, four input fluid tanks, four output fluid tanks and separate FE buffers.
- The Dimensional Workbench builds, measures and simulates farms in one block.
- A Blackbox starts automatically when a programmed core is inserted.
- A cycle only starts when every measured item, fluid, energy and mob input is available.
- Output capacity is checked before production, so resources are never silently deleted.
- Without a Stability Upgrade, measured item peaks and pauses are moderately randomized for every cycle while totals remain exact.
- With a reusable Stability Upgrade, item, fluid and energy output is distributed evenly across the cycle.
- Automation can insert resources from the sides and extract simulated fluid or energy output from the bottom.
- Required mobs can be supplied in occupied boats or minecarts; the passenger is accepted and the vehicle remains outside.

### Upgrades

Core upgrades and machine upgrades have separate visual forms and tooltip categories.
Recipes are intentionally affordable because upgrades reduce server load rather than increase farm output. The Nether Environment Upgrade therefore uses a compass, netherrack and Nether Wart instead of a rarer combat resource.

| Upgrade | Target | Effect |
| --- | --- | --- |
| 2x2 Cell | New core | Expands the default cell to 2x2 chunks |
| 3x3 Cell | New core | Crafted from two 2x2 upgrades and expands the cell to 3x3 chunks |
| Standard Environment | New core | Selects the neutral environment |
| Overworld Environment | New core | Selects Overworld properties |
| Nether Environment | New core | Selects Nether properties |
| End Environment | New core | Selects End properties |
| Mob Spawn | New core | Enables natural spawning for that cell |
| Stability | Workbench or Blackbox | Smooths simulated resource output |

Core setup upgrades are consumed in the Dimensional Workbench and must be applied before the core creates its first cell. The Stability Upgrade remains in the machine and is reusable.

### Blueprint Library

- The blue vanilla-paper-style Blueprint Tool is reusable and contains no farm data itself.
- Right-click in the air to open the searchable library.
- Shift-right-click Dimensional Bedrock to capture a new named template.
- Local storage is the default and works across worlds and servers on the same client.
- Server storage provides shared templates for that world.
- Templates carry revisions and can be renamed or deleted from the library.
- Players can download server templates; operators can publish local templates.
- Survival placement consumes required blocks and leaves occupied positions untouched.

Blueprints currently store block states, not block-entity NBT, inventories, entities or fluids.

### Handbook and Administration

- The survival Handbook uses direct section tabs, explains the complete workflow and renders every crafting recipe.
- The Admin Book uses ghost slots to maintain the server's denied-block list.
- Operators can set the duration used by future measurements directly in the Admin Book.
- Ender Chests, Blackboxes and Dimensional Workbenches are denied in farm cells by default.
- A second Admin Book view lists registered cells and deletes abandoned cells with confirmation.
- Farm ownership can be private, shared with the owner's scoreboard team, or public when allowed by server config.

## Configuration

BlackBox uses a per-world NeoForge server configuration. Current options are:

| Setting | Default | Purpose |
| --- | ---: | --- |
| `measurementSeconds` | 60 | Duration of the recorded production window |
| `warmupSeconds` | 30 | Hidden preparation time before measurement |
| `maxBlueprintBlocks` | 20,000 | Maximum blocks in one template |
| `maxServerBlueprints` | 256 | Maximum shared templates listed by the server |
| `allowPublicFarms` | `true` | Allows owners to select public access |
| `deniedFarmBlocks` | See Admin Book | Blocks that cannot be placed in farm cells |

Useful future configuration candidates include resource-buffer sizes, measurement chunk-ticket limits, maximum cells per owner, public-farm policy by permission level, and a server-wide cap on simultaneous measurements.

## JEI and EMI

All craftable BlackBox content uses standard Minecraft JSON recipes, so JEI and EMI index those recipes automatically when installed. Dynamic farm profiles are unique NBT data rather than global recipes; their measured per-minute item, fluid and energy values are shown in the Dimension Core's Shift tooltip instead of being added to the global recipe list.

## Quick Start

1. Craft a Dimensional Workbench, Dimension Core, both farm ports and the Handbook.
2. Insert the core, enter a farm name and choose its access mode.
3. Optionally apply a 2x2/3x3 cell, environment or mob-spawn upgrade before the first visit.
4. Enter the cell and build the farm.
5. Feed external resources through the blue Farm Input and route products into the green Farm Output.
6. Leave through Dimensional Bedrock and wait for measurement to finish.
7. Keep the programmed core in the workbench or move it into a Blackbox.

## Current Limitations

- Compatibility depends on machines exposing inventories, fluids and energy through Vanilla containers or NeoForge capabilities.
- Experience, gases, source-specific chemical systems and other mod-specific resource APIs are not measured yet.
- Direction-specific machine wrappers may not expose every internal tank through their unsided capability.
- Blueprint templates do not preserve block-entity data, inventories, entities, fluids or live redstone state.
- Fluid and FE profiles store totals; only item outputs currently retain a measured peak timeline.
- Mob accounting stores entity type and count, not professions, trades, equipment, age or other individual entity data.
- Automated compatibility, migration and load testing for large modpacks is still incomplete.
- Interfaces, textures and balance remain work in progress.

## Building From Source

There is no stable release build yet.

1. Install Java 21.
2. Clone the repository and open its root folder in VS Code.
3. Install the recommended Java and Gradle extensions.
4. Run the VS Code task `Minecraft-Client starten (NeoForge)` to start the development client.

Windows build:

```powershell
.\gradlew.bat --no-problems-report build
```

Linux or macOS build:

```bash
./gradlew --no-problems-report build
```

The generated development JAR is written to `build/libs/BlackBox-1.0.jar`.

### Lunar Client Testing

BlackBox uses standard NeoForge 1.21.1 APIs and has no additional client-side dependencies. For Lunar Client, create or select a Minecraft 1.21.1 profile using NeoForge 21.1.190 or newer, then install `BlackBox-1.0.jar` through that profile's Mods page. Lunar Features may stay enabled; disable them in the profile only when isolating a compatibility problem.

A separately packaged and validated server release will be prepared during the final release phase.

## Development

The workspace was originally created with MCreator. Core farm, measurement, simulation, networking, capability and Blueprint logic is now maintained as regular Java code. Relevant MCreator elements are locked where needed to prevent accidental regeneration.

Testing with modded machines, multiplayer ownership, server restarts, unusual farms and high-throughput resource networks is especially valuable. Issues and pull requests are welcome.

The separate [ModJam 2026 concept](docs/MODJAM_2026_CONCEPT.md) describes how the existing recording system could be presented as an "Echoes of the Past" entry. Those presentation ideas are not implemented gameplay features.

## License

No project license has been selected yet. Add an explicit open-source license before redistribution or release.
