package com.tiktel.ttelgo.payment.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentJpaEntity, Long> {
    // TODO: Add custom query methods
}

