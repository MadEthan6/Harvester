package com.fabricharvester.client;

/** Pure eligibility rule for automatic dirt tilling. */
public final class FarmlandRules {
    private FarmlandRules() {
    }

    public static boolean shouldTill(boolean isDirt, boolean airAbove, int adjacentFarmland) {
        return isDirt && airAbove && (adjacentFarmland == 2 || adjacentFarmland == 3);
    }
}
