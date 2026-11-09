package com.swiftride.matching.infrastructure.messaging;

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
public class MatchingRabbitConfiguration {
    @Bean
    TopicExchange matchingEventsExchange() {
        return new TopicExchange(MessagingTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange matchingDeadLetterExchange() {
        return new TopicExchange(MessagingTopology.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue tripAcceptedQueue() {
        return QueueBuilder.durable(MessagingTopology.TRIP_ACCEPTED_QUEUE)
                .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MessagingTopology.TRIP_ACCEPTED_QUEUE + ".dead")
                .build();
    }

    @Bean
    Queue tripAcceptedDeadLetterQueue() {
        return QueueBuilder.durable(MessagingTopology.TRIP_ACCEPTED_QUEUE + ".dlq").build();
    }

    @Bean
    Binding tripAcceptedBinding(Queue tripAcceptedQueue, TopicExchange matchingEventsExchange) {
        return BindingBuilder.bind(tripAcceptedQueue).to(matchingEventsExchange)
                .with(MessagingTopology.TRIP_ACCEPTED_KEY);
    }

    @Bean
    Binding tripAcceptedDeadLetterBinding(Queue tripAcceptedDeadLetterQueue,
                                          TopicExchange matchingDeadLetterExchange) {
        return BindingBuilder.bind(tripAcceptedDeadLetterQueue).to(matchingDeadLetterExchange)
                .with(MessagingTopology.TRIP_ACCEPTED_QUEUE + ".dead");
    }

    @Bean
    MessageConverter matchingMessageConverter() {
        return new JacksonJsonMessageConverter("com.swiftride.contracts.events");
    }
}
