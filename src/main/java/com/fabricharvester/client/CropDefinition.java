package com.fabricharvester.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public record CropDefinition(Block cropBlock, Item plantingItem) {
    public boolean isMature(BlockState state) {
        return state.is(cropBlock)
                && cropBlock instanceof CropBlock crop
                && crop.isMaxAge(state);
    }

    public boolean isReplanted(BlockState state) {
        return state.is(cropBlock) && !isMature(state);
    }
}
