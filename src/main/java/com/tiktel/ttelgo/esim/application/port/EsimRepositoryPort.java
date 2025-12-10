package com.tiktel.ttelgo.esim.application.port;

import com.tiktel.ttelgo.esim.domain.Esim;

import java.util.List;
import java.util.Optional;

public interface EsimRepositoryPort {
    Esim save(Esim esim);
    Optional<Esim> findById(Long id);
    Optional<Esim> findByEsimUuid(String esimUuid);
    Optional<Esim> findByMatchingId(String matchingId);
    Optional<Esim> findByIccid(String iccid);
    Optional<Esim> findByOrderId(Long orderId);
    List<Esim> findByUserId(Long userId);
    List<Esim> findByStatus(com.tiktel.ttelgo.esim.domain.EsimStatus status);
}

