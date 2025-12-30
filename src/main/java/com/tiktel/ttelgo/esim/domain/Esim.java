package com.tiktel.ttelgo.esim.domain;

import com.tiktel.ttelgo.common.domain.enums.EsimStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Esim {
    private Long id;
    private Long orderId;
    private Long userId;
    private Long vendorId;
    
    // Identifiers
    private String iccid;
    private String matchingId;
    private String smdpAddress;
    private String activationCode;
    
    // QR code
    private String qrCode;
    private String qrCodeUrl;
    
    // Bundle
    private String bundleCode;
    private String bundleName;
    
    // Status
    private EsimStatus status;
    
    // Usage
    private Long dataLimitBytes;
    private Long dataUsedBytes;
    private Long dataRemainingBytes;
    
    // Validity
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer validityDays;
    
    // Activation
    private LocalDateTime activatedAt;
    private String activationCodeIos;
    private String activationCodeAndroid;
    
    // Network
    private String countryIso;
    private String networkName;
    
    // Sync
    private LocalDateTime lastSyncedAt;
    private String syncStatus;
    private String syncError;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiredAt;
    
    /**
     * Check if eSIM is active
     */
    public boolean isActive() {
        return status == EsimStatus.ACTIVE;
    }
    
    /**
     * Check if eSIM is expired
     */
    public boolean isExpired() {
        return status == EsimStatus.EXPIRED || 
               (validUntil != null && validUntil.isBefore(LocalDateTime.now()));
    }
    
    /**
     * Get usage percentage
     */
    public double getUsagePercentage() {
        if (dataLimitBytes == null || dataLimitBytes == 0) {
            return 0.0;
        }
        return (dataUsedBytes.doubleValue() / dataLimitBytes.doubleValue()) * 100.0;
    }
}
