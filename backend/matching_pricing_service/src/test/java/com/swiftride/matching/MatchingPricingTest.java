package com.swiftride.matching;

import static org.junit.jupiter.api.Assertions.*;
import com.swiftride.matching.application.*;
import com.swiftride.matching.domain.model.Coordinates;
import com.swiftride.matching.domain.model.DriverLocation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchingPricingTest {
    private final UUID tripId = UUID.randomUUID();
    private BigDecimal quoted;
    private String offered;
    private final TripCatalog trips = new TripCatalog() {
        public TripView get(UUID id) { return new TripView(id, "rider-1", null, offered,
                new Location("A", 10.7769, 106.7009), new Location("B", 10.7869, 106.7109),
                offered == null ? quoted == null ? "Requested" : "Quoted" : "DriverOffered", quoted, null); }
        public void quote(UUID id, BigDecimal amount) { quoted = amount; }
        public void offer(UUID id, String driverId) { offered = driverId; }
        public List<TripView> offers(String driverId) { return List.of(); }
    };
    private final DriverDirectory drivers = new DriverDirectory() {
        public List<DriverLocation> available() { return List.of(
                new DriverLocation("driver-2", new Coordinates(10.80, 106.73), true, Instant.now()),
                new DriverLocation("driver-1", new Coordinates(10.777, 106.701), true, Instant.now())); }
        public void save(DriverLocation d) { }
        public void setAvailable(String id, boolean available) { }
        public long count() { return 2; }
    };
    @Test void quotesOnServerAndChoosesNearestDriver() {
        var quote = new PricingApplicationService(trips).quote(tripId);
        assertTrue(quote.amount().signum() > 0);
        assertEquals(quote.amount(), quoted);
        var match = new MatchingApplicationService(trips, drivers).match(tripId, List.of());
        assertEquals("driver-1", match.driverId());
        assertEquals("driver-1", offered);
    }
}
