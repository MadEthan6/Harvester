package com.fabricharvester.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutomationProfileTest {
    @Test
    void safeProfileUsesConservativeLimits() {
        assertEquals(4, AutomationProfile.SAFE.harvestIntervalTicks());
        assertEquals(1, AutomationProfile.SAFE.blockBreakCooldown());
        assertEquals(1, AutomationProfile.SAFE.itemUseCooldown());
        assertEquals(2, AutomationProfile.SAFE.retryBackoffTicks());
    }

    @Test
    void trustedProfileUsesFastLimits() {
        assertEquals(1, AutomationProfile.TRUSTED.harvestIntervalTicks());
        assertEquals(0, AutomationProfile.TRUSTED.blockBreakCooldown());
        assertEquals(0, AutomationProfile.TRUSTED.itemUseCooldown());
        assertEquals(1, AutomationProfile.TRUSTED.retryBackoffTicks());
    }

    @Test
    void profileCyclingIsStable() {
        assertEquals(AutomationProfile.TRUSTED, AutomationProfile.SAFE.next());
        assertEquals(AutomationProfile.SAFE, AutomationProfile.TRUSTED.next());
    }

    @Test
    void connectionTypeSelectsTheSafeDefault() {
        assertEquals(AutomationProfile.TRUSTED, AutomationProfile.forConnection(true));
        assertEquals(AutomationProfile.SAFE, AutomationProfile.forConnection(false));
    }
}
