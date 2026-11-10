package com.swiftride.trip.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Trip {
    private final UUID id;
    private final String riderId;
    private final Location pickup;
    private final Location dropoff;
    private final Instant createdAt;
    private String driverId, offeredDriverId, status;
    private boolean confirmed;
    private BigDecimal quotedFare, finalFare;
    private UUID paymentId;
    private Instant updatedAt;

    public record Location(String address, Double latitude, Double longitude) {
        public Location {
            if (address == null || address.isBlank()) throw new IllegalArgumentException("Address is required");
            if ((latitude == null) != (longitude == null)) throw new IllegalArgumentException("Both coordinates are required");
            if (latitude != null && (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                    || Math.abs(latitude) > 90 || Math.abs(longitude) > 180)) throw new IllegalArgumentException("Invalid coordinates");
        }
    }
    public Trip(UUID id, String riderId, Location pickup, Location dropoff, Instant createdAt) {
        if (riderId == null || riderId.isBlank()) throw new IllegalArgumentException("Rider is required");
        this.id = id; this.riderId = riderId; this.pickup = pickup; this.dropoff = dropoff;
        this.createdAt = createdAt; this.updatedAt = createdAt; this.status = "Requested";
    }
    public void restore(String driverId, String offeredDriverId, String status, BigDecimal quotedFare,
                        BigDecimal finalFare, UUID paymentId, Instant updatedAt, boolean confirmed) {
        this.driverId = driverId; this.offeredDriverId = offeredDriverId; this.status = status;
        this.quotedFare = quotedFare; this.finalFare = finalFare; this.paymentId = paymentId; this.updatedAt = updatedAt; this.confirmed = confirmed;
    }
    private void require(String... allowed) {
        for (String value : allowed) if (status.equals(value)) return;
        throw new IllegalStateException("Cannot perform this action while trip is " + status);
    }
    private void change(String next) { status = next; updatedAt = Instant.now(); }
    public void quote(BigDecimal amount) {
        require("Requested", "Quoted");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Fare must be positive");
        quotedFare = amount; change("Quoted");
    }
    public void offer(String driver) {
        require("Quoted", "DriverOffered");
        if (driver == null || driver.isBlank()) throw new IllegalArgumentException("Driver is required");
        offeredDriverId = driver; change("DriverOffered");
    }
    public void confirm() { require("DriverOffered"); if (quotedFare == null) throw new IllegalStateException("Quote is missing"); confirmed = true; updatedAt = Instant.now(); }
    public void accept(String driver) {
        require("DriverOffered");
        if (!confirmed) throw new IllegalStateException("Rider has not confirmed the quote");
        if (!driver.equals(offeredDriverId)) throw new IllegalArgumentException("Offer belongs to another driver");
        driverId = driver; offeredDriverId = null; change("DriverAccepted");
    }
    public void reject(String driver) {
        require("DriverOffered");
        if (!driver.equals(offeredDriverId)) throw new IllegalArgumentException("Offer belongs to another driver");
        offeredDriverId = null; change("Quoted");
    }
    public void enRoute() { require("DriverAccepted"); change("EnRoute"); }
    public void pickupPassenger() { require("EnRoute"); change("PickedUp"); }
    public void dropoffPassenger() { require("PickedUp"); finalFare = quotedFare; change("PaymentPending"); }
    public void cancel() { require("Requested", "Quoted", "DriverOffered"); offeredDriverId = null; change("Cancelled"); }
    public void paid(UUID payment) { require("PaymentPending"); paymentId = payment; change("Paid"); }
    public void paymentFailed() { require("PaymentPending"); change("Failed"); }
    public UUID id() { return id; } public String riderId() { return riderId; }
    public Location pickup() { return pickup; } public Location dropoff() { return dropoff; }
    public String driverId() { return driverId; } public String offeredDriverId() { return offeredDriverId; }
    public String status() { return status; } public BigDecimal quotedFare() { return quotedFare; }
    public BigDecimal finalFare() { return finalFare; } public UUID paymentId() { return paymentId; }
    public Instant createdAt() { return createdAt; } public Instant updatedAt() { return updatedAt; }
    public boolean confirmed() { return confirmed; }
}
