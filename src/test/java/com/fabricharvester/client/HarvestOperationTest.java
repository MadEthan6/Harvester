package com.fabricharvester.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarvestOperationTest {
    @Test
    void completesAfterConfirmedBreakAndReplant() {
        HarvestOperation operation = new HarvestOperation(2);

        assertEquals(HarvestOperation.Step.ATTEMPT_REPLANT, operation.tick(true, false));
        assertEquals(HarvestOperation.Step.NONE, operation.onReplantAttempt(true));
        assertEquals(HarvestOperation.Step.COMPLETE, operation.tick(true, true));
        assertEquals(HarvestOperation.State.COMPLETE, operation.state());
        assertTrue(operation.isTerminal());
    }

    @Test
    void breakConfirmationTimesOutAfterTenTicks() {
        HarvestOperation operation = new HarvestOperation(2);
        HarvestOperation.Step step = HarvestOperation.Step.NONE;

        for (int tick = 0; tick < HarvestOperation.CONFIRMATION_TIMEOUT_TICKS; tick++) {
            step = operation.tick(false, false);
        }

        assertEquals(HarvestOperation.Step.BREAK_TIMEOUT, step);
        assertEquals(HarvestOperation.State.FAILED, operation.state());
    }

    @Test
    void rejectedReplantRetriesTwiceWithBackoff() {
        HarvestOperation operation = new HarvestOperation(2);
        assertEquals(HarvestOperation.Step.ATTEMPT_REPLANT, operation.tick(true, false));

        for (int retry = 0; retry < HarvestOperation.MAX_REPLANT_RETRIES; retry++) {
            assertEquals(HarvestOperation.Step.NONE, operation.onReplantAttempt(false));
            assertEquals(HarvestOperation.State.BACKING_OFF, operation.state());
            assertEquals(HarvestOperation.Step.NONE, operation.tick(true, false));
            assertEquals(HarvestOperation.Step.ATTEMPT_REPLANT, operation.tick(true, false));
        }

        assertEquals(HarvestOperation.Step.REPLANT_TIMEOUT, operation.onReplantAttempt(false));
        assertEquals(HarvestOperation.State.FAILED, operation.state());
        assertEquals(2, operation.retries());
    }

    @Test
    void unconfirmedAcceptedReplantIsRetried() {
        HarvestOperation operation = new HarvestOperation(1);
        operation.tick(true, false);
        operation.onReplantAttempt(true);

        HarvestOperation.Step step = HarvestOperation.Step.NONE;
        for (int tick = 0; tick < HarvestOperation.CONFIRMATION_TIMEOUT_TICKS; tick++) {
            step = operation.tick(true, false);
        }

        assertEquals(HarvestOperation.Step.NONE, step);
        assertEquals(HarvestOperation.State.BACKING_OFF, operation.state());
        assertEquals(HarvestOperation.Step.ATTEMPT_REPLANT, operation.tick(true, false));
    }

    @Test
    void emergencyCancellationIsTerminal() {
        HarvestOperation operation = new HarvestOperation(2);
        operation.cancel();
        assertEquals(HarvestOperation.State.CANCELLED, operation.state());
        assertTrue(operation.isTerminal());
    }
}

