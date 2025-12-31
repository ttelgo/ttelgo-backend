package com.tiktel.ttelgo.jobs;

import com.tiktel.ttelgo.common.idempotency.IdempotencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job to clean up expired idempotency records
 * Runs once per hour
 */
@Slf4j
@Component
public class IdempotencyCleanupJob {
    
    private final IdempotencyService idempotencyService;
    
    public IdempotencyCleanupJob(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }
    
    /**
     * Clean up expired idempotency records
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void cleanupExpiredRecords() {
        log.info("Starting idempotency cleanup job");
        
        try {
            int deleted = idempotencyService.cleanupExpiredRecords();
            
            if (deleted > 0) {
                log.info("Cleaned up {} expired idempotency records", deleted);
            } else {
                log.debug("No expired idempotency records to clean up");
            }
            
        } catch (Exception e) {
            log.error("Error during idempotency cleanup job", e);
        }
    }
}

