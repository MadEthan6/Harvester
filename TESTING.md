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
| Either | Hold H near each supported mature crop | One crop is harvested at a time and replanted after break confirmation |
| Either | Release H after the crop breaks | No new crop starts; the pending replant still completes |
| Either | Press K during an operation | All toggles clear and the pending operation is cancelled |
| Either | Change dimension or die | All automation resets and H must be released before harvesting resumes |
| Either | Open a screen | Automation stops and pending work is cancelled |
| Remote server | Introduce latency or rejection | The operation times out or retries without inventory selection loss |
| Either | Remove seeds before replant | Replant retries twice, warns the player, and does not start another crop |

Fast Break and Fast Place must cap their delays at the active profile values.
The HUD should only appear while a feature or replant operation is active.
