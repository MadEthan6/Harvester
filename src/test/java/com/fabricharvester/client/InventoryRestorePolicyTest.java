package com.fabricharvester.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryRestorePolicyTest {
    @Test
    void acceptsNormalUseAndSingleItemDepletion() {
        assertTrue(InventoryRestorePolicy.plantingRemainderIsExpected(8, 7, true));
        assertTrue(InventoryRestorePolicy.plantingRemainderIsExpected(1, 0, false));
    }

    @Test
    void rejectsReplacementIncreaseAndUnexpectedEmptyStack() {
        assertFalse(InventoryRestorePolicy.plantingRemainderIsExpected(8, 7, false));
        assertFalse(InventoryRestorePolicy.plantingRemainderIsExpected(8, 9, true));
        assertFalse(InventoryRestorePolicy.plantingRemainderIsExpected(8, 0, false));
    }

    @Test
    void hotbarRestoreRequiresTemporarySlotToRemainSelected() {
        assertTrue(InventoryRestorePolicy.canRestoreHotbar(4, 4, true));
        assertFalse(InventoryRestorePolicy.canRestoreHotbar(2, 4, true));
    }

    @Test
    void inventorySwapRejectsMidOperationChanges() {
        assertTrue(InventoryRestorePolicy.canRestoreInventorySwap(2, 2, true, true));
        assertFalse(InventoryRestorePolicy.canRestoreInventorySwap(2, 2, false, true));
        assertFalse(InventoryRestorePolicy.canRestoreInventorySwap(3, 2, true, true));
    }
}
