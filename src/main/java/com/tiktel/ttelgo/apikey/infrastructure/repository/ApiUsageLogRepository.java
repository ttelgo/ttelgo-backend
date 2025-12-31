package com.tiktel.ttelgo.apikey.infrastructure.repository;

import com.tiktel.ttelgo.apikey.domain.ApiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiUsageLogRepository extends JpaRepository<ApiUsageLog, Long> {
    
    List<ApiUsageLog> findByApiKeyId(Long apiKeyId);
    
    List<ApiUsageLog> findByApiKeyIdAndCreatedAtBetween(Long apiKeyId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(ul) FROM ApiUsageLog ul WHERE ul.apiKeyId = :apiKeyId AND ul.createdAt >= :start")
    Long countByApiKeyIdSince(@Param("apiKeyId") Long apiKeyId, @Param("start") LocalDateTime start);
    
    @Query("SELECT ul.endpoint, COUNT(ul) as count FROM ApiUsageLog ul WHERE ul.apiKeyId = :apiKeyId AND ul.createdAt >= :start GROUP BY ul.endpoint ORDER BY count DESC")
    List<Object[]> getTopEndpointsByApiKeyId(@Param("apiKeyId") Long apiKeyId, @Param("start") LocalDateTime start);
    
    @Query("SELECT ul.statusCode, COUNT(ul) as count FROM ApiUsageLog ul WHERE ul.apiKeyId = :apiKeyId AND ul.createdAt >= :start GROUP BY ul.statusCode")
    List<Object[]> getStatusCodesByApiKeyId(@Param("apiKeyId") Long apiKeyId, @Param("start") LocalDateTime start);
    
    @Query("SELECT AVG(ul.responseTimeMs) FROM ApiUsageLog ul WHERE ul.apiKeyId = :apiKeyId AND ul.createdAt >= :start")
    Double getAverageResponseTime(@Param("apiKeyId") Long apiKeyId, @Param("start") LocalDateTime start);
    
    @Query("SELECT DATE(ul.createdAt), COUNT(ul) FROM ApiUsageLog ul WHERE ul.apiKeyId = :apiKeyId AND ul.createdAt >= :start GROUP BY DATE(ul.createdAt) ORDER BY DATE(ul.createdAt)")
    List<Object[]> getDailyUsageStats(@Param("apiKeyId") Long apiKeyId, @Param("start") LocalDateTime start);
    
    @Query("SELECT COUNT(ul) FROM ApiUsageLog ul WHERE ul.createdAt >= :start")
    Long countTotalRequestsSince(@Param("start") LocalDateTime start);
    
    @Query("SELECT ul.apiKeyId, COUNT(ul) as count FROM ApiUsageLog ul WHERE ul.createdAt >= :start GROUP BY ul.apiKeyId ORDER BY count DESC")
    List<Object[]> getTopApiKeysByUsage(@Param("start") LocalDateTime start);
}

