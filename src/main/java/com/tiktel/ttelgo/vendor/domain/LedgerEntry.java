package com.tiktel.ttelgo.vendor.domain;

import com.tiktel.ttelgo.common.domain.enums.LedgerEntryStatus;
import com.tiktel.ttelgo.common.domain.enums.LedgerEntryType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class LedgerEntry {
    private Long id;
    private Long vendorId;
    private LedgerEntryType type;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private LedgerEntryStatus status;
    private Long orderId;
    private Long paymentId;
    private Long relatedEntryId;
    private String description;
    private String referenceNumber;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime reversedAt;
    private Long reversedBy;
}

