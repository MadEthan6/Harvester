package com.fabricharvester.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

/** Builds the server-directed packet for an instant crop break without mutating client state. */
final class CropBreakRequest {
    private CropBreakRequest() {
    }

    static ServerboundPlayerActionPacket create(BlockPos position) {
        return new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                position,
                Direction.UP
        );
    }
}
