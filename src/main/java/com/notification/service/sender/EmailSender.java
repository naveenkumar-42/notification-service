package com.notification.service.sender;

import com.notification.entity.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailSender {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private SenderConfig senderConfig;

    /**
     * Send Email using SMTP (Simple Mail Message)
     */
    public void sendSimpleEmail(NotificationEvent event) throws Exception {
        log.info("Sending SIMPLE EMAIL to: {}", event.getRecipient());

        try {
            if (mailSender == null) {
                throw new RuntimeException("JavaMailSender not configured. Add application.properties settings.");
            }

            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(event.getRecipient());
            email.setSubject(generateSubject(event));
            email.setText(event.getMessage());
            email.setFrom(senderConfig.getEmailFromAddress());
            email.setReplyTo(senderConfig.getEmailReplyTo());

            mailSender.send(email);
            log.info("SIMPLE EMAIL SENT SUCCESSFULLY to: {}", event.getRecipient());

        } catch (Exception e) {
            log.error(" SIMPLE EMAIL SEND FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send simple email: " + e.getMessage(), e);
        }
    }

    /**
     * Send HTML Email using MIME Message (Rich formatting)
     */
    public void sendHtmlEmail(NotificationEvent event) throws Exception {
        log.info("Sending HTML EMAIL to: {}", event.getRecipient());

        try {
            if (mailSender == null) {
                throw new RuntimeException("JavaMailSender not configured. Add application.properties settings.");
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(event.getRecipient());
            helper.setSubject(generateSubject(event));
            helper.setFrom(senderConfig.getEmailFromAddress());
            helper.setReplyTo(senderConfig.getEmailReplyTo());

            // Create HTML content with styling
            String htmlContent = buildHtmlEmail(event);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info(" HTML EMAIL SENT SUCCESSFULLY to: {}", event.getRecipient());

        } catch (Exception e) {
            log.error(" HTML EMAIL SEND FAILED: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }

    /**
     * Build HTML email template
     */
    private String buildHtmlEmail(NotificationEvent event) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f5f5f5; }" +
                ".email-container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 4px; text-align: center; }" +
                ".content { padding: 20px; line-height: 1.6; color: #333; }" +
                ".footer { text-align: center; padding-top: 20px; border-top: 1px solid #ddd; color: #999; font-size: 12px; }" +
                ".btn { display: inline-block; background-color: #667eea; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; margin-top: 10px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='email-container'>" +
                "<div class='header'>" +
                "<h2>📧 Notification Service</h2>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Hello,</p>" +
                "<p>" + event.getMessage() + "</p>" +
                "<p>Best regards,<br/>Your Notification System</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>This is an automated message. Please do not reply to this email.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Generate email subject
     */
    private String generateSubject(NotificationEvent event) {
        String message = event.getMessage();
        int maxLength = 50;
        if (message.length() > maxLength) {
            return "✉️ " + message.substring(0, maxLength) + "...";
        }
        return "✉️ " + message;
    }
}