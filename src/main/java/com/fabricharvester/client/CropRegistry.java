package com.fabricharvester.client;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public final class CropRegistry {
    private static final List<CropDefinition> CROPS = List.of(
            new CropDefinition(Blocks.WHEAT, Items.WHEAT_SEEDS),
            new CropDefinition(Blocks.CARROTS, Items.CARROT),
            new CropDefinition(Blocks.POTATOES, Items.POTATO),
            new CropDefinition(Blocks.BEETROOTS, Items.BEETROOT_SEEDS)
    );

    private CropRegistry() {
    }

    public static Optional<CropDefinition> find(Block block) {
        return CROPS.stream().filter(definition -> definition.cropBlock() == block).findFirst();
    }

    public static Optional<CropDefinition> findMature(BlockState state) {
        return find(state.getBlock()).filter(definition -> definition.isMature(state));
    }

    public static List<CropDefinition> definitions() {
        return CROPS;
    }
}
