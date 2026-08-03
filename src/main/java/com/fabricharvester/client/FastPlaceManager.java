package com.fabricharvester.client;

import com.fabricharvester.mixin.MinecraftClientAccessor;
import net.minecraft.client.Minecraft;

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

    public static void tick(Minecraft client) {
        tick(client, 0);
    }

    public static void tick(Minecraft client, int targetCooldown) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        tick(isActive(), targetCooldown, (MinecraftClientAccessor) client);
    }

    public static void tick(boolean active, MinecraftClientAccessor accessor) {
        tick(active, 0, accessor);
    }

    public static void tick(boolean active, int targetCooldown, MinecraftClientAccessor accessor) {
        if (!active || accessor == null) {
            return;
        }
        int safeTarget = Math.max(0, targetCooldown);
        if (accessor.getItemUseCooldown() > safeTarget) {
            accessor.setItemUseCooldown(safeTarget);
        }
    }
}
