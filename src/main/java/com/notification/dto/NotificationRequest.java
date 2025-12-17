package com.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    @NotBlank(message = "Notification type is required")
    private String notificationType;

    @NotBlank(message = "Recipient is required")
    // REMOVE @Email – it breaks SMS
    private String recipient;

    @NotBlank(message = "Message is required")
    private String message;
}
