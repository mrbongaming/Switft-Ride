package com.swiftride.payment.infrastructure.messaging;

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
public class PaymentRabbitConfiguration {
    @Bean
    TopicExchange paymentEventsExchange() {
        return new TopicExchange(MessagingTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange paymentDeadLetterExchange() {
        return new TopicExchange(MessagingTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue rideCompletedQueue() {
        return QueueBuilder.durable(MessagingTopology.RIDE_COMPLETED_QUEUE)
                .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MessagingTopology.RIDE_COMPLETED_QUEUE + ".dead")
                .build();
    }

    @Bean
    Queue rideCompletedDeadLetterQueue() {
        return QueueBuilder.durable(MessagingTopology.RIDE_COMPLETED_QUEUE + ".dlq").build();
    }

    @Bean
    Binding rideCompletedBinding(Queue rideCompletedQueue, TopicExchange paymentEventsExchange) {
        return BindingBuilder.bind(rideCompletedQueue).to(paymentEventsExchange)
                .with(MessagingTopology.RIDE_COMPLETED_KEY);
    }

    @Bean
    Binding rideCompletedDeadLetterBinding(Queue rideCompletedDeadLetterQueue,
                                           TopicExchange paymentDeadLetterExchange) {
        return BindingBuilder.bind(rideCompletedDeadLetterQueue).to(paymentDeadLetterExchange)
                .with(MessagingTopology.RIDE_COMPLETED_QUEUE + ".dead");
    }

    @Bean
    MessageConverter paymentMessageConverter() {
        return new JacksonJsonMessageConverter("com.swiftride.contracts.events");
    }
}
