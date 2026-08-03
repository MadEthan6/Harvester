package com.fabricharvester.client;

public enum AutomationProfile {
    SAFE(4, 1, 1, 2, "profile.fabric_harvester.safe"),
    TRUSTED(1, 0, 0, 1, "profile.fabric_harvester.trusted");

    private final int harvestIntervalTicks;
    private final int blockBreakCooldown;
    private final int itemUseCooldown;
    private final int retryBackoffTicks;
    private final String translationKey;

    AutomationProfile(
            int harvestIntervalTicks,
            int blockBreakCooldown,
            int itemUseCooldown,
            int retryBackoffTicks,
            String translationKey
    ) {
        this.harvestIntervalTicks = harvestIntervalTicks;
        this.blockBreakCooldown = blockBreakCooldown;
        this.itemUseCooldown = itemUseCooldown;
        this.retryBackoffTicks = retryBackoffTicks;
        this.translationKey = translationKey;
    }

    public int harvestIntervalTicks() {
        return harvestIntervalTicks;
    }

    public int blockBreakCooldown() {
        return blockBreakCooldown;
    }

    public int itemUseCooldown() {
        return itemUseCooldown;
    }

    public int retryBackoffTicks() {
        return retryBackoffTicks;
    }

    public String translationKey() {
        return translationKey;
    }

    public AutomationProfile next() {
        return this == SAFE ? TRUSTED : SAFE;
    }

    public static AutomationProfile forConnection(boolean singleplayer) {
        return singleplayer ? TRUSTED : SAFE;
    }
}
