package com.tiktel.ttelgo.auth.infrastructure.adapter;

import com.tiktel.ttelgo.auth.application.port.OtpServicePort;
import com.tiktel.ttelgo.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpServiceAdapter implements OtpServicePort {
    
    private static final Random random = new Random();
    private static final int OTP_EXPIRY_MINUTES = 5;
    
    private final EmailService emailService;
    
    @Override
    public void sendOtp(String email, String phone, String otp, String purpose) {
        // Send email if email is provided (asynchronously - returns immediately)
        if (email != null && !email.isEmpty()) {
            log.info("Initiating async OTP email send to: {} for purpose: {}", email, purpose);
            // Email is sent asynchronously - method returns immediately
            emailService.sendOtpEmail(email, otp, OTP_EXPIRY_MINUTES, purpose);
            log.debug("OTP email send initiated (async) for: {}", email);
        }
        
        // Send SMS if phone is provided (TODO: Implement SMS service)
        if (phone != null && !phone.isEmpty()) {
            log.info("Sending OTP {} to phone {} for purpose {} - SMS service not yet implemented", otp, phone, purpose);
            // TODO: Integrate with SMS service (Twilio, etc.)
        }
    }
    
    @Override
    public String generateOtp() {
        // Generate 6-digit OTP
        return String.format("%06d", random.nextInt(1000000));
    }
}

