package com.fabricharvester.client;

/** Pure validation for deciding whether a temporary inventory mutation is safe to undo. */
public final class InventoryRestorePolicy {
    private InventoryRestorePolicy() {
    }

    public static boolean plantingRemainderIsExpected(
            int originalCount,
            int currentCount,
            boolean sameItemAndComponents
    ) {
        if (originalCount < 1 || currentCount < 0) {
            return false;
        }
        if (currentCount == 0) {
            return originalCount == 1;
        }
        return sameItemAndComponents && currentCount <= originalCount;
    }

    public static boolean canRestoreHotbar(
            int currentSelectedSlot,
            int temporarySlot,
            boolean plantingRemainderExpected
    ) {
        return currentSelectedSlot == temporarySlot && plantingRemainderExpected;
    }

    public static boolean canRestoreInventorySwap(
            int currentSelectedSlot,
            int originalSelectedSlot,
            boolean displacedStackUnchanged,
            boolean plantingRemainderExpected
    ) {
        return currentSelectedSlot == originalSelectedSlot
                && displacedStackUnchanged
                && plantingRemainderExpected;
    }
}
