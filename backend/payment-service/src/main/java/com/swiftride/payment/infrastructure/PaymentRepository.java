package com.swiftride.payment.infrastructure;

import com.swiftride.payment.application.PaymentStore;
import com.swiftride.payment.domain.Payment;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Entity @Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(columnNames = "trip_id"), @UniqueConstraint(columnNames = "idempotency_key")})
class PaymentRow {
    @Id public UUID id;
    public UUID tripId;
    public String riderId, driverId, currency, method, idempotencyKey, status;
    public BigDecimal amount;
    public Instant createdAt;
    protected PaymentRow() { }
    PaymentRow(Payment p) {
        id = p.id(); tripId = p.tripId(); riderId = p.riderId(); driverId = p.driverId(); currency = p.currency();
        method = p.method(); idempotencyKey = p.idempotencyKey(); status = p.status(); amount = p.amount(); createdAt = p.createdAt();
    }
    Payment toDomain() { return new Payment(id, tripId, riderId, driverId, amount, currency, method, idempotencyKey, createdAt, status); }
}
@Entity @Table(name = "ledger_entries")
class LedgerRow {
    @Id public UUID id;
    public UUID paymentId, tripId;
    public String account, type;
    public BigDecimal amount;
    public Instant createdAt;
    protected LedgerRow() { }
    LedgerRow(UUID paymentId, UUID tripId, String account, BigDecimal amount, String type) {
        id = UUID.randomUUID(); this.paymentId = paymentId; this.tripId = tripId; this.account = account;
        this.amount = amount; this.type = type; createdAt = Instant.now();
    }
}
interface PaymentJpaRepository extends JpaRepository<PaymentRow, UUID> {
    Optional<PaymentRow> findByTripId(UUID tripId);
    Optional<PaymentRow> findByIdempotencyKey(String key);
    List<PaymentRow> findByRiderIdOrderByCreatedAtDesc(String riderId);
}
interface LedgerJpaRepository extends JpaRepository<LedgerRow, UUID> { List<LedgerRow> findByAccountOrderByCreatedAtDesc(String account); }
@Repository
public class PaymentRepository implements PaymentStore {
    private final PaymentJpaRepository payments;
    private final LedgerJpaRepository ledger;
    public PaymentRepository(PaymentJpaRepository payments, LedgerJpaRepository ledger) { this.payments = payments; this.ledger = ledger; }
    public Payment save(Payment p) { return payments.save(new PaymentRow(p)).toDomain(); }
    public Optional<Payment> find(UUID id) { return payments.findById(id).map(PaymentRow::toDomain); }
    public Optional<Payment> byTrip(UUID id) { return payments.findByTripId(id).map(PaymentRow::toDomain); }
    public Optional<Payment> byKey(String key) { return payments.findByIdempotencyKey(key).map(PaymentRow::toDomain); }
    public List<Payment> byRider(String id) { return payments.findByRiderIdOrderByCreatedAtDesc(id).stream().map(PaymentRow::toDomain).toList(); }
    public void ledger(UUID paymentId, UUID tripId, String account, BigDecimal amount, String type) {
        ledger.save(new LedgerRow(paymentId, tripId, account, amount, type));
    }
    public List<WalletLine> wallet(String driverId) {
        return ledger.findByAccountOrderByCreatedAtDesc("driver:" + driverId).stream()
                .map(row -> new WalletLine(row.id, row.tripId, row.amount.abs(), row.type, "Succeeded", row.createdAt)).toList();
    }
}
