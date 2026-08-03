package com.fabricharvester.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class AutomationHud {
    private static final int X = 6;
    private static final int Y = 6;
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath("fabric_harvester", "automation_status");

    private AutomationHud() {
    }

    public static void register() {
        HudElementRegistry.addLast(HUD_ID, (extractor, deltaTracker) -> render(extractor));
    }

    private static void render(GuiGraphicsExtractor extractor) {
        Minecraft client = Minecraft.getInstance();
        AutomationController controller = AutomationController.getInstance();
        if (client.player == null || client.gui.hud.isHidden() || !controller.shouldRenderHud()) {
            return;
        }

        Component text = controller.hudText();
        int width = client.font.width(text);
        extractor.fill(X - 2, Y - 2, X + width + 2, Y + 11, 0x90000000);
        extractor.text(client.font, text, X, Y, 0xFFFFFFFF, true);
    }
}
