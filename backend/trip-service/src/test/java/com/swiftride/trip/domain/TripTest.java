package com.swiftride.trip.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripTest {
    private Trip trip() {
        return new Trip(UUID.randomUUID(), "rider-1", new Trip.Location("Pickup", 10.77, 106.70),
                new Trip.Location("Dropoff", 10.78, 106.71), Instant.now());
    }
    @Test void completeRideOnlyAfterRiderConfirmation() {
        Trip trip = trip();
        trip.quote(BigDecimal.valueOf(25000));
        trip.offer("driver-1");
        assertThrows(IllegalStateException.class, () -> trip.accept("driver-1"));
        trip.confirm(); trip.accept("driver-1"); trip.enRoute(); trip.pickupPassenger(); trip.dropoffPassenger();
        assertEquals("PaymentPending", trip.status());
        assertEquals(0, trip.finalFare().compareTo(BigDecimal.valueOf(25000)));
        trip.paid(UUID.randomUUID());
        assertEquals("Paid", trip.status());
    }
    @Test void cannotAcceptAnotherDriversOfferOrCancelAfterPickup() {
        Trip trip = trip(); trip.quote(BigDecimal.valueOf(25000)); trip.offer("driver-1"); trip.confirm();
        assertThrows(IllegalArgumentException.class, () -> trip.accept("driver-2"));
        trip.accept("driver-1"); trip.enRoute(); trip.pickupPassenger();
        assertThrows(IllegalStateException.class, trip::cancel);
    }
}
