package com.swiftride.payment.application;

import com.swiftride.contracts.events.RideCompletedEvent;

/** Implement with idempotent ledger writes before enabling the RabbitMQ consumer. */
public interface RideCompletedHandler {
    void onRideCompleted(RideCompletedEvent event);
}
