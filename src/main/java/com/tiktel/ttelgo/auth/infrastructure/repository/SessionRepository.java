package com.tiktel.ttelgo.auth.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<SessionJpaEntity, Long> {
    // TODO: Add custom query methods
}

