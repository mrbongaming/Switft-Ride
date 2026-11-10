package com.swiftride.matching.infrastructure.client.trip;

import com.swiftride.matching.application.TripCatalog;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TripServiceClient implements TripCatalog {
    private final RestClient client;
    public TripServiceClient(@Value("${swiftride.trip-url:http://localhost:8081}") String url) { client = RestClient.create(url); }
    public TripView get(UUID id) { return client.get().uri("/trips/{id}", id).retrieve().body(TripView.class); }
    public void quote(UUID id, BigDecimal amount) { client.post().uri("/trips/{id}/quote", id).body(Map.of("amount", amount)).retrieve().toBodilessEntity(); }
    public void offer(UUID id, String driver) { client.post().uri("/trips/{id}/offer", id).body(Map.of("driverId", driver)).retrieve().toBodilessEntity(); }
    public List<TripView> offers(String driver) {
        TripView[] result = client.get().uri("/trips/offers?driverId={id}", driver).retrieve().body(TripView[].class);
        return result == null ? List.of() : Arrays.asList(result);
    }
}
