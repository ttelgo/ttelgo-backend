package com.tiktel.ttelgo.order.api.mapper;

import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderApiMapper {
    
    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }
        
        return OrderResponse.builder()
                .id(order.getId())
                .orderReference(order.getOrderReference())
                .userId(order.getUserId())
                .bundleId(order.getBundleId())
                .bundleName(order.getBundleName())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .esimId(order.getEsimId())
                .matchingId(order.getMatchingId())
                .iccid(order.getIccid())
                .smdpAddress(order.getSmdpAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

