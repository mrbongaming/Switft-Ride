package com.swiftride.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        UUID tripId,
        UUID paymentId,
        String reasonCode,
        Instant occurredAt,
        String correlationId
) { }
