package com.tiktel.ttelgo.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_reference", unique = true, nullable = false)
    private String orderReference; // UUID for external reference
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "bundle_id")
    private String bundleId;
    
    @Column(name = "bundle_name")
    private String bundleName;

    @Column(name = "package_name")
    private String packageName;
    
    private Integer quantity;
    
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Column(name = "esim_id")
    private String esimId; // Reference to eSIM
    
    @Column(name = "matching_id")
    private String matchingId; // From eSIMGo
    
    @Column(name = "iccid")
    private String iccid;
    
    @Column(name = "smdp_address")
    private String smdpAddress;
    
    @Column(name = "esimgo_order_id")
    private String esimgoOrderId;
    
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

