package com.tiktel.ttelgo.payment.infrastructure.repository;

import com.tiktel.ttelgo.common.domain.enums.PaymentStatus;
import com.tiktel.ttelgo.common.domain.enums.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentJpaEntity, Long> {
    
    Optional<PaymentJpaEntity> findByPaymentNumber(String paymentNumber);
    
    Optional<PaymentJpaEntity> findByStripePaymentIntentId(String stripePaymentIntentId);
    
    Optional<PaymentJpaEntity> findByOrderId(Long orderId);
    
    Page<PaymentJpaEntity> findByUserId(Long userId, Pageable pageable);
    
    Page<PaymentJpaEntity> findByVendorId(Long vendorId, Pageable pageable);
    
    Page<PaymentJpaEntity> findByStatus(PaymentStatus status, Pageable pageable);
    
    Page<PaymentJpaEntity> findByType(PaymentType type, Pageable pageable);
}
