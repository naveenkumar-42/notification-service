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
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ===== ENDPOINT 1: Send Notification =====
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {

        log.info("REST API: POST /send - {}", request.getNotificationType());
        try {
            NotificationResponse response = notificationService.sendNotification(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(NotificationResponse.builder()
                            .status("ERROR")
                            .message(e.getMessage())
                            .build());
        }
    }

    // ===== ENDPOINT 2: Get Status =====
    @GetMapping("/status/{eventId}")
    public ResponseEntity<?> getStatus(@PathVariable Long eventId) {
        log.info("REST API: GET /status/{}", eventId);
        try {
            NotificationEvent event = notificationService.getEventStatus(eventId);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Event not found");
        }
    }

    // ===== ENDPOINT 3: Get All History =====
    @GetMapping("/history")
    public ResponseEntity<List<NotificationEvent>> getHistory() {
        log.info("REST API: GET /history");
        try {
            List<NotificationEvent> events = notificationService.getAllEvents();
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== ENDPOINT 4: Get by Status =====
    @GetMapping("/status-filter/{status}")
    public ResponseEntity<List<NotificationEvent>> getByStatus(@PathVariable String status) {
        log.info("REST API: GET /status-filter/{}", status);
        try {
            List<NotificationEvent> events = notificationService.getEventsByStatus(status);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== ENDPOINT 5: Health Check =====
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("REST API: GET /health");
        return ResponseEntity.ok("✓ Notification Service is RUNNING");
    }
}
