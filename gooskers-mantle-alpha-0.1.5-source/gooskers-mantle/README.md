# Goosker's Mantle

A focused Fabric 1.20.1 mantling mod for Goosker's Medieval Modpack. Hold the
jump key while airborne and moving into a reachable ledge. A successful mantle
keeps horizontal momentum and applies the same `0.4` upward velocity used by
Enhanced Movement 1.0.6 after the normal jump has begun, with
server-authoritative validation and cooldown.

## Features

- Mantling only: no dash, double jump, wall run, or other movement systems.
- Detects collision-surface heights, including slabs and compatible modded
  blocks, rather than only checking whole block positions.
- Searches both the normal landing area and the approach-side outer lip, so a
  centered railing or iron bars do not invalidate an otherwise standable ledge.
- Default mantle range is 1.1 to 2.0 blocks above the takeoff surface.
- Default cooldown is 30 ticks (1.5 seconds).
- Checks for a ledge every airborne tick while jump is held. The mantle is
  intentionally delayed for two server ticks so it feels like a second lift
  rather than replacing the normal jump.
- While falling, a nearby reachable ledge can be caught immediately even when
  it is above or below the player's original takeoff surface.
- Suppresses mantle input during creative flight and for five ticks after
  leaving flight, preventing the flight-toggle double tap from mantling.
- A 14-tick mantle animation lifts both arms, briefly settles, and smoothly
  lowers them without snapping.
- Compatible pose hooks for vanilla rendering, First-person Model, and Not
  Enough Animations.
- Multiplayer animation sync and server-authoritative movement validation.
- A bundled, positional mantle sound that plays once per accepted mantle.

## Requirements

- Minecraft 1.20.1
- Java 17 or newer
- Fabric Loader 0.16.10 or newer
- Fabric API
- Installed on both the client and server

Remove Enhanced Movement before installing this mod so two mantle systems do
not activate at once.

## Configuration

The server or single-player world creates:

`config/gooskers-mantle.json`

```json
{
  "config_version": 2,
  "minimum_mantle_height": 1.1,
  "maximum_mantle_height": 2.0,
  "cooldown_ticks": 30,
  "upward_velocity": 0.4,
  "detection_reach": 0.45,
  "animation_ticks": 14
}
```

Restart the server/game after editing. Existing alpha-0.1.0 configs using the
old defaults are migrated automatically. During an ordinary jump, the minimum
and maximum heights are measured from the last standing surface to the top of
the target collision shape. Once the player has genuinely fallen below that
surface, the range is measured from the player's current feet instead. Both the
configured minimum and maximum remain enforced, so slabs and one-block steps do
not become mantles during the downward half of a normal jump. Twenty ticks
equals one second.

## Mantle sound

The supplied `mantle 01.wav` cue is bundled as a mono Vorbis OGG so Minecraft
can position it at the mantling player. To replace it later, overwrite
`src/main/resources/assets/gooskersmantle/sounds/mantle.ogg` with another short,
mono Vorbis OGG and rebuild the mod.

## Build

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

The installable jar is written to `build/libs/`. Do not install the
`-sources.jar` file as the mod.

## License

MIT. See [LICENSE](LICENSE) and [CREDITS.md](CREDITS.md).
