package com.fabricharvester.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarvestSchedulerTest {
    @Test
    void safeProfileAllowsNoMoreThanOneStartPerEightTicks() {
        HarvestScheduler scheduler = new HarvestScheduler();
        assertTrue(scheduler.canStart(true, false));
        scheduler.onStarted(AutomationProfile.SAFE);

        for (int tick = 0; tick < 7; tick++) {
            scheduler.tick();
            assertFalse(scheduler.canStart(true, false));
        }
        scheduler.tick();
        assertTrue(scheduler.canStart(true, false));
    }

    @Test
    void trustedProfileAllowsOneStartEveryTwoTicks() {
        HarvestScheduler scheduler = new HarvestScheduler();
        scheduler.onStarted(AutomationProfile.TRUSTED);
        scheduler.tick();
        assertFalse(scheduler.canStart(true, false));
        scheduler.tick();
        assertTrue(scheduler.canStart(true, false));
    }

    @Test
    void operationInFlightAlwaysBlocksAnotherStart() {
        HarvestScheduler scheduler = new HarvestScheduler();
        assertFalse(scheduler.canStart(true, true));
        assertFalse(scheduler.canStart(false, false));
    }
}
