package com.swiftride.trip.infrastructure;

import com.swiftride.trip.domain.Trip;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Entity @Table(name = "trips")
class TripRow {
    @Id public UUID id;
    public String riderId, driverId, offeredDriverId, status, pickupAddress, dropoffAddress;
    public Double pickupLatitude, pickupLongitude, dropoffLatitude, dropoffLongitude;
    public BigDecimal quotedFare, finalFare;
    public UUID paymentId;
    public boolean confirmed;
    public Instant createdAt, updatedAt;
    protected TripRow() { }
    TripRow(Trip trip) {
        id = trip.id(); riderId = trip.riderId(); driverId = trip.driverId(); offeredDriverId = trip.offeredDriverId(); status = trip.status();
        pickupAddress = trip.pickup().address(); pickupLatitude = trip.pickup().latitude(); pickupLongitude = trip.pickup().longitude();
        dropoffAddress = trip.dropoff().address(); dropoffLatitude = trip.dropoff().latitude(); dropoffLongitude = trip.dropoff().longitude();
        quotedFare = trip.quotedFare(); finalFare = trip.finalFare(); paymentId = trip.paymentId(); createdAt = trip.createdAt(); updatedAt = trip.updatedAt(); confirmed = trip.confirmed();
    }
    Trip toDomain() {
        Trip trip = new Trip(id, riderId, new Trip.Location(pickupAddress, pickupLatitude, pickupLongitude),
                new Trip.Location(dropoffAddress, dropoffLatitude, dropoffLongitude), createdAt);
        trip.restore(driverId, offeredDriverId, status, quotedFare, finalFare, paymentId, updatedAt, confirmed);
        return trip;
    }
}
interface TripJpaRepository extends JpaRepository<TripRow, UUID> {
    List<TripRow> findByRiderIdOrderByCreatedAtDesc(String riderId);
    List<TripRow> findByDriverIdOrderByCreatedAtDesc(String driverId);
    List<TripRow> findByOfferedDriverIdAndStatus(String driverId, String status);
}
@Repository
class TripRepositoryJpa implements com.swiftride.trip.application.TripRepository {
    private final TripJpaRepository rows;
    TripRepositoryJpa(TripJpaRepository rows) { this.rows = rows; }
    public Trip save(Trip trip) { return rows.save(new TripRow(trip)).toDomain(); }
    public Optional<Trip> find(UUID id) { return rows.findById(id).map(TripRow::toDomain); }
    public List<Trip> byRider(String id) { return rows.findByRiderIdOrderByCreatedAtDesc(id).stream().map(TripRow::toDomain).toList(); }
    public List<Trip> byDriver(String id) { return rows.findByDriverIdOrderByCreatedAtDesc(id).stream().map(TripRow::toDomain).toList(); }
    public List<Trip> offeredTo(String id) { return rows.findByOfferedDriverIdAndStatus(id, "DriverOffered").stream().map(TripRow::toDomain).toList(); }
}
