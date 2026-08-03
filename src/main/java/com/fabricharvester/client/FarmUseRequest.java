package com.fabricharvester.client;

import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

/** Builds server-directed farming interactions without applying client prediction first. */
final class FarmUseRequest {
    private FarmUseRequest() {
    }

    static ServerboundUseItemOnPacket create(InteractionHand hand, BlockHitResult hitResult) {
        return new ServerboundUseItemOnPacket(hand, hitResult, 0);
    }
}
