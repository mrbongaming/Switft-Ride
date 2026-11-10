package com.swiftride.matching.application.port.in;

import com.swiftride.contracts.events.TripAcceptedEvent;

/** Inbound contract used by the RabbitMQ listener. */
public interface HandleTripAcceptedUseCase {
    void handle(TripAcceptedEvent event);
}

