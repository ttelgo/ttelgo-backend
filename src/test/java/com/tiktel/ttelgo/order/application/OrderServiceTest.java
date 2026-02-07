package com.tiktel.ttelgo.order.application;

import com.tiktel.ttelgo.common.domain.enums.OrderStatus;
import com.tiktel.ttelgo.integration.esimgo.EsimGoService;
import com.tiktel.ttelgo.integration.esimgo.domain.Bundle;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.infrastructure.mapper.OrderMapper;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderJpaEntity;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderRepository;
import com.tiktel.ttelgo.vendor.application.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderMapper orderMapper;
    
    @Mock
    private EsimGoService esimGoService;
    
    @Mock
    private VendorService vendorService;
    
    @InjectMocks
    private OrderService orderService;
    
    private Bundle testBundle;
    
    @BeforeEach
    void setUp() {
        testBundle = Bundle.builder()
                .code("BUNDLE_US_5GB_30D")
                .name("US 5GB 30 Days")
                .price(BigDecimal.valueOf(49.99))
                .currency("USD")
                .dataAmount("5GB")
                .validityDays(30)
                .countryCode("US")
                .available(true)
                .build();
    }
    
    @Test
    void testCreateB2COrder_Success() {
        // Arrange
        when(esimGoService.getBundleDetails("BUNDLE_US_5GB_30D")).thenReturn(testBundle);
        when(orderMapper.toEntity(any(Order.class))).thenReturn(new OrderJpaEntity());
        when(orderRepository.save(any(OrderJpaEntity.class))).thenAnswer(invocation -> {
            OrderJpaEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });
        when(orderMapper.toDomain(any(OrderJpaEntity.class))).thenAnswer(invocation -> {
            return Order.builder()
                    .id(1L)
                    .bundleCode("BUNDLE_US_5GB_30D")
                    .quantity(1)
                    .totalAmount(BigDecimal.valueOf(49.99))
                    .status(OrderStatus.ORDER_CREATED)
                    .build();
        });
        
        // Act
        Order result = orderService.createB2COrder(
                1L, "customer@example.com", "BUNDLE_US_5GB_30D", 1, "127.0.0.1", "TestAgent");
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(OrderStatus.ORDER_CREATED, result.getStatus());
        assertEquals(BigDecimal.valueOf(49.99), result.getTotalAmount());
        verify(orderRepository).save(any(OrderJpaEntity.class));
    }
    
    @Test
    void testCreateB2COrder_BundleNotAvailable_ThrowsException() {
        // Arrange
        Bundle unavailableBundle = Bundle.builder()
                .code("BUNDLE_US_5GB_30D")
                .available(false)
                .build();
        
        when(esimGoService.getBundleDetails("BUNDLE_US_5GB_30D")).thenReturn(unavailableBundle);
        
        // Act & Assert
        assertThrows(Exception.class, () -> {
            orderService.createB2COrder(
                    1L, "customer@example.com", "BUNDLE_US_5GB_30D", 1, "127.0.0.1", "TestAgent");
        });
        
        verify(orderRepository, never()).save(any());
    }
}

