package com.tiktel.ttelgo.vendor.infrastructure.repository;

import com.tiktel.ttelgo.common.domain.enums.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<VendorJpaEntity, Long> {
    
    Optional<VendorJpaEntity> findByEmail(String email);
    
    Page<VendorJpaEntity> findByStatus(VendorStatus status, Pageable pageable);
    
    @Query("SELECT v FROM VendorJpaEntity v WHERE " +
           "(:status IS NULL OR v.status = :status) AND " +
           "(:search IS NULL OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.companyName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(v.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<VendorJpaEntity> findByFilters(@Param("status") VendorStatus status,
                                         @Param("search") String search,
                                         Pageable pageable);
    
    boolean existsByEmail(String email);
}

