package com.swiftride.matching.infrastructure.messaging;

import com.swiftride.contracts.events.MessagingTopology;
import com.swiftride.contracts.events.TripAcceptedEvent;
import com.swiftride.matching.application.TripAcceptedHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "swiftride.messaging", name = "consumers-enabled", havingValue = "true")
public class TripAcceptedListener {
    private final TripAcceptedHandler handler;

    public TripAcceptedListener(TripAcceptedHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = MessagingTopology.TRIP_ACCEPTED_QUEUE)
    public void onTripAccepted(TripAcceptedEvent event) {
        handler.onTripAccepted(event);
    }
}
