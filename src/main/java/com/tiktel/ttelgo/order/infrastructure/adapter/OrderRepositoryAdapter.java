package com.tiktel.ttelgo.order.infrastructure.adapter;

import com.tiktel.ttelgo.order.application.port.OrderRepositoryPort;
import com.tiktel.ttelgo.order.domain.Order;
import com.tiktel.ttelgo.order.infrastructure.mapper.OrderMapper;
import com.tiktel.ttelgo.order.infrastructure.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {
    
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    
    @Autowired
    public OrderRepositoryAdapter(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }
    
    @Override
    public Order save(Order order) {
        var entity = orderMapper.toEntity(order);
        var savedEntity = orderRepository.save(entity);
        return orderMapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toDomain);
    }
    
    @Override
    public Optional<Order> findByOrderReference(String orderReference) {
        // orderReference is an alias for orderNumber
        return orderRepository.findByOrderNumber(orderReference)
                .map(orderMapper::toDomain);
    }
    
    @Override
    public List<Order> findByUserId(Long userId) {
        return orderRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(orderMapper::toDomain)
                .collect(Collectors.toList());
    }
}

