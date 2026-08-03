package com.fabricharvester.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(MultiPlayerGameMode.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("destroyDelay")
    int getBlockBreakingCooldown();

    @Accessor("destroyDelay")
    void setBlockBreakingCooldown(int cooldown);
}
