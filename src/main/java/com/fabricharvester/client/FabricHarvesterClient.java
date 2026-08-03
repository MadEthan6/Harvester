package com.fabricharvester.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricHarvesterClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("fabric_harvester_client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Fabric Harvester Client Keybindings...");
        ModKeyBindings.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ModKeyBindings.onClientTick(client);
            FastBreakManager.tick(client);
            FastPlaceManager.tick(client);
            HarvesterManager.tick(client);
        });
        LOGGER.info("Fabric Harvester Client Keybindings initialized successfully!");
    }
}
