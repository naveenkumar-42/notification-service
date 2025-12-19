package com.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String priority = "MEDIUM";  // Frontend filter

    @Column(length = 100)
    private String notificationType;     // Frontend analytics

    @Column(length = 255)
    private String subject;

    @Column(nullable = false)
    private String status;

    @Column
    private Integer retryCount = 0;

    @Column(length = 255)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Frontend filtering indexes (add these)
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
