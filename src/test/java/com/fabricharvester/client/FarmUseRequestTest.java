package com.fabricharvester.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FarmUseRequestTest {
    @Test
    void createsAServerDirectedUsePacketWithoutLocalPrediction() {
        BlockPos position = new BlockPos(2, 64, 7);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atBottomCenterOf(position.above()),
                Direction.UP,
                position,
                false
        );

        ServerboundUseItemOnPacket packet = FarmUseRequest.create(InteractionHand.OFF_HAND, hit);

        assertEquals(InteractionHand.OFF_HAND, packet.getHand());
        assertEquals(hit, packet.getHitResult());
        assertEquals(0, packet.getSequence());
    }
}
