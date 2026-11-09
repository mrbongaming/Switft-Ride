package com.swiftride.payment.infrastructure.messaging;

import com.swiftride.contracts.events.MessagingTopology;
import com.swiftride.contracts.events.PaymentFailedEvent;
import com.swiftride.contracts.events.PaymentSettledEvent;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public CompletableFuture<Void> publishSettled(PaymentSettledEvent event) {
        return send(MessagingTopology.PAYMENT_SETTLED_KEY, event.eventId(), event.correlationId(), event);
    }

    public CompletableFuture<Void> publishFailed(PaymentFailedEvent event) {
        return send(MessagingTopology.PAYMENT_FAILED_KEY, event.eventId(), event.correlationId(), event);
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
