package com.tiktel.ttelgo.auth.infrastructure.adapter;

import com.tiktel.ttelgo.auth.application.port.OtpServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class OtpServiceAdapter implements OtpServicePort {
    
    private static final Random random = new Random();
    
    @Override
    public void sendOtp(String email, String phone, String otp, String purpose) {
        // TODO: Integrate with actual email/SMS service (SendGrid, Twilio, etc.)
        // For now, just log it
        if (email != null && !email.isEmpty()) {
            log.info("Sending OTP {} to email {} for purpose {}", otp, email, purpose);
            // In production, send email here
        }
        if (phone != null && !phone.isEmpty()) {
            log.info("Sending OTP {} to phone {} for purpose {}", otp, phone, purpose);
            // In production, send SMS here
        }
    }
    
    @Override
    public String generateOtp() {
        // Generate 6-digit OTP
        return String.format("%06d", random.nextInt(1000000));
    }
}

