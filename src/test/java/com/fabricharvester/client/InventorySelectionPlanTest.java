package com.fabricharvester.client;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySelectionPlanTest {
    @Test
    void prefersCurrentHandsWithoutInventoryMutation() {
        InventorySelectionPlan main = choose(true, true, Set.of(4, 12));
        InventorySelectionPlan off = choose(false, true, Set.of(4, 12));

        assertEquals(InventorySelectionPlan.Source.MAIN_HAND, main.source());
        assertEquals(InventorySelectionPlan.Source.OFF_HAND, off.source());
    }

    @Test
    void prefersHotbarOverMainInventory() {
        InventorySelectionPlan plan = choose(false, false, Set.of(6, 14));
        assertEquals(InventorySelectionPlan.Source.HOTBAR, plan.source());
        assertEquals(6, plan.slot());
    }

    @Test
    void fallsBackToMainInventorySwap() {
        InventorySelectionPlan plan = choose(false, false, Set.of(14));
        assertEquals(InventorySelectionPlan.Source.MAIN_INVENTORY, plan.source());
        assertEquals(14, plan.slot());
    }

    @Test
    void reportsMissingItemWithoutMutation() {
        InventorySelectionPlan plan = choose(false, false, Set.of());
        assertEquals(InventorySelectionPlan.Source.NONE, plan.source());
        assertFalse(plan.found());
    }

    @Test
    void reportsAnyUsableSelectionAsFound() {
        assertTrue(choose(false, false, Set.of(35)).found());
    }

    private static InventorySelectionPlan choose(boolean main, boolean off, Set<Integer> matchingSlots) {
        return InventorySelectionPlan.choose(main, off, 41, matchingSlots::contains);
    }
}

