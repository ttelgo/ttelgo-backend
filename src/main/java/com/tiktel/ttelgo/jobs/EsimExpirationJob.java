package com.tiktel.ttelgo.jobs;

import com.tiktel.ttelgo.esim.application.EsimService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job to mark expired eSIMs
 * Runs once per day at midnight
 */
@Slf4j
@Component
public class EsimExpirationJob {
    
    private final EsimService esimService;
    
    public EsimExpirationJob(EsimService esimService) {
        this.esimService = esimService;
    }
    
    /**
     * Mark eSIMs that have passed their validity date as expired
     */
    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    public void markExpiredEsims() {
        log.info("Starting eSIM expiration job");
        
        try {
            int expiredCount = esimService.markExpiredEsims();
            
            if (expiredCount > 0) {
                log.info("Marked {} eSIMs as expired", expiredCount);
            } else {
                log.debug("No eSIMs to expire");
            }
            
        } catch (Exception e) {
            log.error("Error during eSIM expiration job", e);
        }
    }
}

