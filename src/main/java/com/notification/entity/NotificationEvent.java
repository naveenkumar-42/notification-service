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

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String message;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(columnDefinition = "LONGTEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
