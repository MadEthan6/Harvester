package com.fabricharvester.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ModKeyBindings {
    public static final String KEY_FAST_BREAK = "key.fabric_harvester.fast_break";
    public static final String KEY_FAST_PLACE = "key.fabric_harvester.fast_place";
    public static final String KEY_HARVEST = "key.fabric_harvester.harvest";
    public static final String KEY_PROFILE = "key.fabric_harvester.profile";
    public static final String KEY_EMERGENCY_STOP = "key.fabric_harvester.emergency_stop";

    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("fabric_harvester", "general")
    );

    public static KeyMapping fastBreakKey;
    public static KeyMapping fastPlaceKey;
    public static KeyMapping harvestKey;
    public static KeyMapping profileKey;
    public static KeyMapping emergencyStopKey;

    private static final AtomicBoolean fastBreakEnabled = new AtomicBoolean(false);
    private static final AtomicBoolean fastPlaceEnabled = new AtomicBoolean(false);
    private static final AtomicBoolean harvestActive = new AtomicBoolean(false);
    private static boolean harvestSuppressedUntilRelease;

    private ModKeyBindings() {
    }

    public static void register() {
        fastBreakKey = register(KEY_FAST_BREAK, GLFW.GLFW_KEY_B);
        fastPlaceKey = register(KEY_FAST_PLACE, GLFW.GLFW_KEY_V);
        harvestKey = register(KEY_HARVEST, GLFW.GLFW_KEY_H);
        profileKey = register(KEY_PROFILE, GLFW.GLFW_KEY_P);
        emergencyStopKey = register(KEY_EMERGENCY_STOP, GLFW.GLFW_KEY_K);
    }

    public static void onClientTick(Minecraft client) {
        onClientTick(client, AutomationController.getInstance());
    }

    public static void onClientTick(Minecraft client, AutomationController controller) {
        if (consumeEmergencyStop()) {
            controller.emergencyStop(client, true);
            drainToggleKeys();
            return;
        }

        if (profileKey != null) {
            while (profileKey.consumeClick()) {
                controller.cycleProfile(client);
            }
        }
        consumeToggle(
                fastBreakKey,
                fastBreakEnabled,
                client,
                controller,
                "message.fabric_harvester.fast_break"
        );
        consumeToggle(
                fastPlaceKey,
                fastPlaceEnabled,
                client,
                controller,
                "message.fabric_harvester.fast_place"
        );
        updateHarvestState();
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

    public static void resetStates() {
        fastBreakEnabled.set(false);
        fastPlaceEnabled.set(false);
        harvestActive.set(false);
    }

    public static void suppressHarvestUntilRelease() {
        harvestSuppressedUntilRelease = true;
        harvestActive.set(false);
    }

    static boolean isHarvestSuppressedUntilRelease() {
        return harvestSuppressedUntilRelease;
    }

    private static KeyMapping register(String translationKey, int defaultKey) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                defaultKey,
                KEY_CATEGORY
        ));
    }

    private static boolean consumeEmergencyStop() {
        boolean pressed = false;
        if (emergencyStopKey != null) {
            while (emergencyStopKey.consumeClick()) {
                pressed = true;
            }
        }
        return pressed;
    }

    private static void consumeToggle(
            KeyMapping binding,
            AtomicBoolean state,
            Minecraft client,
            AutomationController controller,
            String translationKey
    ) {
        if (binding == null) {
            return;
        }
        while (binding.consumeClick()) {
            boolean enabled = !state.get();
            state.set(enabled);
            Component stateText = Component.translatable(enabled
                    ? "state.fabric_harvester.enabled"
                    : "state.fabric_harvester.disabled");
            AutomationController.notify(
                    client,
                    translationKey,
                    stateText,
                    enabled ? ChatFormatting.GREEN : ChatFormatting.RED
            );
            if (enabled && controller.profile() == AutomationProfile.SAFE) {
                AutomationController.warn(client, "warning.fabric_harvester.safe_limits");
            }
        }
    }

    private static void updateHarvestState() {
        if (harvestKey == null) {
            harvestActive.set(false);
            return;
        }
        if (harvestSuppressedUntilRelease) {
            harvestActive.set(false);
            if (!harvestKey.isDown()) {
                harvestSuppressedUntilRelease = false;
            }
            return;
        }
        harvestActive.set(harvestKey.isDown());
    }

    private static void drainToggleKeys() {
        drain(fastBreakKey);
        drain(fastPlaceKey);
        drain(profileKey);
    }

    private static void drain(KeyMapping binding) {
        if (binding != null) {
            while (binding.consumeClick()) {
                // Consume queued presses after the emergency stop.
            }
        }
    }
}
