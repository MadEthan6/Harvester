package com.example.client.mixin;

import com.example.client.SpeedmineState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Speedmine and Farming Assist mixin for MultiPlayerGameMode.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Shadow
    private int destroyDelay;

    @Shadow
    private float destroyProgress;

    @Shadow
    private boolean isDestroying;

    @Shadow
    public abstract boolean destroyBlock(BlockPos pos);

    @Shadow
    public abstract InteractionResult useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult result);

    @Shadow
    public abstract void handleContainerInput(int containerId, int slotId, int buttonNum, net.minecraft.world.inventory.ContainerInput inputType, Player player);

    @Unique
    private float speedmine_previousProgress = 0.0f;

    @Unique
    private boolean speedmine_isFarming = false;

    /**
     * Before continueDestroyBlock runs, snapshot the current progress so we can
     * compute how much vanilla added this tick.
     */
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void onContinueDestroyBlockHead(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (SpeedmineState.enabled) {
            if (SpeedmineState.stealthMode) {
                this.destroyDelay = (int) (Math.random() * 3) + 1;
            } else {
                this.destroyDelay = 0;
            }
        }
        this.speedmine_previousProgress = this.destroyProgress;
    }

    /**
     * After continueDestroyBlock finishes, if speedmine is enabled, compute the
     * vanilla increment (current - previous) and add the boosted progress.
     * Also zero out destroyDelay for instant sequential breaking.
     */
    @Inject(method = "continueDestroyBlock", at = @At("RETURN"))
    private void onContinueDestroyBlockReturn(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (SpeedmineState.enabled && this.isDestroying) {
            float vanillaIncrement = this.destroyProgress - this.speedmine_previousProgress;
            if (vanillaIncrement > 0.0f) {
                float mult = SpeedmineState.multiplier;
                if (SpeedmineState.stealthMode && mult > 1.4f) {
                    mult = 1.4f;
                }
                this.destroyProgress += vanillaIncrement * (mult - 1.0f);
            }
            if (SpeedmineState.stealthMode) {
                this.destroyDelay = (int) (Math.random() * 3) + 1;
            } else {
                this.destroyDelay = 0;
            }
        }
    }

    /**
     * After destroyBlock finishes, zero out the delay for instant sequential
     * block breaking.
     */
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void onDestroyBlockReturn(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SpeedmineState.enabled) {
            if (SpeedmineState.stealthMode) {
                this.destroyDelay = (int) (Math.random() * 3) + 1;
            } else {
                this.destroyDelay = 0;
            }
        }
    }

    /**
     * When startDestroyBlock is called, if the block won't be insta-broken,
     * boost the initial progress snapshot so that the first tick is also boosted.
     */
    @Inject(method = "startDestroyBlock", at = @At("RETURN"))
    private void onStartDestroyBlockReturn(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (SpeedmineState.enabled && this.isDestroying) {
            float mult = SpeedmineState.multiplier;
            if (SpeedmineState.stealthMode && mult > 1.4f) {
                mult = 1.4f;
            }
            this.destroyProgress *= mult;
            if (SpeedmineState.stealthMode) {
                this.destroyDelay = (int) (Math.random() * 3) + 1;
            } else {
                this.destroyDelay = 0;
            }
        }
    }

    // --- Farming Assist Hooks ---

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlockHead(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (SpeedmineState.farmingAssistEnabled) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                BlockState state = mc.level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                    if (this.speedmine_harvestAndReplant(mc, pos, state)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOnHead(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (SpeedmineState.farmingAssistEnabled && hand == InteractionHand.MAIN_HAND) {
            Minecraft mc = Minecraft.getInstance();
            BlockPos pos = hitResult.getBlockPos();
            if (mc.level != null) {
                BlockState state = mc.level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                    if (this.speedmine_harvestAndReplant(mc, pos, state)) {
                        cir.setReturnValue(InteractionResult.SUCCESS);
                    }
                }
            }
        }
    }

    @Unique
    private boolean speedmine_harvestAndReplant(Minecraft mc, BlockPos pos, BlockState state) {
        if (this.speedmine_isFarming) return false;

        Block block = state.getBlock();
        Item seedItem = speedmine_getSeedItem(block);
        if (seedItem == null) return false;

        this.speedmine_isFarming = true;
        try {
            // 1. Send start break packet to server so the harvest is registered server-side
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                Direction.DOWN
            ));

            // 2. Queue replanting
            SpeedmineState.pendingReplantPos = pos.immutable();
            SpeedmineState.pendingReplantSeed = seedItem;
            SpeedmineState.pendingReplantTimeout = 10;
            return true;
        } finally {
            this.speedmine_isFarming = false;
        }
    }

    @Unique
    private Item speedmine_getSeedItem(Block block) {
        if (block == Blocks.WHEAT) {
            return Items.WHEAT_SEEDS;
        } else if (block == Blocks.CARROTS) {
            return Items.CARROT;
        } else if (block == Blocks.POTATOES) {
            return Items.POTATO;
        } else if (block == Blocks.BEETROOTS) {
            return Items.BEETROOT_SEEDS;
        }
        return null;
    }
}
