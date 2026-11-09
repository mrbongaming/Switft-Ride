package com.swiftride.matching.application;

import com.swiftride.contracts.events.TripAcceptedEvent;

/** Implement idempotent driver availability updates before enabling the listener. */
public interface TripAcceptedHandler {
    void onTripAccepted(TripAcceptedEvent event);
}
