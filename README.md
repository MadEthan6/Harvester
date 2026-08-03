# Fabric Harvester

Fabric Harvester 1.8.0 is a fully client-side Fabric mod for Minecraft 26.2.
It provides Fast Break, Fast Place, and reliable automatic farming tools
through Minecraft's standard remappable controls. It has no accounts, hosted
backend, database, or telemetry.

## Controls

- `B`: toggle Fast Break
- `V`: toggle Fast Place
- `H`: toggle nearby harvesting, empty-farmland planting, and dirt tilling
- `N`: toggle automatic bone meal separately
- `P`: cycle Safe and Trusted automation profiles
- `K`: immediately stop all automation and cancel pending work

All controls can be changed in Minecraft's Controls menu. Automation always
starts off after joining, disconnecting, changing dimensions, or dying.

## Profiles

Safe is selected on remote servers. It allows one farm action every 4 ticks, keeps
only one crop operation in flight, and enforces minimum Fast Break and Fast
Place delays of 1 tick each.

Trusted is selected in singleplayer. It allows one farm action every tick and
removes the Fast Break and Fast Place client cooldowns.

Safe mode reduces packet frequency, but it does not guarantee that automation
is allowed. Always follow the rules of the server you join.

## Reliable farming

Press `H` once and walk around a farm to keep farming enabled; press it again
to stop. The scan now covers the full normal interaction reach (four blocks
horizontally and two vertically). Holding right-click or left-click while
looking directly at a mature crop gives that crop priority, matching the quick
targeted workflow from the older releases. Farming supports wheat, carrots,
potatoes, and beetroots. It harvests
mature crops, plants an available supported seed on empty farmland, and tills
dirt that has exactly two or three horizontally adjacent farmland blocks. A
hoe is selected temporarily and its durability change is validated before the
original inventory selection is restored.

Automatic bone meal is intentionally independent: press `N` to turn it on or
off without changing the `H` farming setting. It grows nearby immature
supported crops one confirmed interaction at a time.

Crop breaks and item uses are sent without changing crops or inventory locally,
so replanting cannot spend a seed until the server confirms that the crop is
gone. The client waits up to 10 ticks for confirmation. Harvest/replant keeps
its two retries with profile-specific backoff, and planting, tilling, and bone
meal actions all restore temporary inventory selections before continuing.

Operations stop safely after an emergency stop, disconnect, dimension change,
death, opened screen, lost reach, missing farmland, or an unexpected inventory
change. The localized HUD shows farming and bone meal independently, while
overlay warnings describe missing seeds, hoes, bone meal, timeouts, and other
actionable failures.

## Build

Minecraft 26.2 requires Java 25. Build and verify the release with:

```text
./gradlew clean release
```

On Windows, use `gradlew.bat clean release`. The distributable mapping-free
JAR is written to `build/libs/fabric_harvester-1.8.0.jar`; its SHA-256 file is
written to `build/checksums/fabric_harvester-1.8.0.jar.sha256`.

## License

CC0-1.0. See [LICENSE](LICENSE).
