package com.notification.service.sender;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Email and SMS senders
 * Load from application.properties or application.yml
 */
@Component
@ConfigurationProperties(prefix = "notification.sender")
@Data
public class SenderConfig {

    // ===== EMAIL CONFIGURATION =====
    private Email email = new Email();

    // ===== SMS (TWILIO) CONFIGURATION =====
    private Sms sms = new Sms();

    // ===== EMAIL NESTED CLASS =====
    @Data
    public static class Email {
        // Gmail SMTP Server Settings
        private String host = "smtp.gmail.com";
        private int port = 587;
        private String username = "";
        private String password = "";
        private String fromAddress = "";
        private String replyTo = "";

        // Email properties
        private boolean tlsEnabled = true;
        private boolean authEnabled = true;
        private int timeout = 5000;
        private int connectionTimeout = 5000;
    }

    // ===== SMS (TWILIO) NESTED CLASS =====
    @Data
    public static class Sms {
        // Twilio API Credentials
        private String accountSid = "";
        private String authToken = "";
        private String fromNumber = "";  // Your Twilio phone number
    }

    // ===== CONVENIENCE GETTERS =====
    public String getEmailFromAddress() {
        return email.getFromAddress();
    }

    public String getEmailReplyTo() {
        return email.getReplyTo() != null ? email.getReplyTo() : email.getFromAddress();
    }

    public String getEmailHost() {
        return email.getHost();
    }

    public int getEmailPort() {
        return email.getPort();
    }

    public String getEmailUsername() {
        return email.getUsername();
    }

    public String getEmailPassword() {
        return email.getPassword();
    }

    public boolean isEmailTlsEnabled() {
        return email.isTlsEnabled();
    }

    public boolean isEmailAuthEnabled() {
        return email.isAuthEnabled();
    }

    public int getEmailTimeout() {
        return email.getTimeout();
    }

    public int getEmailConnectionTimeout() {
        return email.getConnectionTimeout();
    }

    public String getTwilioAccountSid() {
        return sms.getAccountSid();
    }

    public String getTwilioAuthToken() {
        return sms.getAuthToken();
    }

    public String getTwilioPhoneNumber() {
        return sms.getFromNumber();
    }
}