package com.fabricharvester.client;

import com.fabricharvester.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;

public class FastPlaceManager {

    public static boolean isEnabled() {
        return ModKeyBindings.isFastPlaceEnabled();
    }

    public static boolean isActive() {
        return ModKeyBindings.isFastPlaceEnabled();
    }

    public static void setEnabled(boolean enabled) {
        ModKeyBindings.setFastPlaceEnabled(enabled);
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        boolean active = isActive();
        tick(active, (MinecraftClientAccessor) client);
    }

    public static void tick(boolean active, MinecraftClientAccessor accessor) {
        if (!active || accessor == null) {
            return;
        }
        accessor.setItemUseCooldown(0);
    }
}
