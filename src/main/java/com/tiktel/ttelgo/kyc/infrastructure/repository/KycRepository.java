package com.tiktel.ttelgo.kyc.infrastructure.repository;

import com.tiktel.ttelgo.kyc.domain.Kyc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycRepository extends JpaRepository<Kyc, Long> {
    Optional<Kyc> findByUserId(Long userId);
    List<Kyc> findByStatus(com.tiktel.ttelgo.kyc.domain.KycStatus status);
    boolean existsByUserId(Long userId);
}

