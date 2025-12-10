package com.tiktel.ttelgo.payment.domain;

import com.tiktel.ttelgo.order.domain.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "payment_intent_id")
    private String paymentIntentId; // Stripe payment intent ID
    
    @Column(name = "charge_id")
    private String chargeId; // Stripe charge ID
    
    @Column(name = "customer_id")
    private String customerId; // Stripe customer ID
    
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;
    
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "payment_method")
    private String paymentMethod; // card, bank_transfer, etc.
    
    @Column(name = "payment_method_details", columnDefinition = "TEXT")
    private String paymentMethodDetails; // JSON string
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "refund_id")
    private String refundId; // Stripe refund ID if refunded
    
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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

