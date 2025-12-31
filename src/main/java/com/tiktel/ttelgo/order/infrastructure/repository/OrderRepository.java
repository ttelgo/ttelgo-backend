package com.tiktel.ttelgo.order.infrastructure.repository;

import com.tiktel.ttelgo.common.domain.enums.OrderStatus;
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
public interface OrderRepository extends JpaRepository<OrderJpaEntity, Long> {
    
    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);
    
    Page<OrderJpaEntity> findByUserId(Long userId, Pageable pageable);
    
    Page<OrderJpaEntity> findByVendorId(Long vendorId, Pageable pageable);
    
    Page<OrderJpaEntity> findByStatus(OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM OrderJpaEntity o WHERE " +
           "o.status IN (:statuses) AND " +
           "o.createdAt < :beforeTime")
    List<OrderJpaEntity> findStaleOrders(@Param("statuses") List<OrderStatus> statuses,
                                         @Param("beforeTime") LocalDateTime beforeTime);
    
    @Query("SELECT o FROM OrderJpaEntity o WHERE " +
           "(:userId IS NULL OR o.userId = :userId) AND " +
           "(:vendorId IS NULL OR o.vendorId = :vendorId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR o.createdAt <= :endDate)")
    Page<OrderJpaEntity> findByFilters(@Param("userId") Long userId,
                                        @Param("vendorId") Long vendorId,
                                        @Param("status") OrderStatus status,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);
    
    @Query("SELECT COUNT(o) FROM OrderJpaEntity o WHERE " +
           "o.vendorId = :vendorId AND " +
           "o.createdAt >= :startDate AND " +
           "o.createdAt < :endDate")
    long countVendorOrdersInPeriod(@Param("vendorId") Long vendorId,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
    
    boolean existsByOrderNumber(String orderNumber);
}
