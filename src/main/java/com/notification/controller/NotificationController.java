package com.notification.controller;

import com.notification.dto.NotificationRequest;
import com.notification.dto.NotificationResponse;
import com.notification.entity.NotificationEvent;
import com.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000","https://notificationservice-blond.vercel.app", "http://127.0.0.1:3000"})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ✅ SEND NOTIFICATION
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody NotificationRequest request) {
        log.info("📤 POST /send - {}", request.getNotificationType());
        try {
            NotificationResponse response = notificationService.sendNotification(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ /send error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(NotificationResponse.builder().status("ERROR").message(e.getMessage()).build());
        }
    }

    // ✅ GET STATUS
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

    // ✅ HISTORY (ALL EVENTS)
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

    // ✅ FILTER ENDPOINTS (FRONTEND)
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

    // ✅ COMBINED FILTER (History page)
    @GetMapping("/filter")
    public ResponseEntity<List<NotificationEvent>> getFilteredEvents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String dateRange) {
        log.info("🔍 GET /filter?status={},priority={},channel={},dateRange={}",
                status, priority, channel, dateRange);
        try {
            List<NotificationEvent> events = notificationService.getFilteredEvents(status, priority, channel, dateRange);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ HEALTH CHECK
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ Notification Service RUNNING");
    }
}
