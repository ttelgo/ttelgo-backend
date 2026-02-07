package com.tiktel.ttelgo.vendor.infrastructure.mapper;

import com.tiktel.ttelgo.vendor.domain.LedgerEntry;
import com.tiktel.ttelgo.vendor.domain.Vendor;
import com.tiktel.ttelgo.vendor.infrastructure.repository.VendorJpaEntity;
import com.tiktel.ttelgo.vendor.infrastructure.repository.VendorLedgerEntryJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class VendorMapper {
    
    public Vendor toDomain(VendorJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return Vendor.builder()
                .id(entity.getId())
                .name(entity.getName())
                .companyName(entity.getCompanyName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .contactPerson(entity.getContactPerson())
                .billingMode(entity.getBillingMode())
                .status(entity.getStatus())
                .walletBalance(entity.getWalletBalance())
                .creditLimit(entity.getCreditLimit())
                .outstandingBalance(entity.getOutstandingBalance())
                .currency(entity.getCurrency())
                .dailyOrderLimit(entity.getDailyOrderLimit())
                .monthlyOrderLimit(entity.getMonthlyOrderLimit())
                .dailySpendLimit(entity.getDailySpendLimit())
                .monthlySpendLimit(entity.getMonthlySpendLimit())
                .paymentTerms(entity.getPaymentTerms())
                .invoiceCycleDay(entity.getInvoiceCycleDay())
                .riskScore(entity.getRiskScore())
                .isVerified(entity.getIsVerified())
                .kycCompleted(entity.getKycCompleted())
                .webhookUrl(entity.getWebhookUrl())
                .webhookSecret(entity.getWebhookSecret())
                .webhookEnabled(entity.getWebhookEnabled())
                .apiEnabled(entity.getApiEnabled())
                .country(entity.getCountry())
                .timezone(entity.getTimezone())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
    
    public VendorJpaEntity toEntity(Vendor domain) {
        if (domain == null) {
            return null;
        }
        
        return VendorJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .companyName(domain.getCompanyName())
                .email(domain.getEmail())
                .phoneNumber(domain.getPhoneNumber())
                .contactPerson(domain.getContactPerson())
                .billingMode(domain.getBillingMode())
                .status(domain.getStatus())
                .walletBalance(domain.getWalletBalance())
                .creditLimit(domain.getCreditLimit())
                .outstandingBalance(domain.getOutstandingBalance())
                .currency(domain.getCurrency())
                .dailyOrderLimit(domain.getDailyOrderLimit())
                .monthlyOrderLimit(domain.getMonthlyOrderLimit())
                .dailySpendLimit(domain.getDailySpendLimit())
                .monthlySpendLimit(domain.getMonthlySpendLimit())
                .paymentTerms(domain.getPaymentTerms())
                .invoiceCycleDay(domain.getInvoiceCycleDay())
                .riskScore(domain.getRiskScore())
                .isVerified(domain.getIsVerified())
                .kycCompleted(domain.getKycCompleted())
                .webhookUrl(domain.getWebhookUrl())
                .webhookSecret(domain.getWebhookSecret())
                .webhookEnabled(domain.getWebhookEnabled())
                .apiEnabled(domain.getApiEnabled())
                .country(domain.getCountry())
                .timezone(domain.getTimezone())
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedAt(domain.getUpdatedAt())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }
    
    public LedgerEntry toLedgerDomain(VendorLedgerEntryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return LedgerEntry.builder()
                .id(entity.getId())
                .vendorId(entity.getVendorId())
                .type(entity.getType())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .balanceAfter(entity.getBalanceAfter())
                .status(entity.getStatus())
                .orderId(entity.getOrderId())
                .paymentId(entity.getPaymentId())
                .relatedEntryId(entity.getRelatedEntryId())
                .description(entity.getDescription())
                .referenceNumber(entity.getReferenceNumber())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .reversedAt(entity.getReversedAt())
                .reversedBy(entity.getReversedBy())
                .build();
    }
    
    public VendorLedgerEntryJpaEntity toLedgerEntity(LedgerEntry domain) {
        if (domain == null) {
            return null;
        }
        
        return VendorLedgerEntryJpaEntity.builder()
                .id(domain.getId())
                .vendorId(domain.getVendorId())
                .type(domain.getType())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .balanceAfter(domain.getBalanceAfter())
                .status(domain.getStatus())
                .orderId(domain.getOrderId())
                .paymentId(domain.getPaymentId())
                .relatedEntryId(domain.getRelatedEntryId())
                .description(domain.getDescription())
                .referenceNumber(domain.getReferenceNumber())
                .metadata(domain.getMetadata())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .reversedAt(domain.getReversedAt())
                .reversedBy(domain.getReversedBy())
                .build();
    }
}

