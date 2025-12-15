package com.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.amqp.support.converter.StandardJackson2MessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue and Exchange names
    public static final String EXCHANGE = "notification.exchange";
    public static final String MAIN_QUEUE = "notification.queue";
    public static final String DLQ_QUEUE = "notification.dlq";
    public static final String ROUTING_KEY = "notification.send";
    public static final String DLQ_ROUTING_KEY = "notification.dlq";

    // ===== EXCHANGE =====
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    // ===== MAIN QUEUE =====
    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    // ===== DLQ QUEUE =====
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    // ===== BINDINGS =====
    @Bean
    public Binding mainBinding(Queue mainQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(mainQueue)
                .to(notificationExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(dlqQueue)
                .to(notificationExchange)
                .with(DLQ_ROUTING_KEY);
    }

    // ===== MESSAGE CONVERTER (Jackson JSON) =====

    @Bean
    @SuppressWarnings("deprecation")
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }



}
