package com.swiftride.matching.application;

import com.swiftride.matching.domain.model.DriverLocation;
import java.util.List;

public interface DriverDirectory {
    List<DriverLocation> available();
    void save(DriverLocation location);
    void setAvailable(String driverId, boolean available);
    long count();
}
