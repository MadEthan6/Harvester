package com.fabricharvester;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricHarvesterMod implements ModInitializer {
    public static final String MOD_ID = "fabric_harvester";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Fabric Harvester initialized!");
    }
}
