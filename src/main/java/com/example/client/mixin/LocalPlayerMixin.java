package com.example.client.mixin;

import com.example.client.SpeedmineState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Shadow public abstract FoodData getFoodData();
    @Shadow public abstract float getHealth();
    @Shadow public abstract boolean isUsingItem();
    @Shadow public abstract InteractionHand getUsedItemHand();
    @Shadow public abstract ItemStack getOffhandItem();
    @Shadow public abstract void startUsingItem(InteractionHand hand);
    @Shadow public abstract void swing(InteractionHand hand);
    @Shadow public abstract void stopUsingItem();

    @Unique
    private boolean speedmine_isAutoEating = false;

    @Unique
    private int speedmine_originalFoodSlot = -1;

    @Unique
    private int speedmine_tempHotbarSlot = -1;

    @Unique
    private int speedmine_autoFeedCooldown = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (speedmine_autoFeedCooldown > 0) {
            speedmine_autoFeedCooldown--;
        }

        if (!SpeedmineState.autoFeedEnabled) {
            if (speedmine_isAutoEating) {
                speedmine_stopEatingAndRestore();
            }
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;

        if (speedmine_isAutoEating) {
            // Check if eating was interrupted or completed
            if (!this.isUsingItem() || this.getUsedItemHand() != InteractionHand.OFF_HAND || !this.getOffhandItem().has(DataComponents.FOOD)) {
                speedmine_stopEatingAndRestore();
            }
        } else {
            // Check if we can search for food
            if (speedmine_autoFeedCooldown == 0 && !this.isUsingItem()) {
                int foodSlot = speedmine_findBestFoodSlot();
                if (foodSlot != -1) {
                    speedmine_startEating(foodSlot);
                } else {
                    speedmine_autoFeedCooldown = 20; // check again in 1 second
                }
            }
        }
    }

    @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
    private void onIsSlowDueToUsingItem(CallbackInfoReturnable<Boolean> cir) {
        if (SpeedmineState.autoFeedEnabled && speedmine_isAutoEating) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void onItemUseSpeedMultiplier(CallbackInfoReturnable<Float> cir) {
        if (SpeedmineState.autoFeedEnabled && speedmine_isAutoEating) {
            cir.setReturnValue(1.0F);
        }
    }

    @Unique
    private int speedmine_findBestFoodSlot() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return -1;

        FoodData foodData = this.getFoodData();
        int currentHunger = foodData.getFoodLevel();
        int hungerDeficit = 20 - currentHunger;
        float health = this.getHealth();

        int bestSlot = -1;
        int bestScore = -1000;

        // Check offhand first (marker 45)
        ItemStack offhandStack = this.getOffhandItem();
        if (!offhandStack.isEmpty() && offhandStack.has(DataComponents.FOOD)) {
            int score = speedmine_scoreFood(offhandStack, hungerDeficit, health);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = 45;
            }
        }

        // Check main inventory (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                int score = speedmine_scoreFood(stack, hungerDeficit, health);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }

        return (bestScore > 0) ? bestSlot : -1;
    }

    @Unique
    private int speedmine_scoreFood(ItemStack stack, int deficit, float health) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return -1000;

        // Priority 1: Golden Apples at critical health
        boolean isGoldenApple = stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
        if (health < 12.0f && isGoldenApple) {
            return 10000;
        }

        int nutrition = food.nutrition();

        if (health >= 20.0f) {
            // Full health: strictly no waste
            if (deficit >= nutrition) {
                return nutrition * 10;
            } else {
                return -100;
            }
        } else {
            // Injured: want to regenerate health
            if (deficit == 0) {
                if (food.canAlwaysEat()) {
                    return 50; // eat golden apple even if full to heal
                }
                return -100;
            }
            if (deficit >= nutrition) {
                return nutrition * 10;
            } else {
                // minor waste is tolerated to heal, prefer smaller foods to minimize waste
                return 100 - (nutrition - deficit);
            }
        }
    }

    @Unique
    private void speedmine_startEating(int foodSlot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;

        int originalSlot = player.getInventory().getSelectedSlot();

        if (foodSlot == 45) {
            // Food is already in offhand, just eat it
            InteractionResult result = mc.gameMode.useItem(player, InteractionHand.OFF_HAND);
            if (result.consumesAction()) {
                this.startUsingItem(InteractionHand.OFF_HAND);
                this.swing(InteractionHand.OFF_HAND);
                mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.OFF_HAND);
                speedmine_isAutoEating = true;
                speedmine_originalFoodSlot = -1;
                speedmine_tempHotbarSlot = -1;
            } else {
                speedmine_autoFeedCooldown = 40;
            }
        } else {
            // Swap to offhand via a temporary hotbar slot
            int tempSlot = (originalSlot == 8) ? 7 : 8;

            // 1. Swap food from inventory to tempSlot if not already in hotbar
            if (foodSlot >= 9) {
                mc.gameMode.handleContainerInput(
                    player.containerMenu.containerId,
                    foodSlot,
                    tempSlot,
                    ContainerInput.SWAP,
                    player
                );
            } else {
                tempSlot = foodSlot; // already in hotbar, use it directly
            }

            // 2. Swap tempSlot with offhand
            player.getInventory().setSelectedSlot(tempSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(tempSlot));

            player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO,
                Direction.DOWN
            ));

            player.getInventory().setSelectedSlot(originalSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));

            // 3. Initiate offhand usage
            InteractionResult result = mc.gameMode.useItem(player, InteractionHand.OFF_HAND);
            if (result.consumesAction()) {
                this.startUsingItem(InteractionHand.OFF_HAND);
                this.swing(InteractionHand.OFF_HAND);
                mc.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.OFF_HAND);

                speedmine_isAutoEating = true;
                speedmine_originalFoodSlot = foodSlot;
                speedmine_tempHotbarSlot = tempSlot;
            } else {
                // Swap back immediately if failed to start eating
                player.getInventory().setSelectedSlot(tempSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(tempSlot));

                player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ZERO,
                    Direction.DOWN
                ));

                player.getInventory().setSelectedSlot(originalSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));

                if (foodSlot >= 9) {
                    mc.gameMode.handleContainerInput(
                        player.containerMenu.containerId,
                        foodSlot,
                        tempSlot,
                        ContainerInput.SWAP,
                        player
                    );
                }
                speedmine_autoFeedCooldown = 40;
            }
        }
    }

    @Unique
    private void speedmine_stopEatingAndRestore() {
        if (!speedmine_isAutoEating) return;
        speedmine_isAutoEating = false;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;

        if (this.isUsingItem() && this.getUsedItemHand() == InteractionHand.OFF_HAND) {
            this.stopUsingItem();
            player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                Direction.DOWN
            ));
        }

        if (speedmine_tempHotbarSlot != -1) {
            int originalSlot = player.getInventory().getSelectedSlot();

            // 1. Swap offhand back with tempSlot
            player.getInventory().setSelectedSlot(speedmine_tempHotbarSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(speedmine_tempHotbarSlot));

            player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO,
                Direction.DOWN
            ));

            player.getInventory().setSelectedSlot(originalSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));

            // 2. Swap back from tempSlot to original main inventory slot
            if (speedmine_originalFoodSlot >= 9) {
                mc.gameMode.handleContainerInput(
                    player.containerMenu.containerId,
                    speedmine_originalFoodSlot,
                    speedmine_tempHotbarSlot,
                    ContainerInput.SWAP,
                    player
                );
            }
        }

        speedmine_originalFoodSlot = -1;
        speedmine_tempHotbarSlot = -1;
        speedmine_autoFeedCooldown = 10; // small delay before next eat attempt
    }
}
