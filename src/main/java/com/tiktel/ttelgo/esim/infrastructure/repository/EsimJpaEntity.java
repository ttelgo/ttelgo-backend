package com.tiktel.ttelgo.esim.infrastructure.repository;

import com.tiktel.ttelgo.common.domain.enums.EsimStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "esims")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE esims SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class EsimJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // References
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "vendor_id")
    private Long vendorId;
    
    // eSIM identifiers
    @Column(nullable = false, unique = true, length = 30)
    private String iccid;
    
    @Column(name = "matching_id")
    private String matchingId;
    
    @Column(name = "smdp_address")
    private String smdpAddress;
    
    @Column(name = "activation_code", length = 500)
    private String activationCode;
    
    // QR code
    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;
    
    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;
    
    // Bundle details
    @Column(name = "bundle_code", length = 100)
    private String bundleCode;
    
    @Column(name = "bundle_name")
    private String bundleName;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EsimStatus status = EsimStatus.CREATED;
    
    // Usage tracking
    @Column(name = "data_limit_bytes")
    private Long dataLimitBytes;
    
    @Column(name = "data_used_bytes")
    private Long dataUsedBytes = 0L;
    
    @Column(name = "data_remaining_bytes")
    private Long dataRemainingBytes;
    
    // Validity
    @Column(name = "valid_from")
    private LocalDateTime validFrom;
    
    @Column(name = "valid_until")
    private LocalDateTime validUntil;
    
    @Column(name = "validity_days")
    private Integer validityDays;
    
    // Activation
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
    
    @Column(name = "activation_code_ios", length = 500)
    private String activationCodeIos;
    
    @Column(name = "activation_code_android", length = 500)
    private String activationCodeAndroid;
    
    // Country/Network
    @Column(name = "country_iso", length = 2)
    private String countryIso;
    
    @Column(name = "network_name")
    private String networkName;
    
    // Sync with eSIM Go
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
    
    @Column(name = "sync_status", length = 50)
    private String syncStatus;
    
    @Column(name = "sync_error", columnDefinition = "TEXT")
    private String syncError;
    
    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
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

