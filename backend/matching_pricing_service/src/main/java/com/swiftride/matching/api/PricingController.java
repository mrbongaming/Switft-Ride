package com.swiftride.matching.api;

import com.swiftride.matching.application.PricingApplicationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/quote")
public class PricingController {
    private final PricingApplicationService service;
    public PricingController(PricingApplicationService service) { this.service = service; }
    public record Request(UUID tripId) { }
    @PostMapping public PricingApplicationService.Quote quote(@RequestBody Request r) { return service.quote(r.tripId()); }
}
