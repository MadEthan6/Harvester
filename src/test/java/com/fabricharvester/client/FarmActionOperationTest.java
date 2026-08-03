package com.fabricharvester.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FarmActionOperationTest {
    @Test
    void completesOnlyAfterServerConfirmation() {
        FarmActionOperation operation = new FarmActionOperation();

        assertEquals(FarmActionOperation.Step.NONE, operation.tick(false));
        assertEquals(FarmActionOperation.Step.COMPLETE, operation.tick(true));
        assertEquals(FarmActionOperation.State.COMPLETE, operation.state());
    }

    @Test
    void timesOutAfterTenUnconfirmedTicks() {
        FarmActionOperation operation = new FarmActionOperation();
        for (int tick = 1; tick < FarmActionOperation.CONFIRMATION_TIMEOUT_TICKS; tick++) {
            assertEquals(FarmActionOperation.Step.NONE, operation.tick(false));
        }

        assertEquals(FarmActionOperation.Step.TIMEOUT, operation.tick(false));
        assertEquals(FarmActionOperation.State.FAILED, operation.state());
    }

    @Test
    void canBeCancelledWhileWaiting() {
        FarmActionOperation operation = new FarmActionOperation();
        operation.cancel();

        assertEquals(FarmActionOperation.State.CANCELLED, operation.state());
        assertEquals(FarmActionOperation.Step.NONE, operation.tick(true));
    }
}
