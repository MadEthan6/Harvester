# Fabric Harvester Architecture

Fabric Harvester is a client-only Fabric mod for Minecraft 26.2.

## Runtime components

- `AutomationController`: owns connection/world/death/screen resets, the active
  Safe or Trusted profile, feature ticks, HUD state, and emergency shutdown.
- `HarvesterManager`: performs one acknowledged crop operation at a time and
  safely handles temporary inventory selection.
- `HarvestOperation`: pure break/replant confirmation and retry state machine.
- `HarvestScheduler`: pure profile-aware rate limiter.
- `CropRegistry`: maps wheat, carrots, potatoes, and beetroots to their
  maturity checks and planting items.
- `FastBreakManager` and `FastPlaceManager`: cap vanilla client cooldowns at
  the active profile minimum.
- `ModKeyBindings` and `AutomationHud`: standard remappable controls and
  localized player feedback.

The mod has no server component, external service, account, database, or
telemetry.
