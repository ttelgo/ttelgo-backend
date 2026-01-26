package com.tiktel.ttelgo.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "otp_code", nullable = false)
    private String otpCode;
    
    @Column(name = "purpose")
    private String purpose; // LOGIN, REGISTER, RESET_PASSWORD, VERIFY_EMAIL, VERIFY_PHONE
    
    @Column(name = "is_used")
    @Builder.Default
    private Boolean isUsed = false;
    
    @Column(name = "attempts")
    @Builder.Default
    private Integer attempts = 0;
    
    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 3;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // OTP expires in 5 minutes by default
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(5);
        }
    }
    
    /**
     * Verify if the provided plain OTP matches the stored hashed OTP.
     * @param plainOtp The plain OTP to verify
     * @param passwordEncoder The password encoder to use for verification
     * @return true if OTP matches, false otherwise
     */
    public boolean verifyOtp(String plainOtp, PasswordEncoder passwordEncoder) {
        if (plainOtp == null || plainOtp.isEmpty()) {
            return false;
        }
        if (this.otpCode == null || this.otpCode.isEmpty()) {
            return false;
        }
        // Trim the plain OTP before comparison
        return passwordEncoder.matches(plainOtp.trim(), this.otpCode);
    }
}

