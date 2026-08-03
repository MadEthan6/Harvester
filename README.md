# Fabric Harvester

Fabric Harvester 1.6.0 is a fully client-side Fabric mod for Minecraft 26.2.
It provides Fast Break, Fast Place, and reliable hold-to-harvest/replant tools
through Minecraft's standard remappable controls. It has no accounts, hosted
backend, database, or telemetry.

## Controls

- `B`: toggle Fast Break
- `V`: toggle Fast Place
- `H`: hold to harvest mature nearby crops
- `P`: cycle Safe and Trusted automation profiles
- `K`: immediately stop all automation and cancel pending work

All controls can be changed in Minecraft's Controls menu. Automation always
starts off after joining, disconnecting, changing dimensions, or dying.

## Profiles

Safe is selected on remote servers. It allows one harvest every 8 ticks, keeps
only one crop operation in flight, and enforces minimum Fast Break and Fast
Place delays of 1 and 2 ticks.

Trusted is selected in singleplayer. It allows one harvest every 2 ticks and
removes the Fast Break and Fast Place client cooldowns.

Safe mode reduces packet frequency, but it does not guarantee that automation
is allowed. Always follow the rules of the server you join.

## Reliable harvesting

Harvesting supports wheat, carrots, potatoes, and beetroots. The client waits
up to 10 ticks for a break to be confirmed before planting, restores temporary
inventory selections, confirms the replacement crop, and retries twice with
profile-specific backoff. Releasing `H` prevents new harvests while allowing
an already-started replant to finish.

Operations stop safely after an emergency stop, disconnect, dimension change,
death, opened screen, lost reach, missing farmland, or an unexpected inventory
change. The localized HUD and overlay warnings describe active state and
actionable failures.

## Build

Minecraft 26.2 requires Java 25. Build and verify the release with:

```text
./gradlew clean release
```

On Windows, use `gradlew.bat clean release`. The distributable mapping-free
JAR is written to `build/libs/fabric_harvester-1.6.0.jar`; its SHA-256 file is
written to `build/checksums/fabric_harvester-1.6.0.jar.sha256`.

## License

CC0-1.0. See [LICENSE](LICENSE).

