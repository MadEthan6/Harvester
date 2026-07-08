package com.example.client.mixin;

import com.example.client.SpeedmineState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    @Final
    public Options options;

    @Shadow
    private int rightClickDelay;

    @Shadow
    public LocalPlayer player;

    @Shadow
    public MultiPlayerGameMode gameMode;

    @Unique
    private boolean speedmine_isBridging = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // Fast Place logic
        if (SpeedmineState.fastPlaceEnabled && this.options.keyUse.isDown()) {
            this.rightClickDelay = 0;
        }

        // Process pending Farming Assist replant queue
        if (SpeedmineState.pendingReplantPos != null && this.player != null && this.player.level() != null) {
            if (SpeedmineState.pendingReplantTimeout > 0) {
                SpeedmineState.pendingReplantTimeout--;
                BlockPos pos = SpeedmineState.pendingReplantPos;
                if (this.player.level().getBlockState(pos).isAir()) {
                    speedmine_performReplant(pos, SpeedmineState.pendingReplantSeed);
                    SpeedmineState.pendingReplantPos = null;
                    SpeedmineState.pendingReplantSeed = null;
                    SpeedmineState.pendingReplantTimeout = 0;
                }
            } else {
                SpeedmineState.pendingReplantPos = null;
                SpeedmineState.pendingReplantSeed = null;
                SpeedmineState.pendingReplantTimeout = 0;
            }
        }

        // Bridging logic
        if (SpeedmineState.bridgingEnabled && this.player != null && this.gameMode != null && !this.speedmine_isBridging) {
            this.speedmine_isBridging = true;
            try {
                speedmine_doBridging();
            } finally {
                this.speedmine_isBridging = false;
            }
        }
    }

    @Unique
    private void speedmine_doBridging() {
        LocalPlayer p = this.player;
        if (p.level() == null) return;

        // Only bridge while the player is actually moving (has horizontal input)
        Vec3 delta = p.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontalSpeed < 0.01) return;

        // Auto-replenish empty hotbar slots from main inventory
        speedmine_replenishHotbarBlocks(p);

        // Find a solid block in the hotbar
        int blockSlot = speedmine_findSolidBlockSlot(p);
        if (blockSlot == -1) return;

        int originalSlot = p.getInventory().getSelectedSlot();
        boolean placed = false;

        // Position directly below the player's feet
        int y = p.blockPosition().getY() - 1;

        // Project bounding box forward by 3.0x velocity to place ahead
        AABB box = p.getBoundingBox();
        AABB checkArea = box.expandTowards(delta.x * 3.0, 0, delta.z * 3.0);

        int minX = (int) Math.floor(checkArea.minX);
        int maxX = (int) Math.floor(checkArea.maxX);
        int minZ = (int) Math.floor(checkArea.minZ);
        int maxZ = (int) Math.floor(checkArea.maxZ);

        for (int xVal = minX; xVal <= maxX; xVal++) {
            for (int zVal = minZ; zVal <= maxZ; zVal++) {
                BlockPos targetPos = new BlockPos(xVal, y, zVal);
                if (speedmine_canPlaceAt(p, targetPos)) {
                    // Check if current slot ran out of blocks
                    if (placed && p.getInventory().getItem(blockSlot).isEmpty()) {
                        blockSlot = speedmine_findSolidBlockSlot(p);
                        if (blockSlot == -1) break;
                        speedmine_swapToSlot(p, blockSlot);
                    }

                    if (!placed) {
                        speedmine_swapToSlot(p, blockSlot);
                        placed = true;
                    }
                    speedmine_placeBlockAt(p, targetPos);
                }
            }
        }

        // Restore original slot only if it is not empty
        if (placed) {
            ItemStack originalStack = p.getInventory().getItem(originalSlot);
            if (!originalStack.isEmpty()) {
                p.getInventory().setSelectedSlot(originalSlot);
                p.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
            }
        }
    }

    @Unique
    private void speedmine_replenishHotbarBlocks(LocalPlayer p) {
        // Check if any hotbar slot is empty
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack hotbarStack = p.getInventory().getItem(hotbarSlot);
            if (hotbarStack.isEmpty()) {
                // Find a block in main inventory (9-35)
                for (int mainSlot = 9; mainSlot < 36; mainSlot++) {
                    ItemStack mainStack = p.getInventory().getItem(mainSlot);
                    if (!mainStack.isEmpty() && mainStack.getItem() instanceof BlockItem) {
                        // Swap it to the empty hotbar slot
                        this.gameMode.handleContainerInput(
                            p.containerMenu.containerId,
                            mainSlot,
                            hotbarSlot,
                            ContainerInput.SWAP,
                            p
                        );
                        break; // Move to checking next hotbar slot
                    }
                }
            }
        }
    }

    @Unique
    private int speedmine_findSolidBlockSlot(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    @Unique
    private boolean speedmine_canPlaceAt(LocalPlayer p, BlockPos pos) {
        BlockState state = p.level().getBlockState(pos);
        return state.isAir() || state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock;
    }

    @Unique
    private void speedmine_swapToSlot(LocalPlayer p, int slot) {
        p.getInventory().setSelectedSlot(slot);
        p.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    @Unique
    private void speedmine_placeBlockAt(LocalPlayer p, BlockPos pos) {
        // We need to find an adjacent solid block to target for placement
        // Try each direction to find one with a solid neighbor
        Direction[] directions = { Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP };
        for (Direction dir : directions) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = p.level().getBlockState(neighbor);
            if (!neighborState.isAir() && neighborState.getFluidState().isEmpty()) {
                // Place against this neighbor block from the opposite side
                Direction placeSide = dir.getOpposite();
                BlockHitResult hitResult = new BlockHitResult(
                    new Vec3(neighbor.getX() + 0.5, neighbor.getY() + 0.5, neighbor.getZ() + 0.5),
                    placeSide,
                    neighbor,
                    false
                );
                this.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hitResult);
                return;
            }
        }
    }

    @Unique
    private void speedmine_performReplant(BlockPos pos, net.minecraft.world.item.Item seedItem) {
        LocalPlayer p = this.player;
        if (p == null || this.gameMode == null) return;

        if (p.getOffhandItem().is(seedItem)) {
            net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(
                new Vec3(pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5),
                Direction.UP,
                pos.below(),
                false
            );
            this.gameMode.useItemOn(p, InteractionHand.OFF_HAND, hitResult);
            return;
        }

        int seedSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).is(seedItem)) {
                seedSlot = i;
                break;
            }
        }
        if (seedSlot == -1) return;

        int originalSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(seedSlot);
        p.connection.send(new ServerboundSetCarriedItemPacket(seedSlot));

        net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(
            new Vec3(pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5),
            Direction.UP,
            pos.below(),
            false
        );
        this.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hitResult);

        p.getInventory().setSelectedSlot(originalSlot);
        p.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
    }
}
