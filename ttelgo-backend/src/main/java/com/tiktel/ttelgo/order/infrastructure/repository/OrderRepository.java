package com.tiktel.ttelgo.order.infrastructure.repository;

import com.tiktel.ttelgo.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderReference(String orderReference);
    List<Order> findByUserId(Long userId);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByStatus(com.tiktel.ttelgo.order.domain.OrderStatus status);
    org.springframework.data.domain.Page<Order> findByStatus(com.tiktel.ttelgo.order.domain.OrderStatus status, org.springframework.data.domain.Pageable pageable);
    List<Order> findByPaymentStatus(com.tiktel.ttelgo.order.domain.PaymentStatus paymentStatus);
}

