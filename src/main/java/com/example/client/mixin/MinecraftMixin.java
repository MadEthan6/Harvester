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

        // Find a solid block in the hotbar
        int blockSlot = speedmine_findSolidBlockSlot(p);
        if (blockSlot == -1) return;

        int originalSlot = p.getInventory().getSelectedSlot();

        // Position directly below the player's feet
        BlockPos feetPos = p.blockPosition();
        BlockPos belowFeet = feetPos.below();

        // Position one block ahead in movement direction
        double angle = Math.atan2(delta.z, delta.x);
        int dx = (int) Math.round(Math.cos(angle));
        int dz = (int) Math.round(Math.sin(angle));
        BlockPos ahead = feetPos.offset(dx, 0, dz);
        BlockPos belowAhead = ahead.below();

        boolean placed = false;

        // Place block below feet if it's air/liquid
        if (speedmine_canPlaceAt(p, belowFeet)) {
            if (!placed) {
                speedmine_swapToSlot(p, blockSlot);
                placed = true;
            }
            speedmine_placeBlockAt(p, belowFeet);
        }

        // Place block below the ahead position if it's air/liquid
        if (speedmine_canPlaceAt(p, belowAhead)) {
            if (!placed) {
                speedmine_swapToSlot(p, blockSlot);
                placed = true;
            }
            speedmine_placeBlockAt(p, belowAhead);
        }

        // Restore original slot
        if (placed) {
            p.getInventory().setSelectedSlot(originalSlot);
            p.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
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
        return state.isAir() || !state.getFluidState().isEmpty();
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
}
