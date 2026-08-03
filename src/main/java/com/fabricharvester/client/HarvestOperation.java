package com.fabricharvester.client;

/** Pure state machine used by the client interaction layer and unit tests. */
public final class HarvestOperation {
    public static final int CONFIRMATION_TIMEOUT_TICKS = 10;
    public static final int MAX_REPLANT_RETRIES = 2;

    public enum State {
        WAITING_FOR_BREAK,
        READY_TO_REPLANT,
        WAITING_FOR_REPLANT,
        BACKING_OFF,
        COMPLETE,
        FAILED,
        CANCELLED
    }

    public enum Step {
        NONE,
        ATTEMPT_REPLANT,
        COMPLETE,
        BREAK_TIMEOUT,
        REPLANT_TIMEOUT
    }

    private final int retryBackoffTicks;
    private State state = State.WAITING_FOR_BREAK;
    private int ticksInState;
    private int retries;

    public HarvestOperation(int retryBackoffTicks) {
        if (retryBackoffTicks < 1) {
            throw new IllegalArgumentException("retryBackoffTicks must be positive");
        }
        this.retryBackoffTicks = retryBackoffTicks;
    }

    public Step tick(boolean cropRemoved, boolean cropReplanted) {
        return switch (state) {
            case WAITING_FOR_BREAK -> tickBreakConfirmation(cropRemoved);
            case WAITING_FOR_REPLANT -> tickReplantConfirmation(cropReplanted);
            case BACKING_OFF -> tickBackoff();
            case COMPLETE -> Step.COMPLETE;
            default -> Step.NONE;
        };
    }

    public Step onReplantAttempt(boolean accepted) {
        if (state != State.READY_TO_REPLANT) {
            throw new IllegalStateException("Replant attempt is only valid when ready");
        }
        if (accepted) {
            transition(State.WAITING_FOR_REPLANT);
            return Step.NONE;
        }
        return retryOrFail();
    }

    public void cancel() {
        if (!isTerminal()) {
            transition(State.CANCELLED);
        }
    }

    public State state() {
        return state;
    }

    public int retries() {
        return retries;
    }

    public boolean isTerminal() {
        return state == State.COMPLETE || state == State.FAILED || state == State.CANCELLED;
    }

    private Step tickBreakConfirmation(boolean cropRemoved) {
        if (cropRemoved) {
            transition(State.READY_TO_REPLANT);
            return Step.ATTEMPT_REPLANT;
        }
        if (++ticksInState >= CONFIRMATION_TIMEOUT_TICKS) {
            transition(State.FAILED);
            return Step.BREAK_TIMEOUT;
        }
        return Step.NONE;
    }

    private Step tickReplantConfirmation(boolean cropReplanted) {
        if (cropReplanted) {
            transition(State.COMPLETE);
            return Step.COMPLETE;
        }
        if (++ticksInState >= CONFIRMATION_TIMEOUT_TICKS) {
            return retryOrFail();
        }
        return Step.NONE;
    }

    private Step tickBackoff() {
        if (++ticksInState >= retryBackoffTicks) {
            transition(State.READY_TO_REPLANT);
            return Step.ATTEMPT_REPLANT;
        }
        return Step.NONE;
    }

    private Step retryOrFail() {
        if (retries < MAX_REPLANT_RETRIES) {
            retries++;
            transition(State.BACKING_OFF);
            return Step.NONE;
        }
        transition(State.FAILED);
        return Step.REPLANT_TIMEOUT;
    }

    private void transition(State nextState) {
        state = nextState;
        ticksInState = 0;
    }
}

