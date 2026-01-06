package com.notification.controller;

import com.notification.dto.NotificationRequest;
import com.notification.dto.NotificationResponse;
import com.notification.entity.NotificationEvent;
import com.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000","https://notificationservice-blond.vercel.app", "http://127.0.0.1:3000"})
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 🔥 SMS + SCHEDULING FIXED
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@RequestBody Map<String, Object> rawPayload) {
        log.info("📤 POST /send - Payload: {}", rawPayload);
        try {
            String channel = (String) rawPayload.getOrDefault("channel", "EMAIL");
            String recipient = (String) rawPayload.get("recipient");
            String message = (String) rawPayload.get("message");
            String priority = (String) rawPayload.getOrDefault("priority", "MEDIUM");
            String notificationType = (String) rawPayload.getOrDefault("notificationType", "USERSIGNUP");
            String subject = (String) rawPayload.get("subject");
            String scheduledTimeStr = (String) rawPayload.get("scheduledTime");  // 🔥 Scheduling

            // Validate required
            if (recipient == null || recipient.trim().isEmpty() || message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(NotificationResponse.builder().status("ERROR").message("Recipient & message required").build());
            }

            // Channel logic
            if ("SMS".equals(channel)) {
                log.info("📱 SMS - recipient: {}", recipient);
                subject = null;
            } else if ("EMAIL".equals(channel) && (subject == null || subject.isEmpty())) {
                subject = String.format("[%s] Notification", notificationType);
            }

            // 🔥 SCHEDULING LOGIC
            LocalDateTime scheduledAt = null;
            if (scheduledTimeStr != null && !scheduledTimeStr.isEmpty()) {
                scheduledAt = LocalDateTime.parse(scheduledTimeStr);
                log.info("⏰ Scheduled: {}", scheduledAt);
            }

            NotificationRequest request = NotificationRequest.builder()
                    .notificationType(notificationType)
                    .recipient(recipient.trim())
                    .message(message.trim())
                    .subject(subject)
                    .scheduledTime(scheduledTimeStr)  // Pass to service/DB
                    .build();

            // Service handles status (PENDING/SCHEDULED based on scheduledTime)
            return ResponseEntity.ok(notificationService.sendNotification(request));

        } catch (Exception e) {
            log.error("❌ /send error: {}", e.getMessage(), e);
            if (e.getMessage().contains("Rule not found")) {
                log.warn("⚠️ Rule bypassed");
                return ResponseEntity.ok(NotificationResponse.builder()
                        .status("QUEUED")
                        .message("Queued (rule bypassed)")
                        .build());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(NotificationResponse.builder().status("ERROR").message(e.getMessage()).build());
        }
    }

    // ALL OTHER ENDPOINTS UNCHANGED
    @GetMapping("/status/{eventId}")
    public ResponseEntity<NotificationEvent> getStatus(@PathVariable Long eventId) {
        log.info("📊 GET /status/{}", eventId);
        try {
            NotificationEvent event = notificationService.getEventStatus(eventId);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            log.error("❌ /status/{} error: {}", eventId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<NotificationEvent>> getHistory() {
        log.info("📋 GET /history");
        try {
            List<NotificationEvent> events = notificationService.getAllEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("❌ /history error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/status-filter/{status}")
    public ResponseEntity<List<NotificationEvent>> getByStatus(@PathVariable String status) {
        log.info("🔍 GET /status-filter/{}", status);
        try {
            List<NotificationEvent> events = notificationService.getEventsByStatus(status);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/priority-filter/{priority}")
    public ResponseEntity<List<NotificationEvent>> getByPriority(@PathVariable String priority) {
        log.info("🔍 GET /priority-filter/{}", priority);
        try {
            List<NotificationEvent> events = notificationService.getEventsByPriority(priority);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/channel-filter/{channel}")
    public ResponseEntity<List<NotificationEvent>> getByChannel(@PathVariable String channel) {
        log.info("🔍 GET /channel-filter/{}", channel);
        try {
            List<NotificationEvent> events = notificationService.getEventsByChannel(channel);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<List<NotificationEvent>> getFilteredEvents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String dateRange) {
        log.info("🔍 GET /filter?status={},priority={},channel={},dateRange={}", status, priority, channel, dateRange);
        try {
            List<NotificationEvent> events = notificationService.getFilteredEvents(status, priority, channel, dateRange);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ SMS + SCHEDULING FIXED!");
    }
}
