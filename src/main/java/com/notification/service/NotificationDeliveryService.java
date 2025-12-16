package com.notification.service;

import com.notification.entity.NotificationEvent;
import com.notification.service.sender.EmailSender;
import com.notification.service.sender.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main Delivery Service that routes notifications to appropriate channels
 * Supports EMAIL and SMS (Twilio) channels
 */
@Service
@Slf4j
public class NotificationDeliveryService {

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private SmsSender smsSender;

    /**
     * Main deliver method that routes to appropriate sender
     */
    public void deliver(NotificationEvent event) throws Exception {
        String channel = event.getChannel();
        log.info("🚀 NotificationDeliveryService.deliver() called for channel: {}", channel);

        try {
            switch (channel.toUpperCase()) {
                case "EMAIL":
                    deliverEmail(event);
                    break;
                case "SMS":
                    deliverSms(event);
                    break;
                case "PUSH":
                    deliverPushNotification(event);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported channel: " + channel);
            }
        } catch (Exception e) {
            log.error("❌ Delivery failed for channel {}: {}", channel, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Deliver via Email (SMTP)
     */
    private void deliverEmail(NotificationEvent event) throws Exception {
        log.info("📧 Routing to EMAIL sender");
        try {
            // Send HTML email for better formatting
            emailSender.sendHtmlEmail(event);
            event.setStatus("DELIVERED");
            log.info("✅ Email delivery successful");
        } catch (Exception e) {
            event.setStatus("FAILED");
            log.error("❌ Email delivery failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Deliver via SMS (Twilio)
     */
    private void deliverSms(NotificationEvent event) throws Exception {
        log.info("📱 Routing to SMS sender");
        try {
            smsSender.sendSms(event);
            event.setStatus("DELIVERED");
            log.info("✅ SMS delivery successful");
        } catch (Exception e) {
            event.setStatus("FAILED");
            log.error("❌ SMS delivery failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Deliver Push Notification (Placeholder for future implementation)
     */
    private void deliverPushNotification(NotificationEvent event) throws Exception {
        log.info("🔔 Routing to PUSH sender");
        try {
            simulatePushNotificationSend(event);
            event.setStatus("DELIVERED");
            log.info("✅ Push notification delivery successful");
        } catch (Exception e) {
            event.setStatus("FAILED");
            log.error("❌ Push notification delivery failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Simulate push notification send
     */
    private void simulatePushNotificationSend(NotificationEvent event) throws Exception {
        log.info("🔔 [SIMULATED] Sending PUSH to: {}", event.getRecipient());
        Thread.sleep(75);
        log.info("✅ [SIMULATED] PUSH SENT successfully");
    }
}