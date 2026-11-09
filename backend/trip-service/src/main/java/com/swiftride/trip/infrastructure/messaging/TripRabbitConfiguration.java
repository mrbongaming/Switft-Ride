package com.swiftride.trip.infrastructure.messaging;

import com.swiftride.contracts.events.MessagingTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TripRabbitConfiguration {
    @Bean
    TopicExchange tripEventsExchange() {
        return new TopicExchange(MessagingTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange tripDeadLetterExchange() {
        return new TopicExchange(MessagingTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue paymentSettledQueue() {
        return QueueBuilder.durable(MessagingTopology.PAYMENT_SETTLED_QUEUE)
                .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MessagingTopology.PAYMENT_SETTLED_QUEUE + ".dead")
                .build();
    }

    @Bean
    Queue paymentFailedQueue() {
        return QueueBuilder.durable(MessagingTopology.PAYMENT_FAILED_QUEUE)
                .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MessagingTopology.PAYMENT_FAILED_QUEUE + ".dead")
                .build();
    }

    @Bean
    Queue paymentSettledDeadLetterQueue() {
        return QueueBuilder.durable(MessagingTopology.PAYMENT_SETTLED_QUEUE + ".dlq").build();
    }

    @Bean
    Queue paymentFailedDeadLetterQueue() {
        return QueueBuilder.durable(MessagingTopology.PAYMENT_FAILED_QUEUE + ".dlq").build();
    }

    @Bean
    Binding paymentSettledBinding(Queue paymentSettledQueue, TopicExchange tripEventsExchange) {
        return BindingBuilder.bind(paymentSettledQueue).to(tripEventsExchange)
                .with(MessagingTopology.PAYMENT_SETTLED_KEY);
    }

    @Bean
    Binding paymentFailedBinding(Queue paymentFailedQueue, TopicExchange tripEventsExchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(tripEventsExchange)
                .with(MessagingTopology.PAYMENT_FAILED_KEY);
    }

    @Bean
    Binding paymentSettledDeadLetterBinding(Queue paymentSettledDeadLetterQueue,
                                           TopicExchange tripDeadLetterExchange) {
        return BindingBuilder.bind(paymentSettledDeadLetterQueue).to(tripDeadLetterExchange)
                .with(MessagingTopology.PAYMENT_SETTLED_QUEUE + ".dead");
    }

    @Bean
    Binding paymentFailedDeadLetterBinding(Queue paymentFailedDeadLetterQueue,
                                          TopicExchange tripDeadLetterExchange) {
        return BindingBuilder.bind(paymentFailedDeadLetterQueue).to(tripDeadLetterExchange)
                .with(MessagingTopology.PAYMENT_FAILED_QUEUE + ".dead");
    }

    @Bean
    MessageConverter tripMessageConverter() {
        return new JacksonJsonMessageConverter("com.swiftride.contracts.events");
    }
}
