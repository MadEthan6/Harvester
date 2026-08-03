package com.fabricharvester.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

public class ModKeyBindings {
    public static final String KEY_CATEGORY = "key.categories.fabric_harvester";

    public static final String KEY_FAST_BREAK = "key.fabric_harvester.fast_break";
    public static final String KEY_FAST_PLACE = "key.fabric_harvester.fast_place";
    public static final String KEY_HARVEST = "key.fabric_harvester.harvest";

    public static KeyBinding fastBreakKey;
    public static KeyBinding fastPlaceKey;
    public static KeyBinding harvestKey;

    private static final AtomicBoolean fastBreakEnabled = new AtomicBoolean(false);
    private static final AtomicBoolean fastPlaceEnabled = new AtomicBoolean(false);
    private static final AtomicBoolean harvestActive = new AtomicBoolean(false);

    public static void register() {
        fastBreakKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_FAST_BREAK,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KEY_CATEGORY
        ));

        fastPlaceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_FAST_PLACE,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KEY_CATEGORY
        ));

        harvestKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_HARVEST,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEY_CATEGORY
        ));
    }

    public static void onClientTick(MinecraftClient client) {
        if (fastBreakKey != null) {
            while (fastBreakKey.wasPressed()) {
                boolean newState = !fastBreakEnabled.get();
                fastBreakEnabled.set(newState);
                if (client != null && client.player != null) {
                    client.player.sendMessage(
                            Text.literal("Fast Break: " + (newState ? "§aENABLED" : "§cDISABLED")),
                            true
                    );
                }
            }
        }

        if (fastPlaceKey != null) {
            while (fastPlaceKey.wasPressed()) {
                boolean newState = !fastPlaceEnabled.get();
                fastPlaceEnabled.set(newState);
                if (client != null && client.player != null) {
                    client.player.sendMessage(
                            Text.literal("Fast Place: " + (newState ? "§aENABLED" : "§cDISABLED")),
                            true
                    );
                }
            }
        }

        if (harvestKey != null) {
            harvestActive.set(harvestKey.isPressed());
        }
    }

    public static boolean isFastBreakEnabled() {
        return fastBreakEnabled.get();
    }

    public static boolean isFastPlaceEnabled() {
        return fastPlaceEnabled.get();
    }

    public static boolean isHarvestActive() {
        return harvestActive.get();
    }

    public static void setFastBreakEnabled(boolean enabled) {
        fastBreakEnabled.set(enabled);
    }

    public static void setFastPlaceEnabled(boolean enabled) {
        fastPlaceEnabled.set(enabled);
    }

    public static void setHarvestActive(boolean active) {
        harvestActive.set(active);
    }
}
