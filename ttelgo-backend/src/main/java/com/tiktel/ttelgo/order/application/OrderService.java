package com.tiktel.ttelgo.order.application;

import com.tiktel.ttelgo.common.exception.ResourceNotFoundException;
import com.tiktel.ttelgo.order.api.dto.OrderResponse;
import com.tiktel.ttelgo.order.api.mapper.OrderApiMapper;
import com.tiktel.ttelgo.order.application.port.OrderRepositoryPort;
import com.tiktel.ttelgo.order.domain.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    
    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderApiMapper orderApiMapper;
    
    @Autowired
    public OrderService(OrderRepositoryPort orderRepositoryPort, OrderApiMapper orderApiMapper) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.orderApiMapper = orderApiMapper;
    }
    
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderApiMapper.toResponse(order);
    }
    
    public OrderResponse getOrderByReference(String orderReference) {
        Order order = orderRepositoryPort.findByOrderReference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderApiMapper.toResponse(order);
    }
    
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepositoryPort.findByUserId(userId);
        return orders.stream()
                .map(orderApiMapper::toResponse)
                .collect(Collectors.toList());
    }
}

