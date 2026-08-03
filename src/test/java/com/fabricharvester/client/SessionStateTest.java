package com.fabricharvester.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionStateTest {
    @AfterEach
    void reset() {
        ModKeyBindings.resetStates();
    }

    @Test
    void resetTurnsOffEveryAutomationFeature() {
        ModKeyBindings.setFastBreakEnabled(true);
        ModKeyBindings.setFastPlaceEnabled(true);
        ModKeyBindings.setHarvestActive(true);
        ModKeyBindings.setBoneMealEnabled(true);

        ModKeyBindings.resetStates();

        assertFalse(ModKeyBindings.isFastBreakEnabled());
        assertFalse(ModKeyBindings.isFastPlaceEnabled());
        assertFalse(ModKeyBindings.isHarvestActive());
        assertFalse(ModKeyBindings.isBoneMealEnabled());
    }

    @Test
    void farmingAndBoneMealRemainIndependentSettings() {
        ModKeyBindings.setHarvestActive(true);
        ModKeyBindings.setBoneMealEnabled(false);

        assertTrue(ModKeyBindings.isHarvestActive());
        assertFalse(ModKeyBindings.isBoneMealEnabled());

        ModKeyBindings.setHarvestActive(false);
        ModKeyBindings.setBoneMealEnabled(true);

        assertFalse(ModKeyBindings.isHarvestActive());
        assertTrue(ModKeyBindings.isBoneMealEnabled());
    }
}
