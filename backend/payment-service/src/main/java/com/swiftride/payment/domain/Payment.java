package com.swiftride.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Payment {
    private final UUID id, tripId;
    private final String riderId, driverId, currency, method, idempotencyKey;
    private final BigDecimal amount;
    private final Instant createdAt;
    private String status;
    public Payment(UUID id, UUID tripId, String riderId, String driverId, BigDecimal amount,
                   String currency, String method, String idempotencyKey, Instant createdAt, String status) {
        if (tripId == null || riderId == null || riderId.isBlank() || amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Valid trip, rider and positive amount are required");
        if (!"VND".equals(currency)) throw new IllegalArgumentException("Only VND is supported");
        if (!"WALLET".equals(method)) throw new IllegalArgumentException("Only demo wallet payments are supported");
        this.id = id; this.tripId = tripId; this.riderId = riderId; this.driverId = driverId;
        this.amount = amount; this.currency = currency; this.method = method;
        this.idempotencyKey = idempotencyKey; this.createdAt = createdAt; this.status = status;
    }
    public void refund() {
        if (!status.equals("Succeeded")) throw new IllegalStateException("Only successful payments can be refunded");
        status = "Refunded";
    }
    public UUID id() { return id; } public UUID tripId() { return tripId; }
    public String riderId() { return riderId; } public String driverId() { return driverId; }
    public BigDecimal amount() { return amount; } public String currency() { return currency; }
    public String method() { return method; } public String idempotencyKey() { return idempotencyKey; }
    public Instant createdAt() { return createdAt; } public String status() { return status; }
}
