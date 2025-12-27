package com.tiktel.ttelgo.payment.application.port;

import com.tiktel.ttelgo.payment.domain.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepositoryPort {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPaymentIntentId(String paymentIntentId);
    Optional<Payment> findByStripeSessionId(String stripeSessionId);
    Optional<Payment> findByChargeId(String chargeId);
    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByStatus(com.tiktel.ttelgo.order.domain.PaymentStatus status);
}

