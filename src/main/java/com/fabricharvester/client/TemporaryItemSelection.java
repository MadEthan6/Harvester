package com.fabricharvester.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** Temporarily exposes an inventory item in a hand and safely restores the prior selection. */
final class TemporaryItemSelection {
    enum Usage {
        CONSUMABLE,
        DAMAGEABLE_TOOL
    }

    private final InteractionHand hand;
    private final InventorySelectionPlan.Source source;
    private final int originalSelectedSlot;
    private final int selectedOrSwappedSlot;
    private final Item expectedItem;
    private final ItemStack selectedSnapshot;
    private final ItemStack displacedSnapshot;
    private final Usage usage;

    private TemporaryItemSelection(
            InteractionHand hand,
            InventorySelectionPlan.Source source,
            int originalSelectedSlot,
            int selectedOrSwappedSlot,
            Item expectedItem,
            ItemStack selectedSnapshot,
            ItemStack displacedSnapshot,
            Usage usage
    ) {
        this.hand = hand;
        this.source = source;
        this.originalSelectedSlot = originalSelectedSlot;
        this.selectedOrSwappedSlot = selectedOrSwappedSlot;
        this.expectedItem = expectedItem;
        this.selectedSnapshot = selectedSnapshot;
        this.displacedSnapshot = displacedSnapshot;
        this.usage = usage;
    }

    static TemporaryItemSelection selectItem(Minecraft client, Item item, Usage usage) {
        return selectMatching(client, stack -> stack.is(item), usage);
    }

    static TemporaryItemSelection selectMatching(
            Minecraft client,
            Predicate<ItemStack> matcher,
            Usage usage
    ) {
        LocalPlayer player = client.player;
        Inventory inventory = player.getInventory();
        InventorySelectionPlan plan = InventorySelectionPlan.choose(
                matcher.test(player.getMainHandItem()),
                matcher.test(player.getOffhandItem()),
                inventory.getContainerSize(),
                slot -> matcher.test(inventory.getItem(slot))
        );

        return switch (plan.source()) {
            case MAIN_HAND -> simple(player.getMainHandItem(), InteractionHand.MAIN_HAND, plan.source(), usage);
            case OFF_HAND -> simple(player.getOffhandItem(), InteractionHand.OFF_HAND, plan.source(), usage);
            case HOTBAR -> selectHotbar(client, plan.slot(), usage);
            case MAIN_INVENTORY -> swapFromInventory(client, plan.slot(), usage);
            case NONE -> null;
        };
    }

    static boolean hasMatching(LocalPlayer player, Predicate<ItemStack> matcher) {
        Inventory inventory = player.getInventory();
        return InventorySelectionPlan.choose(
                matcher.test(player.getMainHandItem()),
                matcher.test(player.getOffhandItem()),
                inventory.getContainerSize(),
                slot -> matcher.test(inventory.getItem(slot))
        ).found();
    }

    InteractionHand hand() {
        return hand;
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
                    isExpectedRemainder(inventory.getItem(originalSelectedSlot))
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
                    isExpectedRemainder(inventory.getItem(selectedOrSwappedSlot))
            )) {
                return false;
            }
            inventory.setSelectedSlot(originalSelectedSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(originalSelectedSlot));
        }
        return true;
    }

    private static TemporaryItemSelection simple(
            ItemStack stack,
            InteractionHand hand,
            InventorySelectionPlan.Source source,
            Usage usage
    ) {
        return new TemporaryItemSelection(
                hand,
                source,
                -1,
                -1,
                stack.getItem(),
                stack.copy(),
                ItemStack.EMPTY,
                usage
        );
    }

    private static TemporaryItemSelection selectHotbar(Minecraft client, int slot, Usage usage) {
        LocalPlayer player = client.player;
        Inventory inventory = player.getInventory();
        int originalSlot = inventory.getSelectedSlot();
        ItemStack selectedSnapshot = inventory.getItem(slot).copy();
        inventory.setSelectedSlot(slot);
        player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        return new TemporaryItemSelection(
                InteractionHand.MAIN_HAND,
                InventorySelectionPlan.Source.HOTBAR,
                originalSlot,
                slot,
                selectedSnapshot.getItem(),
                selectedSnapshot,
                ItemStack.EMPTY,
                usage
        );
    }

    private static TemporaryItemSelection swapFromInventory(Minecraft client, int slot, Usage usage) {
        LocalPlayer player = client.player;
        Inventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        ItemStack selectedSnapshot = inventory.getItem(slot).copy();
        ItemStack displacedSnapshot = inventory.getItem(selectedSlot).copy();
        client.gameMode.handleContainerInput(
                player.containerMenu.containerId,
                slot,
                selectedSlot,
                ContainerInput.SWAP,
                player
        );
        return new TemporaryItemSelection(
                InteractionHand.MAIN_HAND,
                InventorySelectionPlan.Source.MAIN_INVENTORY,
                selectedSlot,
                slot,
                selectedSnapshot.getItem(),
                selectedSnapshot,
                displacedSnapshot,
                usage
        );
    }

    private boolean isExpectedRemainder(ItemStack current) {
        if (usage == Usage.CONSUMABLE) {
            boolean sameItemAndComponents = !current.isEmpty()
                    && current.is(expectedItem)
                    && ItemStack.isSameItemSameComponents(current, selectedSnapshot);
            return InventoryRestorePolicy.plantingRemainderIsExpected(
                    selectedSnapshot.getCount(),
                    current.getCount(),
                    sameItemAndComponents
            );
        }

        boolean sameIgnoringDamage = false;
        if (!current.isEmpty() && current.is(expectedItem)) {
            ItemStack normalized = current.copy();
            normalized.setDamageValue(selectedSnapshot.getDamageValue());
            sameIgnoringDamage = ItemStack.isSameItemSameComponents(normalized, selectedSnapshot);
        }
        return InventoryRestorePolicy.toolRemainderIsExpected(
                selectedSnapshot.getCount(),
                current.getCount(),
                selectedSnapshot.getDamageValue(),
                current.getDamageValue(),
                selectedSnapshot.getMaxDamage(),
                sameIgnoringDamage
        );
    }
}
