package com.fabricharvester.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FarmingRangeTest {
    @Test
    void scansThePlayersFullNormalInteractionArea() {
        assertEquals(4, HarvesterManager.HORIZONTAL_RADIUS);
        assertEquals(2, HarvesterManager.VERTICAL_RADIUS);
    }
}
