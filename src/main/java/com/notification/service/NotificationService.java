package com.notification.service;

import com.notification.dto.NotificationRequest;
import com.notification.dto.NotificationResponse;
import com.notification.entity.NotificationEvent;
import com.notification.entity.NotificationRule;
import com.notification.repository.NotificationEventRepository;
import com.notification.repository.NotificationRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NotificationService {

    private final NotificationEventRepository eventRepository;
    private final NotificationRuleRepository ruleRepository;
    private final NotificationProducer producer;

    public NotificationService(
            NotificationEventRepository eventRepository,
            NotificationRuleRepository ruleRepository,
            NotificationProducer producer) {
        this.eventRepository = eventRepository;
        this.ruleRepository = ruleRepository;
        this.producer = producer;
    }

    // Send notification - main entry point
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.info("Processing notification request: {}", request.getNotificationType());

        try {
            // Step 1: Fetch rule for notification type
            NotificationRule rule = ruleRepository.findByNotificationType(
                    request.getNotificationType()
            );

            if (rule == null) {
                throw new IllegalArgumentException(
                        "Rule not found for type: " + request.getNotificationType()
                );
            }

            // Step 2: Create notification event
            NotificationEvent event = NotificationEvent.builder()
                    .recipient(request.getRecipient())
                    .message(request.getMessage())
                    .channel(rule.getChannel())
                    .status("PENDING")
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Step 3: Save event to database
            NotificationEvent savedEvent = eventRepository.save(event);
            log.info("Event saved to database - ID: {}", savedEvent.getId());

            // Step 4: Send to queue
            producer.sendNotification(savedEvent);

            // Step 5: Return response
            return NotificationResponse.builder()
                    .eventId(savedEvent.getId())
                    .status("QUEUED")
                    .message("Notification queued successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error in sendNotification: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // Get notification status by ID
    public NotificationEvent getEventStatus(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));
    }

    // Get all notifications
    public List<NotificationEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    // Get notifications by status
    public List<NotificationEvent> getEventsByStatus(String status) {
        return eventRepository.findByStatus(status);
    }

    // Get notifications by recipient
    public List<NotificationEvent> getEventsByRecipient(String recipient) {
        return eventRepository.findByRecipient(recipient);
    }
}
