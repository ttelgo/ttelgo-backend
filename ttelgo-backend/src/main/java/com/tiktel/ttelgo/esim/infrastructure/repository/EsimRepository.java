package com.tiktel.ttelgo.esim.infrastructure.repository;

import com.tiktel.ttelgo.esim.domain.Esim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsimRepository extends JpaRepository<Esim, Long> {
    Optional<Esim> findByEsimUuid(String esimUuid);
    Optional<Esim> findByMatchingId(String matchingId);
    Optional<Esim> findByIccid(String iccid);
    Optional<Esim> findByOrderId(Long orderId);
    List<Esim> findByUserId(Long userId);
    List<Esim> findByStatus(com.tiktel.ttelgo.esim.domain.EsimStatus status);
    org.springframework.data.domain.Page<Esim> findByStatus(com.tiktel.ttelgo.esim.domain.EsimStatus status, org.springframework.data.domain.Pageable pageable);
}

