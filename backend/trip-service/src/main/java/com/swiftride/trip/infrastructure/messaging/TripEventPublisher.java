package com.swiftride.trip.infrastructure.messaging;

import com.swiftride.contracts.events.MessagingTopology;
import com.swiftride.contracts.events.RideCompletedEvent;
import com.swiftride.contracts.events.TripAcceptedEvent;
import com.swiftride.trip.application.TripEvents;
import com.swiftride.trip.domain.Trip;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TripEventPublisher implements TripEvents {
    private final RabbitTemplate rabbitTemplate;

    public TripEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public CompletableFuture<Void> publishTripAccepted(TripAcceptedEvent event) {
        return send(MessagingTopology.TRIP_ACCEPTED_KEY, event.eventId(), event.correlationId(), event);
    }
    public void tripAccepted(Trip trip) {
        publishTripAccepted(new TripAcceptedEvent(UUID.randomUUID(), trip.id(), trip.driverId(), Instant.now(), trip.id().toString())).join();
    }
    public void rideCompleted(Trip trip) {
        publishRideCompleted(new RideCompletedEvent(UUID.randomUUID(), trip.id(), trip.riderId(), trip.driverId(),
                trip.finalFare(), "VND", "WALLET", Instant.now(), trip.id().toString(), "ride-" + trip.id())).join();
    }

    public CompletableFuture<Void> publishRideCompleted(RideCompletedEvent event) {
        return send(MessagingTopology.RIDE_COMPLETED_KEY, event.eventId(), event.correlationId(), event);
    }

    private CompletableFuture<Void> send(String routingKey, UUID eventId, String correlationId, Object event) {
        CorrelationData confirmation = new CorrelationData(eventId.toString());
        rabbitTemplate.convertAndSend(MessagingTopology.EVENTS_EXCHANGE, routingKey, event, message -> {
            message.getMessageProperties().setMessageId(eventId.toString());
            message.getMessageProperties().setCorrelationId(correlationId);
            return message;
        }, confirmation);
        return confirmation.getFuture().thenAccept(result -> {
            if (!result.ack() || confirmation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ did not confirm routing for event " + eventId);
            }
        });
    }
}
