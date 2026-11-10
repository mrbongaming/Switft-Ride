package com.swiftride.matching.application;

/** Application use case invoked when a trip-accepted event is received. */
public interface HandleTripAcceptedUseCase {
    void handle(TripAcceptedCommand command);
}
