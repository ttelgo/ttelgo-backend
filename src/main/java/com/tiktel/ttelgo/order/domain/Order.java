package com.tiktel.ttelgo.order.domain;

import com.tiktel.ttelgo.common.domain.enums.OrderStatus;
import com.tiktel.ttelgo.common.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class Order {
    private Long id;
    private String orderNumber;
    
    // Customer/Vendor
    private Long userId;
    private Long vendorId;
    private String customerEmail;
    
    // Order details
    private String bundleCode;
    private String bundleName;
    private Integer quantity;
    
    // Pricing
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String currency;
    
    // eSIM Go reference
    private String esimgoOrderId;
    
    // Status
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    
    // Metadata
    private String countryIso;
    private String dataAmount;
    private Integer validityDays;
    
    // Tracking
    private String ipAddress;
    private String userAgent;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime provisionedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime updatedAt;
    
    // Error tracking
    private String errorCode;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime lastRetryAt;
    
    /**
     * Check if order is B2C
     */
    public boolean isB2C() {
        return userId != null && vendorId == null;
    }
    
    /**
     * Check if order is B2B
     */
    public boolean isB2B() {
        return vendorId != null && userId == null;
    }
    
    /**
     * Check if order can be canceled
     */
    public boolean canBeCanceled() {
        return status == OrderStatus.ORDER_CREATED || 
               status == OrderStatus.PAYMENT_PENDING ||
               status == OrderStatus.PENDING_SYNC;
    }
    
    /**
     * Check if order is in a final state
     */
    public boolean isFinalState() {
        return status == OrderStatus.COMPLETED ||
               status == OrderStatus.FAILED ||
               status == OrderStatus.CANCELED ||
               status == OrderStatus.REFUNDED;
    }
    
    /**
     * Check if order needs provisioning
     */
    public boolean needsProvisioning() {
        return status == OrderStatus.PAID && provisionedAt == null;
    }
}
