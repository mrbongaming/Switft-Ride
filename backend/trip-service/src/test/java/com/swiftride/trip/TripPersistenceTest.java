package com.swiftride.trip;

import static org.junit.jupiter.api.Assertions.*;
import com.swiftride.trip.application.TripApplicationService;
import com.swiftride.trip.application.UserApplicationService;
import com.swiftride.trip.domain.Trip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:triptest;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "swiftride.messaging.consumers-enabled=false",
    "spring.rabbitmq.listener.simple.auto-startup=false"
})
class TripPersistenceTest {
    @Autowired TripApplicationService trips;
    @Autowired UserApplicationService users;
    @Test void persistsTripsAndSeedsDemoAccounts() {
        assertFalse(users.list("rider").isEmpty());
        Trip created = trips.create("rider-1", new Trip.Location("A", 10.77, 106.70), new Trip.Location("B", 10.78, 106.71));
        assertEquals(created.id(), trips.get(created.id()).id());
        assertEquals(1, trips.list("rider-1", null).size());
    }
}
