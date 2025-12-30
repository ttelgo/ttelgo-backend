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
        // OTP expires in 10 minutes by default
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(10);
        }
    }
}

