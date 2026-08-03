package com.fabricharvester.client;

import com.fabricharvester.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.client.Minecraft;

public class FastBreakManager {
    public static boolean isEnabled() {
        return ModKeyBindings.isFastBreakEnabled();
    }

    public static boolean isActive() {
        return ModKeyBindings.isFastBreakEnabled();
    }

    public static void setEnabled(boolean enabled) {
        ModKeyBindings.setFastBreakEnabled(enabled);
    }

    public static void tick(Minecraft client) {
        tick(client, 0);
    }

    public static void tick(Minecraft client, int targetCooldown) {
        if (client == null || client.player == null || client.level == null || client.gameMode == null) {
            return;
        }
        tick(isActive(), targetCooldown, (ClientPlayerInteractionManagerAccessor) client.gameMode);
    }

    public static void tick(boolean active, ClientPlayerInteractionManagerAccessor accessor) {
        tick(active, 0, accessor);
    }

    public static void tick(boolean active, int targetCooldown, ClientPlayerInteractionManagerAccessor accessor) {
        if (!active || accessor == null) {
            return;
        }
        int safeTarget = Math.max(0, targetCooldown);
        if (accessor.getBlockBreakingCooldown() > safeTarget) {
            accessor.setBlockBreakingCooldown(safeTarget);
        }
    }
}
