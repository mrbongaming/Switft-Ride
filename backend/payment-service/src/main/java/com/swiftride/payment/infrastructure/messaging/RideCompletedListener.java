package com.swiftride.payment.infrastructure.messaging;

import com.swiftride.contracts.events.MessagingTopology;
import com.swiftride.contracts.events.RideCompletedEvent;
import com.swiftride.payment.application.RideCompletedHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "swiftride.messaging", name = "consumers-enabled", havingValue = "true")
public class RideCompletedListener {
    private final RideCompletedHandler handler;

    public RideCompletedListener(RideCompletedHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = MessagingTopology.RIDE_COMPLETED_QUEUE)
    public void onRideCompleted(RideCompletedEvent event) {
        handler.onRideCompleted(event);
    }
}
