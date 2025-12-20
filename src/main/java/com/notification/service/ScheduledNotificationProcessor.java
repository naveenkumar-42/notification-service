package com.notification.service;

import com.notification.entity.NotificationEvent;
import com.notification.repository.NotificationEventRepository;
import com.notification.repository.NotificationRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class ScheduledNotificationProcessor {

    private final NotificationEventRepository eventRepository;
    private final NotificationRuleRepository ruleRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String QUEUE_NAME = "notification.queue";

    public ScheduledNotificationProcessor(NotificationEventRepository eventRepository,
                                          NotificationRuleRepository ruleRepository,
                                          RabbitTemplate rabbitTemplate) {
        this.eventRepository = eventRepository;
        this.ruleRepository = ruleRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Runs every 60 seconds
    @Scheduled(fixedDelay = 60000)
    public void processScheduledNotifications() {
        final LocalDateTime now = LocalDateTime.now();   // final → safe in lambda
        final List<NotificationEvent> dueEvents =
                eventRepository.findByStatusAndScheduledAtBefore("SCHEDULED", now);

        if (dueEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} scheduled notifications", dueEvents.size());

        for (NotificationEvent event : dueEvents) {
            // compute priority in a local final variable
            int computedPriority = 5;
            var rule = ruleRepository.findByNotificationType(event.getNotificationType());
            if (rule != null) {
                computedPriority = mapPriority(rule.getPriority());
            }
            final int priority = computedPriority; // effectively final for lambda

            // event, QUEUE_NAME, priority are all final/effectively final
            rabbitTemplate.convertAndSend(QUEUE_NAME, event, msg -> {
                msg.getMessageProperties().setPriority(priority);
                return msg;
            });

            event.setStatus("QUEUED");
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);

            log.info("Scheduled event {} moved to QUEUED and published to queue", event.getId());
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
}
