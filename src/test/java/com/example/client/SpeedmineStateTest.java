package com.example.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SpeedmineStateTest {

    @Test
    public void testIsValidKeycode() {
        // Valid ranges [32, 348] or 0. ESC (256) excluded.
        assertTrue(SpeedmineState.isValidKeycode(0));
        assertTrue(SpeedmineState.isValidKeycode(32));
        assertTrue(SpeedmineState.isValidKeycode(86));
        assertTrue(SpeedmineState.isValidKeycode(348));

        assertFalse(SpeedmineState.isValidKeycode(256)); // ESC
        assertFalse(SpeedmineState.isValidKeycode(10));  // below 32
        assertFalse(SpeedmineState.isValidKeycode(400)); // above 348
        assertFalse(SpeedmineState.isValidKeycode(-1));
    }

    @Test
    public void testSpeedIndexClampingUnderStealth() {
        // Test stealth mode enabled
        SpeedmineState.stealthMode = true;
        SpeedmineState.speedIndex = 0;
        
        // Setting higher index should clamp/lock to 1
        SpeedmineState.setSpeedIndex(3);
        assertEquals(1, SpeedmineState.speedIndex);
        assertTrue(SpeedmineState.enabled);
        assertEquals(1.4f, SpeedmineState.multiplier);

        // Cycle should toggle between 0 and 1
        SpeedmineState.cycle(); // from 1 to 0
        assertEquals(0, SpeedmineState.speedIndex);
        assertFalse(SpeedmineState.enabled);

        SpeedmineState.cycle(); // from 0 to 1
        assertEquals(1, SpeedmineState.speedIndex);

        // Test stealth mode disabled
        SpeedmineState.stealthMode = false;
        SpeedmineState.setSpeedIndex(3);
        assertEquals(3, SpeedmineState.speedIndex);
        assertEquals(2.2f, SpeedmineState.multiplier);

        SpeedmineState.cycle(); // from 3 to 4
        assertEquals(4, SpeedmineState.speedIndex);
        assertEquals(2.6f, SpeedmineState.multiplier);
    }

    @Test
    public void testFarmingAssistQueueOverwrites() {
        net.minecraft.core.BlockPos pos1 = new net.minecraft.core.BlockPos(1, 2, 3);
        net.minecraft.core.BlockPos pos2 = new net.minecraft.core.BlockPos(4, 5, 6);

        // Queue first replant (avoiding any reference to the pendingReplantSeed field to prevent Item classloading)
        SpeedmineState.pendingReplantPos = pos1;
        SpeedmineState.pendingReplantTimeout = 10;

        assertEquals(pos1, SpeedmineState.pendingReplantPos);
        assertEquals(10, SpeedmineState.pendingReplantTimeout);

        // Queue second replant before the first is processed
        SpeedmineState.pendingReplantPos = pos2;
        SpeedmineState.pendingReplantTimeout = 10;

        // The first queue is completely overwritten and lost
        assertEquals(pos2, SpeedmineState.pendingReplantPos);
        assertEquals(10, SpeedmineState.pendingReplantTimeout);
    }

    @Test
    public void testAutoBridgeCardinalProjections() {
        // Test pure X axis movement
        assertEquals(1, getProjectedDx(0.5, 0.0));
        assertEquals(0, getProjectedDz(0.5, 0.0));

        assertEquals(-1, getProjectedDx(-0.5, 0.0));
        assertEquals(0, getProjectedDz(-0.5, 0.0));

        // Test pure Z axis movement
        assertEquals(0, getProjectedDx(0.0, 0.5));
        assertEquals(1, getProjectedDz(0.0, 0.5));

        assertEquals(0, getProjectedDx(0.0, -0.5));
        assertEquals(-1, getProjectedDz(0.0, -0.5));

        // Test mostly X axis movement (diagonal)
        assertEquals(1, getProjectedDx(0.5, 0.2));
        assertEquals(0, getProjectedDz(0.5, 0.2));

        // Test mostly Z axis movement (diagonal)
        assertEquals(0, getProjectedDx(0.2, 0.5));
        assertEquals(1, getProjectedDz(0.2, 0.5));

        // Test exact diagonal movement (should fallback to Z projection)
        assertEquals(0, getProjectedDx(0.5, 0.5));
        assertEquals(1, getProjectedDz(0.5, 0.5));

        assertEquals(0, getProjectedDx(-0.5, 0.5));
        assertEquals(1, getProjectedDz(-0.5, 0.5));

        assertEquals(0, getProjectedDx(0.5, -0.5));
        assertEquals(-1, getProjectedDz(0.5, -0.5));
    }

    private int getProjectedDx(double deltaX, double deltaZ) {
        if (Math.abs(deltaX) > Math.abs(deltaZ)) {
            return deltaX > 0 ? 1 : -1;
        } else {
            return 0;
        }
    }

    private int getProjectedDz(double deltaX, double deltaZ) {
        if (Math.abs(deltaX) > Math.abs(deltaZ)) {
            return 0;
        } else {
            return deltaZ > 0 ? 1 : -1;
        }
    }
}
