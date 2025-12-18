package com.notification.service;

import com.notification.config.RabbitMQConfig;
import com.notification.entity.AuditLog;
import com.notification.entity.NotificationEvent;
import com.notification.repository.AuditLogRepository;
import com.notification.repository.NotificationEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

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
    public void consumeNotification(NotificationEvent event) {
        log.info("========================================");
        log.info("📨 CONSUMING Message - Event ID: {}", event.getId());
        log.info(" Recipient: {}", event.getRecipient());
        log.info(" Channel: {}", event.getChannel());
        log.info("========================================");

        try {
            deliveryService.deliver(event);

            event.setStatus("SENT");
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);

            auditLogRepository.save(AuditLog.builder()
                    .eventId(event.getId())
                    .action("SENT")
                    .details("Notification sent successfully to " + event.getRecipient())
                    .timestamp(LocalDateTime.now())
                    .build());

            log.info("✓✓ NOTIFICATION SENT SUCCESSFULLY ✓✓\n");

        } catch (Exception e) {
            log.error("Error processing notification: {}", e.getMessage(), e);
            handleFailure(event, e);
        }
    }

    private void handleFailure(NotificationEvent event, Exception e) {
        log.warn("Notification failed - Event ID: {}", event.getId());
        event.setFailureReason(e.getMessage());
        event.setUpdatedAt(LocalDateTime.now());

        if (event.getRetryCount() < 3) {
            log.warn("→ Scheduling retry (Attempt {}/3)", event.getRetryCount() + 1);
            event.setRetryCount(event.getRetryCount() + 1);
            event.setStatus("RETRY");
            eventRepository.save(event);

            auditLogRepository.save(AuditLog.builder()
                    .eventId(event.getId())
                    .action("RETRY_SCHEDULED")
                    .details("Retrying notification, attempt " + event.getRetryCount())
                    .timestamp(LocalDateTime.now())
                    .build());

            producer.sendToRetryQueue(event);

        } else {
            log.error("MAX RETRIES EXCEEDED - Moving to DLQ");
            event.setStatus("FAILED");
            eventRepository.save(event);

            auditLogRepository.save(AuditLog.builder()
                    .eventId(event.getId())
                    .action("FAILED")
                    .details("Max retries (3) exceeded: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
}
