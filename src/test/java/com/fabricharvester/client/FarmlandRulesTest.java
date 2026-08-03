package com.fabricharvester.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmlandRulesTest {
    @Test
    void tillsDirtWithExactlyTwoOrThreeAdjacentFarmlandBlocks() {
        assertTrue(FarmlandRules.shouldTill(true, true, 2));
        assertTrue(FarmlandRules.shouldTill(true, true, 3));
    }

    @Test
    void rejectsOtherNeighborCountsAndBlockedOrNonDirtTargets() {
        assertFalse(FarmlandRules.shouldTill(true, true, 1));
        assertFalse(FarmlandRules.shouldTill(true, true, 4));
        assertFalse(FarmlandRules.shouldTill(false, true, 2));
        assertFalse(FarmlandRules.shouldTill(true, false, 2));
    }
}
