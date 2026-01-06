package com.notification.service;

import com.notification.config.RabbitMQConfig;
import com.notification.entity.AuditLog;
import com.notification.entity.NotificationEvent;
import com.notification.repository.AuditLogRepository;
import com.notification.repository.NotificationEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
@Slf4j
public class NotificationConsumer {

    private final NotificationEventRepository eventRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationProducer producer;
    private final NotificationDeliveryService deliveryService;

    public NotificationConsumer(NotificationEventRepository eventRepository,
                                AuditLogRepository auditLogRepository,
                                NotificationProducer producer,
                                NotificationDeliveryService deliveryService) {
        this.eventRepository = eventRepository;
        this.auditLogRepository = auditLogRepository;
        this.producer = producer;
        this.deliveryService = deliveryService;
    }

    @RabbitListener(queues = RabbitMQConfig.MAIN_QUEUE)
    public void consumeNotification(NotificationEvent event,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("📨 CONSUMING Message - Event ID: {}", event.getId());
        log.info("📧 Recipient: {}", event.getRecipient());
        log.info("📤 Channel: {}", event.getChannel());

        try {
            // ✅ SEND NOTIFICATION
            deliveryService.deliver(event);

            // ✅ UPDATE STATUS TO DELIVERED
            event.setStatus("DELIVERED");
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);

            // ✅ CREATE AUDIT LOG
            auditLogRepository.save(AuditLog.builder()
                    .eventId(event.getId())
                    .action("SENT")
                    .details("Notification sent successfully to " + event.getRecipient())
                    .timestamp(LocalDateTime.now())
                    .build());

            // ✅ ACKNOWLEDGE MESSAGE (CRITICAL - tells RabbitMQ to delete from queue)
            channel.basicAck(deliveryTag, false);
            log.info("✅ Message acknowledged and removed from queue - Event ID: {}", event.getId());

            log.info("✅ NOTIFICATION SENT SUCCESSFULLY - Event ID: {}", event.getId());

        } catch (Exception e) {
            log.error("❌ Error processing notification - Event ID: {}, Error: {}", event.getId(), e.getMessage(), e);
            handleFailure(event, channel, deliveryTag, e);
        }
    }

    private void handleFailure(NotificationEvent event, Channel channel, long deliveryTag, Exception e) {
        log.warn("🔄 Handling notification failure for Event ID: {}", event.getId());

        event.setFailureReason(e.getMessage());
        event.setUpdatedAt(LocalDateTime.now());

        try {
            if (event.getRetryCount() < 3) {
                log.warn("📤 Scheduling retry - Attempt {}/3 for Event ID: {}",
                        event.getRetryCount() + 1, event.getId());

                event.setRetryCount(event.getRetryCount() + 1);
                event.setStatus("RETRY_SCHEDULED");
                eventRepository.save(event);

                auditLogRepository.save(AuditLog.builder()
                        .eventId(event.getId())
                        .action("RETRY_SCHEDULED")
                        .details("Retrying notification, attempt " + event.getRetryCount())
                        .timestamp(LocalDateTime.now())
                        .build());

                producer.sendToRetryQueue(event);

                // ✅ NACK and requeue to retry queue
                channel.basicNack(deliveryTag, false, false);
                log.warn("⚠️ Message NACK'd - Event ID: {}", event.getId());

            } else {
                log.error("❌ MAX RETRIES EXCEEDED - Moving to DLQ for Event ID: {}", event.getId());

                event.setStatus("FAILED");
                eventRepository.save(event);

                auditLogRepository.save(AuditLog.builder()
                        .eventId(event.getId())
                        .action("FAILED")
                        .details("Max retries (3) exceeded: " + e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());

                // Send to DLQ
                producer.sendToDLQ(event);

                // ✅ Acknowledge to remove from main queue (going to DLQ)
                channel.basicAck(deliveryTag, false);
                log.error("❌ Message moved to DLQ and acknowledged - Event ID: {}", event.getId());
            }
        } catch (IOException ioException) {
            log.error("❌ Failed to send ACK/NACK: {}", ioException.getMessage());
        }
    }
}