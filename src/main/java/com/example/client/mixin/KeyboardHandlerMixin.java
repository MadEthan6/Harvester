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
        // event.key() == 86 is the V key
        if (action == 1 && event.key() == 86) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null) {
                SpeedmineState.cycle();

                String label = SpeedmineState.getLabel();
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
        if (action == 1 && event.key() == 74) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null) {
                SpeedmineState.fastPlaceEnabled = !SpeedmineState.fastPlaceEnabled;
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
        if (action == 1 && event.key() == 75) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null) {
                SpeedmineState.farmingAssistEnabled = !SpeedmineState.farmingAssistEnabled;
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
        if (action == 1 && event.key() == 66) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null) {
                SpeedmineState.bridgingEnabled = !SpeedmineState.bridgingEnabled;
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
        if (action == 1 && event.key() == 71) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null) {
                SpeedmineState.autoFeedEnabled = !SpeedmineState.autoFeedEnabled;
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
    }
}
