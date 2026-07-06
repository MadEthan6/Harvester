package com.example.client;

/**
 * Holds the current Speedmine state.
 * Pressing V cycles through speed modes:
 *   OFF → 1.4x → 1.8x → 2.2x → 2.6x → 3.0x → OFF
 */
public class SpeedmineState {
    public static final float[] SPEED_LEVELS = { 0.0f, 1.4f, 1.8f, 2.2f, 2.6f, 3.0f };
    public static final String[] SPEED_LABELS = { "OFF", "1.4x", "1.8x", "2.2x", "2.6x", "3.0x" };

    /** Current index into SPEED_LEVELS. 0 = off. */
    public static int speedIndex = 0;

    /** Convenience: true when any speed mode is active. */
    public static boolean enabled = false;

    /** The current multiplier (e.g. 1.4f). Only meaningful when enabled. */
    public static float multiplier = 1.0f;

    public static boolean fastPlaceEnabled = false;
    public static boolean farmingAssistEnabled = false;

    /** Cycle to the next speed mode. */
    public static void cycle() {
        speedIndex = (speedIndex + 1) % SPEED_LEVELS.length;
        if (speedIndex == 0) {
            enabled = false;
            multiplier = 1.0f;
        } else {
            enabled = true;
            multiplier = SPEED_LEVELS[speedIndex];
        }
    }

    /** Get a display label for the current mode. */
    public static String getLabel() {
        return SPEED_LABELS[speedIndex];
    }
}
