package com.notification.service;

import com.notification.dto.NotificationRequest;
import com.notification.dto.NotificationResponse;
import com.notification.entity.NotificationEvent;
import com.notification.entity.AuditLog;
import com.notification.repository.NotificationEventRepository;
import com.notification.repository.AuditLogRepository;
import com.notification.repository.NotificationRuleRepository;
import com.notification.service.NotificationDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationEventRepository eventRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRuleRepository ruleRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationDeliveryService deliveryService;

    private static final String QUEUE_NAME = "notification.queue";

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) throws Exception {
        log.info("sendNotification() called - Type: {}, Recipient: {}",
                request.getNotificationType(), request.getRecipient());

        try {
            // 1. Create notification event
            NotificationEvent event = createNotificationEvent(request);
            eventRepository.save(event);
            log.info(" Notification event created with ID: {}", event.getId());

            // 2. Create audit log
            createAuditLog(event, "CREATED", "Notification created");

            // 3. Publish to RabbitMQ for async processing
            publishToQueue(event);
            log.info(" Event published to queue: {}", QUEUE_NAME);

            // 4. Attempt direct delivery
            try {
                deliveryService.deliver(event);
                event.setStatus("DELIVERED");
                eventRepository.save(event);
                createAuditLog(event, "DELIVERED", "Notification delivered successfully via " + request.getNotificationType());
                log.info("✅ Notification delivered successfully!");
            } catch (Exception e) {
                event.setStatus("PENDING");
                eventRepository.save(event);
                createAuditLog(event, "PENDING", "Will retry: " + e.getMessage());
                log.warn("Initial delivery failed, queued for retry: {}", e.getMessage());
            }

            return NotificationResponse.builder()
                    .eventId(event.getId())
                    .status(event.getStatus())
                    .message("Notification " + event.getStatus())
                    .build();

        } catch (Exception e) {
            log.error("❌ sendNotification() failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }

    private NotificationEvent createNotificationEvent(NotificationRequest request) {
        NotificationEvent event = new NotificationEvent();
        event.setChannel(request.getNotificationType().toUpperCase());
        event.setRecipient(request.getRecipient());
        event.setMessage(request.getMessage());
        event.setStatus("PENDING");
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        return event;
    }

    private void publishToQueue(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(QUEUE_NAME, event);
            log.info("Event ID {} published to queue", event.getId());
        } catch (Exception e) {
            log.error("Failed to publish to queue: {}", e.getMessage());
        }
    }

    private void createAuditLog(NotificationEvent event, String action, String details) {
        try {
            AuditLog log = AuditLog.builder()
                    .eventId(event.getId())
                    .action(action)
                    .details(details)
                    .timestamp(LocalDateTime.now())  // ✅ Uses 'timestamp' field
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            log.error(" Failed to create audit log: {}", e.getMessage());
        }
    }

    public NotificationEvent getEventStatus(Long eventId) throws Exception {
        Optional<NotificationEvent> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new IllegalArgumentException("Event not found with ID: " + eventId);
        }
        return event.get();
    }

    public List<NotificationEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    public List<NotificationEvent> getEventsByStatus(String status) {
        return eventRepository.findByStatus(status.toUpperCase());
    }
}
