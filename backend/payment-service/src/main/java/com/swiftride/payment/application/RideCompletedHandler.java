package com.swiftride.payment.application;

import java.math.BigDecimal;
import java.util.UUID;

/** Handles a completed ride with idempotent payment and ledger writes. */
public interface RideCompletedHandler {
    void onRideCompleted(UUID tripId, String riderId, String driverId, BigDecimal amount, String currency, String idempotencyKey);
}
