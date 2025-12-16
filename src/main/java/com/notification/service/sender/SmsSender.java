package com.notification.service.sender;

import com.notification.entity.NotificationEvent;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsSender {

    @Autowired
    private SenderConfig senderConfig;

    static {
        // Initialize Twilio (will be set via properties)
    }

    /**
     * Send SMS using Twilio
     */
    public void sendSms(NotificationEvent event) throws Exception {
        log.info(" Sending SMS to: {}", event.getRecipient());

        try {
            // Initialize Twilio with credentials from config
            Twilio.init(senderConfig.getTwilioAccountSid(), senderConfig.getTwilioAuthToken());

            // Validate phone number format
            String phoneNumber = normalizePhoneNumber(event.getRecipient());
            if (!isValidPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Invalid phone number format: " + event.getRecipient());
            }

            // Create and send message
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),      // To number
                    new PhoneNumber(senderConfig.getTwilioPhoneNumber()), // From number
                    event.getMessage()                  // Message body
            ).create();

            log.info(" SMS SENT SUCCESSFULLY! SID: {}", message.getSid());
            log.info("📊 SMS Status: {}, To: {}", message.getStatus(), message.getTo());

        } catch (Exception e) {
            log.error("❌ SMS SEND FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Normalize phone number to E.164 format
     * Expected format: +1234567890 or 1234567890
     */
    private String normalizePhoneNumber(String phoneNumber) {
        // Remove all non-digit characters except leading +
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");

        // If no + prefix, add it
        if (!cleaned.startsWith("+")) {
            // If it starts with 1 (US), keep as is and add +
            if (cleaned.startsWith("1") && cleaned.length() == 11) {
                cleaned = "+" + cleaned;
            }
            // If it's 10 digits (US), add +1
            else if (cleaned.length() == 10) {
                cleaned = "+1" + cleaned;
            }
            // Otherwise add +
            else {
                cleaned = "+" + cleaned;
            }
        }

        return cleaned;
    }

    /**
     * Validate phone number is in proper E.164 format
     * E.164: +[country code][number] (11-15 digits total)
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.startsWith("+")) {
            return false;
        }

        // Extract digits only
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");

        // E.164 format requires 11-15 digits (including country code)
        return digitsOnly.length() >= 11 && digitsOnly.length() <= 15;
    }

    /**
     * Send bulk SMS to multiple recipients
     */
    public void sendBulkSms(NotificationEvent event, String[] phoneNumbers) throws Exception {
        log.info("Sending BULK SMS to {} recipients", phoneNumbers.length);

        try {
            Twilio.init(senderConfig.getTwilioAccountSid(), senderConfig.getTwilioAuthToken());

            int successCount = 0;
            int failureCount = 0;

            for (String phoneNumber : phoneNumbers) {
                try {
                    String normalizedNumber = normalizePhoneNumber(phoneNumber);
                    if (isValidPhoneNumber(normalizedNumber)) {
                        Message.creator(
                                new PhoneNumber(normalizedNumber),
                                new PhoneNumber(senderConfig.getTwilioPhoneNumber()),
                                event.getMessage()
                        ).create();
                        successCount++;
                        log.info("SMS sent to: {}", normalizedNumber);
                    } else {
                        failureCount++;
                        log.warn("Invalid phone number skipped: {}", phoneNumber);
                    }
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
                }
            }

            log.info(" Bulk SMS Result - Success: {}, Failed: {}", successCount, failureCount);

        } catch (Exception e) {
            log.error("❌ BULK SMS SEND FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send bulk SMS: " + e.getMessage(), e);
        }
    }
}