package com.tiktel.ttelgo.order.application.port;

import com.tiktel.ttelgo.order.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(Long id);
    Optional<Order> findByOrderReference(String orderReference);
    List<Order> findByUserId(Long userId);
}

