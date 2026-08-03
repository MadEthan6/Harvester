package com.fabricharvester.client;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CropCandidateSelector {
    private CropCandidateSelector() {
    }

    public static <T> Optional<T> nearestUsable(List<Candidate<T>> candidates) {
        return candidates.stream()
                .filter(Candidate::mature)
                .filter(Candidate::reachable)
                .min(Comparator.comparingDouble(Candidate::squaredDistance))
                .map(Candidate::value);
    }

    public record Candidate<T>(T value, boolean mature, boolean reachable, double squaredDistance) {
        public Candidate {
            if (squaredDistance < 0.0) {
                throw new IllegalArgumentException("squaredDistance cannot be negative");
            }
        }
    }
}

