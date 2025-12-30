package com.tiktel.ttelgo.vendor.infrastructure.repository;

import com.tiktel.ttelgo.common.domain.enums.LedgerEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VendorLedgerRepository extends JpaRepository<VendorLedgerEntryJpaEntity, Long> {
    
    Page<VendorLedgerEntryJpaEntity> findByVendorIdOrderByCreatedAtDesc(Long vendorId, Pageable pageable);
    
    List<VendorLedgerEntryJpaEntity> findByVendorIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long vendorId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT SUM(CASE WHEN e.type = 'CREDIT' THEN e.amount ELSE -e.amount END) " +
           "FROM VendorLedgerEntryJpaEntity e " +
           "WHERE e.vendorId = :vendorId AND e.status = 'COMPLETED'")
    BigDecimal calculateBalance(@Param("vendorId") Long vendorId);
    
    @Query("SELECT e FROM VendorLedgerEntryJpaEntity e WHERE e.vendorId = :vendorId AND " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:startDate IS NULL OR e.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR e.createdAt <= :endDate) " +
           "ORDER BY e.createdAt DESC")
    Page<VendorLedgerEntryJpaEntity> findByFilters(@Param("vendorId") Long vendorId,
                                                     @Param("type") LedgerEntryType type,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     Pageable pageable);
}

