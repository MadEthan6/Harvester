package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import com.example.SpeedmineMod;

public class SpeedmineModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SpeedmineState.loadConfig();
		SpeedmineMod.LOGGER.info("Speedmine Client Initialized and loaded config!");
	}
}