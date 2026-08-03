package com.fabricharvester.client;

import com.fabricharvester.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.client.MinecraftClient;

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

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) {
            return;
        }
        boolean active = isActive();
        tick(active, (ClientPlayerInteractionManagerAccessor) client.interactionManager);
    }

    public static void tick(boolean active, ClientPlayerInteractionManagerAccessor accessor) {
        if (!active || accessor == null) {
            return;
        }
        accessor.setBlockBreakingCooldown(0);
    }
}
