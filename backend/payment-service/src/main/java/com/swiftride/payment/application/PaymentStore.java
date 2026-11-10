package com.swiftride.payment.application;

import com.swiftride.payment.domain.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentStore {
    Payment save(Payment payment);
    Optional<Payment> find(UUID id);
    Optional<Payment> byTrip(UUID tripId);
    Optional<Payment> byKey(String key);
    List<Payment> byRider(String riderId);
    void ledger(UUID paymentId, UUID tripId, String account, BigDecimal amount, String type);
    List<WalletLine> wallet(String driverId);
    record WalletLine(UUID id, UUID tripId, BigDecimal amount, String type, String status, Instant createdAt) { }
}
