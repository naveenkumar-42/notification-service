package com.notification.controller;

import com.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import jakarta.mail.internet.MimeMessage;

@RestController
@RequestMapping("/api/test")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://notificationservice-blond.vercel.app"})
public class NotificationTestController {

    private final JavaMailSender mailSender;

    // Inject properties directly instead of SenderConfig bean to avoid cycles
    @Value("${notification.sender.sms.account-sid:}")
    private String twilioSid;

    // Use notification.sender.email.from-address (matches SenderConfig) with an ENV fallback
    @Value("${notification.sender.email.from-address:${NOTIFICATION_SENDER_FROM_ADDRESS:noreply@example.com}}")
    private String fromEmail;

    public NotificationTestController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ===== EMAIL TESTS =====

    @GetMapping("/email/simple")
    public NotificationResponse testSimpleEmail() {
        log.info("📧 Testing SIMPLE EMAIL");
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo("naveenkumarpoff@gmail.com");
            email.setSubject("✅ Test Simple Email");
            email.setText("Email service is working!");
            email.setFrom(fromEmail);

            mailSender.send(email);
            return NotificationResponse.builder().status("SUCCESS").message("✅ Email sent!").build();
        } catch (Exception e) {
            log.error("❌ Email failed", e);
            return NotificationResponse.builder().status("FAILED").message(e.getMessage()).build();
        }
    }

    @GetMapping("/email/html")
    public NotificationResponse testHtmlEmail() {
        log.info("📧 Testing HTML EMAIL");
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo("naveenkumarpoff@gmail.com");
            helper.setSubject("✅ Test HTML Email");
            helper.setFrom(fromEmail);
            helper.setText("<h1>✅ HTML Working!</h1><p>Rich text email test.</p>", true);

            mailSender.send(mimeMessage);
            return NotificationResponse.builder().status("SUCCESS").message("✅ HTML Email sent!").build();
        } catch (Exception e) {
            return NotificationResponse.builder().status("FAILED").message(e.getMessage()).build();
        }
    }

    // ===== SMS TESTS =====

    @GetMapping("/sms/config")
    public NotificationResponse testSmsConfig() {
        log.info("📱 Testing SMS CONFIGURATION");
        if (twilioSid == null || twilioSid.isEmpty()) {
            return NotificationResponse.builder()
                    .status("WARNING")
                    .message("⚠️ Twilio SID missing in properties")
                    .build();
        }
        return NotificationResponse.builder()
                .status("SUCCESS")
                .message("✅ Twilio Configured: " + twilioSid.substring(0, 5) + "...")
                .build();
    }

    @GetMapping("/testpush")
    public NotificationResponse testPush() {
        log.info("🧪 Testing PUSH config");
        return NotificationResponse.builder()
                .status("READY")
                .message("✅ Firebase PUSH Endpoint Ready. Use POST /api/notifications/send")
                .build();
    }

    @GetMapping("/health")
    public NotificationResponse healthCheck() {
        return NotificationResponse.builder().status("OK").message("✅ Service RUNNING").build();
    }
}
