package com.fabricharvester.e2e.verify;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

public class ModSetupVerifyTest {

    @Test
    public void testReleasesFileExistsAndValid() throws Exception {
        File releasesFile = new File("RELEASES.md");
        assertTrue(releasesFile.exists(), "RELEASES.md must exist in project root");

        String version = System.getProperty("fabricHarvester.modVersion");
        String content = Files.readString(releasesFile.toPath());
        assertTrue(content.contains("26.2"), "RELEASES.md must reference the tested Minecraft version");
        assertTrue(
                content.contains("build/libs/fabric_harvester-" + version + ".jar"),
                "RELEASES.md must derive its artifact path from the configured mod version"
        );
    }

    @Test
    public void testFabricModJsonExists() {
        File modJson = new File("src/main/resources/fabric.mod.json");
        assertTrue(modJson.exists(), "fabric.mod.json must exist in resources");
    }

    @Test
    public void testClientOnlyMetadataAndLicense() throws Exception {
        String metadata = Files.readString(new File("src/main/resources/fabric.mod.json").toPath());
        assertTrue(metadata.contains("\"environment\": \"client\""));
        assertTrue(metadata.contains("\"minecraft\": \"=26.2\""));
        assertTrue(metadata.contains("\"java\": \">=25\""));
        assertTrue(metadata.contains("\"license\": \"CC0-1.0\""));
        assertTrue(metadata.contains("https://github.com/MadEthan6/Harvester"));
        assertFalse(metadata.contains("\"main\":"), "Client-only mod must not publish a common entrypoint");
        assertTrue(new File("LICENSE").isFile(), "Public release must include a license");
        assertTrue(new File("CHANGELOG.md").isFile(), "Public release must include a changelog");
    }
}
