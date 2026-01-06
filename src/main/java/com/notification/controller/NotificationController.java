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

    // ============================================================================
    // SEND NOTIFICATION (USER PROVIDES CHANNEL & PRIORITY DIRECTLY)
    // ============================================================================

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(
            @RequestBody Map<String, Object> rawPayload) {
        log.info("📤 POST /send - Payload: {}", rawPayload);

        try {
            // Extract all values from user input
            String channel = (String) rawPayload.getOrDefault("channel", "EMAIL");
            String recipient = (String) rawPayload.get("recipient");
            String message = (String) rawPayload.get("message");
            String priority = (String) rawPayload.getOrDefault("priority", "MEDIUM");
            String notificationType = (String) rawPayload.getOrDefault("notificationType", "GENERAL");
            String subject = (String) rawPayload.getOrDefault("subject", "");
            String scheduledTimeStr = (String) rawPayload.get("scheduledTime");

            // ============================================================================
            // VALIDATION
            // ============================================================================
            if (recipient == null || recipient.trim().isEmpty() ||
                    message == null || message.trim().isEmpty()) {
                log.warn("⚠️ Missing recipient or message field");
                return ResponseEntity.badRequest()
                        .body(NotificationResponse.builder()
                                .status("ERROR")
                                .message("❌ Recipient and message are required")
                                .build());
            }

            // ============================================================================
            // BUILD REQUEST WITH USER'S CHANNEL & PRIORITY
            // ============================================================================
            NotificationRequest request = NotificationRequest.builder()
                    .notificationType(notificationType)
                    .recipient(recipient.trim())
                    .message(message.trim())
                    .subject(subject)
                    .channel(channel)              // ✨ User's choice - directly from input
                    .priority(priority)            // ✨ User's choice - directly from input
                    .scheduledTime(scheduledTimeStr)
                    .build();

            log.info("✅ Request built: channel={}, priority={}", channel, priority);

            // ============================================================================
            // SEND NOTIFICATION (SERVICE VALIDATES & PROCESSES)
            // ============================================================================
            return ResponseEntity.ok(notificationService.sendNotification(request));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(NotificationResponse.builder()
                            .status("ERROR")
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("❌ /send error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NotificationResponse.builder()
                            .status("ERROR")
                            .message("Server error: " + e.getMessage())
                            .build());
        }
    }

    // ============================================================================
    // GET EVENT STATUS
    // ============================================================================

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

    // ============================================================================
    // GET ALL EVENTS HISTORY
    // ============================================================================

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

    // ============================================================================
    // FILTER BY STATUS
    // ============================================================================

    @GetMapping("/status-filter/{status}")
    public ResponseEntity<List<NotificationEvent>> getByStatus(@PathVariable String status) {
        log.info("🔍 GET /status-filter/{}", status);
        try {
            List<NotificationEvent> events = notificationService.getEventsByStatus(status);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("❌ /status-filter error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================================
    // FILTER BY PRIORITY
    // ============================================================================

    @GetMapping("/priority-filter/{priority}")
    public ResponseEntity<List<NotificationEvent>> getByPriority(@PathVariable String priority) {
        log.info("🔍 GET /priority-filter/{}", priority);
        try {
            List<NotificationEvent> events = notificationService.getEventsByPriority(priority);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("❌ /priority-filter error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================================
    // FILTER BY CHANNEL
    // ============================================================================

    @GetMapping("/channel-filter/{channel}")
    public ResponseEntity<List<NotificationEvent>> getByChannel(@PathVariable String channel) {
        log.info("🔍 GET /channel-filter/{}", channel);
        try {
            List<NotificationEvent> events = notificationService.getEventsByChannel(channel);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("❌ /channel-filter error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================================
    // COMBINED FILTERING
    // ============================================================================

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
            log.error("❌ /filter error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================================
    // HEALTH CHECK
    // ============================================================================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("❤️ Health check requested");
        Map<String, Object> healthInfo = new java.util.HashMap<>();
        healthInfo.put("status", "✅ RUNNING");
        healthInfo.put("service", "Notification Orchestration Service");
        healthInfo.put("mode", "Direct User Input (Channel & Priority)");
        healthInfo.put("supportedChannels", new String[]{"EMAIL", "SMS", "PUSH"});
        healthInfo.put("supportedPriorities", new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL"});
        healthInfo.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(healthInfo);
    }

    // ============================================================================
    // OPTIONAL: TEST ENDPOINT (Helps verify all channels work)
    // ============================================================================

    @PostMapping("/test-payload")
    public ResponseEntity<NotificationResponse> testPayload(
            @RequestBody(required = false) Map<String, Object> rawPayload) {
        log.info("🧪 TEST endpoint - Payload: {}", rawPayload);

        if (rawPayload == null || rawPayload.isEmpty()) {
            return ResponseEntity.ok(NotificationResponse.builder()
                    .status("INFO")
                    .message("""
                            ✅ Test Payload Example (copy & modify):
                            {
                              "channel": "PUSH",
                              "priority": "CRITICAL",
                              "recipient": "YOUR_FCM_TOKEN",
                              "message": "Test message from your app",
                              "notificationType": "TEST_EVENT",
                              "subject": "Test Subject"
                            }
                            
                            Channels: EMAIL, SMS, PUSH
                            Priorities: LOW, MEDIUM, HIGH, CRITICAL
                            """)
                    .build());
        }

        // If payload provided, treat as normal send
        return sendNotification(rawPayload);
    }
}
