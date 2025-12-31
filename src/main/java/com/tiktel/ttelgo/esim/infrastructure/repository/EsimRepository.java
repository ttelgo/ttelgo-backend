package com.tiktel.ttelgo.esim.infrastructure.repository;

import com.tiktel.ttelgo.common.domain.enums.EsimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EsimRepository extends JpaRepository<EsimJpaEntity, Long> {
    
    Optional<EsimJpaEntity> findByIccid(String iccid);
    
    Optional<EsimJpaEntity> findByMatchingId(String matchingId);
    
    List<EsimJpaEntity> findByOrderId(Long orderId);
    
    Page<EsimJpaEntity> findByUserId(Long userId, Pageable pageable);
    
    Page<EsimJpaEntity> findByVendorId(Long vendorId, Pageable pageable);
    
    Page<EsimJpaEntity> findByStatus(EsimStatus status, Pageable pageable);
    
    @Query("SELECT e FROM EsimJpaEntity e WHERE " +
           "e.validUntil < :now AND e.status = :status")
    List<EsimJpaEntity> findExpiredEsims(@Param("now") LocalDateTime now, @Param("status") EsimStatus status);
    
    boolean existsByIccid(String iccid);
}
