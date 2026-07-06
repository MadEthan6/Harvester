# Project: Speedmine Fabric Mod

## Architecture
- Client-side Speedmine state management (`SpeedmineState`)
- Keybinding registration & tick listening (`ExampleModClient`)
- Packet-Abuse and Mining Speed Optimization Mixin (`MultiPlayerGameModeMixin` / `MultiPlayerGameModeAccessor`)
- HUD Status Render overlay (`HudMixin` targeting `Hud#extractRenderState`)
- In-World block outline renderer overlay (`LevelRendererMixin` targeting `LevelRenderer#submitBlockOutline`)

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Speedmine Toggle & Keybinding & HUD | Implement SpeedmineState, register keybinding in ExampleModClient, and HUD text display in HudMixin | none | DONE |
| 2 | M2: Packet-Abuse and Mining Speed Mixin | Implement MultiPlayerGameModeMixin overriding break delay to 0 and scaling getDestroyProgress by 1.4F | M1 | DONE |
| 3 | M3: In-World Block Overlay Renderer | Implement MultiPlayerGameModeAccessor and LevelRendererMixin to render green outline overlay on target block | M1, M2 | DONE |
| 4 | M4: Compilation, Packaging & Verification | Compile the project, build release JAR, and verify against requirements | M1, M2, M3 | DONE |

## Interface Contracts
- `SpeedmineState.enabled`: boolean flag for global Speedmine state.
- `MultiPlayerGameModeAccessor`: Accessor to expose `destroyBlockPos` block position.
- Keybinding category: Gameplay, Keybind: V, Toggle text rendered via HudMixin.
