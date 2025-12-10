package com.tiktel.ttelgo.esim.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "esims")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Esim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "esim_uuid", unique = true)
    private String esimUuid; // UUID for QR code endpoint
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "bundle_id")
    private String bundleId;
    
    @Column(name = "bundle_name")
    private String bundleName;
    
    @Column(name = "matching_id", unique = true)
    private String matchingId; // From eSIMGo
    
    @Column(name = "iccid", unique = true)
    private String iccid;
    
    @Column(name = "smdp_address")
    private String smdpAddress;
    
    @Column(name = "activation_code", columnDefinition = "TEXT")
    private String activationCode; // QR code data
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EsimStatus status = EsimStatus.PENDING;
    
    @Column(name = "data_amount")
    private Integer dataAmount; // in MB
    
    @Column(name = "data_used")
    @Builder.Default
    private Integer dataUsed = 0; // in MB
    
    @Column(name = "duration_days")
    private Integer durationDays;
    
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "country_iso")
    private String countryIso;
    
    @Column(name = "country_name")
    private String countryName;
    
    @Column(name = "esimgo_order_id")
    private String esimgoOrderId;
    
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
}

