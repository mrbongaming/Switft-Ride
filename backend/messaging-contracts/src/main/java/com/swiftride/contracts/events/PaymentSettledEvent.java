package com.swiftride.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentSettledEvent(
        UUID eventId,
        UUID tripId,
        UUID paymentId,
        Instant occurredAt,
        String correlationId
) { }
