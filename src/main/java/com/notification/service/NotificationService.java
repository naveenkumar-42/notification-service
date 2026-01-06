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

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationEventRepository eventRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRuleRepository ruleRepository; // Kept for backwards compatibility (unused now)

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String QUEUE_NAME = "notification.queue";

    // ============================================================================
    // MAIN NOTIFICATION SENDING (NO RULE DEPENDENCY)
    // ============================================================================


    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("📨 sendNotification - Type: {}, Channel: {}, Priority: {}",
                request.getNotificationType(), request.getChannel(), request.getPriority());

        try {
            // Validate required fields
            if (request.getRecipient() == null || request.getRecipient().trim().isEmpty()) {
                throw new IllegalArgumentException("❌ Recipient is required");
            }
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                throw new IllegalArgumentException("❌ Message is required");
            }

            // ✅ VALIDATE & NORMALIZE: Channel from user input
            String channel = (request.getChannel() != null && !request.getChannel().trim().isEmpty())
                    ? request.getChannel().toUpperCase()
                    : "EMAIL";

            if (!isValidChannel(channel)) {
                throw new IllegalArgumentException("❌ Invalid channel: " + channel + ". Allowed: EMAIL, SMS, PUSH");
            }

            // ✅ VALIDATE & NORMALIZE: Priority from user input
            String priority = (request.getPriority() != null && !request.getPriority().trim().isEmpty())
                    ? request.getPriority().toUpperCase()
                    : "MEDIUM";

            if (!isValidPriority(priority)) {
                throw new IllegalArgumentException("❌ Invalid priority: " + priority + ". Allowed: LOW, MEDIUM, HIGH, CRITICAL");
            }

            // ✅ Parse scheduled time (optional)
            LocalDateTime scheduledAt = null;
            if (request.getScheduledTime() != null && !request.getScheduledTime().isBlank()) {
                try {
                    scheduledAt = LocalDateTime.parse(request.getScheduledTime());
                    log.info("📅 Scheduled time parsed: {}", scheduledAt);
                } catch (DateTimeParseException ex) {
                    log.warn("⚠️ Invalid scheduledTime format (expected ISO-8601): {}", request.getScheduledTime());
                    throw new IllegalArgumentException("❌ Invalid scheduledTime format. Use ISO-8601: 2026-01-06T15:30:00");
                }
            }

            // ✅ Decide initial status based on scheduling
            boolean isFutureSchedule = scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
            String initialStatus = isFutureSchedule ? "SCHEDULED" : "QUEUED";

            // ✅ Create event with USER VALUES (NOT from rule!)
            NotificationEvent event = NotificationEvent.builder()
                    .channel(channel)                                    // ✨ From user input
                    .priority(priority)                                  // ✨ From user input
                    .notificationType(request.getNotificationType())
                    .recipient(request.getRecipient().trim())
                    .message(request.getMessage().trim())
                    .subject(request.getSubject() != null ? request.getSubject() : "Notification")
                    .status(initialStatus)
                    .retryCount(0)
                    .scheduledAt(scheduledAt)
                    .build();

            eventRepository.save(event);
            log.info("✅ Event created: ID={}, Status={}, Channel={}, Priority={}, Recipient={}",
                    event.getId(), event.getStatus(), event.getChannel(), event.getPriority(), event.getRecipient());

            // ✅ Create audit log
            String auditMsg = isFutureSchedule
                    ? "Notification scheduled for " + scheduledAt
                    : "Notification created and queued by user";
            createAuditLog(event, "CREATED", auditMsg);

            // ============================================================================
            // 🎯 NEW CODE: Mark as QUEUED BEFORE publishing (prevents duplicate processing)
            // ============================================================================

            if (!isFutureSchedule) {
                event.setStatus("QUEUED");
                eventRepository.save(event);  // Update DB with QUEUED status BEFORE queue publish
                log.info("✅ Event marked as QUEUED in DB - ID: {}", event.getId());
            }

            // ============================================================================
            // Publish to queue if immediate (not scheduled)
            // ============================================================================

            if (!isFutureSchedule) {
                publishToQueue(event, priority);
                log.info("📤 Published to RabbitMQ queue with priority: {}", priority);
            } else {
                log.info("⏰ Future scheduled notification - will be processed by ScheduledNotificationProcessor");
            }

            return NotificationResponse.builder()
                    .eventId(event.getId())
                    .status(initialStatus)
                    .message(isFutureSchedule
                            ? "✅ Notification scheduled successfully"
                            : "✅ Notification queued successfully")
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ sendNotification failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }
    // ============================================================================
    // VALIDATION METHODS
    // ============================================================================

    private boolean isValidChannel(String channel) {
        return channel.equals("EMAIL") || channel.equals("SMS") || channel.equals("PUSH");
    }

    private boolean isValidPriority(String priority) {
        return priority.equals("LOW") || priority.equals("MEDIUM") ||
                priority.equals("HIGH") || priority.equals("CRITICAL");
    }

    // ============================================================================
    // QUEUE PUBLISHING
    // ============================================================================

    private void publishToQueue(NotificationEvent event, String priorityStr) {
        try {
            int priorityInt = mapPriority(priorityStr);
            rabbitTemplate.convertAndSend(QUEUE_NAME, event, msg -> {
                msg.getMessageProperties().setPriority(priorityInt);
                return msg;
            });
            log.info("📤 Event {} published to RabbitMQ queue (priority: {})", event.getId(), priorityInt);
        } catch (Exception e) {
            log.error("❌ Queue publish failed: {}", e.getMessage());
            throw new RuntimeException("Failed to publish to queue", e);
        }
    }

    private int mapPriority(String priority) {
        return switch (priority.toUpperCase()) {
            case "CRITICAL" -> 10;
            case "HIGH" -> 7;
            case "MEDIUM" -> 5;
            case "LOW" -> 1;
            default -> 3;
        };
    }

    // ============================================================================
    // AUDIT LOGGING
    // ============================================================================

    private void createAuditLog(NotificationEvent event, String action, String details) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .eventId(event.getId())
                    .action(action)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(logEntry);
            log.debug("📋 Audit log created: eventId={}, action={}", event.getId(), action);
        } catch (Exception e) {
            log.error("⚠️ Audit log save failed: {}", e.getMessage());
        }
    }

    // ============================================================================
    // FILTERING & RETRIEVAL (Frontend API endpoints)
    // ============================================================================

    public List<NotificationEvent> getEventsByStatus(String status) {
        log.info("🔍 Getting events by status: {}", status);
        return eventRepository.findByStatusIgnoreCase(status);
    }

    public List<NotificationEvent> getEventsByPriority(String priority) {
        log.info("🔍 Getting events by priority: {}", priority);
        return eventRepository.findByPriorityIgnoreCase(priority);
    }

    public List<NotificationEvent> getEventsByChannel(String channel) {
        log.info("🔍 Getting events by channel: {}", channel);
        return eventRepository.findByChannelIgnoreCase(channel);
    }

    public List<NotificationEvent> getFilteredEvents(String status, String priority, String channel, String dateRange) {
        log.info("🔍 Filtering: status={}, priority={}, channel={}, dateRange='{}'",
                status, priority, channel, dateRange);

        LocalDateTime startDate = getDateRangeStart(dateRange);
        log.debug("📅 Calculated startDate: {}", startDate);

        List<NotificationEvent> events = eventRepository.findByFilters(status, priority, channel, startDate);
        log.info("✅ Found {} events after filtering", events.size());

        return events;
    }

    // ============================================================================
    // DATE RANGE HELPER
    // ============================================================================

    private LocalDateTime getDateRangeStart(String dateRange) {
        if (dateRange == null || dateRange.trim().isEmpty() ||
                dateRange.equalsIgnoreCase("all") ||
                dateRange.equalsIgnoreCase("all time") ||
                dateRange.equalsIgnoreCase("alltime")) {
            log.debug("📅 DateRange 'all' → returning NULL (show all records)");
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedRange = dateRange.toLowerCase().trim();

        LocalDateTime result = switch (normalizedRange) {
            case "24h", "24hr" -> now.minusHours(24);
            case "7d", "7day", "7days" -> now.minusDays(7);
            case "30d", "30day", "30days" -> now.minusDays(30);
            default -> null;
        };

        if (result != null) {
            log.debug("📅 DateRange '{}' → from {}", dateRange, result);
        }
        return result;
    }

    // ============================================================================
    // SINGLE EVENT RETRIEVAL
    // ============================================================================

    public NotificationEvent getEventStatus(Long eventId) {
        log.info("🔍 Getting event status for ID: {}", eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("⚠️ Event not found: {}", eventId);
                    return new IllegalArgumentException("Event not found: " + eventId);
                });
    }

    public List<NotificationEvent> getAllEvents() {
        log.info("🔍 Fetching all events");
        return eventRepository.findAll();
    }
}