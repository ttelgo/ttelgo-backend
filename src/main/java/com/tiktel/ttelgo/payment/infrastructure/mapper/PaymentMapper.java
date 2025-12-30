package com.tiktel.ttelgo.payment.infrastructure.mapper;

import com.tiktel.ttelgo.payment.domain.Payment;
import com.tiktel.ttelgo.payment.infrastructure.repository.PaymentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    
    public Payment toDomain(PaymentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return Payment.builder()
                .id(entity.getId())
                .paymentNumber(entity.getPaymentNumber())
                .type(entity.getType())
                .orderId(entity.getOrderId())
                .vendorId(entity.getVendorId())
                .userId(entity.getUserId())
                .stripePaymentIntentId(entity.getStripePaymentIntentId())
                .stripeChargeId(entity.getStripeChargeId())
                .stripeRefundId(entity.getStripeRefundId())
                .amount(entity.getAmount())
                .refundedAmount(entity.getRefundedAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .customerEmail(entity.getCustomerEmail())
                .customerName(entity.getCustomerName())
                .paymentMethodType(entity.getPaymentMethodType())
                .paymentMethodLast4(entity.getPaymentMethodLast4())
                .paymentMethodBrand(entity.getPaymentMethodBrand())
                .metadata(entity.getMetadata())
                .idempotencyKey(entity.getIdempotencyKey())
                .createdAt(entity.getCreatedAt())
                .succeededAt(entity.getSucceededAt())
                .failedAt(entity.getFailedAt())
                .refundedAt(entity.getRefundedAt())
                .updatedAt(entity.getUpdatedAt())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
    
    public PaymentJpaEntity toEntity(Payment domain) {
        if (domain == null) {
            return null;
        }
        
        return PaymentJpaEntity.builder()
                .id(domain.getId())
                .paymentNumber(domain.getPaymentNumber())
                .type(domain.getType())
                .orderId(domain.getOrderId())
                .vendorId(domain.getVendorId())
                .userId(domain.getUserId())
                .stripePaymentIntentId(domain.getStripePaymentIntentId())
                .stripeChargeId(domain.getStripeChargeId())
                .stripeRefundId(domain.getStripeRefundId())
                .amount(domain.getAmount())
                .refundedAmount(domain.getRefundedAmount())
                .currency(domain.getCurrency())
                .status(domain.getStatus())
                .customerEmail(domain.getCustomerEmail())
                .customerName(domain.getCustomerName())
                .paymentMethodType(domain.getPaymentMethodType())
                .paymentMethodLast4(domain.getPaymentMethodLast4())
                .paymentMethodBrand(domain.getPaymentMethodBrand())
                .metadata(domain.getMetadata())
                .idempotencyKey(domain.getIdempotencyKey())
                .createdAt(domain.getCreatedAt())
                .succeededAt(domain.getSucceededAt())
                .failedAt(domain.getFailedAt())
                .refundedAt(domain.getRefundedAt())
                .updatedAt(domain.getUpdatedAt())
                .errorCode(domain.getErrorCode())
                .errorMessage(domain.getErrorMessage())
                .build();
    }
}

