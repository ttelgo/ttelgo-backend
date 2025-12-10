package com.tiktel.ttelgo.payment.infrastructure.adapter;

import com.tiktel.ttelgo.payment.application.port.PaymentRepositoryPort;
import com.tiktel.ttelgo.payment.domain.Payment;
import com.tiktel.ttelgo.payment.infrastructure.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {
    
    private final PaymentRepository paymentRepository;
    
    @Autowired
    public PaymentRepositoryAdapter(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    @Override
    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }
    
    @Override
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }
    
    @Override
    public Optional<Payment> findByPaymentIntentId(String paymentIntentId) {
        return paymentRepository.findByPaymentIntentId(paymentIntentId);
    }
    
    @Override
    public Optional<Payment> findByChargeId(String chargeId) {
        return paymentRepository.findByChargeId(chargeId);
    }
    
    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
    
    @Override
    public List<Payment> findByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }
    
    @Override
    public List<Payment> findByStatus(com.tiktel.ttelgo.order.domain.PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }
}

