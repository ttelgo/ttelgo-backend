package com.tiktel.ttelgo.common.idempotency.infrastructure.repository;

import com.tiktel.ttelgo.common.idempotency.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    
    /**
     * Find existing idempotency record by composite key.
     * Used to check if request was already processed.
     */
    Optional<IdempotencyRecord> findByIdempotencyKeyAndHttpMethodAndRequestPathAndActorId(
            String idempotencyKey,
            String httpMethod,
            String requestPath,
            Long actorId
    );
    
    /**
     * Delete expired records (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM IdempotencyRecord ir WHERE ir.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") LocalDateTime now);
}

