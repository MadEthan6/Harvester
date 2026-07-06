package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.example.SpeedmineMod;

public class SpeedmineModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SpeedmineMod.LOGGER.info("Speedmine Client Initialized!");
	}
}