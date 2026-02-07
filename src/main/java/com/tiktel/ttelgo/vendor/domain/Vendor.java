package com.tiktel.ttelgo.vendor.domain;

import com.tiktel.ttelgo.common.domain.enums.BillingMode;
import com.tiktel.ttelgo.common.domain.enums.VendorStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class Vendor {
    private Long id;
    private String name;
    private String companyName;
    private String email;
    private String phoneNumber;
    private String contactPerson;
    private BillingMode billingMode;
    private VendorStatus status;
    
    // Financial
    private BigDecimal walletBalance;
    private BigDecimal creditLimit;
    private BigDecimal outstandingBalance;
    private String currency;
    
    // Limits
    private Integer dailyOrderLimit;
    private Integer monthlyOrderLimit;
    private BigDecimal dailySpendLimit;
    private BigDecimal monthlySpendLimit;
    
    // Payment terms
    private String paymentTerms;
    private Integer invoiceCycleDay;
    
    // Risk
    private Integer riskScore;
    private Boolean isVerified;
    private Boolean kycCompleted;
    
    // Webhook
    private String webhookUrl;
    private String webhookSecret;
    private Boolean webhookEnabled;
    
    // API
    private Boolean apiEnabled;
    
    // Metadata
    private String country;
    private String timezone;
    private String notes;
    
    // Audit
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    
    /**
     * Check if vendor has sufficient balance for an order
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        if (billingMode == BillingMode.PREPAID) {
            return walletBalance.compareTo(amount) >= 0;
        } else if (billingMode == BillingMode.POSTPAID) {
            BigDecimal availableCredit = creditLimit.subtract(outstandingBalance);
            return availableCredit.compareTo(amount) >= 0;
        }
        return false;
    }
    
    /**
     * Check if vendor is active and can place orders
     */
    public boolean canPlaceOrders() {
        return status == VendorStatus.ACTIVE && 
               Boolean.TRUE.equals(apiEnabled) &&
               Boolean.TRUE.equals(isVerified);
    }
    
    /**
     * Get available balance/credit
     */
    public BigDecimal getAvailableBalance() {
        if (billingMode == BillingMode.PREPAID) {
            return walletBalance;
        } else {
            return creditLimit.subtract(outstandingBalance);
        }
    }
}

