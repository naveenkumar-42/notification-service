package com.notification.service.sender;

import com.notification.entity.NotificationEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    }

    /**
     * Send plain text email
     */
    public void sendSimpleEmail(NotificationEvent event) {
        log.info("Sending SIMPLE EMAIL to: {}", event.getRecipient());
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(event.getRecipient());
            email.setSubject(resolveSubject(event));
            email.setText(event.getMessage());
            email.setFrom(senderConfig.getEmailFromAddress());
            email.setReplyTo(senderConfig.getEmailReplyTo());

            mailSender.send(email);
            log.info("SIMPLE EMAIL SENT successfully to: {}", event.getRecipient());
        } catch (Exception e) {
            log.error("SIMPLE EMAIL SEND FAILED", e);
            throw new RuntimeException("Failed to send simple email", e);
        }
    }

    /**
     * Send HTML email
     */
    public void sendHtmlEmail(NotificationEvent event) {
        log.info("Sending HTML EMAIL to: {}", event.getRecipient());
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            helper.setTo(event.getRecipient());
            helper.setSubject(resolveSubject(event));
            helper.setFrom(senderConfig.getEmailFromAddress());
            helper.setReplyTo(senderConfig.getEmailReplyTo());
            helper.setText(buildHtmlEmail(event), true);

            mailSender.send(mimeMessage);
            log.info("HTML EMAIL SENT successfully to: {}", event.getRecipient());
        } catch (Exception e) {
            log.error("HTML EMAIL SEND FAILED", e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    private String resolveSubject(NotificationEvent event) {
        if (event.getSubject() != null && !event.getSubject().isBlank()) {
            return event.getSubject();
        }
        String type = event.getNotificationType() != null ? event.getNotificationType() : "Update";
        return "[Notification] " + type + " | ID " + event.getId();
    }

    private String buildHtmlEmail(NotificationEvent event) {

        String message = HtmlUtils.htmlEscape(
                event.getMessage() != null ? event.getMessage() : ""
        );

        String notificationType =
                event.getNotificationType() != null ? event.getNotificationType() : "N/A";

        String channel =
                event.getChannel() != null ? event.getChannel() : "EMAIL";

        String eventId =
                event.getId() != null ? event.getId().toString() : "N/A";

        String timestamp =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.now());

        String year =
                String.valueOf(Instant.now().atZone(ZoneId.systemDefault()).getYear());

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Enterprise Notification</title>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#f2f4f7;
                             font-family: Arial, Helvetica, sans-serif; color:#1f2933;">
                
                <table width="100%%" cellpadding="0" cellspacing="0"
                       style="background-color:#f2f4f7; padding:24px 0;">
                    <tr>
                        <td align="center">
                
                            <table width="600" cellpadding="0" cellspacing="0"
                                   style="background-color:#ffffff; border-radius:8px;
                                          box-shadow:0 2px 6px rgba(0,0,0,0.08);">
                
                                <tr>
                                    <td style="background-color:#0b5ed7; padding:20px 24px;">
                                        <h1 style="margin:0; font-size:20px; color:#ffffff;">
                                            Enterprise Notification
                                        </h1>
                                    </td>
                                </tr>
                
                                <tr>
                                    <td style="padding:24px;">
                                        <p>Hello,</p>
                
                                        <p style="line-height:1.6;">%s</p>
                
                                        <hr style="border:none; border-top:1px solid #e5e7eb; margin:24px 0;">
                
                                        <table width="100%%" style="font-size:13px;">
                                            <tr>
                                                <td width="35%%"><strong>Notification Type:</strong></td>
                                                <td>%s</td>
                                            </tr>
                                            <tr>
                                                <td><strong>Channel:</strong></td>
                                                <td>%s</td>
                                            </tr>
                                            <tr>
                                                <td><strong>Event ID:</strong></td>
                                                <td>%s</td>
                                            </tr>
                                            <tr>
                                                <td><strong>Timestamp:</strong></td>
                                                <td>%s</td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                
                                <tr>
                                    <td style="background-color:#f9fafb; padding:16px 24px;
                                               font-size:12px; color:#6b7280;">
                                        <p>This is an automated message. Do not reply.</p>
                                    </td>
                                </tr>
                
                            </table>
                
                            <p style="font-size:11px; color:#9ca3af; margin-top:16px;">
                                © %s Your Organization. All rights reserved.
                            </p>
                
                        </td>
                    </tr>
                </table>
                
                </body>
                </html>
                """
                .formatted(message, notificationType, channel, eventId, timestamp, year);
    }
}