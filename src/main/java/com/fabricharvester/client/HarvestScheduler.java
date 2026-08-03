package com.fabricharvester.client;

/** Pure rate limiter that prevents overlapping crop operations. */
public final class HarvestScheduler {
    private int cooldownTicks;

    public void tick() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    public boolean canStart(boolean harvestHeld, boolean operationInFlight) {
        return harvestHeld && !operationInFlight && cooldownTicks == 0;
    }

    public void onStarted(AutomationProfile profile) {
        cooldownTicks = profile.harvestIntervalTicks();
    }

    public void reset() {
        cooldownTicks = 0;
    }

    int cooldownTicks() {
        return cooldownTicks;
    }
}
