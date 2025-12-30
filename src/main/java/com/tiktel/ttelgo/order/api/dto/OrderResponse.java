package com.tiktel.ttelgo.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderReference;
    private Long userId;
    private String bundleId;
    private String bundleName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private String paymentStatus;
    private String esimId;
    private String matchingId;
    private String iccid;
    private String smdpAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

