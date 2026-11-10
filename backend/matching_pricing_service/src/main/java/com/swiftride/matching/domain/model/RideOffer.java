package com.swiftride.matching.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RideOffer {

    private final UUID offerId;
    private final UUID sessionId;
    private final UUID tripId;
    private final String driverId;

    private final double searchRadiusKm;
    private final double distanceToPickupKm;
    private final int etaMinutes;
    private final int attempt;

    private final Instant createdAt;
    private final Instant expiresAt;

    private RideOfferStatus status;
    private Instant respondedAt;

    public RideOffer(
            UUID offerId,
            UUID sessionId,
            UUID tripId,
            String driverId,
            double searchRadiusKm,
            double distanceToPickupKm,
            int etaMinutes,
            int attempt,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.offerId = Objects.requireNonNull(
                offerId,
                "Offer ID must not be null"
        );

        this.sessionId = Objects.requireNonNull(
                sessionId,
                "Session ID must not be null"
        );

        this.tripId = Objects.requireNonNull(
                tripId,
                "Trip ID must not be null"
        );

        this.driverId = validateDriverId(driverId);
        this.searchRadiusKm = validateSearchRadius(searchRadiusKm);
        this.distanceToPickupKm =
                validateDistanceToPickup(distanceToPickupKm);
        this.etaMinutes = validateEtaMinutes(etaMinutes);
        this.attempt = validateAttempt(attempt);

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Created time must not be null"
        );

        this.expiresAt = validateExpiresAt(
                expiresAt,
                createdAt
        );

        this.status = RideOfferStatus.PENDING;
    }

    public void accept(Instant responseTime) {
        ensurePending();
        validateResponseTime(responseTime);

        if (isExpired(responseTime)) {
            throw new IllegalStateException(
                    "Cannot accept an expired offer"
            );
        }

        status = RideOfferStatus.ACCEPTED;
        respondedAt = responseTime;
    }

    public void reject(Instant responseTime) {
        ensurePending();
        validateResponseTime(responseTime);

        if (isExpired(responseTime)) {
            throw new IllegalStateException(
                    "Cannot reject an expired offer"
            );
        }

        status = RideOfferStatus.REJECTED;
        respondedAt = responseTime;
    }

    public void expire(Instant expirationTime) {
        Objects.requireNonNull(
                expirationTime,
                "Expiration time must not be null"
        );

        /*
         * Giúp xử lý tác vụ kiểm tra hết hạn nhiều lần
         * mà không làm thay đổi trạng thái lần nữa.
         */
        if (status == RideOfferStatus.EXPIRED) {
            return;
        }

        ensurePending();

        if (expirationTime.isBefore(expiresAt)) {
            throw new IllegalStateException(
                    "Offer has not expired yet"
            );
        }

        status = RideOfferStatus.EXPIRED;
        respondedAt = expirationTime;
    }

    public void cancel(Instant cancellationTime) {
        Objects.requireNonNull(
                cancellationTime,
                "Cancellation time must not be null"
        );

        if (status == RideOfferStatus.CANCELLED) {
            return;
        }

        ensurePending();
        validateResponseTime(cancellationTime);

        status = RideOfferStatus.CANCELLED;
        respondedAt = cancellationTime;
    }

    public boolean isExpired(Instant currentTime) {
        Objects.requireNonNull(
                currentTime,
                "Current time must not be null"
        );

        return !currentTime.isBefore(expiresAt);
    }

    public boolean isPending() {
        return status == RideOfferStatus.PENDING;
    }

    private void ensurePending() {
        if (status != RideOfferStatus.PENDING) {
            throw new IllegalStateException(
                    "Offer is not pending. Current status: " + status
            );
        }
    }

    private void validateResponseTime(Instant responseTime) {
        Objects.requireNonNull(
                responseTime,
                "Response time must not be null"
        );

        if (responseTime.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Response time cannot be before creation time"
            );
        }
    }

    private static String validateDriverId(String driverId) {
        if (driverId == null || driverId.isBlank()) {
            throw new IllegalArgumentException(
                    "Driver ID must not be blank"
            );
        }

        return driverId;
    }

    private static double validateSearchRadius(
            double searchRadiusKm
    ) {
        if (!Double.isFinite(searchRadiusKm)
                || searchRadiusKm
                < MatchingSession.INITIAL_RADIUS_KM
                || searchRadiusKm
                > MatchingSession.MAX_RADIUS_KM) {
            throw new IllegalArgumentException(
                    "Search radius must be between 5 and 10 km"
            );
        }

        return searchRadiusKm;
    }

    private static double validateDistanceToPickup(
            double distanceToPickupKm
    ) {
        if (!Double.isFinite(distanceToPickupKm)
                || distanceToPickupKm < 0) {
            throw new IllegalArgumentException(
                    "Distance to pickup cannot be negative"
            );
        }

        return distanceToPickupKm;
    }

    private static int validateEtaMinutes(int etaMinutes) {
        if (etaMinutes < 0) {
            throw new IllegalArgumentException(
                    "ETA minutes cannot be negative"
            );
        }

        return etaMinutes;
    }

    private static int validateAttempt(int attempt) {
        if (attempt < 1
                || attempt > MatchingSession.MAX_FAILED_DRIVERS) {
            throw new IllegalArgumentException(
                    "Attempt must be between 1 and 3"
            );
        }

        return attempt;
    }

    private static Instant validateExpiresAt(
            Instant expiresAt,
            Instant createdAt
    ) {
        Objects.requireNonNull(
                expiresAt,
                "Expiration time must not be null"
        );

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "Expiration time must be after creation time"
            );
        }

        return expiresAt;
    }

    public UUID offerId() {
        return offerId;
    }

    public UUID sessionId() {
        return sessionId;
    }

    public UUID tripId() {
        return tripId;
    }

    public String driverId() {
        return driverId;
    }

    public double searchRadiusKm() {
        return searchRadiusKm;
    }

    public double distanceToPickupKm() {
        return distanceToPickupKm;
    }

    public int etaMinutes() {
        return etaMinutes;
    }

    public int attempt() {
        return attempt;
    }

    public RideOfferStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant respondedAt() {
        return respondedAt;
    }
}