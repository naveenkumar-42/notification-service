package com.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_rule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String notificationType;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private Integer retryLimit;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
