package com.tiktel.ttelgo.payment.infrastructure.repository;

import com.tiktel.ttelgo.common.domain.enums.PaymentStatus;
import com.tiktel.ttelgo.common.domain.enums.PaymentType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "payment_number", nullable = false, unique = true)
    private String paymentNumber;
    
    // Type and references
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "vendor_id")
    private Long vendorId;
    
    @Column(name = "user_id")
    private Long userId;
    
    // Stripe references
    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;
    
    @Column(name = "stripe_charge_id")
    private String stripeChargeId;
    
    @Column(name = "stripe_refund_id")
    private String stripeRefundId;
    
    // Amount
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "refunded_amount", precision = 15, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, length = 3)
    private String currency = "USD";
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.CREATED;
    
    // Customer details
    @Column(name = "customer_email")
    private String customerEmail;
    
    @Column(name = "customer_name")
    private String customerName;
    
    // Payment method
    @Column(name = "payment_method_type", length = 50)
    private String paymentMethodType;
    
    @Column(name = "payment_method_last4", length = 10)
    private String paymentMethodLast4;
    
    @Column(name = "payment_method_brand", length = 50)
    private String paymentMethodBrand;
    
    // Metadata (from Stripe)
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    // Idempotency
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    
    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "succeeded_at")
    private LocalDateTime succeededAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Error tracking
    @Column(name = "error_code", length = 100)
    private String errorCode;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
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

