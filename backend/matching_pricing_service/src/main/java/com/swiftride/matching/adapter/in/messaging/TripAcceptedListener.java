package com.swiftride.matching.adapter.in.messaging;

import com.swiftride.contracts.events.MessagingTopology;
import com.swiftride.contracts.events.TripAcceptedEvent;
import com.swiftride.matching.application.port.in.HandleTripAcceptedUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "swiftride.messaging", name = "consumers-enabled", havingValue = "true")
public class TripAcceptedListener {
    private final HandleTripAcceptedUseCase useCase;

    public TripAcceptedListener(HandleTripAcceptedUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = MessagingTopology.TRIP_ACCEPTED_QUEUE)
    public void onTripAccepted(TripAcceptedEvent event) {
        useCase.handle(event);
    }
}

