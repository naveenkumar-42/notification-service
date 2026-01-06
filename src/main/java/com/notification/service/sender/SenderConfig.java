package com.notification.service.sender;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Email and SMS senders.
 * Values are loaded from application.properties with prefix: notification.sender
 */
@Component
@ConfigurationProperties(prefix = "notification.sender")
@Data
public class SenderConfig {

    // ===== EMAIL CONFIGURATION =====
    private Email email = new Email();

    // ===== SMS (TWILIO) CONFIGURATION =====
    private Sms sms = new Sms();

    @Data
    public static class Email {
        private String host = "smtp.gmail.com";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String fromAddress = "";
        private String replyTo = "";
        private boolean tlsEnabled = true;
        private boolean authEnabled = true;
        private int timeout = 5000;
        private int connectionTimeout = 5000;
    }

    @Data
    public static class Sms {
        private String accountSid = "";
        private String authToken = "";
        private String fromNumber = "";
    }


    @Autowired
    private SenderConfig senderConfig;

    // EMAIL convenience getters
    public String getEmailFromAddress() { return email.getFromAddress(); }
    public String getEmailReplyTo() { return email.getReplyTo() != null ? email.getReplyTo() : email.getFromAddress(); }
    public String getEmailHost() { return email.getHost(); }
    public int getEmailPort() { return email.getPort(); }
    public String getEmailUsername() { return email.getUsername(); }
    public String getEmailPassword() { return email.getPassword(); }
    public boolean isEmailTlsEnabled() { return email.isTlsEnabled(); }
    public boolean isEmailAuthEnabled() { return email.isAuthEnabled(); }
    public int getEmailTimeout() { return email.getTimeout(); }
    public int getEmailConnectionTimeout() { return email.getConnectionTimeout(); }

    // SMS convenience getters
    public String getTwilioAccountSid() { return sms.getAccountSid(); }
    public String getTwilioAuthToken() { return sms.getAuthToken(); }
    public String getTwilioPhoneNumber() { return sms.getFromNumber(); }


    // PUSH Config (NEW)
    private boolean pushEnabled = true;
    private String pushServiceAccountPath = "classpath:firebase-service-account.json";
}
