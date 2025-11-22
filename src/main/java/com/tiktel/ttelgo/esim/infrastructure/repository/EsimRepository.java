package com.tiktel.ttelgo.esim.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EsimRepository extends JpaRepository<EsimJpaEntity, Long> {
    // TODO: Add custom query methods
}

