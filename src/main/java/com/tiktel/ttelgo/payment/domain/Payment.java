package com.tiktel.ttelgo.payment.domain;

import com.tiktel.ttelgo.common.domain.enums.PaymentStatus;
import com.tiktel.ttelgo.common.domain.enums.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class Payment {
    private Long id;
    private String paymentNumber;
    
    // Type and references
    private PaymentType type;
    private Long orderId;
    private Long vendorId;
    private Long userId;
    
    // Stripe references
    private String stripePaymentIntentId;
    private String stripeChargeId;
    private String stripeRefundId;
    
    // Amount
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private String currency;
    
    // Status
    private PaymentStatus status;
    
    // Customer details
    private String customerEmail;
    private String customerName;
    
    // Payment method
    private String paymentMethodType;
    private String paymentMethodLast4;
    private String paymentMethodBrand;
    
    // Metadata
    private Map<String, Object> metadata;
    
    // Idempotency
    private String idempotencyKey;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime succeededAt;
    private LocalDateTime failedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime updatedAt;
    
    // Error tracking
    private String errorCode;
    private String errorMessage;
    
    /**
     * Check if payment is successful
     */
    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCEEDED;
    }
    
    /**
     * Check if payment can be refunded
     */
    public boolean canBeRefunded() {
        return status == PaymentStatus.SUCCEEDED &&
               refundedAmount.compareTo(amount) < 0;
    }
    
    /**
     * Get remaining refundable amount
     */
    public BigDecimal getRefundableAmount() {
        return amount.subtract(refundedAmount);
    }
}
