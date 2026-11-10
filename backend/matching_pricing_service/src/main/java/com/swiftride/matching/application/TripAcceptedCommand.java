package com.swiftride.matching.application;

import java.time.Instant;
import java.util.UUID;

/** Framework-independent input passed to the trip-accepted use case. */
public record TripAcceptedCommand(
        UUID eventId,
        UUID tripId,
        String driverId,
        Instant occurredAt,
        String correlationId
) {
}
