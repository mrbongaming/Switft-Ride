package com.swiftride.trip.api;

import com.swiftride.trip.application.TripApplicationService;
import com.swiftride.trip.domain.Trip;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/trips")
public class TripController {
    private final TripApplicationService service;
    public TripController(TripApplicationService service) { this.service = service; }
    public record CreateRequest(String riderId, Trip.Location pickup, Trip.Location dropoff) { }
    public record QuoteRequest(BigDecimal amount) { }
    public record DriverRequest(String driverId) { }
    public record Response(UUID id, String riderId, String driverId, String offeredDriverId, Trip.Location pickup, Trip.Location dropoff,
                           String status, BigDecimal quotedFare, BigDecimal finalFare, String currency,
                           UUID paymentId, java.time.Instant createdAt, java.time.Instant updatedAt) {
        static Response from(Trip t) { return new Response(t.id(), t.riderId(), t.driverId(), t.offeredDriverId(), t.pickup(), t.dropoff(),
                t.status(), t.quotedFare(), t.finalFare(), "VND", t.paymentId(), t.createdAt(), t.updatedAt()); }
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Response create(@RequestBody CreateRequest r) { return Response.from(service.create(r.riderId(), r.pickup(), r.dropoff())); }
    @GetMapping public List<Response> list(@RequestParam(required = false) String riderId, @RequestParam(required = false) String driverId) {
        return service.list(riderId, driverId).stream().map(Response::from).toList();
    }
    @GetMapping("/offers") public List<Response> offers(@RequestParam String driverId) {
        return service.offers(driverId).stream().map(Response::from).toList();
    }
    @GetMapping("/{id}") public Response get(@PathVariable UUID id) { return Response.from(service.get(id)); }
    @PostMapping("/{id}/quote") public Response quote(@PathVariable UUID id, @RequestBody QuoteRequest r) { return Response.from(service.quote(id, r.amount())); }
    @PostMapping("/{id}/offer") public Response offer(@PathVariable UUID id, @RequestBody DriverRequest r) { return Response.from(service.offer(id, r.driverId())); }
    @PostMapping("/{id}/confirm-quote") public Response confirm(@PathVariable UUID id) { return Response.from(service.confirm(id)); }
    @PostMapping("/{id}/cancel") public Response cancel(@PathVariable UUID id) { return Response.from(service.cancel(id)); }
    @PostMapping("/{id}/driver-accept") public Response accept(@PathVariable UUID id, @RequestBody DriverRequest r) { return Response.from(service.accept(id, r.driverId())); }
    @PostMapping("/{id}/driver-reject") public Response reject(@PathVariable UUID id, @RequestBody DriverRequest r) { return Response.from(service.reject(id, r.driverId())); }
    @PostMapping("/{id}/en-route") public Response enRoute(@PathVariable UUID id) { return Response.from(service.enRoute(id)); }
    @PostMapping("/{id}/pickup") public Response pickup(@PathVariable UUID id) { return Response.from(service.pickup(id)); }
    @PostMapping("/{id}/dropoff") public Response dropoff(@PathVariable UUID id) { return Response.from(service.dropoff(id)); }
}
