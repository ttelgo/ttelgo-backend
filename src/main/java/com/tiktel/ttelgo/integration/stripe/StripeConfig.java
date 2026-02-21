package com.tiktel.ttelgo.integration.stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class StripeConfig {
    
    @Value("${stripe.secret.key}")
    private String secretKey;
    
    @Value("${stripe.publishable.key}")
    private String publishableKey;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    @PostConstruct
    public void init() {
        log.info("=== Stripe Configuration Initialization ===");
        
        // Trim all keys to remove any whitespace/newlines
        if (secretKey != null) {
            secretKey = secretKey.trim();
        }
        if (publishableKey != null) {
            publishableKey = publishableKey.trim();
        }
        if (webhookSecret != null) {
            webhookSecret = webhookSecret.trim();
        }
        
        if (secretKey == null || secretKey.isEmpty()) {
            log.error("=== CRITICAL ERROR: Stripe Secret Key is NULL or EMPTY ===");
            log.error("Please set STRIPE_SECRET_KEY environment variable");
            log.error("Current value: '{}'", secretKey);
            log.error("Example: $env:STRIPE_SECRET_KEY=\"sk_test_...\"");
        } else {
            log.info("Stripe Secret Key: {}...{} (length: {})", 
                secretKey.substring(0, Math.min(20, secretKey.length())),
                secretKey.length() > 20 ? secretKey.substring(secretKey.length() - 4) : "",
                secretKey.length());
            // Validate key format
            if (!secretKey.startsWith("sk_test_") && !secretKey.startsWith("sk_live_")) {
                log.error("=== WARNING: Stripe Secret Key format looks invalid ===");
                log.error("Expected format: sk_test_... or sk_live_...");
                log.error("Actual format: {}...", secretKey.substring(0, Math.min(10, secretKey.length())));
            }
        }
        
        if (publishableKey == null || publishableKey.isEmpty()) {
            log.warn("Stripe Publishable Key is not set");
        } else {
            log.info("Stripe Publishable Key: {}... (length: {})", 
                publishableKey.substring(0, Math.min(20, publishableKey.length())), 
                publishableKey.length());
        }
        
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Stripe Webhook Secret is not set");
        } else {
            log.info("Stripe Webhook Secret: {}... (length: {})", 
                webhookSecret.substring(0, Math.min(20, webhookSecret.length())), 
                webhookSecret.length());
        }
        
        Stripe.apiKey = secretKey;
        log.info("Stripe API key configured: {}", 
            (secretKey != null && !secretKey.isEmpty()) ? "SUCCESS" : "FAILED - EMPTY");
    }
}

