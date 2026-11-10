package com.swiftride.matching.infrastructure.configuration;

import com.swiftride.matching.application.MatchingApplicationService;
import com.swiftride.matching.application.DriverDirectory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DemoDrivers {
    @Bean CommandLineRunner seedDrivers(DriverDirectory repository, MatchingApplicationService service) {
        return args -> {
            if (repository.count() == 0) {
                service.updateLocation("driver-1", 10.7769, 106.7009, true);
                service.updateLocation("driver-2", 10.7820, 106.6930, true);
            }
        };
    }
}
