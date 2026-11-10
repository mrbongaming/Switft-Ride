package com.swiftride.matching.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TripCatalog {
    TripView get(UUID id);
    void quote(UUID id, BigDecimal amount);
    void offer(UUID id, String driverId);
    List<TripView> offers(String driverId);
    record Location(String address, Double latitude, Double longitude) { }
    record TripView(UUID id, String riderId, String driverId, String offeredDriverId, Location pickup, Location dropoff,
                    String status, BigDecimal quotedFare, BigDecimal finalFare) { }
}
