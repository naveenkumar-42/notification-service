package com.notification.service.sender;

import com.notification.entity.NotificationEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailSender {

    private final JavaMailSender mailSender;
    private final SenderConfig senderConfig;

    @Autowired
    public EmailSender(JavaMailSender mailSender, SenderConfig senderConfig) {
        this.mailSender = mailSender;
        this.senderConfig = senderConfig;

        // 🔧 DEBUG LOG
        log.info("🔧 EmailSender init - FROM='{}' | REPLY_TO='{}'",
                senderConfig.getEmailFromAddress(),
                senderConfig.getEmailReplyTo());
    }

    public void sendSimpleEmail(NotificationEvent event) {
        log.info("Sending SIMPLE EMAIL to: {}", event.getRecipient());
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(event.getRecipient());
            email.setSubject(resolveSubject(event));
            email.setText(event.getMessage());
            email.setFrom(resolveFrom());
            email.setReplyTo(resolveReplyTo());

            mailSender.send(email);
            log.info("✅ SIMPLE EMAIL SENT to: {}", event.getRecipient());
        } catch (Exception e) {
            log.error("❌ SIMPLE EMAIL FAILED", e);
            throw new RuntimeException("Failed to send simple email", e);
        }
    }

    public void sendHtmlEmail(NotificationEvent event) {
        log.info("Sending HTML EMAIL to: {}", event.getRecipient());
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            helper.setTo(event.getRecipient());
            helper.setSubject(resolveSubject(event));
            helper.setFrom(resolveFrom());  // 🔧 FIXED
            helper.setReplyTo(resolveReplyTo());  // 🔧 FIXED
            helper.setText(buildHtmlEmail(event), true);

            mailSender.send(mimeMessage);
            log.info("✅ HTML EMAIL SENT to: {}", event.getRecipient());
        } catch (Exception e) {
            log.error("❌ HTML EMAIL FAILED", e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    // 🔧 SAFE RESOLVE METHODS
    private String resolveFrom() {
        String from = senderConfig.getEmailFromAddress();
        if (from == null || from.trim().isEmpty()) {
            log.warn("⚠️ senderConfig.fromAddress empty, using fallback");
            from = "naveenkumarpoff@gmail.com";  // FALLBACK
        }
        String cleanFrom = from.trim();
        log.debug("📧 Using FROM address: '{}'", cleanFrom);
        return cleanFrom;
    }

    private String resolveReplyTo() {
        String replyTo = senderConfig.getEmailReplyTo();
        if (replyTo == null || replyTo.trim().isEmpty()) {
            log.warn("⚠️ senderConfig.replyTo empty, using fallback");
            replyTo = "naveenkumarpoff@gmail.com";  // FALLBACK
        }
        return replyTo.trim();
    }

    private String resolveSubject(NotificationEvent event) {
        if (event.getSubject() != null && !event.getSubject().isBlank()) {
            return event.getSubject();
        }
        String type = event.getNotificationType() != null ? event.getNotificationType() : "Update";
        return "[Notification] " + type + " | ID " + event.getId();
    }

    private String buildHtmlEmail(NotificationEvent event) {
        String message = HtmlUtils.htmlEscape(event.getMessage() != null ? event.getMessage() : "");
        String notificationType = event.getNotificationType() != null ? event.getNotificationType() : "N/A";
        String channel = event.getChannel() != null ? event.getChannel() : "EMAIL";
        String eventId = event.getId() != null ? event.getId().toString() : "N/A";
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        String year = String.valueOf(Instant.now().atZone(ZoneId.systemDefault()).getYear());

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Enterprise Notification</title>
                <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700&display=swap" rel="stylesheet">
            </head>
            <body style="margin: 0; padding: 0; background-color: #0f172a; font-family: 'Outfit', sans-serif;">
                <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #0f172a; padding: 40px 0;">
                    <tr>
                        <td align="center">
                            <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color: #1e293b; border-radius: 16px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.5); border: 1px solid #334155;">
                                <tr><td height="4" style="background: linear-gradient(90deg, #4f46e5 0%%, #818cf8 100%%);"></td></tr>
                                <tr>
                                    <td style="padding: 40px 48px 24px;">
                                        <div style="display: inline-block; padding: 6px 16px; background-color: rgba(79,70,229,0.15); border: 1px solid rgba(79,70,229,0.3); border-radius: 9999px; color: #818cf8; font-size: 12px; font-weight: 600; text-transform: uppercase; margin-bottom: 24px;">%s</div>
                                        <h1 style="margin: 0; font-size: 28px; font-weight: 700; color: #f8fafc;">New Notification</h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 0 48px 32px;">
                                        <p style="margin: 0 0 16px 0; font-size: 16px; color: #cbd5e1;">Hello,</p>
                                        <p style="margin: 0 0 32px 0; font-size: 16px; line-height: 1.7; color: #cbd5e1;">%s</p>
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #0f172a; border-radius: 12px; border: 1px solid #334155; padding: 20px;">
                                            <tr><td width="30%%" style="padding: 10px 0; font-size: 12px; color: #64748b; font-weight: 600;">Channel</td><td style="padding: 10px 0; font-size: 14px; color: #f8fafc;">%s</td></tr>
                                            <tr><td style="padding: 10px 0; font-size: 12px; color: #64748b; font-weight: 600;">Event ID</td><td style="padding: 10px 0; font-size: 14px; color: #f8fafc; font-family: monospace;">%s</td></tr>
                                            <tr><td style="padding: 10px 0; font-size: 12px; color: #64748b; font-weight: 600;">Time</td><td style="padding: 10px 0; font-size: 14px; color: #f8fafc;">%s</td></tr>
                                        </table>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 32px 48px; background-color: #1e293b; border-top: 1px solid #334155;">
                                        <p style="margin: 0 0 16px 0; font-size: 13px; color: #94a3b8; text-align: center;">This is an automated message. Please do not reply.</p>
                                    </td>
                                </tr>
                            </table>
                            <p style="margin-top: 32px; font-size: 12px; color: #64748b;">© %s NK's Notification. All rights reserved.</p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(notificationType, message, channel, eventId, timestamp, year);
    }
}
