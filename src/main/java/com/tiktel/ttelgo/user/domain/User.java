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
    
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "password_hash")
    private String password;
    
    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;
    
    @Column(name = "provider_id")
    private String providerId;
    
    @Column(name = "picture_url")
    private String pictureUrl;
    
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    @Builder.Default
    private UserType userType = UserType.CUSTOMER;
    
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
        USER, ADMIN, SUPER_ADMIN, SUPPORT
    }
    
    public enum UserType {
        CUSTOMER, VENDOR, ADMIN
    }
    
    public enum AuthProvider {
        LOCAL, GOOGLE, FACEBOOK, APPLE, EMAIL
    }
}

