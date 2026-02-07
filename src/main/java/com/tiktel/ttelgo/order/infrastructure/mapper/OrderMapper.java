package com.tiktel.ttelgo.order.infrastructure.mapper;

import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
    
    public Order toDomain(OrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return Order.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .userId(entity.getUserId())
                .vendorId(entity.getVendorId())
                .customerEmail(entity.getCustomerEmail())
                .bundleCode(entity.getBundleCode())
                .bundleName(entity.getBundleName())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .esimgoOrderId(entity.getEsimgoOrderId())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .countryIso(entity.getCountryIso())
                .dataAmount(entity.getDataAmount())
                .validityDays(entity.getValidityDays())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .paidAt(entity.getPaidAt())
                .provisionedAt(entity.getProvisionedAt())
                .completedAt(entity.getCompletedAt())
                .failedAt(entity.getFailedAt())
                .canceledAt(entity.getCanceledAt())
                .updatedAt(entity.getUpdatedAt())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .retryCount(entity.getRetryCount())
                .lastRetryAt(entity.getLastRetryAt())
                .build();
    }
    
    public OrderJpaEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }
        
        return OrderJpaEntity.builder()
                .id(domain.getId())
                .orderNumber(domain.getOrderNumber())
                .userId(domain.getUserId())
                .vendorId(domain.getVendorId())
                .customerEmail(domain.getCustomerEmail())
                .bundleCode(domain.getBundleCode())
                .bundleName(domain.getBundleName())
                .quantity(domain.getQuantity())
                .unitPrice(domain.getUnitPrice())
                .totalAmount(domain.getTotalAmount())
                .currency(domain.getCurrency())
                .esimgoOrderId(domain.getEsimgoOrderId())
                .status(domain.getStatus())
                .paymentStatus(domain.getPaymentStatus())
                .countryIso(domain.getCountryIso())
                .dataAmount(domain.getDataAmount())
                .validityDays(domain.getValidityDays())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .createdAt(domain.getCreatedAt())
                .paidAt(domain.getPaidAt())
                .provisionedAt(domain.getProvisionedAt())
                .completedAt(domain.getCompletedAt())
                .failedAt(domain.getFailedAt())
                .canceledAt(domain.getCanceledAt())
                .updatedAt(domain.getUpdatedAt())
                .errorCode(domain.getErrorCode())
                .errorMessage(domain.getErrorMessage())
                .retryCount(domain.getRetryCount())
                .lastRetryAt(domain.getLastRetryAt())
                .build();
    }
}

