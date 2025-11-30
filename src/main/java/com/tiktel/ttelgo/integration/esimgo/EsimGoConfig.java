package com.tiktel.ttelgo.integration.esimgo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EsimGoConfig {
    
    @Value("${esimgo.api.endpoint:https://api.esim-go.com/v2.4}")
    private String apiEndpoint;
    
    @Value("${esimgo.api.key:}")
    private String apiKey;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    public String getApiEndpoint() {
        return apiEndpoint;
    }
    
    public String getApiKey() {
        return apiKey;
    }
}
