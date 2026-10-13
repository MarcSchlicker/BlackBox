# BlackBox

**Record a farm once. Replay its measured production anywhere.**

BlackBox moves automated farms out of permanently loaded areas. Build a farm in its own bounded farm cell, record its net change over time, and store the result in a Dimension Core. Put that core into a Blackbox and it reproduces the measured profile while the original farm cell stays unloaded.

## What BlackBox Records

- Item, fluid, and FE input and output balances
- Persistent mobs consumed by a farm
- Production timing, including natural peaks and quieter periods
- Farm size, environment, ownership, and access settings

BlackBox compares the entire farm cell at the start and end of a recording. Moving pre-filled storage around cannot create free resources.

## Main Features

- Bounded 1x1, 2x2, and 3x3 chunk farm cells
- Standard, Overworld, Nether, and End environments
- Per-side item, fluid, and FE routing
- Output filters that apply to one Blackbox without changing the core
- Stability upgrades to smooth recorded output
- Blueprint storage for farm layouts
- Optional Spawner Blackboxes for compatible modpacks

## Getting Started

1. Place a Blackbox and insert an empty Dimension Core.
2. Name the farm and apply any upgrades at an Anvil.
3. Enter the farm cell, build the farm, and connect it with Farm Input and Farm Output blocks.
4. Leave through Dimensional Bedrock to start recording.
5. Insert the programmed core into any Blackbox to replay the result.

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.190 or newer
- Java 21

This is an alpha release. Please back up important worlds and test unusual modded machines before using BlackBox on a production server.

## Links

- Source and issue tracker: https://github.com/MarcSchlicker/BlackBox
- License: MIT
