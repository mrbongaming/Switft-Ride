package com.swiftride.trip.application;

import com.swiftride.trip.domain.Trip;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TripApplicationService implements PaymentResultHandler {
    private final TripRepository trips;
    private final TripEvents events;
    public TripApplicationService(TripRepository trips, TripEvents events) { this.trips = trips; this.events = events; }
    public Trip create(String riderId, Trip.Location pickup, Trip.Location dropoff) {
        return trips.save(new Trip(UUID.randomUUID(), riderId, pickup, dropoff, Instant.now()));
    }
    public Trip get(UUID id) { return trips.find(id).orElseThrow(() -> new java.util.NoSuchElementException("Trip not found")); }
    public List<Trip> list(String riderId, String driverId) {
        if (riderId != null) return trips.byRider(riderId);
        if (driverId != null) return trips.byDriver(driverId);
        throw new IllegalArgumentException("riderId or driverId is required");
    }
    public List<Trip> offers(String driverId) { return trips.offeredTo(driverId); }
    public Trip quote(UUID id, BigDecimal amount) { Trip t = get(id); t.quote(amount); return trips.save(t); }
    public Trip offer(UUID id, String driverId) { Trip t = get(id); t.offer(driverId); return trips.save(t); }
    public Trip confirm(UUID id) { Trip t = get(id); t.confirm(); return trips.save(t); }
    public Trip cancel(UUID id) { Trip t = get(id); t.cancel(); return trips.save(t); }
    public Trip reject(UUID id, String driverId) { Trip t = get(id); t.reject(driverId); return trips.save(t); }
    public Trip accept(UUID id, String driverId) {
        Trip t = get(id); t.accept(driverId); t = trips.save(t);
        events.tripAccepted(t);
        return t;
    }
    public Trip enRoute(UUID id) { Trip t = get(id); t.enRoute(); return trips.save(t); }
    public Trip pickup(UUID id) { Trip t = get(id); t.pickupPassenger(); return trips.save(t); }
    public Trip dropoff(UUID id) {
        Trip t = get(id);
        if (!t.status().equals("PaymentPending")) { t.dropoffPassenger(); t = trips.save(t); }
        events.rideCompleted(t);
        return t;
    }
    public void onPaymentSettled(UUID tripId, UUID paymentId) {
        Trip t = get(tripId);
        if (t.status().equals("Paid")) return;
        t.paid(paymentId); trips.save(t);
    }
    public void onPaymentFailed(UUID tripId) {
        Trip t = get(tripId);
        if (t.status().equals("Failed")) return;
        t.paymentFailed(); trips.save(t);
    }
}
