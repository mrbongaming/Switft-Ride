package com.swiftride.trip.application;

import com.swiftride.trip.domain.Trip;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRepository {
    Trip save(Trip trip);
    Optional<Trip> find(UUID id);
    List<Trip> byRider(String riderId);
    List<Trip> byDriver(String driverId);
    List<Trip> offeredTo(String driverId);
}
