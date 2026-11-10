package com.swiftride.matching.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class MatchingSession {

    public static final double INITIAL_RADIUS_KM = 5.0;
    public static final double RADIUS_STEP_KM = 1.0;
    public static final double MAX_RADIUS_KM = 10.0;
    public static final int MAX_FAILED_DRIVERS = 3;

    private final UUID sessionId;
    private final UUID tripId;
    private final Set<String> excludedDriverIds;
    private final Instant createdAt;

    private double currentRadiusKm;
    private int failedDriverCount;
    private MatchingSessionStatus status;
    private String currentDriverId;
    private String matchedDriverId;
    private Instant updatedAt;

    public MatchingSession(
            UUID sessionId,
            UUID tripId,
            Instant createdAt
    ) {
        this.sessionId = Objects.requireNonNull(
                sessionId,
                "Session ID must not be null"
        );
        this.tripId = Objects.requireNonNull(
                tripId,
                "Trip ID must not be null"
        );
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Created time must not be null"
        );

        this.currentRadiusKm = INITIAL_RADIUS_KM;
        this.failedDriverCount = 0;
        this.status = MatchingSessionStatus.SEARCHING;
        this.excludedDriverIds = new HashSet<>();
        this.updatedAt = createdAt;
    }

    public void waitForDriver(
            String driverId,
            Instant updateTime
    ) {
        ensureStatus(MatchingSessionStatus.SEARCHING);
        validateDriverId(driverId);
        validateUpdateTime(updateTime);

        if (excludedDriverIds.contains(driverId)) {
            throw new IllegalArgumentException(
                    "Cannot send another offer to an excluded driver"
            );
        }

        currentDriverId = driverId;
        status = MatchingSessionStatus.WAITING_FOR_DRIVER;
        updatedAt = updateTime;
    }

    public void recordOfferFailure(
            String driverId,
            Instant updateTime
    ) {
        validateDriverId(driverId);
        validateUpdateTime(updateTime);

        if (excludedDriverIds.contains(driverId)) {
            return;
        }

        ensureStatus(MatchingSessionStatus.WAITING_FOR_DRIVER);

        if (!driverId.equals(currentDriverId)) {
            throw new IllegalArgumentException(
                    "Driver is not the current offered driver"
            );
        }

        excludedDriverIds.add(driverId);
        failedDriverCount++;
        currentDriverId = null;
        updatedAt = updateTime;

        if (failedDriverCount >= MAX_FAILED_DRIVERS) {
            status = MatchingSessionStatus.FAILED;
            return;
        }

        if (currentRadiusKm >= MAX_RADIUS_KM) {
            status = MatchingSessionStatus.FAILED;
            return;
        }

        currentRadiusKm = Math.min(
                currentRadiusKm + RADIUS_STEP_KM,
                MAX_RADIUS_KM
        );

        status = MatchingSessionStatus.SEARCHING;
    }

    public void expandSearchRadius(Instant updateTime) {
        ensureStatus(MatchingSessionStatus.SEARCHING);
        validateUpdateTime(updateTime);

        if (currentRadiusKm >= MAX_RADIUS_KM) {
            status = MatchingSessionStatus.FAILED;
            updatedAt = updateTime;
            return;
        }

        currentRadiusKm = Math.min(
                currentRadiusKm + RADIUS_STEP_KM,
                MAX_RADIUS_KM
        );

        updatedAt = updateTime;
    }

    public void markMatched(
            String driverId,
            Instant updateTime
    ) {
        ensureStatus(MatchingSessionStatus.WAITING_FOR_DRIVER);
        validateDriverId(driverId);
        validateUpdateTime(updateTime);

        if (!driverId.equals(currentDriverId)) {
            throw new IllegalArgumentException(
                    "Driver is not the current offered driver"
            );
        }

        matchedDriverId = driverId;
        currentDriverId = null;
        status = MatchingSessionStatus.MATCHED;
        updatedAt = updateTime;
    }

    public void cancel(Instant updateTime) {
        validateUpdateTime(updateTime);

        if (status == MatchingSessionStatus.MATCHED) {
            throw new IllegalStateException(
                    "A matched session cannot be cancelled"
            );
        }

        if (status == MatchingSessionStatus.FAILED
                || status == MatchingSessionStatus.CANCELLED) {
            return;
        }

        currentDriverId = null;
        status = MatchingSessionStatus.CANCELLED;
        updatedAt = updateTime;
    }

    private void ensureStatus(MatchingSessionStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException(
                    "Expected status " + expectedStatus
                            + " but current status is " + status
            );
        }
    }

    private static void validateDriverId(String driverId) {
        if (driverId == null || driverId.isBlank()) {
            throw new IllegalArgumentException(
                    "Driver ID must not be blank"
            );
        }
    }

    private void validateUpdateTime(Instant updateTime) {
        Objects.requireNonNull(
                updateTime,
                "Update time must not be null"
        );

        if (updateTime.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "Update time cannot be older than current state"
            );
        }
    }

    public UUID sessionId() {
        return sessionId;
    }

    public UUID tripId() {
        return tripId;
    }

    public double currentRadiusKm() {
        return currentRadiusKm;
    }

    public int failedDriverCount() {
        return failedDriverCount;
    }

    public MatchingSessionStatus status() {
        return status;
    }

    public String currentDriverId() {
        return currentDriverId;
    }

    public String matchedDriverId() {
        return matchedDriverId;
    }

    public Set<String> excludedDriverIds() {
        return Set.copyOf(excludedDriverIds);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}