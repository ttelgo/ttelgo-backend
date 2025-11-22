package com.tiktel.ttelgo.kyc.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KycRepository extends JpaRepository<KycJpaEntity, Long> {
    // TODO: Add custom query methods
}

