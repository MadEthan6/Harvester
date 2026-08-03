package com.fabricharvester.client;

/** Pure confirmation state for planting, tilling, and bone-meal interactions. */
public final class FarmActionOperation {
    public static final int CONFIRMATION_TIMEOUT_TICKS = 10;

    public enum State {
        WAITING_FOR_CONFIRMATION,
        COMPLETE,
        FAILED,
        CANCELLED
    }

    public enum Step {
        NONE,
        COMPLETE,
        TIMEOUT
    }

    private State state = State.WAITING_FOR_CONFIRMATION;
    private int ticksWaiting;

    public Step tick(boolean confirmed) {
        if (state != State.WAITING_FOR_CONFIRMATION) {
            return state == State.COMPLETE ? Step.COMPLETE : Step.NONE;
        }
        if (confirmed) {
            state = State.COMPLETE;
            return Step.COMPLETE;
        }
        if (++ticksWaiting >= CONFIRMATION_TIMEOUT_TICKS) {
            state = State.FAILED;
            return Step.TIMEOUT;
        }
        return Step.NONE;
    }

    public void cancel() {
        if (state == State.WAITING_FOR_CONFIRMATION) {
            state = State.CANCELLED;
        }
    }

    public State state() {
        return state;
    }

    int ticksWaiting() {
        return ticksWaiting;
    }
}
