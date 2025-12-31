package com.tiktel.ttelgo.webhook.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEventJpaEntity, Long> {
    
    Optional<WebhookEventJpaEntity> findBySourceAndEventId(String source, String eventId);
    
    boolean existsBySourceAndEventId(String source, String eventId);
    
    @Query("SELECT w FROM WebhookEventJpaEntity w WHERE " +
           "w.processed = false AND " +
           "w.processingAttempts < :maxAttempts AND " +
           "(:minTime IS NULL OR w.lastProcessingAttemptAt < :minTime OR w.lastProcessingAttemptAt IS NULL)")
    List<WebhookEventJpaEntity> findUnprocessedEvents(
            @Param("maxAttempts") int maxAttempts,
            @Param("minTime") LocalDateTime minTime);
}

