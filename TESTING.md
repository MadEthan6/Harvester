# Testing Fabric Harvester

Run the automated release verification with Java 25:

```text
./gradlew clean release
```

Before publishing a build, verify this in-game matrix:

| Environment | Scenario | Expected result |
|---|---|---|
| Singleplayer | Join a world | Trusted profile selected; every automation feature starts off |
| Remote server | Join a server | Safe profile selected; every automation feature starts off |
| Either | Press H once, then walk near each supported mature crop | Farming stays on; one crop is harvested at a time and replanted after confirmation |
| Remote server | Reject or delay a crop break | The crop stays visible until the server responds and no replant seed is used |
| Either | Press H again after a crop breaks | No new farm action starts; the pending replant still completes |
| Either | Walk near empty farmland with supported seeds | One available seed/crop is planted and the selected slot is restored |
| Either | Walk near dirt with exactly 2 or 3 horizontal farmland neighbors and a hoe | Dirt is tilled, hoe durability is accepted, and the selected slot is restored |
| Either | Walk near dirt with 1 or 4 horizontal farmland neighbors | Dirt is left unchanged |
| Either | Press N near immature crops with bone meal | Crops are grown one confirmed use at a time without enabling H farming |
| Either | Press N again | New bone-meal uses stop independently of H farming |
| Either | Press K during an operation | All toggles clear and the pending operation is cancelled |
| Either | Change dimension or die | Every toggle, including H and N, resets off |
| Either | Open a screen | Automation stops and pending work is cancelled |
| Remote server | Introduce latency or rejection | The operation times out or retries without inventory selection loss |
| Either | Remove seeds before replant | Replant retries twice, warns the player, and does not start another crop |
| Either | Remove seeds, hoe, or bone meal before its action | The matching warning appears and inventory selection is not changed |

Fast Break and Fast Place must cap their delays at the active profile values.
The HUD should show Farm and Meal independently and only appear while a feature
or pending operation is active.
