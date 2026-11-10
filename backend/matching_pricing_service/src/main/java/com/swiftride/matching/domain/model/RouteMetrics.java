package com.swiftride.matching.domain.model;

/** Distance and duration calculated by a routing provider. */
public record RouteMetrics(double distanceKm, int durationMinutes) {
    public RouteMetrics {
        if (!Double.isFinite(distanceKm) || distanceKm < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }
        if (durationMinutes < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
    }
}

