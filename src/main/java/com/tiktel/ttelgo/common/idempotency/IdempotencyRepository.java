package com.tiktel.ttelgo.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecordJpaEntity, Long> {
    
    @Query("SELECT i FROM IdempotencyRecordJpaEntity i WHERE " +
           "i.idempotencyKey = :key AND " +
           "(:userId IS NULL OR i.userId = :userId) AND " +
           "(:vendorId IS NULL OR i.vendorId = :vendorId) AND " +
           "i.expiresAt > :now")
    Optional<IdempotencyRecordJpaEntity> findValidRecord(
            @Param("key") String idempotencyKey,
            @Param("userId") Long userId,
            @Param("vendorId") Long vendorId,
            @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM IdempotencyRecordJpaEntity i WHERE i.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") LocalDateTime now);
}

