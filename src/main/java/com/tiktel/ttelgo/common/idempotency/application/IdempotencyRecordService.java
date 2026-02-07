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
 * Service for managing idempotency records (filter layer).
 *
 * Handles:
 * - Creating idempotency records for new requests
 * - Retrieving cached responses for retries
 * - Detecting conflicting payloads (different body with same key)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyRecordService {

    private final IdempotencyRecordRepository repository;

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
            return Optional.empty();
        }

        IdempotencyRecord record = recordOpt.get();

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.debug("Idempotency record expired: {}", idempotencyKey);
            repository.delete(record);
            return Optional.empty();
        }

        String currentHash = hashRequestBody(requestBody);
        if (!record.getRequestHash().equals(currentHash)) {
            log.warn("Idempotency key conflict: same key with different payload. key={}, path={}",
                    idempotencyKey, requestPath);
            return Optional.of(IdempotencyResult.conflict());
        }

        if (record.getStatus() == IdempotencyRecord.IdempotencyStatus.PENDING) {
            log.debug("Idempotency record is PENDING (concurrent request): key={}", idempotencyKey);
            return Optional.of(IdempotencyResult.conflict());
        }

        return Optional.of(IdempotencyResult.cached(
                record.getResponseStatus(),
                record.getResponseBody(),
                record.getStatus()
        ));
    }

    @Transactional
    public Optional<IdempotencyRecord> createPendingRecord(
            String idempotencyKey,
            String httpMethod,
            String requestPath,
            Long actorId,
            String requestBody,
            int ttlHours
    ) {
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .httpMethod(httpMethod)
                    .requestPath(requestPath)
                    .actorId(actorId)
                    .requestHash(hashRequestBody(requestBody))
                    .status(IdempotencyRecord.IdempotencyStatus.PENDING)
                    .responseStatus(0)
                    .expiresAt(LocalDateTime.now().plusHours(ttlHours))
                    .build();

            return Optional.of(repository.save(record));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.debug("Concurrent idempotency record creation detected: key={}", idempotencyKey);
            return Optional.empty();
        }
    }

    @Transactional
    public void updateRecordWithResponse(
            Long recordId,
            int responseStatus,
            String responseBody
    ) {
        repository.findById(recordId).ifPresent(record -> {
            record.setResponseStatus(responseStatus);
            record.setResponseBody(responseBody);

            if (responseStatus >= 200 && responseStatus < 300) {
                record.setStatus(IdempotencyRecord.IdempotencyStatus.COMPLETED);
            } else {
                record.setStatus(IdempotencyRecord.IdempotencyStatus.FAILED);
            }

            repository.save(record);
        });
    }

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
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

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
