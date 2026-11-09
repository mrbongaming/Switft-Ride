package com.swiftride.contracts.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RideCompletedEvent(
        UUID eventId,
        UUID tripId,
        String riderId,
        BigDecimal amount,
        String currency,
        String paymentMethodId,
        Instant occurredAt,
        String correlationId,
        String idempotencyKey
) { }
