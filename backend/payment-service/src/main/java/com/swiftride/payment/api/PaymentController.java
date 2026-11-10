package com.swiftride.payment.api;

import com.swiftride.payment.application.PaymentApplicationService;
import com.swiftride.payment.domain.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {
    private final PaymentApplicationService service;
    public PaymentController(PaymentApplicationService service) { this.service = service; }
    public record CreateRequest(UUID tripId, String riderId, BigDecimal amount, String currency, String method) { }
    public record RefundRequest(String reason) { }
    public record Response(UUID id, UUID tripId, BigDecimal amount, String currency, String status, Instant createdAt, String method) {
        static Response from(Payment p) { return new Response(p.id(), p.tripId(), p.amount(), p.currency(), p.status(), p.createdAt(), p.method()); }
    }
    @GetMapping("/payments") public List<Response> list(@RequestParam String riderId) { return service.list(riderId).stream().map(Response::from).toList(); }
    @GetMapping("/payments/{id}") public Response get(@PathVariable UUID id) { return Response.from(service.get(id)); }
    @PostMapping("/payments") @ResponseStatus(HttpStatus.CREATED)
    public Response create(@RequestBody CreateRequest r, @RequestHeader("Idempotency-Key") String key) {
        return Response.from(service.create(r.tripId(), r.riderId(), null, r.amount(), r.currency(), r.method(), key));
    }
    @PostMapping("/payments/{id}/refund") public Response refund(@PathVariable UUID id, @RequestBody RefundRequest r,
                                                                   @RequestHeader("Idempotency-Key") String key) {
        return Response.from(service.refund(id, r.reason(), key));
    }
    @GetMapping("/drivers/{id}/wallet") public PaymentApplicationService.Wallet wallet(@PathVariable String id) { return service.wallet(id); }
}
