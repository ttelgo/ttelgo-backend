package com.tiktel.ttelgo.auth.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpTokenJpaEntity, Long> {
    // TODO: Add custom query methods
}

