package com.tiktel.ttelgo.order.api.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderApiMapper {
    
    private final ObjectMapper objectMapper;
    
    public OrderApiMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }
        
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .bundleCode(order.getBundleCode())
                .bundleName(order.getBundleName())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .countryIso(order.getCountryIso())
                .dataAmount(order.getDataAmount())
                .validityDays(order.getValidityDays())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .completedAt(order.getCompletedAt())
                .errorMessage(order.getErrorMessage())
                .esimgoOrderId(order.getEsimgoOrderId())
                .orderReference(order.getEsimgoOrderId()) // Alias for frontend compatibility
                .build();
    }
    
    public String serializeOrderResponse(OrderResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    public OrderResponse parseOrderResponse(String json) {
        try {
            return objectMapper.readValue(json, OrderResponse.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
