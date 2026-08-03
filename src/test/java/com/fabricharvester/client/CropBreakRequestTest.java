package com.fabricharvester.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CropBreakRequestTest {
    @Test
    void createsARealServerDirectedBreakStartPacket() {
        BlockPos position = new BlockPos(4, 70, -3);

        ServerboundPlayerActionPacket packet = CropBreakRequest.create(position);

        assertEquals(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, packet.getAction());
        assertEquals(position, packet.getPos());
        assertEquals(Direction.UP, packet.getDirection());
    }
}
