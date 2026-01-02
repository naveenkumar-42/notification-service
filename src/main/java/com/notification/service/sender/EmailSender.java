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
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Enterprise Notification</title>
                            <!-- Import Outfit Font -->
                            <link rel="preconnect" href="https://fonts.googleapis.com">
                            <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                            <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700&display=swap" rel="stylesheet">
                            <style>
                                @media only screen and (max-width: 600px) {
                                    .container {
                                        width: 100%% !important;
                                        padding: 0 !important;
                                    }
                                    .content-card {
                                        width: 100%% !important;
                                        border-radius: 0 !important;
                                    }
                                    .mobile-pad {
                                        padding: 24px !important;
                                    }
                                }
                            </style>
                        </head>
                        <body
                            style="margin: 0; padding: 0; background-color: #0f172a; font-family: 'Outfit', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; -webkit-font-smoothing: antialiased; color: #f8fafc;">
                            <!-- Main Container -->
                            <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #0f172a; padding: 40px 0;">
                                <tr>
                                    <td align="center">
                                        <!-- Brand / Logo Area -->
                                        <table border="0" cellpadding="0" cellspacing="0" width="600" style="margin-bottom: 24px;">
                                            <tr>
                                                <td align="center">
                                                    <h2
                                                        style="margin: 0; color: #f8fafc; font-size: 20px; font-weight: 700; letter-spacing: -0.025em; text-transform: uppercase;">
                                                        <span style="color: #4f46e5;">NK's</span> Notification
                                                    </h2>
                                                </td>
                                            </tr>
                                        </table>
                                        <!-- Card Container -->
                                        <table border="0" cellpadding="0" cellspacing="0" width="600" class="content-card"
                                            style="background-color: #1e293b; border-radius: 16px; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.5), 0 4px 6px -2px rgba(0, 0, 0, 0.3); overflow: hidden; border: 1px solid #334155;">
                                            <!-- Decorative Top Border -->
                                            <tr>
                                                <td height="4" style="background: linear-gradient(90deg, #4f46e5 0%%, #818cf8 100%%);"></td>
                                            </tr>
                                            <!-- Header Section -->
                                            <tr>
                                                <td class="mobile-pad" style="padding: 40px 48px 24px 48px; text-align: center;">
                                                    <!-- Status Badge -->
                                                    <div
                                                        style="display: inline-block; padding: 6px 16px; background-color: rgba(79, 70, 229, 0.15); border: 1px solid rgba(79, 70, 229, 0.3); border-radius: 9999px; color: #818cf8; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 24px;">
                                                        %s
                                                    </div>
                                                    <!-- Main Headline -->
                                                    <h1
                                                        style="margin: 0; font-size: 28px; font-weight: 700; color: #f8fafc; letter-spacing: -0.025em; line-height: 1.2;">
                                                        New Notification
                                                    </h1>
                                                </td>
                                            </tr>
                                            <!-- Body Content -->
                                            <tr>
                                                <td class="mobile-pad" style="padding: 0 48px 32px 48px;">
                                                    <!-- Greeting -->
                                                    <p style="margin: 0 0 16px 0; font-size: 16px; color: #cbd5e1;">
                                                        Hello,
                                                    </p>
                                                    <!-- Dynamic Message -->
                                                    <p style="margin: 0 0 32px 0; font-size: 16px; line-height: 1.7; color: #cbd5e1;">
                                                        %s
                                                    </p>
                                                    <!-- CTA Button Placeholder -->
                                                    <!-- <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="margin-bottom: 32px;">
                                                        <tr>
                                                            <td align="center">
                                                                <a href="#" style="display: inline-block; padding: 14px 32px; background-color: #4f46e5; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 15px; box-shadow: 0 4px 6px -1px rgba(79, 70, 229, 0.3);">
                                                                    View Details
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    </table> -->
                                                    <!-- Details Table -->
                                                    <table border="0" cellpadding="0" cellspacing="0" width="100%%"
                                                        style="background-color: #0f172a; border-radius: 12px; border: 1px solid #334155; padding: 20px;">
                                                        <tr>
                                                            <td width="30%%"
                                                                style="padding: 10px 0; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; font-weight: 600;">
                                                                Channel</td>
                                                            <td
                                                                style="padding: 10px 0; font-size: 14px; color: #f8fafc; font-weight: 500; border-bottom: 1px solid #1e293b;">
                                                                %s</td>
                                                        </tr>
                                                        <tr>
                                                            <td
                                                                style="padding: 10px 0; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; font-weight: 600;">
                                                                Event ID</td>
                                                            <td
                                                                style="padding: 10px 0; font-size: 14px; color: #f8fafc; font-family: 'Courier New', monospace; border-bottom: 1px solid #1e293b;">
                                                                %s</td>
                                                        </tr>
                                                        <tr>
                                                            <td
                                                                style="padding: 10px 0; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b; font-weight: 600;">
                                                                Time</td>
                                                            <td style="padding: 10px 0; font-size: 14px; color: #f8fafc;">%s</td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                            <!-- Footer -->
                                            <tr>
                                                <td style="padding: 32px 48px; background-color: #1e293b; border-top: 1px solid #334155;">
                                                    <p
                                                        style="margin: 0 0 16px 0; font-size: 13px; line-height: 1.5; color: #94a3b8; text-align: center;">
                                                        Need help? <a href="#" style="color: #818cf8; text-decoration: none;">Contact
                                                            Support</a>
                                                    </p>
                                                    <p
                                                        style="margin: 0; font-size: 12px; line-height: 1.5; color: #64748b; text-align: center;">
                                                        This is an automated message requested by the system.<br>
                                                        Please do not reply to this email.
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                        <!-- Copyright -->
                                        <p style="margin-top: 32px; font-size: 12px; color: #64748b;">
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