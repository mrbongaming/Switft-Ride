package com.swiftride.matching.infrastructure.messaging;

import com.swiftride.contracts.events.MessagingTopology;
import com.swiftride.contracts.events.TripAcceptedEvent;
import com.swiftride.matching.application.HandleTripAcceptedUseCase;
import com.swiftride.matching.application.TripAcceptedCommand;
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
        useCase.handle(new TripAcceptedCommand(
                event.eventId(),
                event.tripId(),
                event.driverId(),
                event.occurredAt(),
                event.correlationId()
        ));
    }
}
