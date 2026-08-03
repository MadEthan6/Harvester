# Changelog

## 1.7.1 - 2026-08-03

- Fixed harvesting changing only the predicted client world without sending a
  block-break request to the server.
- Replanting now waits for an authoritative server block update, preventing
  rejected breaks from triggering predicted seed consumption and retry spam.
- Planting, tilling, and bone-meal requests also avoid local prediction, so
  rejected interactions cannot display phantom item use.

## 1.7.0 - 2026-08-03

- Changed `H` from a hold control to a persistent auto-farming toggle.
- Added automatic planting on empty farmland and automatic tilling for dirt
  with exactly two or three horizontal farmland neighbors.
- Added a separate remappable `N` toggle for automatic bone meal.
- Added confirmation timeouts, rate limiting, inventory restoration, HUD state,
  localized warnings, and tests for the new farming actions.

## 1.6.0 - 2026-08-03

- Upgraded and pinned the mod to Minecraft 26.2, Fabric Loader 0.19.3,
  Fabric API 0.156.0+26.2, Loom 1.17, and Java 25.
- Added the central automation controller and connection/world/death resets.
- Added Safe and Trusted profiles with bounded harvest and interaction rates.
- Added remappable profile-cycle and emergency-stop controls.
- Replaced same-tick harvesting with confirmed break/replant sequencing,
  two retries, profile-specific backoff, and single-operation scheduling.
- Added crop definitions for wheat, carrots, potatoes, and beetroots.
- Added inventory selection restoration and mid-operation change detection.
- Added localized HUD state and actionable timeout, seed, reach, farmland,
  inventory, rejection, and profile warnings.
- Marked the package client-only and removed the common initializer.
- Added reproducible archives, version-derived checksums, and Windows/Linux CI.

## 1.5

- Previous public release.

## 1.4.0

- Previous public release.
