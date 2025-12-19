package com.notification.service.sender;

import com.notification.entity.NotificationEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailSender {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private SenderConfig senderConfig;

    /** Send plain text email */
    public void sendSimpleEmail(NotificationEvent event) {
        log.info("Sending SIMPLE EMAIL to: {}", event.getRecipient());
        try {
            ensureConfigured();
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(event.getRecipient());
            email.setSubject(generateSubject(event));
            email.setText(event.getMessage());
            email.setFrom(senderConfig.getEmailFromAddress());
            email.setReplyTo(senderConfig.getEmailReplyTo());
            mailSender.send(email);
            log.info("SIMPLE EMAIL SENT successfully to: {}", event.getRecipient());
        } catch (Exception e) {
            log.error("SIMPLE EMAIL SEND FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send simple email: " + e.getMessage(), e);
        }
    }

    /** Send HTML email */
    public void sendHtmlEmail(NotificationEvent event) {

        String subject = event.getSubject();
        if (subject == null || subject.isBlank()) {
            subject = "[Notification] " + (event.getNotificationType() != null ? event.getNotificationType() : "Update");
        }

        log.info("Sending HTML EMAIL to: {}", event.getRecipient());
        try {
            ensureConfigured();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(event.getRecipient());
            helper.setSubject(subject);
            helper.setFrom(senderConfig.getEmailFromAddress());
            helper.setReplyTo(senderConfig.getEmailReplyTo());
            helper.setText(buildHtmlEmail(event), true);
            mailSender.send(mimeMessage);
            log.info("HTML EMAIL SENT successfully to: {}", event.getRecipient());
        } catch (Exception e) {
            log.error("HTML EMAIL SEND FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }

    private void ensureConfigured() {
        if (mailSender == null) {
            throw new RuntimeException("JavaMailSender not configured. Check SMTP settings.");
        }
    }

    private String generateSubject(NotificationEvent event) {
        // Simple subject using channel and id
        String channel = event.getChannel() != null ? event.getChannel() : "NOTIFICATION";
        return "Notification (" + channel + ") - ID " + event.getId();
    }


    private String buildHtmlEmail(NotificationEvent event) {
        String message = event.getMessage() != null ? event.getMessage() : "";
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; background-color:#f5f5f5; padding:20px;">
                    <div style="max-width:600px; margin:0 auto; background:white; padding:20px; border-radius:8px;">
                      <h2 style="color:#333;">Notification</h2>
                      <p>Hello,</p>
                      <p>%s</p>
                      <p>Best regards,<br/>Your Notification System</p>
                    </div>
                  </body>
                </html>
                """.formatted(message);
    }
}
