package com.fabricharvester.e2e.verify;

import com.fabricharvester.client.ModKeyBindings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class KeyBindingsVerifyTest {

    @BeforeEach
    public void setUp() {
        ModKeyBindings.setFastBreakEnabled(false);
        ModKeyBindings.setFastPlaceEnabled(false);
        ModKeyBindings.setHarvestActive(false);
    }

    @Test
    public void testKeyBindingConstants() {
        assertEquals("key.fabric_harvester.fast_break", ModKeyBindings.KEY_FAST_BREAK);
        assertEquals("key.fabric_harvester.fast_place", ModKeyBindings.KEY_FAST_PLACE);
        assertEquals("key.fabric_harvester.harvest", ModKeyBindings.KEY_HARVEST);
        assertEquals("key.fabric_harvester.profile", ModKeyBindings.KEY_PROFILE);
        assertEquals("key.fabric_harvester.emergency_stop", ModKeyBindings.KEY_EMERGENCY_STOP);
    }

    @Test
    public void testInitialStateIsDisabled() {
        assertFalse(ModKeyBindings.isFastBreakEnabled());
        assertFalse(ModKeyBindings.isFastPlaceEnabled());
        assertFalse(ModKeyBindings.isHarvestActive());
    }

    @Test
    public void testStateMutatorsAndGetters() {
        ModKeyBindings.setFastBreakEnabled(true);
        assertTrue(ModKeyBindings.isFastBreakEnabled());

        ModKeyBindings.setFastPlaceEnabled(true);
        assertTrue(ModKeyBindings.isFastPlaceEnabled());

        ModKeyBindings.setHarvestActive(true);
        assertTrue(ModKeyBindings.isHarvestActive());

        ModKeyBindings.setFastBreakEnabled(false);
        assertFalse(ModKeyBindings.isFastBreakEnabled());
    }

    @Test
    public void testLocalizationJsonContainsAllKeys() throws Exception {
        File langFile = new File("src/main/resources/assets/fabric_harvester/lang/en_us.json");
        assertTrue(langFile.exists(), "en_us.json must exist in resources");

        String jsonContent = Files.readString(langFile.toPath());
        assertTrue(jsonContent.contains("\"key.categories.fabric_harvester\""), "Must contain category key");
        assertTrue(jsonContent.contains("\"key.fabric_harvester.fast_break\""), "Must contain fast_break key");
        assertTrue(jsonContent.contains("\"key.fabric_harvester.fast_place\""), "Must contain fast_place key");
        assertTrue(jsonContent.contains("\"key.fabric_harvester.harvest\""), "Must contain harvest key");
        assertTrue(jsonContent.contains("\"key.fabric_harvester.profile\""), "Must contain profile key");
        assertTrue(jsonContent.contains("\"key.fabric_harvester.emergency_stop\""), "Must contain emergency key");
        assertTrue(jsonContent.contains("\"hud.fabric_harvester.status\""), "Must contain HUD status text");
    }
}
