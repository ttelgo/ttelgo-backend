package com.tiktel.ttelgo.order.api.dto;

import com.tiktel.ttelgo.common.domain.enums.OrderStatus;
import com.tiktel.ttelgo.common.domain.enums.PaymentStatus;
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
    private String orderNumber;
    private String bundleCode;
    private String bundleName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String currency;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String countryIso;
    private String dataAmount;
    private Integer validityDays;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}
