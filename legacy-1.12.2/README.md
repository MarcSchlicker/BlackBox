# BlackBox Legacy for Forge 1.12.2

This is the separate Forge 1.12.2 edition of BlackBox, built for modpacks such as SkyFactory 4. It requires Forge `14.23.5.2854` or a newer compatible 1.12.2 Forge build and Java 8.

## Current Legacy Build

- Dimension Cores have persistent farm ids and a Creative-only Iron Farm profile.
- The Dimensional Workbench stores one core. Right-click it with an empty hand to enter that core's isolated 3x3 farm cell.
- Each cell has a bedrock floor and a full-height Barrier ring. Right-click Dimensional Bedrock to return to the workbench.
- The BlackBox accepts a programmed core and exposes its output inventory through the standard Forge item-handler capability, so hoppers and SkyFactory item transport can extract its products.
- The included Iron Farm Core produces 360 iron and 90 poppies per hour in two peaks per 80-second cycle.

Sneak-right-click a Workbench or BlackBox with an empty hand to remove its core. Breaking either block drops its core and buffered output.

## Not Ported Yet

The complete 1.21.1 measurement system is intentionally not claimed as finished here. Live inventory balance scanning, Farm Input and Output ports, machine-fluid or energy accounting, upgrades, Blueprints, the Admin Book and Village Archive require a dedicated Forge 1.12.2 implementation. The Legacy project is isolated so those systems can be added without destabilizing the modern NeoForge build.

## Build

Use a Java 8 JDK and run:

```powershell
gradlew.bat build
```

The reobfuscated mod jar is written to `build/libs/`.
