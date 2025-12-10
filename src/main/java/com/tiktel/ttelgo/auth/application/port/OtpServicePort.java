package com.tiktel.ttelgo.auth.application.port;

public interface OtpServicePort {
    void sendOtp(String email, String phone, String otp, String purpose);
    String generateOtp();
}

