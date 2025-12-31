package com.tiktel.ttelgo.integration.esimgo.domain;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Internal domain model for Order Result from eSIM Go
 */
@Data
@Builder
public class OrderResult {
    private String orderId;
    private String iccid;
    private String matchingId;
    private String smdpAddress;
    private String activationCode;
    private String bundleCode;
    private String bundleName;
    private BigDecimal price;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private String qrCodeUrl;
}

