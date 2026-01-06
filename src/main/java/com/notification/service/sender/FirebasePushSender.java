package com.notification.service.sender;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.notification.entity.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class FirebasePushSender {

    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private SenderConfig senderConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void initFirebase() {
        try {
            log.info("🔥 Initializing Firebase with: {}", senderConfig.getPushServiceAccountPath());
            InputStream serviceAccountStream = new ClassPathResource(
                    senderConfig.getPushServiceAccountPath().replace("classpath:", "")
            ).getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase App initialized - Project: notification-management-5d62d");
            }

            firebaseMessaging = FirebaseMessaging.getInstance(FirebaseApp.getInstance());
            log.info("🚀 Firebase Messaging ready!");

        } catch (Exception e) {
            log.error("❌ Firebase init FAILED: {}", e.getMessage());
            throw new RuntimeException("Firebase setup failed", e);
        }
    }
    public void sendPush(NotificationEvent event) throws Exception {
        if (!senderConfig.isPushEnabled()) {
            throw new IllegalStateException("PUSH disabled in config");
        }

        String fcmToken = event.getRecipient().trim();
        log.info("📱 Sending PUSH to token: {} | Event: {}", fcmToken.substring(0, 20) + "...", event.getId());

        if (fcmToken.isEmpty()) {
            throw new IllegalArgumentException("FCM token required (recipient field)");
        }

        // ✅ CORRECTED: No setImageUrl() - use data payload instead
        Notification notification = Notification.builder()
                .setTitle(event.getSubject() != null ? event.getSubject() : "📱 Notification")
                .setBody(event.getMessage())
                .build();

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(notification)
                .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                .putData("sound", "default")
                .putData("imageUrl", "https://firebase.google.com/static/downloads/brand-guidelines/SVG/logo-logomark-color.svg") // Custom image via data
                .putData("eventId", event.getId().toString())
                .putData("type", event.getNotificationType())
                .putData("priority", event.getPriority())
                .build();

        String response = firebaseMessaging.send(message);
        log.info("✅ PUSH SUCCESS: {} → {}", event.getId(), response);
    }

}
