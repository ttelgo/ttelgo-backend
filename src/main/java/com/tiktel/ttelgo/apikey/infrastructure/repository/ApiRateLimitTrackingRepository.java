package com.tiktel.ttelgo.apikey.infrastructure.repository;

import com.tiktel.ttelgo.apikey.domain.ApiRateLimitTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ApiRateLimitTrackingRepository extends JpaRepository<ApiRateLimitTracking, Long> {
    
    Optional<ApiRateLimitTracking> findByApiKeyIdAndWindowTypeAndWindowStart(
        Long apiKeyId, 
        String windowType, 
        LocalDateTime windowStart
    );
    
    @Query("DELETE FROM ApiRateLimitTracking art WHERE art.windowStart < :cutoff")
    void deleteOldRecords(@Param("cutoff") LocalDateTime cutoff);
}

