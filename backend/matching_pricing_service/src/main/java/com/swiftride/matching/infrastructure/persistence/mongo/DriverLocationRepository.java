package com.swiftride.matching.infrastructure.persistence.mongo;

import com.swiftride.matching.domain.model.Coordinates;
import com.swiftride.matching.domain.model.DriverLocation;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Document("driver_locations")
class DriverRow {
    @Id public String driverId;
    public double latitude, longitude;
    public boolean available;
    public Instant updatedAt;
    protected DriverRow() { }
    DriverRow(DriverLocation d) { driverId = d.driverId(); latitude = d.coordinates().latitude(); longitude = d.coordinates().longitude(); available = d.available(); updatedAt = d.updatedAt(); }
    DriverLocation toDomain() { return new DriverLocation(driverId, new Coordinates(latitude, longitude), available, updatedAt); }
}
interface DriverMongoRepository extends MongoRepository<DriverRow, String> { List<DriverRow> findByAvailableTrue(); }
@Repository
public class DriverLocationRepository implements com.swiftride.matching.application.DriverDirectory {
    private final DriverMongoRepository rows;
    public DriverLocationRepository(DriverMongoRepository rows) { this.rows = rows; }
    public List<DriverLocation> available() { return rows.findByAvailableTrue().stream().map(DriverRow::toDomain).toList(); }
    public void save(DriverLocation d) { rows.save(new DriverRow(d)); }
    public void setAvailable(String driverId, boolean available) {
        rows.findById(driverId).ifPresent(row -> { row.available = available; row.updatedAt = Instant.now(); rows.save(row); });
    }
    public long count() { return rows.count(); }
}
