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

    public NotificationConsumer(
            NotificationEventRepository eventRepository,
            AuditLogRepository auditLogRepository,
            NotificationProducer producer) {
        this.eventRepository = eventRepository;
        this.auditLogRepository = auditLogRepository;
        this.producer = producer;
    }

    // Listen on main queue and process messages
    @RabbitListener(queues = RabbitMQConfig.MAIN_QUEUE)
    public void consumeNotification(NotificationEvent event) {
        log.info("========================================");
        log.info("📨 CONSUMING Message - Event ID: {}", event.getId());
        log.info("   Recipient: {}", event.getRecipient());
        log.info("   Channel: {}", event.getChannel());
        log.info("========================================");

        try {
            // Step 1: Send to notification channel
            sendNotificationToChannel(event);

            // Step 2: Mark as SENT
            event.setStatus("SENT");
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);
            log.info("✓ Event status updated to SENT");

            // Step 3: Create audit log
            auditLogRepository.save(AuditLog.builder()
                    .eventId(event.getId())
                    .action("SENT")
                    .details("Notification sent successfully to " + event.getRecipient())
                    .timestamp(LocalDateTime.now())
                    .build());

            log.info("✓ Audit log created");
            log.info("✓✓ NOTIFICATION SENT SUCCESSFULLY ✓✓\n");

        } catch (Exception e) {
            log.error("✗ Error processing notification: {}", e.getMessage());
            handleFailure(event, e);
        }
    }

    // Simulate sending to different channels
    private void sendNotificationToChannel(NotificationEvent event) throws Exception {
        String channel = event.getChannel();
        String recipient = event.getRecipient();
        String message = event.getMessage();

        log.info("   → Sending via {}", channel);

        switch (channel) {
            case "EMAIL":
                sendEmail(recipient, message);
                break;
            case "SMS":
                sendSMS(recipient, message);
                break;
            case "PUSH":
                sendPushNotification(recipient, message);
                break;
            default:
                throw new IllegalArgumentException("Unknown channel: " + channel);
        }
    }

    // Simulate Email sending
    private void sendEmail(String recipient, String message) throws Exception {
        log.debug("   📧 Email channel: Sending to {}", recipient);
        Thread.sleep(100); // Simulate network delay
    }

    // Simulate SMS sending
    private void sendSMS(String recipient, String message) throws Exception {
        log.debug("   📱 SMS channel: Sending to {}", recipient);
        Thread.sleep(50); // Simulate network delay
    }

    // Simulate Push notification sending
    private void sendPushNotification(String recipient, String message) throws Exception {
        log.debug("   🔔 Push channel: Sending to {}", recipient);
        Thread.sleep(75); // Simulate network delay
    }

    // Handle failure and retry logic
    private void handleFailure(NotificationEvent event, Exception e) {
        log.warn("✗ Notification failed - Event ID: {}", event.getId());
        event.setFailureReason(e.getMessage());
        event.setUpdatedAt(LocalDateTime.now());

        if (event.getRetryCount() < 3) {
            // RETRY
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

            // Re-queue to main queue
            producer.sendToRetryQueue(event);

        } else {
            // MOVE TO DEAD LETTER QUEUE
            log.error("✗✗ MAX RETRIES EXCEEDED - Moving to DLQ");

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
