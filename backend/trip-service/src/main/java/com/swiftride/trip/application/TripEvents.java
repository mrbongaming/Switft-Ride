package com.swiftride.trip.application;

import com.swiftride.trip.domain.Trip;

public interface TripEvents {
    void tripAccepted(Trip trip);
    void rideCompleted(Trip trip);
}
