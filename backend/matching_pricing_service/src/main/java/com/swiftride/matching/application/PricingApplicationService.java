package com.swiftride.matching.application;

import com.swiftride.matching.domain.model.Distance;
import com.swiftride.matching.domain.pricing.StandardPricing;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PricingApplicationService {
    private final TripCatalog trips;
    public PricingApplicationService(TripCatalog trips) { this.trips = trips; }
    public record Quote(UUID tripId, BigDecimal amount, String currency, double distanceKm,
                        int durationMinutes, int surgeMultiplier) { }
    public Quote quote(UUID id) {
        TripCatalog.TripView trip = trips.get(id);
        if (!trip.status().equals("Requested") && !trip.status().equals("Quoted")) throw new IllegalStateException("Trip cannot be quoted");
        var from = trip.pickup(); var to = trip.dropoff();
        if (from.latitude() == null || to.latitude() == null) throw new IllegalArgumentException("Coordinates are required for pricing");
        double distance = Math.max(1, Math.round(Distance.km(from.latitude(), from.longitude(), to.latitude(), to.longitude()) * 1.3 * 10) / 10.0);
        int duration = (int) Math.ceil(distance / 24 * 60);
        BigDecimal amount = new StandardPricing().fare(distance);
        trips.quote(id, amount);
        return new Quote(id, amount, "VND", distance, duration, 1);
    }
}
