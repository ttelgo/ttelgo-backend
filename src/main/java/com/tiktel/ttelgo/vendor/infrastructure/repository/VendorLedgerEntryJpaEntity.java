package com.tiktel.ttelgo.vendor.infrastructure.repository;

import com.tiktel.ttelgo.common.domain.enums.LedgerEntryStatus;
import com.tiktel.ttelgo.common.domain.enums.LedgerEntryType;
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
@Table(name = "vendor_ledger_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorLedgerEntryJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType type;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency = "USD";
    
    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryStatus status = LedgerEntryStatus.COMPLETED;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "payment_id")
    private Long paymentId;
    
    @Column(name = "related_entry_id")
    private Long relatedEntryId;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;
    
    @Column(name = "reversed_by")
    private Long reversedBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

