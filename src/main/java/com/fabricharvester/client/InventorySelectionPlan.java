package com.fabricharvester.client;

import java.util.function.IntPredicate;

public record InventorySelectionPlan(Source source, int slot) {
    public enum Source {
        MAIN_HAND,
        OFF_HAND,
        HOTBAR,
        MAIN_INVENTORY,
        NONE
    }

    public static InventorySelectionPlan choose(
            boolean mainHandMatches,
            boolean offHandMatches,
            int inventorySize,
            IntPredicate slotMatches
    ) {
        if (mainHandMatches) {
            return new InventorySelectionPlan(Source.MAIN_HAND, -1);
        }
        if (offHandMatches) {
            return new InventorySelectionPlan(Source.OFF_HAND, -1);
        }

        int hotbarEnd = Math.min(9, inventorySize);
        for (int slot = 0; slot < hotbarEnd; slot++) {
            if (slotMatches.test(slot)) {
                return new InventorySelectionPlan(Source.HOTBAR, slot);
            }
        }

        int inventoryEnd = Math.min(36, inventorySize);
        for (int slot = 9; slot < inventoryEnd; slot++) {
            if (slotMatches.test(slot)) {
                return new InventorySelectionPlan(Source.MAIN_INVENTORY, slot);
            }
        }
        return new InventorySelectionPlan(Source.NONE, -1);
    }

    public boolean found() {
        return source != Source.NONE;
    }
}

