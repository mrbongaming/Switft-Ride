package com.swiftride.trip.application;

import com.swiftride.contracts.events.PaymentFailedEvent;
import com.swiftride.contracts.events.PaymentSettledEvent;

/** Implement with transactional state changes and event-id deduplication before enabling consumers. */
public interface PaymentResultHandler {
    void onPaymentSettled(PaymentSettledEvent event);
    void onPaymentFailed(PaymentFailedEvent event);
}
