package com.tiktel.ttelgo.jobs;

import com.tiktel.ttelgo.webhook.infrastructure.repository.WebhookEventJpaEntity;
import com.tiktel.ttelgo.webhook.infrastructure.repository.WebhookEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job to retry failed webhook processing
 * Runs every 5 minutes
 */
@Slf4j
@Component
public class WebhookRetryJob {
    
    private final WebhookEventRepository webhookEventRepository;
    private static final int MAX_ATTEMPTS = 5;
    
    public WebhookRetryJob(WebhookEventRepository webhookEventRepository) {
        this.webhookEventRepository = webhookEventRepository;
    }
    
    /**
     * Retry processing failed webhook events
     */
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void retryFailedWebhooks() {
        log.info("Starting webhook retry job");
        
        try {
            // Find unprocessed events that haven't reached max attempts
            // and haven't been attempted in the last 5 minutes
            LocalDateTime minTime = LocalDateTime.now().minusMinutes(5);
            List<WebhookEventJpaEntity> failedEvents = 
                    webhookEventRepository.findUnprocessedEvents(MAX_ATTEMPTS, minTime);
            
            if (failedEvents.isEmpty()) {
                log.debug("No failed webhooks to retry");
                return;
            }
            
            log.info("Found {} failed webhooks to retry", failedEvents.size());
            
            // Note: Actual retry logic should call the appropriate webhook service
            // For now, just logging
            for (WebhookEventJpaEntity event : failedEvents) {
                log.info("Webhook needs retry: eventId={}, source={}, attempts={}",
                        event.getEventId(), event.getSource(), event.getProcessingAttempts());
                
                // TODO: Call appropriate webhook service to retry processing
                // if (event.getSource().equals("STRIPE")) {
                //     stripeWebhookService.retryProcessing(event);
                // }
            }
            
        } catch (Exception e) {
            log.error("Error during webhook retry job", e);
        }
    }
}

