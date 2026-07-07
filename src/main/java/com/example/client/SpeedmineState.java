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

    // Configurable Keybindings
    public static int speedmineKey = 86;      // V
    public static int fastPlaceKey = 74;      // J
    public static int farmingAssistKey = 75;  // K
    public static int bridgingKey = 66;       // B
    public static int autoFeedKey = 71;       // G

    public static void saveConfig() {
        try {
            java.io.File configDir = new java.io.File(net.minecraft.client.Minecraft.getInstance().gameDirectory, "config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            java.io.File configFile = new java.io.File(configDir, "harvester.properties");
            java.util.Properties props = new java.util.Properties();
            props.setProperty("speedmineKey", String.valueOf(speedmineKey));
            props.setProperty("fastPlaceKey", String.valueOf(fastPlaceKey));
            props.setProperty("farmingAssistKey", String.valueOf(farmingAssistKey));
            props.setProperty("bridgingKey", String.valueOf(bridgingKey));
            props.setProperty("autoFeedKey", String.valueOf(autoFeedKey));

            java.io.FileOutputStream out = new java.io.FileOutputStream(configFile);
            props.store(out, "Harvester Config");
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadConfig() {
        try {
            java.io.File configFile = new java.io.File(new java.io.File(net.minecraft.client.Minecraft.getInstance().gameDirectory, "config"), "harvester.properties");
            if (configFile.exists()) {
                java.util.Properties props = new java.util.Properties();
                java.io.FileInputStream in = new java.io.FileInputStream(configFile);
                props.load(in);
                in.close();

                speedmineKey = Integer.parseInt(props.getProperty("speedmineKey", "86"));
                fastPlaceKey = Integer.parseInt(props.getProperty("fastPlaceKey", "74"));
                farmingAssistKey = Integer.parseInt(props.getProperty("farmingAssistKey", "75"));
                bridgingKey = Integer.parseInt(props.getProperty("bridgingKey", "66"));
                autoFeedKey = Integer.parseInt(props.getProperty("autoFeedKey", "71"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
