package com.tiktel.ttelgo.integration.esimgo.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Internal domain model for eSIM details
 */
@Data
@Builder
public class EsimDetails {
    private String iccid;
    private String matchingId;
    private String smdpAddress;
    private String activationCode;
    private String status;
    private Long dataUsedBytes;
    private Long dataLimitBytes;
    private Long dataRemainingBytes;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime activatedAt;
    private String networkName;
    private String countryCode;
}

