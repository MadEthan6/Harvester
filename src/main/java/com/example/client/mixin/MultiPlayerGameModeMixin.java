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
            this.destroyDelay = 0;
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
                // Since multiplier is e.g. 1.4f, the extra fraction is multiplier - 1.0f (e.g. 0.4f).
                this.destroyProgress += vanillaIncrement * (SpeedmineState.multiplier - 1.0f);
            }
            this.destroyDelay = 0;
        }
    }

    /**
     * After destroyBlock finishes, zero out the delay for instant sequential
     * block breaking.
     */
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void onDestroyBlockReturn(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SpeedmineState.enabled) {
            this.destroyDelay = 0;
        }
    }

    /**
     * When startDestroyBlock is called, if the block won't be insta-broken,
     * boost the initial progress snapshot so that the first tick is also boosted.
     */
    @Inject(method = "startDestroyBlock", at = @At("RETURN"))
    private void onStartDestroyBlockReturn(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (SpeedmineState.enabled && this.isDestroying) {
            // The initial destroyProgress was set by startDestroyBlock.
            // Boost it by the dynamic multiplier.
            this.destroyProgress *= SpeedmineState.multiplier;
            this.destroyDelay = 0;
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
            // 1. Send block-break packet to server so the harvest is registered server-side
            mc.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                Direction.DOWN
            ));

            // 2. Destroy/Harvest crop client-side
            boolean destroyed = this.destroyBlock(pos);
            if (!destroyed) return false;

            // 2. Check offhand first
            if (mc.player.getOffhandItem().is(seedItem)) {
                BlockHitResult hitResult = new BlockHitResult(
                    new Vec3(pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5),
                    Direction.UP,
                    pos.below(),
                    false
                );
                this.useItemOn(mc.player, InteractionHand.OFF_HAND, hitResult);
                return true;
            }

            // 3. Find seed in main inventory/hotbar
            int seedSlot = speedmine_findSeedSlot(mc.player.getInventory(), seedItem);
            if (seedSlot == -1) {
                return true; // Harvested, but cannot replant
            }

            int originalSlot = mc.player.getInventory().getSelectedSlot();
            boolean isHotbar = (seedSlot < 9);

            if (isHotbar) {
                // Swap slot
                mc.player.getInventory().setSelectedSlot(seedSlot);
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(seedSlot));

                // Replant
                BlockHitResult hitResult = new BlockHitResult(
                    new Vec3(pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5),
                    Direction.UP,
                    pos.below(),
                    false
                );
                this.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);

                // Restore slot
                mc.player.getInventory().setSelectedSlot(originalSlot);
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
            } else {
                // Swap item in main inventory to active hotbar slot
                int containerSlotId = seedSlot;
                this.handleContainerInput(
                    mc.player.containerMenu.containerId,
                    containerSlotId,
                    originalSlot,
                    net.minecraft.world.inventory.ContainerInput.SWAP,
                    mc.player
                );

                // Replant
                BlockHitResult hitResult = new BlockHitResult(
                    new Vec3(pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5),
                    Direction.UP,
                    pos.below(),
                    false
                );
                this.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);

                // Swap back
                this.handleContainerInput(
                    mc.player.containerMenu.containerId,
                    containerSlotId,
                    originalSlot,
                    net.minecraft.world.inventory.ContainerInput.SWAP,
                    mc.player
                );
            }
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

    @Unique
    private int speedmine_findSeedSlot(net.minecraft.world.entity.player.Inventory inventory, Item seedItem) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(seedItem)) {
                return i;
            }
        }
        return -1;
    }
}
