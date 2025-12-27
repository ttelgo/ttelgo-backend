package com.tiktel.ttelgo.stripe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {
    /**
     * Secret key used server-side for all Stripe API calls.
     */
    private String secretKey;

    /**
     * Publishable key (not returned to clients here; frontend should load from env).
     */
    private String publishableKey;

    /**
     * Webhook signing secret (whsec_...).
     */
    private String webhookSecret;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}

