package com.tiktel.ttelgo.kyc.application.port;

import com.tiktel.ttelgo.kyc.domain.Kyc;

import java.util.List;
import java.util.Optional;

public interface KycRepositoryPort {
    Kyc save(Kyc kyc);
    Optional<Kyc> findById(Long id);
    Optional<Kyc> findByUserId(Long userId);
    List<Kyc> findByStatus(com.tiktel.ttelgo.kyc.domain.KycStatus status);
    boolean existsByUserId(Long userId);
}

