package com.tiktel.ttelgo.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(unique = true)
    private String phone;
    
    private String password;
    
    private String firstName;
    
    private String lastName;
    
    private String country;
    
    private String city;
    
    private String address;
    
    private String postalCode;
    
    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;
    
    @Column(name = "is_phone_verified")
    @Builder.Default
    private Boolean isPhoneVerified = false;
    
    @Column(name = "referral_code", unique = true)
    private String referralCode;
    
    @Column(name = "referred_by")
    private Long referredBy;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.USER;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum UserRole {
        USER, ADMIN, SUPER_ADMIN
    }
}

