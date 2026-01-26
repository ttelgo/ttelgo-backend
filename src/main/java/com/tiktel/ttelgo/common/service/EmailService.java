package com.tiktel.ttelgo.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Email service for sending emails using Gmail SMTP.
 * Handles OTP email sending with proper formatting and error handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${app.mail.from:support@ttelgo.com}")
    private String fromEmail;
    
    /**
     * Sends an OTP email to the specified recipient asynchronously.
     * This method returns immediately while email is sent in the background.
     * 
     * @param to Recipient email address
     * @param otp The 6-digit OTP code
     * @param expiryMinutes Number of minutes until OTP expires
     * @param purpose Purpose of the OTP (e.g., LOGIN, REGISTER)
     */
    @Async
    public void sendOtpEmail(String to, String otp, int expiryMinutes, String purpose) {
        if (to == null || to.isEmpty()) {
            log.warn("Cannot send OTP email: recipient email is null or empty");
            return;
        }
        
        // At this point, 'to' is guaranteed to be non-null and non-empty
        final String recipientEmail = to;
        final String senderEmail = fromEmail != null ? fromEmail : "support@ttelgo.com";
        
        long startTime = System.currentTimeMillis();
        try {
            log.debug("Preparing OTP email for: {}", recipientEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Your OTP Code");
            
            String emailBody = buildOtpEmailBody(otp, expiryMinutes, purpose);
            if (emailBody == null || emailBody.isEmpty()) {
                log.error("Failed to generate email body for: {}", recipientEmail);
                return;
            }
            helper.setText(emailBody, true); // true = HTML content
            
            log.debug("Sending OTP email to: {} via SMTP", recipientEmail);
            mailSender.send(message);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ OTP email sent successfully to: {} for purpose: {} (took {}ms)", 
                    recipientEmail, purpose != null ? purpose : "UNKNOWN", duration);
        } catch (MessagingException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Failed to send OTP email to: {} after {}ms - Error: {}", 
                    to, duration, e.getMessage(), e);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Unexpected error while sending OTP email to: {} after {}ms - Error: {}", 
                    to, duration, e.getMessage(), e);
        }
    }
    
    /**
     * Builds the HTML email body for OTP emails.
     * 
     * @param otp The OTP code
     * @param expiryMinutes Number of minutes until expiry
     * @param purpose Purpose of the OTP
     * @return HTML formatted email body
     */
    private String buildOtpEmailBody(String otp, int expiryMinutes, String purpose) {
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(expiryMinutes);
        String formattedExpiryTime = expiryTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border-radius: 8px;
                        padding: 30px;
                        border: 1px solid #e0e0e0;
                    }
                    .otp-code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #0066cc;
                        text-align: center;
                        letter-spacing: 8px;
                        padding: 20px;
                        background-color: #ffffff;
                        border-radius: 4px;
                        margin: 20px 0;
                        border: 2px dashed #0066cc;
                    }
                    .info {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 15px;
                        margin: 20px 0;
                    }
                    .footer {
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #e0e0e0;
                        font-size: 12px;
                        color: #666;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Your OTP Code</h2>
                    <p>Hello,</p>
                    <p>You have requested an OTP code for <strong>%s</strong>.</p>
                    <p>Your OTP code is:</p>
                    <div class="otp-code">%s</div>
                    <div class="info">
                        <strong>Important:</strong>
                        <ul>
                            <li>This OTP code will expire in <strong>%d minutes</strong></li>
                            <li>Expires at: <strong>%s</strong></li>
                            <li>Do not share this code with anyone</li>
                            <li>If you did not request this code, please ignore this email</li>
                        </ul>
                    </div>
                    <p>Enter this code to complete your request.</p>
                    <div class="footer">
                        <p>This is an automated email from TTelGo. Please do not reply to this email.</p>
                        <p>&copy; %d TTelGo. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                purpose,
                otp,
                expiryMinutes,
                formattedExpiryTime,
                LocalDateTime.now().getYear()
            );
    }
}

