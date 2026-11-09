package com.swiftride.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record TripAcceptedEvent(
        UUID eventId,
        UUID tripId,
        String driverId,
        Instant occurredAt,
        String correlationId
) { }
