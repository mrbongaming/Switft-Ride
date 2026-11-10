package com.swiftride.matching.application;

import com.swiftride.matching.domain.model.Coordinates;
import com.swiftride.matching.domain.model.Distance;
import com.swiftride.matching.domain.model.DriverLocation;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MatchingApplicationService {
    private final TripCatalog trips;
    private final DriverDirectory drivers;
    public MatchingApplicationService(TripCatalog trips, DriverDirectory drivers) { this.trips = trips; this.drivers = drivers; }
    public record Match(UUID tripId, String driverId, String driverName, String vehicle, int etaMinutes, int attempt, int maxAttempts) { }
    public Match match(UUID id, List<String> excluded) {
        var trip = trips.get(id);
        if (trip.status().equals("DriverOffered") && trip.offeredDriverId() != null)
            return new Match(id, trip.offeredDriverId(), name(trip.offeredDriverId()), vehicle(trip.offeredDriverId()), 4, 1, 3);
        if (!trip.status().equals("Quoted")) throw new IllegalStateException("Trip must be quoted before matching");
        var pickup = trip.pickup();
        if (pickup.latitude() == null) throw new IllegalArgumentException("Pickup coordinates are required");
        Set<String> skipped = excluded == null ? Set.of() : Set.copyOf(excluded);
        DriverLocation driver = drivers.available().stream().filter(d -> !skipped.contains(d.driverId()))
                .filter(d -> Distance.km(pickup.latitude(), pickup.longitude(), d.coordinates().latitude(), d.coordinates().longitude()) <= 10)
                .min(Comparator.comparingDouble(d -> Distance.km(pickup.latitude(), pickup.longitude(), d.coordinates().latitude(), d.coordinates().longitude())))
                .orElseThrow(() -> new IllegalStateException("No driver is available within 10 km"));
        trips.offer(id, driver.driverId());
        double km = Distance.km(pickup.latitude(), pickup.longitude(), driver.coordinates().latitude(), driver.coordinates().longitude());
        return new Match(id, driver.driverId(), name(driver.driverId()), vehicle(driver.driverId()),
                Math.max(1, (int) Math.ceil(km / 25 * 60)), skipped.size() + 1, 3);
    }
    private String name(String id) { return id.equals("driver-1") ? "Trần Quốc Bảo" : id.equals("driver-2") ? "Lê Thanh Tùng" : id; }
    private String vehicle(String id) { return id.equals("driver-1") ? "Toyota Vios" : id.equals("driver-2") ? "Honda City" : "Xe ô tô"; }
    public List<TripCatalog.TripView> offers(String driver) { return trips.offers(driver); }
    public void updateLocation(String id, double latitude, double longitude, boolean available) {
        drivers.save(new DriverLocation(id, new Coordinates(latitude, longitude), available, Instant.now()));
    }
    public void accepted(String driver) { drivers.setAvailable(driver, false); }
}
