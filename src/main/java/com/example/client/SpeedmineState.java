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
    public static boolean bridgingEnabled = false;
    public static boolean autoFeedEnabled = false;

    // Hardening and Anti-Cheat Evasion flags
    public static boolean hudOverlayEnabled = false;
    public static boolean blockOutlineEnabled = false;
    public static boolean stealthMode = true;

    // Configurable Keybindings
    public static int speedmineKey = 86;      // V
    public static int fastPlaceKey = 74;      // J
    public static int farmingAssistKey = 75;  // K
    public static int bridgingKey = 66;       // B
    public static int autoFeedKey = 71;       // G
    public static int hudOverlayKey = 0;      // Unbound by default
    public static int blockOutlineKey = 0;    // Unbound by default
    public static int stealthModeKey = 0;     // Unbound by default

    // Farming Assist replant queue fields
    public static net.minecraft.core.BlockPos pendingReplantPos = null;
    public static net.minecraft.world.item.Item pendingReplantSeed = null;
    public static int pendingReplantTimeout = 0;

    public static boolean isValidKeycode(int keycode) {
        return (keycode >= 32 && keycode <= 348 && keycode != 256) || keycode == 0;
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static void setSpeedIndex(int index) {
        int maxIndex = SPEED_LEVELS.length - 1;
        if (stealthMode) {
            maxIndex = 1; // Under stealthMode, only allow OFF (0) and 1.4x (1)
        }
        if (index >= 0 && index <= maxIndex) {
            speedIndex = index;
        } else {
            if (stealthMode && index > 1) {
                speedIndex = 1;
            } else {
                speedIndex = 0;
            }
        }
        enabled = (speedIndex != 0);
        multiplier = enabled ? SPEED_LEVELS[speedIndex] : 1.0f;
    }

    public static void clampSpeedIndexUnderStealth() {
        if (stealthMode && speedIndex > 1) {
            setSpeedIndex(1);
        }
    }

    public static void saveConfig() {
        try {
            java.io.File gameDir = net.minecraft.client.Minecraft.getInstance().gameDirectory.getCanonicalFile();
            java.io.File configDir = new java.io.File(gameDir, "config").getCanonicalFile();
            java.io.File configFile = new java.io.File(configDir, "harvester.properties").getCanonicalFile();

            if (!configFile.getPath().startsWith(gameDir.getPath())) {
                throw new SecurityException("Directory Traversal Attempt Blocked!");
            }

            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            java.util.Properties props = new java.util.Properties();
            props.setProperty("speedmineKey", String.valueOf(speedmineKey));
            props.setProperty("fastPlaceKey", String.valueOf(fastPlaceKey));
            props.setProperty("farmingAssistKey", String.valueOf(farmingAssistKey));
            props.setProperty("bridgingKey", String.valueOf(bridgingKey));
            props.setProperty("autoFeedKey", String.valueOf(autoFeedKey));
            props.setProperty("hudOverlayKey", String.valueOf(hudOverlayKey));
            props.setProperty("blockOutlineKey", String.valueOf(blockOutlineKey));
            props.setProperty("stealthModeKey", String.valueOf(stealthModeKey));

            props.setProperty("hudOverlayEnabled", String.valueOf(hudOverlayEnabled));
            props.setProperty("blockOutlineEnabled", String.valueOf(blockOutlineEnabled));
            props.setProperty("stealthMode", String.valueOf(stealthMode));

            try (java.io.FileOutputStream out = new java.io.FileOutputStream(configFile)) {
                props.store(out, "Harvester Config");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadConfig() {
        try {
            java.io.File gameDir = net.minecraft.client.Minecraft.getInstance().gameDirectory.getCanonicalFile();
            java.io.File configDir = new java.io.File(gameDir, "config").getCanonicalFile();
            java.io.File configFile = new java.io.File(configDir, "harvester.properties").getCanonicalFile();

            if (!configFile.getPath().startsWith(gameDir.getPath())) {
                throw new SecurityException("Directory Traversal Attempt Blocked!");
            }

            if (configFile.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream in = new java.io.FileInputStream(configFile)) {
                    props.load(in);
                }

                speedmineKey = parseIntOrDefault(props.getProperty("speedmineKey"), 86);
                fastPlaceKey = parseIntOrDefault(props.getProperty("fastPlaceKey"), 74);
                farmingAssistKey = parseIntOrDefault(props.getProperty("farmingAssistKey"), 75);
                bridgingKey = parseIntOrDefault(props.getProperty("bridgingKey"), 66);
                autoFeedKey = parseIntOrDefault(props.getProperty("autoFeedKey"), 71);
                hudOverlayKey = parseIntOrDefault(props.getProperty("hudOverlayKey"), 0);
                blockOutlineKey = parseIntOrDefault(props.getProperty("blockOutlineKey"), 0);
                stealthModeKey = parseIntOrDefault(props.getProperty("stealthModeKey"), 0);

                hudOverlayEnabled = Boolean.parseBoolean(props.getProperty("hudOverlayEnabled", "false"));
                blockOutlineEnabled = Boolean.parseBoolean(props.getProperty("blockOutlineEnabled", "false"));
                stealthMode = Boolean.parseBoolean(props.getProperty("stealthMode", "true"));

                if (!isValidKeycode(speedmineKey)) speedmineKey = 86;
                if (!isValidKeycode(fastPlaceKey)) fastPlaceKey = 74;
                if (!isValidKeycode(farmingAssistKey)) farmingAssistKey = 75;
                if (!isValidKeycode(bridgingKey)) bridgingKey = 66;
                if (!isValidKeycode(autoFeedKey)) autoFeedKey = 71;
                if (!isValidKeycode(hudOverlayKey)) hudOverlayKey = 0;
                if (!isValidKeycode(blockOutlineKey)) blockOutlineKey = 0;
                if (!isValidKeycode(stealthModeKey)) stealthModeKey = 0;

                clampSpeedIndexUnderStealth();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Cycle to the next speed mode. */
    public static void cycle() {
        int maxIndex = SPEED_LEVELS.length - 1;
        if (stealthMode) {
            maxIndex = 1;
        }
        int nextIndex = (speedIndex + 1) % (maxIndex + 1);
        setSpeedIndex(nextIndex);
    }

    /** Get a display label for the current mode. */
    public static String getLabel() {
        return SPEED_LABELS[speedIndex];
    }
}
