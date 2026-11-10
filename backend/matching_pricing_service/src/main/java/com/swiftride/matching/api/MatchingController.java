package com.swiftride.matching.api;

import com.swiftride.matching.application.MatchingApplicationService;
import com.swiftride.matching.application.TripCatalog;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/match")
public class MatchingController {
    private final MatchingApplicationService service;
    public MatchingController(MatchingApplicationService service) { this.service = service; }
    public record Request(UUID tripId, List<String> excludedDriverIds) { }
    public record LocationRequest(double latitude, double longitude, boolean available) { }
    @PostMapping public MatchingApplicationService.Match match(@RequestBody Request r) { return service.match(r.tripId(), r.excludedDriverIds()); }
    @GetMapping("/offers") public List<TripCatalog.TripView> offers(@RequestParam String driverId) { return service.offers(driverId); }
    @PostMapping("/drivers/{id}/location") public void location(@PathVariable String id, @RequestBody LocationRequest r) {
        service.updateLocation(id, r.latitude(), r.longitude(), r.available());
    }
}
