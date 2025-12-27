package com.tiktel.ttelgo.payment.infrastructure.repository;

import com.tiktel.ttelgo.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentIntentId(String paymentIntentId);
    Optional<Payment> findByStripeSessionId(String stripeSessionId);
    Optional<Payment> findByChargeId(String chargeId);
    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByStatus(com.tiktel.ttelgo.order.domain.PaymentStatus status);
}

