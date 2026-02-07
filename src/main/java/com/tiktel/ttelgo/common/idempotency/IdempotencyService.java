package com.tiktel.ttelgo.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for handling idempotency of critical operations
 */
@Slf4j
@Service
public class IdempotencyService {
    
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${app.idempotency.ttl-hours:24}")
    private int ttlHours;
    
    public IdempotencyService(IdempotencyRepository idempotencyRepository,
                             ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Check if an idempotency key has been used before
     * If it has, return the previous response
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyRecord> checkIdempotency(String idempotencyKey, Long userId, 
                                                         Long vendorId, String endpoint, 
                                                         String httpMethod, Object requestBody) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String requestHash = hashRequest(requestBody);
        LocalDateTime now = LocalDateTime.now();
        
        Optional<IdempotencyRecordJpaEntity> existing = idempotencyRepository.findValidRecord(
                idempotencyKey, userId, vendorId, now);
        
        if (existing.isPresent()) {
            IdempotencyRecordJpaEntity record = existing.get();
            
            // Verify request hash matches (same idempotency key but different request = error)
            if (!record.getRequestHash().equals(requestHash)) {
                log.error("Idempotency key reused with different request body: key={}", idempotencyKey);
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_MISMATCH,
                        "Idempotency key has been used with a different request");
            }
            
            log.info("Idempotency check: Found existing record for key={}", idempotencyKey);
            return Optional.of(toIdempotencyRecord(record));
        }
        
        return Optional.empty();
    }
    
    /**
     * Store an idempotency record for a successful operation
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void storeIdempotencyRecord(String idempotencyKey, Long userId, Long vendorId,
                                      String endpoint, String httpMethod, Object requestBody,
                                      int statusCode, String responseBody,
                                      String resourceType, Long resourceId) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return;
        }
        
        String requestHash = hashRequest(requestBody);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(ttlHours);
        
        IdempotencyRecordJpaEntity record = IdempotencyRecordJpaEntity.builder()
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .vendorId(vendorId)
                .endpoint(endpoint)
                .httpMethod(httpMethod)
                .requestHash(requestHash)
                .responseStatusCode(statusCode)
                .responseBody(responseBody)
                .createdResourceType(resourceType)
                .createdResourceId(resourceId)
                .expiresAt(expiresAt)
                .build();
        
        idempotencyRepository.save(record);
        log.info("Stored idempotency record: key={}, resourceType={}, resourceId={}", 
                idempotencyKey, resourceType, resourceId);
    }
    
    /**
     * Clean up expired idempotency records
     */
    @Transactional
    public int cleanupExpiredRecords() {
        int deleted = idempotencyRepository.deleteExpiredRecords(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency records", deleted);
        }
        return deleted;
    }
    
    /**
     * Hash request body for comparison
     */
    private String hashRequest(Object requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Error hashing request", e);
            return "";
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private IdempotencyRecord toIdempotencyRecord(IdempotencyRecordJpaEntity entity) {
        return new IdempotencyRecord(
                entity.getResponseStatusCode(),
                entity.getResponseBody(),
                entity.getCreatedResourceType(),
                entity.getCreatedResourceId()
        );
    }
    
    /**
     * DTO for idempotency record
     */
    public record IdempotencyRecord(
            int statusCode,
            String responseBody,
            String resourceType,
            Long resourceId
    ) {}
}

