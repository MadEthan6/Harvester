# Harvester (Fabric-Only Minecraft Mod)

**Harvester** is a powerful **client-side Minecraft utility mod** built with the **Fabric mod loader**, optimized for **Minecraft 26.2**. Designed specifically to work seamlessly on restricted client environments like **Lunar Client**, it serves as an advanced tool for players seeking **anti-cheat bypasses**, **fast mining**, and automated assistance. It overrides inputs at the GLFW layer and uses refmap-free mixins to completely prevent common bootstrap crashes, making it an ideal **Minecraft Fabric mod** for enhanced gameplay.

Keywords: Minecraft, Fabric mod, client-side, anti-cheat, Lunar Client, Speedmine, Farming Assist, fast mining, fast place, utility mod, Minecraft 26.2, GitHub.

> [!IMPORTANT]
> **This is a Fabric-only mod.** It will not work on Forge or NeoForge loaders.

---

## What's New in 1.3.0
- **Settings GUI (Toggle: `RSHIFT`)**: Brand new settings menu designed in clean Modern Dark Glassmorphism. Press `RSHIFT` to open. Directly toggle all features, cycle through Speedmine levels, change keybinds in-game, and persist settings automatically via config file saving.

## What's New in 1.2.0
- **Auto Feed (Toggle: `G`)**: Keeps hunger and health full using inventory food. Intelligently selects the best food, handles golden apples in critical situations, and eats from the offhand automatically to avoid movement slowdown or mining interruption.

## What's New in 1.1.0
- **Auto-Bridge (Toggle: `B`)**: Automatically places solid blocks from your hotbar beneath your feet and one block ahead in your movement direction. Never fall into lava again.

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

### 🌉 Auto-Bridge (Toggle: `B`)
- **Walk-and-Place**: While moving, automatically places a solid block from your hotbar under your feet and one block ahead in your movement direction.
- **Smart Block Selection**: Scans your hotbar for any placeable solid block (cobblestone, netherrack, dirt, etc.).
- **All Dimensions**: Works in the Nether, Overworld, End — anywhere you need a safe path.

### 🍖 Auto Feed (Toggle: `G`)
- **Smart Feeding Strategy**: Automatically eats when hunger deficit is >= food nutrition. When injured, eats immediately to stay at 20 hunger for fast health regeneration.
- **Critical Golden Apple Priority**: If health drops below 12 HP, prioritizes eating Golden Apples or Enchanted Golden Apples if available in the inventory.
- **Seamless Offhand Mechanics**: Swaps the selected food to the offhand, eats it, and swaps back your original offhand item. This lets you mine or fight continuously while feeding.
- **No Movement Slowdown**: Bypasses the vanilla eating slowness, allowing full-speed running, walking, or sprinting while eating.

### ⚙️ Settings GUI (Toggle: `RSHIFT`)
- **Clickable GUI**: Change keybindings and toggle settings in a premium Dark Glassmorphism flat panel interface with smooth hover borders.
- **Custom Key Rebinding**: Rebind any of the 5 main features to any GLFW key in-game by clicking the rebind button and pressing a key.
- **Speedmine Level Cycle**: Click to cycle between multipliers (`1.4x`, `1.8x`, `2.2x`, `2.6x`, `3.0x`, `OFF`).
- **Configuration Persistence**: Saves all your custom key bindings automatically into `config/harvester.properties` when you close the GUI. Settings are automatically loaded when Minecraft boots.

---

## Lunar Client Compatibility
Unlike standard Fabric mods that rely on Fabric API's client-tick events and keybinding helpers (which are blocked or overridden by Lunar Client's bootloader), **Harvester**:
- Intercepts raw keyboard inputs directly inside the `KeyboardHandler` (GLFW keycodes: `V`=86, `J`=74, `K`=75, `B`=66, `G`=71, `RSHIFT`=344).
- Provides a custom graphical settings menu extending `net.minecraft.client.gui.screens.Screen` without third-party libraries (e.g. Cloth Config or ModMenu) which can cause Lunar Client loader conflicts.
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
