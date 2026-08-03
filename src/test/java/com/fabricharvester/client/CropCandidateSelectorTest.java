package com.fabricharvester.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropCandidateSelectorTest {
    @Test
    void choosesNearestMatureReachableCrop() {
        var candidates = List.of(
                new CropCandidateSelector.Candidate<>("far", true, true, 9.0),
                new CropCandidateSelector.Candidate<>("nearest", true, true, 2.0),
                new CropCandidateSelector.Candidate<>("immature", false, true, 1.0),
                new CropCandidateSelector.Candidate<>("unreachable", true, false, 0.5)
        );

        assertEquals("nearest", CropCandidateSelector.nearestUsable(candidates).orElseThrow());
    }

    @Test
    void returnsEmptyWhenNothingCanBeHarvested() {
        var candidates = List.of(
                new CropCandidateSelector.Candidate<>("immature", false, true, 1.0),
                new CropCandidateSelector.Candidate<>("unreachable", true, false, 2.0)
        );

        assertTrue(CropCandidateSelector.nearestUsable(candidates).isEmpty());
    }
}

