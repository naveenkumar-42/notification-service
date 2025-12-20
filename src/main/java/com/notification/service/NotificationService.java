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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class NotificationService {

    @Autowired private NotificationEventRepository eventRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private NotificationRuleRepository ruleRepository;
    @Autowired private RabbitTemplate rabbitTemplate;

    private static final String QUEUE_NAME = "notification.queue";

    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("sendNotification() - Type: {}, Recipient: {}",
                request.getNotificationType(), request.getRecipient());

        try {
            var rule = ruleRepository.findByNotificationType(request.getNotificationType());
            if (rule == null) {
                throw new IllegalArgumentException("Rule not found for type: " + request.getNotificationType());
            }

            // Parse scheduled time (if any) from frontend
            LocalDateTime scheduledAt = null;
            if (request.getScheduledTime() != null && !request.getScheduledTime().isBlank()) {
                try {
                    // Frontend datetime-local: "2025-12-19T11:45"
                    scheduledAt = LocalDateTime.parse(request.getScheduledTime());
                } catch (DateTimeParseException ex) {
                    log.warn("Invalid scheduledTime format: {}", request.getScheduledTime());
                }
            }

            // Decide initial status based on schedule
            boolean isFutureSchedule = scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
            String initialStatus = isFutureSchedule ? "SCHEDULED" : "QUEUED";

            NotificationEvent event = NotificationEvent.builder()
                    .channel(rule.getChannel())
                    .notificationType(request.getNotificationType())
                    .priority(rule.getPriority())
                    .recipient(request.getRecipient())
                    .subject(request.getSubject())
                    .message(request.getMessage())
                    .status(initialStatus)
                    .retryCount(0)
                    .scheduledAt(scheduledAt)
                    .build();

            eventRepository.save(event);
            log.info("Event created ID: {} with status {}", event.getId(), event.getStatus());

            String auditMsg = isFutureSchedule
                    ? "Notification scheduled for " + scheduledAt
                    : "Notification created and queued";
            createAuditLog(event, "CREATED", auditMsg);

            // Only publish to queue if we should send now
            if (!isFutureSchedule) {
                publishToQueue(event, rule.getPriority());
            }

            return NotificationResponse.builder()
                    .eventId(event.getId())
                    .status(initialStatus)
                    .message(isFutureSchedule
                            ? "Notification scheduled successfully"
                            : "Notification queued successfully")
                    .build();

        } catch (Exception e) {
            log.error("sendNotification failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }

    // FRONTEND FILTERING METHODS
    public List<NotificationEvent> getEventsByStatus(String status) {
        return eventRepository.findByStatusIgnoreCase(status);
    }

    public List<NotificationEvent> getEventsByPriority(String priority) {
        return eventRepository.findByPriorityIgnoreCase(priority);
    }

    public List<NotificationEvent> getEventsByChannel(String channel) {
        return eventRepository.findByChannelIgnoreCase(channel);
    }

    public List<NotificationEvent> getFilteredEvents(String status, String priority, String channel, String dateRange) {
        log.info("Filtering: status={}, priority={}, channel={}, dateRange='{}'",
                status, priority, channel, dateRange);

        LocalDateTime startDate = getDateRangeStart(dateRange);
        log.info("Calculated startDate: {}", startDate);

        List<NotificationEvent> events = eventRepository.findByFilters(status, priority, channel, startDate);
        log.info("Found {} events after filtering", events.size());

        return events;
    }

    // PERFECT DATE FILTER - "all time" = all records
    private LocalDateTime getDateRangeStart(String dateRange) {
        if (dateRange == null || dateRange.trim().isEmpty() ||
                dateRange.equalsIgnoreCase("all") ||
                dateRange.equalsIgnoreCase("all time") ||
                dateRange.equalsIgnoreCase("alltime")) {
            log.info("DateRange 'all' → Returning NULL (show all records)");
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedRange = dateRange.toLowerCase().trim();

        return switch (normalizedRange) {
            case "24h", "24hr" -> now.minusHours(24);
            case "7d", "7day", "7days" -> now.minusDays(7);
            case "30d", "30day", "30days" -> now.minusDays(30);
            default -> null;
        };
    }

    // Existing methods
    public NotificationEvent getEventStatus(Long eventId) {
        Optional<NotificationEvent> event = eventRepository.findById(eventId);
        if (event.isEmpty()) throw new IllegalArgumentException("Event not found: " + eventId);
        return event.get();
    }

    public List<NotificationEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    private void publishToQueue(NotificationEvent event, String priorityStr) {
        try {
            int priority = mapPriority(priorityStr);
            rabbitTemplate.convertAndSend(QUEUE_NAME, event, msg -> {
                msg.getMessageProperties().setPriority(priority);
                return msg;
            });
            log.info("Event {} published to queue (priority: {})", event.getId(), priority);
        } catch (Exception e) {
            log.error("Queue publish failed: {}", e.getMessage());
            throw e;
        }
    }

    private int mapPriority(String priority) {
        return switch (priority.toUpperCase()) {
            case "CRITICAL" -> 10;
            case "HIGH"     -> 7;
            case "MEDIUM"   -> 5;
            case "LOW"      -> 1;
            default         -> 3;
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
            log.error("Audit log failed: {}", e.getMessage());
        }
    }
}
