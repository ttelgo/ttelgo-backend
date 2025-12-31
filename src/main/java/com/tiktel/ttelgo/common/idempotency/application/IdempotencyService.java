package com.tiktel.ttelgo.common.idempotency.application;

import com.tiktel.ttelgo.common.idempotency.domain.IdempotencyRecord;
import com.tiktel.ttelgo.common.idempotency.infrastructure.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for managing idempotency records.
 * 
 * Handles:
 * - Creating idempotency records for new requests
 * - Retrieving cached responses for retries
 * - Detecting conflicting payloads (different body with same key)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final IdempotencyRecordRepository repository;
    
    /**
     * Check if a request with this idempotency key was already processed.
     * Returns cached response if found (same payload), or empty if new/conflicting.
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyResult> getCachedResponse(
            String idempotencyKey,
            String httpMethod,
            String requestPath,
            Long actorId,
            String requestBody
    ) {
        Optional<IdempotencyRecord> recordOpt = repository.findByIdempotencyKeyAndHttpMethodAndRequestPathAndActorId(
                idempotencyKey, httpMethod, requestPath, actorId
        );
        
        if (recordOpt.isEmpty()) {
            return Optional.empty(); // New request
        }
        
        IdempotencyRecord record = recordOpt.get();
        
        // Check if expired
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.debug("Idempotency record expired: {}", idempotencyKey);
            repository.delete(record);
            return Optional.empty();
        }
        
        // Check if request body matches (prevent replay with different payload)
        String currentHash = hashRequestBody(requestBody);
        if (!record.getRequestHash().equals(currentHash)) {
            log.warn("Idempotency key conflict: same key with different payload. key={}, path={}", 
                    idempotencyKey, requestPath);
            return Optional.of(IdempotencyResult.conflict());
        }
        
        // If record is PENDING, another request is currently processing (concurrent request)
        if (record.getStatus() == IdempotencyRecord.IdempotencyStatus.PENDING) {
            log.debug("Idempotency record is PENDING (concurrent request): key={}", idempotencyKey);
            return Optional.of(IdempotencyResult.conflict());
        }
        
        // Return cached response (COMPLETED or FAILED)
        return Optional.of(IdempotencyResult.cached(
                record.getResponseStatus(),
                record.getResponseBody(),
                record.getStatus()
        ));
    }
    
    /**
     * Create a new idempotency record for an in-flight request.
     * Should be called before processing the request.
     * 
     * Returns empty if record already exists (concurrent request detected).
     */
    @Transactional
    public Optional<IdempotencyRecord> createPendingRecord(
            String idempotencyKey,
            String httpMethod,
            String requestPath,
            Long actorId,
            String requestBody,
            int ttlHours
    ) {
        // Double-check: verify record doesn't already exist (race condition protection)
        // This is already checked in getCachedResponse, but we check again here for extra safety
        
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .httpMethod(httpMethod)
                    .requestPath(requestPath)
                    .actorId(actorId)
                    .requestHash(hashRequestBody(requestBody))
                    .status(IdempotencyRecord.IdempotencyStatus.PENDING)
                    .responseStatus(0) // Placeholder, will be updated
                    .expiresAt(LocalDateTime.now().plusHours(ttlHours))
                    .build();
            
            return Optional.of(repository.save(record));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Unique constraint violation: concurrent request created record between check and save
            log.debug("Concurrent idempotency record creation detected: key={}", idempotencyKey);
            return Optional.empty();
        }
    }
    
    /**
     * Update idempotency record with response after processing.
     */
    @Transactional
    public void updateRecordWithResponse(
            Long recordId,
            int responseStatus,
            String responseBody
    ) {
        repository.findById(recordId).ifPresent(record -> {
            record.setResponseStatus(responseStatus);
            record.setResponseBody(responseBody);
            
            // Mark as completed (2xx) or failed (non-2xx)
            if (responseStatus >= 200 && responseStatus < 300) {
                record.setStatus(IdempotencyRecord.IdempotencyStatus.COMPLETED);
            } else {
                record.setStatus(IdempotencyRecord.IdempotencyStatus.FAILED);
            }
            
            repository.save(record);
        });
    }
    
    /**
     * Clean up expired idempotency records
     */
    @Transactional
    public int cleanupExpiredRecords() {
        return repository.deleteExpiredRecords(LocalDateTime.now());
    }
    
    /**
     * Compute SHA-256 hash of request body for conflict detection.
     */
    private String hashRequestBody(String requestBody) {
        if (requestBody == null || requestBody.isEmpty()) {
            return hashString("");
        }
        return hashString(requestBody);
    }
    
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen (SHA-256 is standard)
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Result of idempotency check.
     */
    public static class IdempotencyResult {
        private final boolean isCached;
        private final boolean isConflict;
        private final Integer responseStatus;
        private final String responseBody;
        private final IdempotencyRecord.IdempotencyStatus status;
        
        private IdempotencyResult(boolean isCached, boolean isConflict, Integer responseStatus, 
                                 String responseBody, IdempotencyRecord.IdempotencyStatus status) {
            this.isCached = isCached;
            this.isConflict = isConflict;
            this.responseStatus = responseStatus;
            this.responseBody = responseBody;
            this.status = status;
        }
        
        public static IdempotencyResult cached(int status, String body, IdempotencyRecord.IdempotencyStatus recordStatus) {
            return new IdempotencyResult(true, false, status, body, recordStatus);
        }
        
        public static IdempotencyResult conflict() {
            return new IdempotencyResult(false, true, null, null, null);
        }
        
        public static IdempotencyResult newRequest() {
            return new IdempotencyResult(false, false, null, null, null);
        }
        
        public boolean isCached() { return isCached; }
        public boolean isConflict() { return isConflict; }
        public Integer getResponseStatus() { return responseStatus; }
        public String getResponseBody() { return responseBody; }
        public IdempotencyRecord.IdempotencyStatus getStatus() { return status; }
    }
}

