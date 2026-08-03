package com.fabricharvester.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

public final class AutomationController {
    private static final AutomationController INSTANCE = new AutomationController();

    private final HarvesterManager harvester = new HarvesterManager();
    private AutomationProfile profile = AutomationProfile.SAFE;
    private ClientLevel lastWorld;
    private boolean deathHandled;

    private AutomationController() {
    }

    public static AutomationController getInstance() {
        return INSTANCE;
    }

    public void registerLifecycleEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onDisconnect());
    }

    public void tick(Minecraft client) {
        if (client.level != lastWorld) {
            onWorldChanged(client);
        }
        if (client.player == null || client.level == null) {
            return;
        }
        if (!client.player.isAlive()) {
            if (!deathHandled) {
                emergencyStop(client, false);
                deathHandled = true;
            }
            return;
        }
        deathHandled = false;

        if (client.gui.screen() != null) {
            emergencyStop(client, false);
            return;
        }

        ModKeyBindings.onClientTick(client, this);
        FastBreakManager.tick(client, profile.blockBreakCooldown());
        FastPlaceManager.tick(client, profile.itemUseCooldown());
        harvester.tick(
                client,
                ModKeyBindings.isHarvestActive(),
                ModKeyBindings.isBoneMealEnabled(),
                profile
        );
    }

    public void cycleProfile(Minecraft client) {
        profile = profile.next();
        notify(client, "message.fabric_harvester.profile", Component.translatable(profile.translationKey()), ChatFormatting.AQUA);
        if (profile == AutomationProfile.TRUSTED && !client.hasSingleplayerServer()) {
            warn(client, "warning.fabric_harvester.trusted_multiplayer");
        }
    }

    public void emergencyStop(Minecraft client, boolean notify) {
        ModKeyBindings.resetStates();
        harvester.cancel();
        if (notify) {
            notify(client, "message.fabric_harvester.emergency_stop", ChatFormatting.RED);
        }
    }

    public AutomationProfile profile() {
        return profile;
    }

    public boolean hasPendingHarvest() {
        return harvester.hasPendingOperation();
    }

    public HarvestOperation.State harvestState() {
        return harvester.operationState();
    }

    public boolean shouldRenderHud() {
        return ModKeyBindings.isFastBreakEnabled()
                || ModKeyBindings.isFastPlaceEnabled()
                || ModKeyBindings.isHarvestActive()
                || ModKeyBindings.isBoneMealEnabled()
                || harvester.hasPendingOperation();
    }

    public Component hudText() {
        return Component.translatable(
                "hud.fabric_harvester.status",
                Component.translatable(profile.translationKey()),
                stateText(ModKeyBindings.isFastBreakEnabled()),
                stateText(ModKeyBindings.isFastPlaceEnabled()),
                stateText(ModKeyBindings.isHarvestActive() || harvester.hasPendingFarmingOperation()),
                stateText(ModKeyBindings.isBoneMealEnabled() || harvester.hasPendingBoneMealOperation())
        );
    }

    private void onJoin(Minecraft client) {
        profile = AutomationProfile.forConnection(client.hasSingleplayerServer());
        lastWorld = client.level;
        resetSession();
    }

    private void onDisconnect() {
        lastWorld = null;
        profile = AutomationProfile.SAFE;
        resetSession();
    }

    private void onWorldChanged(Minecraft client) {
        boolean firstWorld = lastWorld == null;
        lastWorld = client.level;
        if (firstWorld) {
            profile = AutomationProfile.forConnection(client.hasSingleplayerServer());
        }
        resetSession();
    }

    private void resetSession() {
        ModKeyBindings.resetStates();
        harvester.cancel();
        deathHandled = false;
    }

    private static Component stateText(boolean enabled) {
        return Component.translatable(enabled
                ? "state.fabric_harvester.on"
                : "state.fabric_harvester.off");
    }

    static void notify(Minecraft client, String translationKey, ChatFormatting color) {
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.translatable(translationKey).withStyle(color));
        }
    }

    static void notify(
            Minecraft client,
            String translationKey,
            Component argument,
            ChatFormatting color
    ) {
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.translatable(translationKey, argument).withStyle(color));
        }
    }

    static void warn(Minecraft client, String translationKey) {
        notify(client, translationKey, ChatFormatting.RED);
    }
}
