package com.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String MAIN_QUEUE = "notification.queue";
    public static final String DLQ_QUEUE = "notification.dlq";

    // Exchange
    public static final String EXCHANGE = "notification.exchange";

    // Routing keys
    public static final String ROUTING_KEY = "notification.send";
    public static final String DLQ_ROUTING_KEY = "notification.dlq";

    // ===== MAIN QUEUE =====
    @Bean
    public Queue mainQueue() {
        return QueueBuilder
                .durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    // ===== DEAD LETTER QUEUE =====
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder
                .durable(DLQ_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 hours
                .build();
    }

    // ===== EXCHANGE =====
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    // ===== BINDINGS =====
    @Bean
    public Binding mainBinding() {
        return BindingBuilder
                .bind(mainQueue())
                .to(exchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(dlqQueue())
                .to(exchange())
                .with(DLQ_ROUTING_KEY);
    }
}
