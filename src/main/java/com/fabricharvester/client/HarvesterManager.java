package com.fabricharvester.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Client-thread crop harvesting and replanting logic. */
public final class HarvesterManager {
    private static final int HORIZONTAL_RADIUS = 1;
    private static final int VERTICAL_RADIUS = 1;
    private static final int HOTBAR_SIZE = 9;
    private static final int MAIN_INVENTORY_SIZE = 36;
    private static final int PLAYER_HANDLER_HOTBAR_START = 36;
    private static final int ACTION_INTERVAL_TICKS = 2;

    private static int actionCooldown;

    private HarvesterManager() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null) {
            return;
        }
        if (!client.isOnThread()) {
            client.execute(() -> tick(client));
            return;
        }
        if (!ModKeyBindings.isHarvestActive()) {
            actionCooldown = 0;
            return;
        }
        if (client.player == null || client.world == null || client.interactionManager == null
                || client.currentScreen != null) {
            return;
        }
        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }

        BlockPos cropPos = findNearestMatureCrop(client);
        if (cropPos == null) {
            return;
        }

        BlockState cropState = client.world.getBlockState(cropPos);
        Item plantingItem = getPlantingItem(cropState.getBlock());
        if (plantingItem == null || !isWithinReach(client, cropPos)) {
            return;
        }

        HeldItemSelection selection = HeldItemSelection.select(client, plantingItem);
        if (selection == null) {
            return;
        }

        try {
            harvestAndReplant(client, cropPos, selection.hand());
        } finally {
            selection.restore(client);
        }
    }

    private static BlockPos findNearestMatureCrop(MinecraftClient client) {
        BlockPos origin = client.player.getBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int y = -VERTICAL_RADIUS; y <= VERTICAL_RADIUS; y++) {
            for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
                for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                    BlockPos candidate = origin.add(x, y, z);
                    BlockState state = client.world.getBlockState(candidate);
                    if (!isSupportedMatureCrop(state) || !isWithinReach(client, candidate)) {
                        continue;
                    }

                    double distance = client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(candidate));
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = candidate.toImmutable();
                    }
                }
            }
        }

        return nearest;
    }

    private static boolean isSupportedMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (getPlantingItem(block) == null || !(block instanceof CropBlock cropBlock)) {
            return false;
        }
        return cropBlock.isMature(state);
    }

    private static Item getPlantingItem(Block cropBlock) {
        if (cropBlock == Blocks.WHEAT) {
            return Items.WHEAT_SEEDS;
        }
        if (cropBlock == Blocks.CARROTS) {
            return Items.CARROT;
        }
        if (cropBlock == Blocks.POTATOES) {
            return Items.POTATO;
        }
        if (cropBlock == Blocks.BEETROOTS) {
            return Items.BEETROOT_SEEDS;
        }
        return null;
    }

    private static boolean isWithinReach(MinecraftClient client, BlockPos pos) {
        double reach = client.player.getBlockInteractionRange();
        return client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= reach * reach;
    }

    private static void harvestAndReplant(MinecraftClient client, BlockPos cropPos, Hand plantingHand) {
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;

        if (!interactionManager.breakBlock(cropPos)) {
            return;
        }

        player.swingHand(Hand.MAIN_HAND);

        BlockPos farmlandPos = cropPos.down();
        if (!client.world.getBlockState(farmlandPos).isOf(Blocks.FARMLAND)) {
            return;
        }

        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofBottomCenter(cropPos),
                Direction.UP,
                farmlandPos,
                false
        );
        ActionResult result = interactionManager.interactBlock(player, plantingHand, hitResult);
        if (result.isAccepted()) {
            player.swingHand(plantingHand);
        }
        actionCooldown = ACTION_INTERVAL_TICKS;
    }

    private record HeldItemSelection(Hand hand, int originalSelectedSlot, int swappedInventorySlot) {
        private static final int NO_SLOT = -1;

        static HeldItemSelection select(MinecraftClient client, Item item) {
            ClientPlayerEntity player = client.player;
            PlayerInventory inventory = player.getInventory();

            if (player.getMainHandStack().isOf(item)) {
                return new HeldItemSelection(Hand.MAIN_HAND, NO_SLOT, NO_SLOT);
            }
            if (player.getOffHandStack().isOf(item)) {
                return new HeldItemSelection(Hand.OFF_HAND, NO_SLOT, NO_SLOT);
            }

            int originalSelectedSlot = inventory.selectedSlot;
            for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
                if (inventory.getStack(slot).isOf(item)) {
                    inventory.selectedSlot = slot;
                    player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                    return new HeldItemSelection(Hand.MAIN_HAND, originalSelectedSlot, NO_SLOT);
                }
            }

            int inventoryEnd = Math.min(MAIN_INVENTORY_SIZE, inventory.size());
            for (int slot = HOTBAR_SIZE; slot < inventoryEnd; slot++) {
                ItemStack stack = inventory.getStack(slot);
                if (!stack.isOf(item)) {
                    continue;
                }

                client.interactionManager.clickSlot(
                        player.playerScreenHandler.syncId,
                        slot,
                        originalSelectedSlot,
                        SlotActionType.SWAP,
                        player
                );
                return new HeldItemSelection(Hand.MAIN_HAND, NO_SLOT, slot);
            }

            return null;
        }

        void restore(MinecraftClient client) {
            ClientPlayerEntity player = client.player;
            if (player == null || client.interactionManager == null) {
                return;
            }

            if (swappedInventorySlot != NO_SLOT) {
                client.interactionManager.clickSlot(
                        player.playerScreenHandler.syncId,
                        swappedInventorySlot,
                        player.getInventory().selectedSlot,
                        SlotActionType.SWAP,
                        player
                );
            }
            if (originalSelectedSlot != NO_SLOT) {
                player.getInventory().selectedSlot = originalSelectedSlot;
                player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSelectedSlot));
            }
        }
    }
}
