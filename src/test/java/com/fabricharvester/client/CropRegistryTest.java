package com.fabricharvester.client;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropRegistryTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void detectsWheatMaturity() {
        CropDefinition wheat = CropRegistry.find(Blocks.WHEAT).orElseThrow();
        assertFalse(wheat.isMature(Blocks.WHEAT.defaultBlockState()));
        assertTrue(wheat.age(Blocks.WHEAT.defaultBlockState()) == 0);
        assertTrue(wheat.isMature(
                Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE)
        ));
        assertTrue(wheat.age(Blocks.DIRT.defaultBlockState()) == -1);
    }

    @Test
    void retainsTheFourSupportedCrops() {
        assertTrue(CropRegistry.find(Blocks.WHEAT).isPresent());
        assertTrue(CropRegistry.find(Blocks.CARROTS).isPresent());
        assertTrue(CropRegistry.find(Blocks.POTATOES).isPresent());
        assertTrue(CropRegistry.find(Blocks.BEETROOTS).isPresent());
    }
}
