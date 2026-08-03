package com.fabricharvester.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Runs at most one acknowledged harvest/replant operation at a time. */
public final class HarvesterManager {
    private static final int HORIZONTAL_RADIUS = 1;
    private static final int VERTICAL_RADIUS = 1;
    private static final int WARNING_COOLDOWN_TICKS = 40;

    private final HarvestScheduler scheduler = new HarvestScheduler();
    private PendingHarvest pending;
    private int warningCooldown;

    public void tick(Minecraft client, boolean harvestHeld, AutomationProfile profile) {
        if (warningCooldown > 0) {
            warningCooldown--;
        }
        scheduler.tick();
        if (!isReady(client)) {
            return;
        }

        if (pending != null) {
            tickPending(client);
            return;
        }
        if (!scheduler.canStart(harvestHeld, false)) {
            return;
        }

        CropTarget target = findNearestMatureCrop(client);
        if (target == null) {
            return;
        }
        if (!hasPlantingItem(client.player, target.definition().plantingItem())) {
            warn(client, "warning.fabric_harvester.missing_seeds");
            scheduler.onStarted(profile);
            return;
        }
        if (!client.gameMode.destroyBlock(target.position())) {
            warn(client, "warning.fabric_harvester.break_rejected");
            scheduler.onStarted(profile);
            return;
        }

        client.player.swing(InteractionHand.MAIN_HAND);
        pending = new PendingHarvest(
                target.position(),
                target.definition(),
                new HarvestOperation(profile.retryBackoffTicks())
        );
        scheduler.onStarted(profile);
    }

    public void cancel() {
        if (pending != null) {
            pending.operation().cancel();
            pending = null;
        }
        scheduler.reset();
        warningCooldown = 0;
    }

    public boolean hasPendingOperation() {
        return pending != null;
    }

    public HarvestOperation.State operationState() {
        return pending == null ? null : pending.operation().state();
    }

    private void tickPending(Minecraft client) {
        BlockPos cropPos = pending.position();
        BlockPos farmlandPos = cropPos.below();
        if (!isWithinReach(client, cropPos)) {
            failPending(client, "warning.fabric_harvester.out_of_reach");
            return;
        }
        if (!client.level.getBlockState(farmlandPos).is(Blocks.FARMLAND)) {
            failPending(client, "warning.fabric_harvester.farmland_missing");
            return;
        }

        BlockState currentState = client.level.getBlockState(cropPos);
        boolean cropRemoved = !currentState.is(pending.definition().cropBlock());
        boolean cropReplanted = pending.definition().isReplanted(currentState);
        HarvestOperation.Step step = pending.operation().tick(cropRemoved, cropReplanted);
        handleStep(client, step);
    }

    private void handleStep(Minecraft client, HarvestOperation.Step step) {
        switch (step) {
            case ATTEMPT_REPLANT -> {
                ReplantAttempt attempt = attemptReplant(client, pending);
                if (attempt == ReplantAttempt.INVENTORY_CHANGED) {
                    failPending(client, "warning.fabric_harvester.inventory_changed");
                    return;
                }
                HarvestOperation.Step result = pending.operation().onReplantAttempt(
                        attempt == ReplantAttempt.ACCEPTED
                );
                if (result != HarvestOperation.Step.NONE) {
                    handleStep(client, result);
                }
            }
            case COMPLETE -> pending = null;
            case BREAK_TIMEOUT -> failPending(client, "warning.fabric_harvester.break_timeout");
            case REPLANT_TIMEOUT -> failPending(client, "warning.fabric_harvester.replant_timeout");
            case NONE -> {
            }
        }
    }

    private ReplantAttempt attemptReplant(Minecraft client, PendingHarvest harvest) {
        if (!client.level.getBlockState(harvest.position()).isAir()) {
            return ReplantAttempt.REJECTED;
        }

        HeldItemSelection selection = HeldItemSelection.select(client, harvest.definition().plantingItem());
        if (selection == null) {
            warn(client, "warning.fabric_harvester.missing_seeds");
            return ReplantAttempt.REJECTED;
        }

        boolean accepted;
        boolean restored;
        try {
            BlockPos farmlandPos = harvest.position().below();
            BlockHitResult hitResult = new BlockHitResult(
                    Vec3.atBottomCenterOf(harvest.position()),
                    Direction.UP,
                    farmlandPos,
                    false
            );
            InteractionResult result = client.gameMode.useItemOn(
                    client.player,
                    selection.hand(),
                    hitResult
            );
            if (result.consumesAction()) {
                client.player.swing(selection.hand());
            }
            accepted = result.consumesAction();
        } finally {
            restored = selection.restore(client);
        }
        if (!restored) {
            return ReplantAttempt.INVENTORY_CHANGED;
        }
        return accepted ? ReplantAttempt.ACCEPTED : ReplantAttempt.REJECTED;
    }

    private CropTarget findNearestMatureCrop(Minecraft client) {
        BlockPos origin = client.player.blockPosition();
        List<CropCandidateSelector.Candidate<CropTarget>> candidates = new ArrayList<>();

        for (int y = -VERTICAL_RADIUS; y <= VERTICAL_RADIUS; y++) {
            for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
                for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    BlockState state = client.level.getBlockState(candidate);
                    CropDefinition definition = CropRegistry.findMature(state).orElse(null);
                    double distance = client.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(candidate));
                    candidates.add(new CropCandidateSelector.Candidate<>(
                            definition == null ? null : new CropTarget(candidate.immutable(), definition),
                            definition != null,
                            isWithinReach(client, candidate),
                            distance
                    ));
                }
            }
        }
        return CropCandidateSelector.nearestUsable(candidates).orElse(null);
    }

    private static boolean hasPlantingItem(LocalPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        return InventorySelectionPlan.choose(
                player.getMainHandItem().is(item),
                player.getOffhandItem().is(item),
                inventory.getContainerSize(),
                slot -> inventory.getItem(slot).is(item)
        ).found();
    }

    private static boolean isWithinReach(Minecraft client, BlockPos pos) {
        double reach = client.player.blockInteractionRange();
        return client.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= reach * reach;
    }

    private static boolean isReady(Minecraft client) {
        return client != null
                && client.isSameThread()
                && client.player != null
                && client.player.isAlive()
                && client.level != null
                && client.gameMode != null;
    }

    private void failPending(Minecraft client, String translationKey) {
        warn(client, translationKey);
        if (pending != null) {
            pending.operation().cancel();
        }
        pending = null;
    }

    private void warn(Minecraft client, String translationKey) {
        if (warningCooldown > 0 || client.player == null) {
            return;
        }
        client.player.sendOverlayMessage(Component.translatable(translationKey).withStyle(ChatFormatting.RED));
        warningCooldown = WARNING_COOLDOWN_TICKS;
    }

    private record CropTarget(BlockPos position, CropDefinition definition) {
    }

    private enum ReplantAttempt {
        ACCEPTED,
        REJECTED,
        INVENTORY_CHANGED
    }

    private record PendingHarvest(
            BlockPos position,
            CropDefinition definition,
            HarvestOperation operation
    ) {
    }

    private record HeldItemSelection(
            InteractionHand hand,
            InventorySelectionPlan.Source source,
            int originalSelectedSlot,
            int selectedOrSwappedSlot,
            Item plantingItem,
            ItemStack plantingSnapshot,
            ItemStack displacedSnapshot
    ) {
        static HeldItemSelection select(Minecraft client, Item item) {
            LocalPlayer player = client.player;
            Inventory inventory = player.getInventory();
            InventorySelectionPlan plan = InventorySelectionPlan.choose(
                    player.getMainHandItem().is(item),
                    player.getOffhandItem().is(item),
                    inventory.getContainerSize(),
                    slot -> inventory.getItem(slot).is(item)
            );

            return switch (plan.source()) {
                case MAIN_HAND -> simple(InteractionHand.MAIN_HAND, plan.source(), item);
                case OFF_HAND -> simple(InteractionHand.OFF_HAND, plan.source(), item);
                case HOTBAR -> selectHotbar(client, plan.slot(), item);
                case MAIN_INVENTORY -> swapFromInventory(client, plan.slot(), item);
                case NONE -> null;
            };
        }

        private static HeldItemSelection simple(
                InteractionHand hand,
                InventorySelectionPlan.Source source,
                Item item
        ) {
            return new HeldItemSelection(
                    hand,
                    source,
                    -1,
                    -1,
                    item,
                    ItemStack.EMPTY,
                    ItemStack.EMPTY
            );
        }

        private static HeldItemSelection selectHotbar(Minecraft client, int slot, Item item) {
            LocalPlayer player = client.player;
            int originalSlot = player.getInventory().getSelectedSlot();
            ItemStack plantingSnapshot = player.getInventory().getItem(slot).copy();
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            return new HeldItemSelection(
                    InteractionHand.MAIN_HAND,
                    InventorySelectionPlan.Source.HOTBAR,
                    originalSlot,
                    slot,
                    item,
                    plantingSnapshot,
                    ItemStack.EMPTY
            );
        }

        private static HeldItemSelection swapFromInventory(Minecraft client, int slot, Item item) {
            LocalPlayer player = client.player;
            Inventory inventory = player.getInventory();
            int selectedSlot = inventory.getSelectedSlot();
            ItemStack plantingSnapshot = inventory.getItem(slot).copy();
            ItemStack displacedSnapshot = inventory.getItem(selectedSlot).copy();
            client.gameMode.handleContainerInput(
                    player.containerMenu.containerId,
                    slot,
                    selectedSlot,
                    ContainerInput.SWAP,
                    player
            );
            return new HeldItemSelection(
                    InteractionHand.MAIN_HAND,
                    InventorySelectionPlan.Source.MAIN_INVENTORY,
                    selectedSlot,
                    slot,
                    item,
                    plantingSnapshot,
                    displacedSnapshot
            );
        }

        boolean restore(Minecraft client) {
            if (client.player == null || client.gameMode == null) {
                return false;
            }

            LocalPlayer player = client.player;
            Inventory inventory = player.getInventory();
            if (source == InventorySelectionPlan.Source.MAIN_INVENTORY) {
                if (!InventoryRestorePolicy.canRestoreInventorySwap(
                        inventory.getSelectedSlot(),
                        originalSelectedSlot,
                        ItemStack.matches(inventory.getItem(selectedOrSwappedSlot), displacedSnapshot),
                        isExpectedPlantingRemainder(inventory.getItem(originalSelectedSlot))
                )) {
                    return false;
                }
                client.gameMode.handleContainerInput(
                        player.containerMenu.containerId,
                        selectedOrSwappedSlot,
                        originalSelectedSlot,
                        ContainerInput.SWAP,
                        player
                );
            } else if (source == InventorySelectionPlan.Source.HOTBAR) {
                if (!InventoryRestorePolicy.canRestoreHotbar(
                        inventory.getSelectedSlot(),
                        selectedOrSwappedSlot,
                        isExpectedPlantingRemainder(inventory.getItem(selectedOrSwappedSlot))
                )) {
                    return false;
                }
                inventory.setSelectedSlot(originalSelectedSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(originalSelectedSlot));
            }
            return true;
        }

        private boolean isExpectedPlantingRemainder(ItemStack current) {
            boolean sameItemAndComponents = !current.isEmpty()
                    && current.is(plantingItem)
                    && ItemStack.isSameItemSameComponents(current, plantingSnapshot);
            return InventoryRestorePolicy.plantingRemainderIsExpected(
                    plantingSnapshot.getCount(),
                    current.getCount(),
                    sameItemAndComponents
            );
        }
    }
}
