package com.swiftride.payment.application;

import com.swiftride.payment.domain.Payment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Demo wallet settlement with balanced ledger entries and idempotent ride processing. */
@Service
public class PaymentApplicationService implements RideCompletedHandler {
    private final PaymentStore store;
    private final PaymentEvents events;
    public PaymentApplicationService(PaymentStore store, PaymentEvents events) { this.store = store; this.events = events; }
    public Payment get(UUID id) { return store.find(id).orElseThrow(() -> new java.util.NoSuchElementException("Payment not found")); }
    public List<Payment> list(String rider) { return store.byRider(rider); }
    @Transactional
    public Payment create(UUID tripId, String riderId, String driverId, BigDecimal amount, String currency, String method, String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Idempotency-Key is required");
        var previous = store.byKey(key);
        if (previous.isPresent()) {
            if (!previous.get().tripId().equals(tripId) || previous.get().amount().compareTo(amount) != 0)
                throw new IllegalArgumentException("Idempotency key belongs to another payment request");
            return previous.get();
        }
        var sameTrip = store.byTrip(tripId);
        if (sameTrip.isPresent()) return sameTrip.get();
        Payment p = new Payment(UUID.randomUUID(), tripId, riderId, driverId, amount, currency, method, key, Instant.now(), "Succeeded");
        p = store.save(p);
        recordLedger(p, false);
        return p;
    }
    @Transactional
    public Payment refund(UUID id, String reason, String key) {
        if (reason == null || reason.isBlank() || key == null || key.isBlank()) throw new IllegalArgumentException("Reason and Idempotency-Key are required");
        Payment p = get(id);
        if (p.status().equals("Refunded")) return p;
        p.refund(); p = store.save(p); recordLedger(p, true); return p;
    }
    private void recordLedger(Payment p, boolean reverse) {
        BigDecimal driverShare = p.driverId() == null ? BigDecimal.ZERO : p.amount().multiply(new BigDecimal("0.80")).setScale(0, RoundingMode.HALF_UP);
        BigDecimal platformShare = p.amount().subtract(driverShare);
        BigDecimal sign = reverse ? BigDecimal.valueOf(-1) : BigDecimal.ONE;
        store.ledger(p.id(), p.tripId(), "rider:" + p.riderId(), p.amount().negate().multiply(sign), "ADJUSTMENT");
        if (p.driverId() != null) store.ledger(p.id(), p.tripId(), "driver:" + p.driverId(), driverShare.multiply(sign), reverse ? "WITHDRAWAL" : "EARNING");
        store.ledger(p.id(), p.tripId(), "platform", platformShare.multiply(sign), "ADJUSTMENT");
    }
    public record Wallet(String driverId, BigDecimal balance, BigDecimal pendingBalance, String currency, List<PaymentStore.WalletLine> entries) { }
    public Wallet wallet(String driverId) {
        var lines = store.wallet(driverId);
        BigDecimal balance = lines.stream().map(line -> line.type().equals("WITHDRAWAL") ? line.amount().negate() : line.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Wallet(driverId, balance, BigDecimal.ZERO, "VND", lines);
    }
    @Override @Transactional public void onRideCompleted(UUID tripId, String riderId, String driverId,
                                                           BigDecimal amount, String currency, String idempotencyKey) {
        Payment p = create(tripId, riderId, driverId, amount, currency, "WALLET", idempotencyKey);
        events.settled(tripId, p.id());
    }
}
