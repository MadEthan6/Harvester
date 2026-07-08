package com.example.client.mixin;

import com.example.client.SpeedmineState;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        // action == 1 means key pressed (GLFW_PRESS)
        if (action == 1) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.gui == null || mc.level == null) return;
            if (mc.gui.screen() != null) return;

            int key = event.key();
            if (!SpeedmineState.isValidKeycode(key)) return;

            // Open Settings GUI (Right Shift = 344)
            if (key == 344) {
                mc.setScreenAndShow(new com.example.client.HarvesterSettingsScreen());
                return;
            }

            // Toggle Speedmine
            if (key == SpeedmineState.speedmineKey && SpeedmineState.speedmineKey != 0) {
                SpeedmineState.cycle();
                String label = SpeedmineState.getLabel();
                if (mc.player != null) {
                    if (SpeedmineState.enabled) {
                        mc.player.sendOverlayMessage(
                            Component.literal("Speedmine: " + label)
                                .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        mc.player.sendOverlayMessage(
                            Component.literal("Speedmine: OFF")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            // Toggle Fast Place
            if (key == SpeedmineState.fastPlaceKey && SpeedmineState.fastPlaceKey != 0) {
                SpeedmineState.fastPlaceEnabled = !SpeedmineState.fastPlaceEnabled;
                if (mc.player != null) {
                    if (SpeedmineState.fastPlaceEnabled) {
                        mc.player.sendOverlayMessage(
                            Component.literal("Fast Place: ACTIVE")
                                .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        mc.player.sendOverlayMessage(
                            Component.literal("Fast Place: INACTIVE")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            // Toggle Farming Assist
            if (key == SpeedmineState.farmingAssistKey && SpeedmineState.farmingAssistKey != 0) {
                SpeedmineState.farmingAssistEnabled = !SpeedmineState.farmingAssistEnabled;
                if (mc.player != null) {
                    if (SpeedmineState.farmingAssistEnabled) {
                        mc.player.sendOverlayMessage(
                            Component.literal("Farming Assist: ACTIVE")
                                .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        mc.player.sendOverlayMessage(
                            Component.literal("Farming Assist: INACTIVE")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            // Toggle Auto-Bridge
            if (key == SpeedmineState.bridgingKey && SpeedmineState.bridgingKey != 0) {
                SpeedmineState.bridgingEnabled = !SpeedmineState.bridgingEnabled;
                if (mc.player != null) {
                    if (SpeedmineState.bridgingEnabled) {
                        mc.player.sendOverlayMessage(
                            Component.literal("Bridge: ACTIVE")
                                .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        mc.player.sendOverlayMessage(
                            Component.literal("Bridge: INACTIVE")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            // Toggle Auto Feed
            if (key == SpeedmineState.autoFeedKey && SpeedmineState.autoFeedKey != 0) {
                SpeedmineState.autoFeedEnabled = !SpeedmineState.autoFeedEnabled;
                if (mc.player != null) {
                    if (SpeedmineState.autoFeedEnabled) {
                        mc.player.sendOverlayMessage(
                            Component.literal("Auto Feed: ACTIVE")
                                .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        mc.player.sendOverlayMessage(
                            Component.literal("Auto Feed: INACTIVE")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            // Toggle HUD Overlay
            if (key == SpeedmineState.hudOverlayKey && SpeedmineState.hudOverlayKey != 0) {
                SpeedmineState.hudOverlayEnabled = !SpeedmineState.hudOverlayEnabled;
                if (mc.player != null) {
                    if (SpeedmineState.hudOverlayEnabled) {
                        mc.player.sendOverlayMessage(
                            Component.literal("HUD Overlay: ACTIVE")
                                .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        mc.player.sendOverlayMessage(
                            Component.literal("HUD Overlay: INACTIVE")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            // Toggle Block Outline
            if (key == SpeedmineState.blockOutlineKey && SpeedmineState.blockOutlineKey != 0) {
                SpeedmineState.blockOutlineEnabled = !SpeedmineState.blockOutlineEnabled;
                if (mc.player != null) {
                    if (SpeedmineState.blockOutlineEnabled) {
                        mc.player.sendOverlayMessage(
                            Component.literal("Block Outline: ACTIVE")
                                .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        mc.player.sendOverlayMessage(
                            Component.literal("Block Outline: INACTIVE")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }

            // Toggle Stealth Mode
            if (key == SpeedmineState.stealthModeKey && SpeedmineState.stealthModeKey != 0) {
                SpeedmineState.stealthMode = !SpeedmineState.stealthMode;
                SpeedmineState.clampSpeedIndexUnderStealth();
                if (mc.player != null) {
                    if (SpeedmineState.stealthMode) {
                        mc.player.sendOverlayMessage(
                            Component.literal("Stealth Mode: ACTIVE")
                                .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        mc.player.sendOverlayMessage(
                            Component.literal("Stealth Mode: INACTIVE")
                                .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }
        }
    }
}
