package com.tiktel.ttelgo.integration.stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {
    
    @Value("${stripe.secret.key:}")
    private String secretKey;
    
    @Value("${stripe.publishable.key:}")
    private String publishableKey;
    
    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.trim().isEmpty()) {
            Stripe.apiKey = secretKey;
        }
    }
}
