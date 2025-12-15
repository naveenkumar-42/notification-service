package com.notification.service;

import com.notification.config.RabbitMQConfig;
import com.notification.entity.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        setupCallbacks();
    }

    // Setup callbacks for message acknowledgment
    private void setupCallbacks() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("Message acknowledged by broker: {}", correlationData.getId());
            } else {
                log.error("Message NOT acknowledged: {}", cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.warn("Message returned - Code: {}, Text: {}",
                    returnedMessage.getReplyCode(),
                    returnedMessage.getReplyText());
        });
    }

    // Send notification to main queue
    public void sendNotification(NotificationEvent event) {
        try {
            String payload = "EVENT_ID=" + event.getId();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    payload
            );
            log.info("✓ Notification queued successfully - Event ID: {}", event.getId());
        } catch (Exception e) {
            log.error("✗ Failed to send notification: {}", e.getMessage());
            throw new RuntimeException("Failed to queue notification", e);
        }
    }

    // Re-queue message for retry
    public void sendToRetryQueue(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    event
            );
            log.info("→ Notification re-queued for retry - Event ID: {}", event.getId());
        } catch (Exception e) {
            log.error("✗ Failed to re-queue notification: {}", e.getMessage());
        }
    }
}
