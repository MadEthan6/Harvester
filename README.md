# Harvester (Fabric-Only)

**Harvester** is a client-side Minecraft utility mod built with **Fabric**, optimized for Minecraft 26.2. Designed specifically to work seamlessly on restricted client environments like **Lunar Client**, it overrides inputs at the GLFW layer and uses refmap-free mixins to completely prevent common bootstrap crashes.

> [!IMPORTANT]
> **This is a Fabric-only mod.** It will not work on Forge or NeoForge loaders.

---

## What's New in 1.0.0
- **Farming Assist (Toggle: `K`)**: Left-clicking or right-clicking a mature crop instantly harvests it and replants matching seeds. Synchronized via server action packets to prevent desync/rollback loops.
- **Fast Place (Toggle: `J`)**: Places blocks continuously every tick while holding right-click, bypassing the standard 4-tick cooldown.
- **Dynamic Speedmine (Toggle: `V`)**: Cycles through custom speed multipliers (`OFF` → `1.4x` → `1.8x` → `2.2x` → `2.6x` → `3.0x` → `OFF`) with on-screen action bar notifications.
- **Lunar Client Optimization**: Inputs hooked at GLFW level and refmap-free injection to resolve initialization crashes.

---

## Features

### 🚜 Farming Assist (Toggle: `K`)
- **Smart Harvesting & Replanting**: Left-clicking or right-clicking any fully-grown crop block (Wheat, Carrots, Potatoes, Beetroots) instantly harvests the crop and replants it.
- **Server-Synced Packet Flow**: Automatically sends `ServerboundPlayerActionPacket` (`START_DESTROY_BLOCK`) to ensure crop destruction registers server-side before executing client-side placement. This avoids rollbacks and desync loops.
- **Inventory Swapping**: Instantly scans your offhand, hotbar, and main inventory for seeds, swaps them into your hand to plant, and restores your active slot.

### ⚡ Speedmine (Toggle: `V`)
- **Cyclic Speed Levels**: Pressing `V` cycles through modes: `OFF` → `1.4x` → `1.8x` → `2.2x` → `2.6x` → `3.0x` → `OFF`.
- **Delay Bypass**: Overrides client-side mining delay (`destroyDelay = 0`) to allow instant sequential block breaking.
- **Overlay Warnings**: Sends colorful action bar messages indicating the current speed mode.

### 🏗️ Fast Place (Toggle: `J`)
- Bypasses the vanilla 4-tick right-click placement cooldown.
- While active, holding right-click places blocks continuously on every client tick.

---

## Lunar Client Compatibility
Unlike standard Fabric mods that rely on Fabric API's client-tick events and keybinding helpers (which are blocked or overridden by Lunar Client's bootloader), **Harvester**:
- Intercepts raw keyboard inputs directly inside the `KeyboardHandler` (GLFW keycodes: `V`=86, `J`=74, `K`=75).
- Employs `@Inject` annotations rather than `@Redirect` on cross-class targets, eliminating the need for complex runtime obfuscation refmaps.

---

## Setup & Compilation

### Requirements
- **Java Development Kit (JDK)**: Java 25 (e.g. Zulu-25)

### Building the Mod JAR
Run the following Gradle build command in the project directory:
```powershell
# Windows
$env:JAVA_HOME="C:\Program Files\Zulu\zulu-25"; .\gradlew.bat clean build
```

The compiled mod package will be located at:
`build/libs/speedmine-1.0.0.jar`

---

## Author & License
- **Author**: MadEthan6
- **License**: CC0-1.0 (Public Domain)
