package com.swiftride.trip.infrastructure.messaging;

import com.swiftride.contracts.events.MessagingTopology;
import com.swiftride.contracts.events.PaymentFailedEvent;
import com.swiftride.contracts.events.PaymentSettledEvent;
import com.swiftride.trip.application.PaymentResultHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "swiftride.messaging", name = "consumers-enabled", havingValue = "true")
public class PaymentResultListener {
    private final PaymentResultHandler handler;

    public PaymentResultListener(PaymentResultHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = MessagingTopology.PAYMENT_SETTLED_QUEUE)
    public void onSettled(PaymentSettledEvent event) {
        handler.onPaymentSettled(event);
    }

    @RabbitListener(queues = MessagingTopology.PAYMENT_FAILED_QUEUE)
    public void onFailed(PaymentFailedEvent event) {
        handler.onPaymentFailed(event);
    }
}
