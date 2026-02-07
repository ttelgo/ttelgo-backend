package com.tiktel.ttelgo.vendor.infrastructure.repository;

import com.tiktel.ttelgo.common.domain.enums.BillingMode;
import com.tiktel.ttelgo.common.domain.enums.VendorStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE vendors SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class VendorJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "company_name", nullable = false)
    private String companyName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "contact_person")
    private String contactPerson;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false)
    private BillingMode billingMode = BillingMode.PREPAID;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorStatus status = VendorStatus.PENDING_APPROVAL;
    
    // Financial fields
    @Column(name = "wallet_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;
    
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;
    
    @Column(name = "outstanding_balance", precision = 15, scale = 2)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;
    
    @Column(length = 3)
    private String currency = "USD";
    
    // Limits
    @Column(name = "daily_order_limit")
    private Integer dailyOrderLimit;
    
    @Column(name = "monthly_order_limit")
    private Integer monthlyOrderLimit;
    
    @Column(name = "daily_spend_limit", precision = 15, scale = 2)
    private BigDecimal dailySpendLimit;
    
    @Column(name = "monthly_spend_limit", precision = 15, scale = 2)
    private BigDecimal monthlySpendLimit;
    
    // Payment terms (for POSTPAID)
    @Column(name = "payment_terms", length = 50)
    private String paymentTerms;
    
    @Column(name = "invoice_cycle_day")
    private Integer invoiceCycleDay;
    
    // Risk and compliance
    @Column(name = "risk_score")
    private Integer riskScore = 0;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "kyc_completed")
    private Boolean kycCompleted = false;
    
    // Webhook configuration
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;
    
    @Column(name = "webhook_secret")
    private String webhookSecret;
    
    @Column(name = "webhook_enabled")
    private Boolean webhookEnabled = false;
    
    // API access
    @Column(name = "api_enabled")
    private Boolean apiEnabled = true;
    
    // Metadata
    @Column(length = 2)
    private String country;
    
    @Column(length = 50)
    private String timezone = "UTC";
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
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

