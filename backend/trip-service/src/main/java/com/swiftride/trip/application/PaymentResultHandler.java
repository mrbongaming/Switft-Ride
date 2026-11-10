package com.swiftride.trip.application;

import java.util.UUID;

/** Applies payment outcomes to the trip state machine. */
public interface PaymentResultHandler {
    void onPaymentSettled(UUID tripId, UUID paymentId);
    void onPaymentFailed(UUID tripId);
}
