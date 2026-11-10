package com.swiftride.matching.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Pure domain aggregate. Add location and availability invariants here; no Spring annotations. */
public final class DriverLocation {

    private final String driverId;
    private Coordinates coordinates;
    private boolean available;
    private Instant updatedAt;

    public DriverLocation(
        String driverId, 
        Coordinates coordinates, 
        boolean available, 
        Instant updatedAt) {
        if (driverId == null || driverId.isEmpty()) {
            throw new IllegalArgumentException("driverId cannot be null or empty");
        }
        this.driverId = driverId;
        this.coordinates = Objects.requireNonNull(
            coordinates,
            "coordinate cannot be null");
        this.available = available;
        this.updatedAt = Objects.requireNonNull(
            updatedAt,
            "updatedAt cannot be null");
    }

    public void updateLocation(
        Coordinates newCoordinates,
        Instant updateTime) {
        Objects.requireNonNull(
            newCoordinates,
            "newCoordinate cannot be null");
        Objects.requireNonNull(
            updateTime,
            "updateTime cannot be null");

        if(updateTime.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("updateTime cannot be before the last updated time");
        }

    coordinates = newCoordinates;
    updatedAt = updateTime;
    }
    
    public void markAvailable(){
        available = true;
    }

    public String driverId() {
        return driverId;
    }

    public  Coordinates coordinates() {
        return coordinates;
    }

    public boolean available() {
        return available;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
    
}

