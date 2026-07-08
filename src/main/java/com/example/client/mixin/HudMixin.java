package com.example.client.mixin;

import com.example.client.SpeedmineState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {

    @Shadow
    public abstract Font getFont();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void extractSpeedmineOverlay(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!SpeedmineState.hudOverlayEnabled) return;
        int color = SpeedmineState.enabled ? 0xFF00FF00 : 0xFFFF0000;
        String text = SpeedmineState.enabled ? "Speedmine: ACTIVE" : "Speedmine: INACTIVE";
        extractor.text(this.getFont(), text, 10, 10, color);
    }
}
