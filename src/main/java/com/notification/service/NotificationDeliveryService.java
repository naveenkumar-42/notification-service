package com.notification.service;

import com.notification.entity.NotificationEvent;
import com.notification.service.sender.EmailSender;
import com.notification.service.sender.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Routes notifications to EMAIL, SMS, or PUSH */
@Service
@Slf4j
public class NotificationDeliveryService {

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private SmsSender smsSender;

    public void deliver(NotificationEvent event) throws Exception {
        String channel = event.getChannel();
        log.info("🚀 NotificationDeliveryService.deliver() called for channel: {}", channel);

        switch (channel.toUpperCase()) {
            case "EMAIL" -> deliverEmail(event);
            case "SMS"   -> deliverSms(event);
            case "PUSH"  -> deliverPush(event);
            default      -> throw new IllegalArgumentException("Unsupported channel: " + channel);
        }
    }

    private void deliverEmail(NotificationEvent event) throws Exception {
        log.info("📧 Routing to EMAIL sender");
        try {
            emailSender.sendHtmlEmail(event);
            event.setStatus("DELIVERED");
        } catch (Exception e) {
            event.setStatus("FAILED");
            throw e;
        }
    }

    private void deliverSms(NotificationEvent event) throws Exception {
        log.info("📱 Routing to SMS sender");
        try {
            smsSender.sendSms(event);
            event.setStatus("DELIVERED");
        } catch (Exception e) {
            event.setStatus("FAILED");
            throw e;
        }
    }

    private void deliverPush(NotificationEvent event) throws Exception {
        log.info("🔔 [SIMULATED] Sending PUSH to: {}", event.getRecipient());
        Thread.sleep(75);
        event.setStatus("DELIVERED");
    }
}
