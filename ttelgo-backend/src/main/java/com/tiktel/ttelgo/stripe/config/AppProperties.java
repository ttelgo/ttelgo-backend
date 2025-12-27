package com.tiktel.ttelgo.stripe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /**
     * Base URL of the frontend used to construct success/cancel URLs.
     */
    private String frontendUrl = "http://localhost:5173";

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }
}

