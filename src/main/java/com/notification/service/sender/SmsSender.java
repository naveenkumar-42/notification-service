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

    /** Send SMS using Twilio */
    public void sendSms(NotificationEvent event) {
        log.info("Sending SMS to: {}", event.getRecipient());
        try {
            Twilio.init(senderConfig.getTwilioAccountSid(), senderConfig.getTwilioAuthToken());

            String phoneNumber = normalizePhoneNumber(event.getRecipient());
            if (!isValidPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Invalid phone number format: " + event.getRecipient());
            }

            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(senderConfig.getTwilioPhoneNumber()),
                    event.getMessage()
            ).create();

            log.info("SMS SENT SUCCESSFULLY! SID: {}", message.getSid());
            log.info("SMS Status: {}, To: {}", message.getStatus(), message.getTo());
        } catch (Exception e) {
            log.error("SMS SEND FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    private String normalizePhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        if (!cleaned.startsWith("+")) {
            if (cleaned.startsWith("1") && cleaned.length() == 11) {
                cleaned = "+" + cleaned;
            } else if (cleaned.length() == 10) {
                cleaned = "+1" + cleaned;
            } else {
                cleaned = "+" + cleaned;
            }
        }
        return cleaned;
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.startsWith("+")) {
            return false;
        }
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        return digitsOnly.length() >= 11 && digitsOnly.length() <= 15;
    }
}
