package com.notification.service;

import com.notification.dto.NotificationRequest;
import com.notification.dto.NotificationResponse;
import com.notification.entity.AuditLog;
import com.notification.entity.NotificationEvent;
import com.notification.repository.AuditLogRepository;
import com.notification.repository.NotificationEventRepository;
import com.notification.repository.NotificationRuleRepository;
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

    private static final String QUEUE_NAME = "notification.queue";

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("sendNotification() called - Type: {}, Recipient: {}",
                request.getNotificationType(), request.getRecipient());
        try {
            var rule = ruleRepository.findByNotificationType(request.getNotificationType());
            if (rule == null) {
                throw new IllegalArgumentException("Rule not found for type: " + request.getNotificationType());
            }

            NotificationEvent event = new NotificationEvent();
            event.setChannel(rule.getChannel());
            event.setRecipient(request.getRecipient());
            event.setMessage(request.getMessage());
            event.setStatus("QUEUED");
            event.setRetryCount(0);
            event.setCreatedAt(LocalDateTime.now());
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);

            log.info("Notification event created with ID: {}", event.getId());

            createAuditLog(event, "CREATED", "Notification created and queued");

            publishToQueue(event, rule.getPriority());

            return NotificationResponse.builder()
                    .eventId(event.getId())
                    .status("QUEUED")
                    .message("Notification queued successfully")
                    .build();

        } catch (Exception e) {
            log.error("sendNotification() failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }

    private void publishToQueue(NotificationEvent event, String priorityStr) {
        try {
            int priority = mapPriority(priorityStr);
            rabbitTemplate.convertAndSend(QUEUE_NAME, event, msg -> {
                msg.getMessageProperties().setPriority(priority);
                return msg;
            });
            log.info("Event ID {} published to queue with priority {}", event.getId(), priority);
        } catch (Exception e) {
            log.error("Failed to publish to queue: {}", e.getMessage());
            throw e;
        }
    }

    private int mapPriority(String priority) {
        return switch (priority.toUpperCase()) {
            case "CRITICAL" -> 10;
            case "HIGH"     -> 7;
            case "MEDIUM"   -> 5;
            case "LOW"      -> 1;
            default -> {
                try { yield Integer.parseInt(priority); }
                catch (NumberFormatException ex) { yield 3; }
            }
        };
    }

    private void createAuditLog(NotificationEvent event, String action, String details) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .eventId(event.getId())
                    .action(action)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    public NotificationEvent getEventStatus(Long eventId) {
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
