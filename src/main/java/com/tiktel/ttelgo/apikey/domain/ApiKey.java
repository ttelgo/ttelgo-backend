package com.tiktel.ttelgo.apikey.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "key_name", nullable = false)
    private String keyName;
    
    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;
    
    @Column(name = "api_secret", nullable = false)
    private String apiSecret;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "customer_name")
    private String customerName;
    
    @Column(name = "customer_email")
    private String customerEmail;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "rate_limit_per_minute")
    @Builder.Default
    private Integer rateLimitPerMinute = 60;
    
    @Column(name = "rate_limit_per_hour")
    @Builder.Default
    private Integer rateLimitPerHour = 1000;
    
    @Column(name = "rate_limit_per_day")
    @Builder.Default
    private Integer rateLimitPerDay = 10000;
    
    @Column(name = "allowed_ips", columnDefinition = "TEXT")
    private String allowedIps; // JSON array
    
    @Column(name = "scopes", columnDefinition = "TEXT")
    private String scopes; // JSON array of allowed endpoints
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    public boolean isValid() {
        return isActive && !isExpired();
    }
}

