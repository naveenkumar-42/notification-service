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
public class DLQConsumer {

    private final NotificationEventRepository eventRepository;
    private final AuditLogRepository auditLogRepository;

    public DLQConsumer(
            NotificationEventRepository eventRepository,
            AuditLogRepository auditLogRepository) {
        this.eventRepository = eventRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // Listen on Dead Letter Queue
    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void processDLQ(NotificationEvent event) {
        log.error("========================================");
        log.error("⚠️  DEAD LETTER QUEUE - Event ID: {}", event.getId());
        log.error("    Recipient: {}", event.getRecipient());
        log.error("    Reason: {}", event.getFailureReason());
        log.error("========================================");

        try {
            // Update status
            event.setStatus("DEAD_LETTERED");
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);

            // Create audit log
            auditLogRepository.save(AuditLog.builder()
                    .eventId(event.getId())
                    .action("DEAD_LETTERED")
                    .details("Message moved to DLQ after max retries")
                    .timestamp(LocalDateTime.now())
                    .build());

            log.error("⚠️  ALERT: Message dead-lettered. Manual intervention required.");

        } catch (Exception e) {
            log.error("Error processing DLQ message: {}", e.getMessage());
        }
    }
}
