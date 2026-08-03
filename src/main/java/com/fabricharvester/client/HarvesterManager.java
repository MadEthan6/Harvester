package com.fabricharvester.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Runs at most one acknowledged farming operation at a time. */
public final class HarvesterManager {
    static final int HORIZONTAL_RADIUS = 4;
    static final int VERTICAL_RADIUS = 2;
    private static final int WARNING_COOLDOWN_TICKS = 40;
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };
    private static final Predicate<ItemStack> HOE_MATCHER = stack -> stack.is(ItemTags.HOES);

    private final HarvestScheduler scheduler = new HarvestScheduler();
    private PendingHarvest pendingHarvest;
    private PendingFarmAction pendingAction;
    private int warningCooldown;

    public void tick(
            Minecraft client,
            boolean farmingEnabled,
            boolean boneMealEnabled,
            AutomationProfile profile
    ) {
        if (warningCooldown > 0) {
            warningCooldown--;
        }
        scheduler.tick();
        if (!isReady(client)) {
            return;
        }

        if (pendingHarvest != null) {
            tickPendingHarvest(client);
            return;
        }
        if (pendingAction != null) {
            tickPendingAction(client);
            return;
        }
        if (!scheduler.canStart(farmingEnabled || boneMealEnabled, false)) {
            return;
        }

        if (farmingEnabled && tryStartHarvest(client, profile)) {
            return;
        }
        if (farmingEnabled && tryStartPlanting(client, profile)) {
            return;
        }
        if (farmingEnabled && tryStartTilling(client, profile)) {
            return;
        }
        if (boneMealEnabled) {
            tryStartBoneMeal(client, profile);
        }
    }

    public void cancel() {
        if (pendingHarvest != null) {
            pendingHarvest.operation().cancel();
            pendingHarvest = null;
        }
        if (pendingAction != null) {
            pendingAction.operation().cancel();
            pendingAction = null;
        }
        scheduler.reset();
        warningCooldown = 0;
    }

    public boolean hasPendingOperation() {
        return pendingHarvest != null || pendingAction != null;
    }

    public boolean hasPendingFarmingOperation() {
        return pendingHarvest != null
                || (pendingAction != null && pendingAction.type() != FarmActionType.BONE_MEAL);
    }

    public boolean hasPendingBoneMealOperation() {
        return pendingAction != null && pendingAction.type() == FarmActionType.BONE_MEAL;
    }

    public HarvestOperation.State operationState() {
        return pendingHarvest == null ? null : pendingHarvest.operation().state();
    }

    private boolean tryStartHarvest(Minecraft client, AutomationProfile profile) {
        CropTarget target = findLookedAtMatureCrop(client);
        if (target == null) {
            target = findNearestMatureCrop(client);
        }
        if (target == null) {
            return false;
        }
        if (!hasItem(client.player, target.definition().plantingItem())) {
            warn(client, "warning.fabric_harvester.missing_seeds");
            return false;
        }

        scheduler.onStarted(profile);
        if (!sendBreakRequest(client, target.position())) {
            warn(client, "warning.fabric_harvester.break_rejected");
            return true;
        }

        client.player.swing(InteractionHand.MAIN_HAND);
        pendingHarvest = new PendingHarvest(
                target.position(),
                target.definition(),
                new HarvestOperation(profile.retryBackoffTicks())
        );
        return true;
    }

    private static boolean sendBreakRequest(Minecraft client, BlockPos position) {
        BlockState state = client.level.getBlockState(position);
        if (state.isAir()
                || !client.level.getWorldBorder().isWithinBounds(position)
                || client.player.blockActionRestricted(
                        client.level,
                        position,
                        client.gameMode.getPlayerMode()
                )
                || !client.player.getMainHandItem().canDestroyBlock(
                        state,
                        client.level,
                        position,
                        client.player
                )) {
            return false;
        }

        client.player.connection.send(CropBreakRequest.create(position));
        return true;
    }

    private boolean tryStartPlanting(Minecraft client, AutomationProfile profile) {
        BlockPos cropPos = findNearestEmptyFarmland(client);
        if (cropPos == null) {
            return false;
        }
        CropDefinition definition = firstAvailableCrop(client.player);
        if (definition == null) {
            warn(client, "warning.fabric_harvester.missing_seeds");
            return false;
        }

        TemporaryItemSelection selection = TemporaryItemSelection.selectItem(
                client,
                definition.plantingItem(),
                TemporaryItemSelection.Usage.CONSUMABLE
        );
        scheduler.onStarted(profile);
        InteractionAttempt attempt = useSelectedItemOn(
                client,
                selection,
                new BlockHitResult(
                        Vec3.atBottomCenterOf(cropPos),
                        Direction.UP,
                        cropPos.below(),
                        false
                )
        );
        if (!handleInteractionFailure(client, attempt)) {
            pendingAction = PendingFarmAction.plant(cropPos, definition);
        }
        return true;
    }

    private boolean tryStartTilling(Minecraft client, AutomationProfile profile) {
        BlockPos dirtPos = findNearestTillableDirt(client);
        if (dirtPos == null) {
            return false;
        }
        if (!TemporaryItemSelection.hasMatching(client.player, HOE_MATCHER)) {
            warn(client, "warning.fabric_harvester.missing_hoe");
            return false;
        }

        TemporaryItemSelection selection = TemporaryItemSelection.selectMatching(
                client,
                HOE_MATCHER,
                TemporaryItemSelection.Usage.DAMAGEABLE_TOOL
        );
        scheduler.onStarted(profile);
        InteractionAttempt attempt = useSelectedItemOn(
                client,
                selection,
                new BlockHitResult(Vec3.atBottomCenterOf(dirtPos.above()), Direction.UP, dirtPos, false)
        );
        if (!handleInteractionFailure(client, attempt)) {
            pendingAction = PendingFarmAction.till(dirtPos);
        }
        return true;
    }

    private boolean tryStartBoneMeal(Minecraft client, AutomationProfile profile) {
        CropTarget target = findNearestImmatureCrop(client);
        if (target == null) {
            return false;
        }
        if (!hasItem(client.player, Items.BONE_MEAL)) {
            warn(client, "warning.fabric_harvester.missing_bone_meal");
            scheduler.onStarted(profile);
            return true;
        }

        TemporaryItemSelection selection = TemporaryItemSelection.selectItem(
                client,
                Items.BONE_MEAL,
                TemporaryItemSelection.Usage.CONSUMABLE
        );
        scheduler.onStarted(profile);
        InteractionAttempt attempt = useSelectedItemOn(
                client,
                selection,
                new BlockHitResult(
                        Vec3.atCenterOf(target.position()),
                        Direction.UP,
                        target.position(),
                        false
                )
        );
        if (!handleInteractionFailure(client, attempt)) {
            pendingAction = PendingFarmAction.boneMeal(
                    target.position(),
                    target.definition(),
                    target.definition().age(client.level.getBlockState(target.position()))
            );
        }
        return true;
    }

    private void tickPendingHarvest(Minecraft client) {
        BlockPos cropPos = pendingHarvest.position();
        BlockPos farmlandPos = cropPos.below();
        if (!isWithinReach(client, cropPos)) {
            failPendingHarvest(client, "warning.fabric_harvester.out_of_reach");
            return;
        }
        if (!client.level.getBlockState(farmlandPos).is(Blocks.FARMLAND)) {
            failPendingHarvest(client, "warning.fabric_harvester.farmland_missing");
            return;
        }

        BlockState currentState = client.level.getBlockState(cropPos);
        boolean cropRemoved = !currentState.is(pendingHarvest.definition().cropBlock());
        boolean cropReplanted = pendingHarvest.definition().isReplanted(currentState);
        HarvestOperation.Step step = pendingHarvest.operation().tick(cropRemoved, cropReplanted);
        handleHarvestStep(client, step);
    }

    private void handleHarvestStep(Minecraft client, HarvestOperation.Step step) {
        switch (step) {
            case ATTEMPT_REPLANT -> {
                InteractionAttempt attempt = attemptReplant(client, pendingHarvest);
                if (attempt == InteractionAttempt.INVENTORY_CHANGED) {
                    failPendingHarvest(client, "warning.fabric_harvester.inventory_changed");
                    return;
                }
                if (attempt == InteractionAttempt.MISSING_ITEM) {
                    warn(client, "warning.fabric_harvester.missing_seeds");
                }
                HarvestOperation.Step result = pendingHarvest.operation().onReplantAttempt(
                        attempt == InteractionAttempt.SENT
                );
                if (result != HarvestOperation.Step.NONE) {
                    handleHarvestStep(client, result);
                }
            }
            case COMPLETE -> pendingHarvest = null;
            case BREAK_TIMEOUT -> failPendingHarvest(client, "warning.fabric_harvester.break_timeout");
            case REPLANT_TIMEOUT -> failPendingHarvest(client, "warning.fabric_harvester.replant_timeout");
            case NONE -> {
            }
        }
    }

    private InteractionAttempt attemptReplant(Minecraft client, PendingHarvest harvest) {
        if (!client.level.getBlockState(harvest.position()).isAir()) {
            return InteractionAttempt.REJECTED;
        }
        TemporaryItemSelection selection = TemporaryItemSelection.selectItem(
                client,
                harvest.definition().plantingItem(),
                TemporaryItemSelection.Usage.CONSUMABLE
        );
        if (selection == null) {
            return InteractionAttempt.MISSING_ITEM;
        }
        return useSelectedItemOn(
                client,
                selection,
                new BlockHitResult(
                        Vec3.atBottomCenterOf(harvest.position()),
                        Direction.UP,
                        harvest.position().below(),
                        false
                )
        );
    }

    private void tickPendingAction(Minecraft client) {
        if (!isWithinReach(client, pendingAction.position())) {
            failPendingAction(client, "warning.fabric_harvester.out_of_reach");
            return;
        }
        if (pendingAction.type() == FarmActionType.PLANT
                && !client.level.getBlockState(pendingAction.position().below()).is(Blocks.FARMLAND)) {
            failPendingAction(client, "warning.fabric_harvester.farmland_missing");
            return;
        }

        BlockState state = client.level.getBlockState(pendingAction.position());
        FarmActionOperation.Step step = pendingAction.operation().tick(pendingAction.confirmed(state));
        if (step == FarmActionOperation.Step.COMPLETE) {
            pendingAction = null;
        } else if (step == FarmActionOperation.Step.TIMEOUT) {
            String warningKey = switch (pendingAction.type()) {
                case PLANT -> "warning.fabric_harvester.plant_timeout";
                case TILL -> "warning.fabric_harvester.till_timeout";
                case BONE_MEAL -> "warning.fabric_harvester.bone_meal_timeout";
            };
            failPendingAction(client, warningKey);
        }
    }

    private InteractionAttempt useSelectedItemOn(
            Minecraft client,
            TemporaryItemSelection selection,
            BlockHitResult hitResult
    ) {
        if (selection == null) {
            return InteractionAttempt.MISSING_ITEM;
        }

        boolean sent;
        boolean restored;
        try {
            if (!client.level.getWorldBorder().isWithinBounds(hitResult.getBlockPos())) {
                sent = false;
            } else {
                client.player.connection.send(FarmUseRequest.create(selection.hand(), hitResult));
                client.player.swing(selection.hand());
                sent = true;
            }
        } finally {
            restored = selection.restore(client);
        }
        if (!restored) {
            return InteractionAttempt.INVENTORY_CHANGED;
        }
        return sent ? InteractionAttempt.SENT : InteractionAttempt.REJECTED;
    }

    private boolean handleInteractionFailure(Minecraft client, InteractionAttempt attempt) {
        return switch (attempt) {
            case SENT -> false;
            case INVENTORY_CHANGED -> {
                warn(client, "warning.fabric_harvester.inventory_changed");
                yield true;
            }
            case MISSING_ITEM -> {
                warn(client, "warning.fabric_harvester.item_missing");
                yield true;
            }
            case REJECTED -> {
                warn(client, "warning.fabric_harvester.action_rejected");
                yield true;
            }
        };
    }

    private CropTarget findNearestMatureCrop(Minecraft client) {
        return findNearest(client, pos -> {
            BlockState state = client.level.getBlockState(pos);
            CropDefinition definition = CropRegistry.findMature(state).orElse(null);
            return definition == null ? null : new CropTarget(pos.immutable(), definition);
        });
    }

    private CropTarget findLookedAtMatureCrop(Minecraft client) {
        boolean targetingCrop = client.options.keyUse.isDown() || client.options.keyAttack.isDown();
        if (!targetingCrop || !(client.hitResult instanceof BlockHitResult hitResult)) {
            return null;
        }

        BlockPos position = hitResult.getBlockPos();
        if (!isWithinReach(client, position)) {
            return null;
        }
        BlockState state = client.level.getBlockState(position);
        CropDefinition definition = CropRegistry.findMature(state).orElse(null);
        return definition == null ? null : new CropTarget(position.immutable(), definition);
    }

    private CropTarget findNearestImmatureCrop(Minecraft client) {
        return findNearest(client, pos -> {
            BlockState state = client.level.getBlockState(pos);
            CropDefinition definition = CropRegistry.find(state.getBlock())
                    .filter(crop -> !crop.isMature(state))
                    .orElse(null);
            return definition == null ? null : new CropTarget(pos.immutable(), definition);
        });
    }

    private BlockPos findNearestEmptyFarmland(Minecraft client) {
        return findNearest(client, pos -> client.level.getBlockState(pos).isAir()
                && client.level.getBlockState(pos.below()).is(Blocks.FARMLAND)
                ? pos.immutable()
                : null);
    }

    private BlockPos findNearestTillableDirt(Minecraft client) {
        return findNearest(client, pos -> FarmlandRules.shouldTill(
                client.level.getBlockState(pos).is(Blocks.DIRT),
                client.level.getBlockState(pos.above()).isAir(),
                adjacentFarmland(client, pos)
        ) ? pos.immutable() : null);
    }

    private <T> T findNearest(Minecraft client, Function<BlockPos, T> classifier) {
        BlockPos origin = client.player.blockPosition();
        List<CropCandidateSelector.Candidate<T>> candidates = new ArrayList<>();
        for (int y = -VERTICAL_RADIUS; y <= VERTICAL_RADIUS; y++) {
            for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
                for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    T value = classifier.apply(candidate);
                    candidates.add(new CropCandidateSelector.Candidate<>(
                            value,
                            value != null,
                            isWithinReach(client, candidate),
                            client.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(candidate))
                    ));
                }
            }
        }
        return CropCandidateSelector.nearestUsable(candidates).orElse(null);
    }

    private static int adjacentFarmland(Minecraft client, BlockPos pos) {
        int count = 0;
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            if (client.level.getBlockState(pos.relative(direction)).is(Blocks.FARMLAND)) {
                count++;
            }
        }
        return count;
    }

    private static CropDefinition firstAvailableCrop(LocalPlayer player) {
        return CropRegistry.definitions().stream()
                .filter(definition -> hasItem(player, definition.plantingItem()))
                .findFirst()
                .orElse(null);
    }

    private static boolean hasItem(LocalPlayer player, Item item) {
        return TemporaryItemSelection.hasMatching(player, stack -> stack.is(item));
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

    private void failPendingHarvest(Minecraft client, String translationKey) {
        warn(client, translationKey);
        if (pendingHarvest != null) {
            pendingHarvest.operation().cancel();
        }
        pendingHarvest = null;
    }

    private void failPendingAction(Minecraft client, String translationKey) {
        warn(client, translationKey);
        if (pendingAction != null) {
            pendingAction.operation().cancel();
        }
        pendingAction = null;
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

    private enum InteractionAttempt {
        SENT,
        REJECTED,
        INVENTORY_CHANGED,
        MISSING_ITEM
    }

    private enum FarmActionType {
        PLANT,
        TILL,
        BONE_MEAL
    }

    private record PendingHarvest(
            BlockPos position,
            CropDefinition definition,
            HarvestOperation operation
    ) {
    }

    private record PendingFarmAction(
            FarmActionType type,
            BlockPos position,
            CropDefinition definition,
            int originalAge,
            FarmActionOperation operation
    ) {
        static PendingFarmAction plant(BlockPos position, CropDefinition definition) {
            return new PendingFarmAction(
                    FarmActionType.PLANT,
                    position,
                    definition,
                    -1,
                    new FarmActionOperation()
            );
        }

        static PendingFarmAction till(BlockPos position) {
            return new PendingFarmAction(
                    FarmActionType.TILL,
                    position,
                    null,
                    -1,
                    new FarmActionOperation()
            );
        }

        static PendingFarmAction boneMeal(BlockPos position, CropDefinition definition, int originalAge) {
            return new PendingFarmAction(
                    FarmActionType.BONE_MEAL,
                    position,
                    definition,
                    originalAge,
                    new FarmActionOperation()
            );
        }

        boolean confirmed(BlockState state) {
            return switch (type) {
                case PLANT -> state.is(definition.cropBlock());
                case TILL -> state.is(Blocks.FARMLAND);
                case BONE_MEAL -> definition.age(state) > originalAge;
            };
        }
    }
}
