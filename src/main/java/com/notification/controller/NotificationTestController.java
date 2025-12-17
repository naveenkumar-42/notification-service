package com.notification.controller;

import com.notification.dto.NotificationRequest;
import com.notification.dto.NotificationResponse;
import com.notification.service.NotificationService;
import com.notification.service.sender.SenderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import jakarta.mail.internet.MimeMessage;

/**
 * Test Controller for Email & SMS Testing
 * Standalone endpoints to test functionality
 */
@RestController
@RequestMapping("/api/test")
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationTestController {

    private final JavaMailSender mailSender;

    public NotificationTestController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ===== EMAIL TESTS =====

    /**
     * Test Simple Email Send
     * GET: http://localhost:8080/api/test/email/simple
     */
    @GetMapping("/email/simple")
    public NotificationResponse testSimpleEmail() {
        log.info("📧 Testing SIMPLE EMAIL");
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo("naveenkumarpoff@gmail.com");
            email.setSubject("✅ Test Simple Email from Spring Boot");
            email.setText("If you see this, EMAIL is working!\n\n" +
                    "Your notification system is ready! 🎉");
            email.setFrom("your-email@gmail.com");

            mailSender.send(email);
            log.info("✅ SIMPLE EMAIL TEST PASSED!");

            return NotificationResponse.builder()
                    .status("SUCCESS")
                    .message("✅ Simple Email sent successfully!")
                    .build();

        } catch (Exception e) {
            log.error("❌ SIMPLE EMAIL TEST FAILED: {}", e.getMessage(), e);
            return NotificationResponse.builder()
                    .status("FAILED")
                    .message("❌ Simple Email send failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Test HTML Email Send
     * GET: http://localhost:8080/api/test/email/html
     */
    @GetMapping("/email/html")
    public NotificationResponse testHtmlEmail() {
        log.info("📧 Testing HTML EMAIL");
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo("naveenkumarpoff@gmail.com");
            helper.setSubject("✅ Test HTML Email from Spring Boot");
            helper.setFrom("your-email@gmail.com");

            String htmlContent = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head><meta charset='UTF-8'></head>" +
                    "<body style='font-family: Arial; background-color: #f5f5f5;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px;'>" +
                    "<h2 style='color: #667eea;'>✅ HTML Email Test</h2>" +
                    "<p>Congratulations! Your <strong>HTML Email</strong> is working perfectly! 🎉</p>" +
                    "<p>This email demonstrates rich text formatting capabilities.</p>" +
                    "<hr style='border: none; border-top: 1px solid #ddd;'/>" +
                    "<p style='color: #999; font-size: 12px;'>This is an automated message. Please do not reply.</p>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("✅ HTML EMAIL TEST PASSED!");

            return NotificationResponse.builder()
                    .status("SUCCESS")
                    .message("✅ HTML Email sent successfully!")
                    .build();

        } catch (Exception e) {
            log.error("❌ HTML EMAIL TEST FAILED: {}", e.getMessage(), e);
            return NotificationResponse.builder()
                    .status("FAILED")
                    .message("❌ HTML Email send failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Test Email with Custom Parameters
     * POST: http://localhost:8080/api/test/email/custom
     * Body: { "notificationType": "EMAIL", "recipient": "test@example.com", "message": "Test message" }
     */
    @PostMapping("/email/custom")
    public NotificationResponse testCustomEmail(@RequestBody NotificationRequest request) {
        log.info("📧 Testing CUSTOM EMAIL to: {}", request.getRecipient());
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(request.getRecipient());
            email.setSubject("✅ Custom Notification: " + request.getMessage().substring(0, Math.min(30, request.getMessage().length())));
            email.setText(request.getMessage());
            email.setFrom("your-email@gmail.com");

            mailSender.send(email);
            log.info("✅ CUSTOM EMAIL TEST PASSED to: {}", request.getRecipient());

            return NotificationResponse.builder()
                    .status("SUCCESS")
                    .message("✅ Custom Email sent to: " + request.getRecipient())
                    .build();

        } catch (Exception e) {
            log.error("❌ CUSTOM EMAIL TEST FAILED: {}", e.getMessage(), e);
            return NotificationResponse.builder()
                    .status("FAILED")
                    .message("❌ Custom Email send failed: " + e.getMessage())
                    .build();
        }
    }

    // ===== SMS TESTS =====

    /**
     * Test SMS Configuration (Validate Twilio Setup)
     * GET: http://localhost:8080/api/test/sms/config
     */

    @Autowired
    private SenderConfig  senderConfig;

    @GetMapping("/sms/config")
    public NotificationResponse testSmsConfig() {
        log.info("📱 Testing SMS CONFIGURATION");
        try {
            String accountSid = senderConfig.getTwilioAccountSid();
            String authToken  = senderConfig.getTwilioAuthToken();
            String phoneNumber      = senderConfig.getTwilioPhoneNumber();

            if (accountSid == null || authToken == null || phoneNumber == null) {
                log.warn("⚠️ Twilio credentials not found in environment variables");
                return NotificationResponse.builder()
                        .status("WARNING")
                        .message("⚠️ Twilio credentials not found. Please set environment variables or application.properties")
                        .build();
            }

            log.info("✅ SMS CONFIG TEST PASSED!");
            return NotificationResponse.builder()
                    .status("SUCCESS")
                    .message("✅ Twilio SMS is configured correctly!")
                    .build();

        } catch (Exception e) {
            log.error("❌ SMS CONFIG TEST FAILED: {}", e.getMessage(), e);
            return NotificationResponse.builder()
                    .status("FAILED")
                    .message("❌ SMS Config test failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get SMS Instructions
     * GET: http://localhost:8080/api/test/sms/instructions
     */
    @GetMapping("/sms/instructions")
    public String getSmsInstructions() {
        return "📱 SMS (TWILIO) SETUP INSTRUCTIONS:\n\n" +
                "1. Sign up at: https://www.twilio.com\n" +
                "2. Get your credentials from: https://www.twilio.com/console\n" +
                "3. Add to application.properties:\n" +
                "   notification.sender.sms.account-sid=your-account-sid\n" +
                "   notification.sender.sms.auth-token=your-auth-token\n" +
                "   notification.sender.sms.from-number=+1234567890\n" +
                "4. Format phone numbers: +1234567890 or 1234567890\n" +
                "5. Send SMS via: POST /api/notifications/send\n" +
                "   Body: { \"notificationType\": \"SMS\", \"recipient\": \"+1234567890\", \"message\": \"Hello\" }\n";
    }

    // ===== GENERAL TESTS =====

    /**
     * Health Check
     * GET: http://localhost:8080/api/test/health
     */
    @GetMapping("/health")
    public NotificationResponse healthCheck() {
        log.info("❤️ Health Check Called");
        return NotificationResponse.builder()
                .status("OK")
                .message("✅ Notification Service is RUNNING")
                .build();
    }

    /**
     * Get All Test Endpoints
     * GET: http://localhost:8080/api/test/endpoints
     */
    @GetMapping("/endpoints")
    public String getTestEndpoints() {
        return "📋 AVAILABLE TEST ENDPOINTS:\n\n" +
                "EMAIL TESTS:\n" +
                "  1. Simple Email: GET http://localhost:8080/api/test/email/simple\n" +
                "  2. HTML Email: GET http://localhost:8080/api/test/email/html\n" +
                "  3. Custom Email: POST http://localhost:8080/api/test/email/custom\n" +
                "     Body: {\"notificationType\":\"EMAIL\", \"recipient\":\"test@example.com\", \"message\":\"Your message\"}\n\n" +
                "SMS TESTS:\n" +
                "  1. SMS Config: GET http://localhost:8080/api/test/sms/config\n" +
                "  2. SMS Instructions: GET http://localhost:8080/api/test/sms/instructions\n\n" +
                "GENERAL:\n" +
                "  1. Health Check: GET http://localhost:8080/api/test/health\n" +
                "  2. Endpoints: GET http://localhost:8080/api/test/endpoints\n";
    }
}
