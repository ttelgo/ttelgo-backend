package com.tiktel.ttelgo.auth.infrastructure.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Service for verifying Facebook OAuth access tokens.
 * Verifies tokens using Facebook Graph API.
 */
@Service
@Slf4j
public class FacebookOAuthService {
    
    private static final String FACEBOOK_GRAPH_API_URL = "https://graph.facebook.com/me";
    private final String appId;
    private final String appSecret;
    private final RestTemplate restTemplate;
    
    public FacebookOAuthService(
            @Value("${facebook.oauth.app-id:}") String appId,
            @Value("${facebook.oauth.app-secret:}") String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.restTemplate = new RestTemplate();
        log.info("FacebookOAuthService initialized with appId: {}", appId != null && !appId.isEmpty() ? "configured" : "not configured");
    }
    
    /**
     * Verify Facebook access token and get user information.
     * 
     * @param accessToken Facebook access token
     * @return Optional containing FacebookUserInfo if token is valid, empty otherwise
     */
    public Optional<FacebookUserInfo> verifyAccessToken(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            log.warn("Facebook access token is null or empty");
            return Optional.empty();
        }
        
        try {
            // Verify token and get user info from Facebook Graph API
            String url = String.format("%s?fields=id,name,email&access_token=%s", 
                    FACEBOOK_GRAPH_API_URL, accessToken.trim());
            
            log.debug("Verifying Facebook token with Graph API");
            FacebookUserInfo userInfo = restTemplate.getForObject(url, FacebookUserInfo.class);
            
            if (userInfo == null || userInfo.getId() == null || userInfo.getId().isEmpty()) {
                log.warn("Invalid response from Facebook Graph API");
                return Optional.empty();
            }
            
            log.info("Facebook token verified successfully. User ID: {}, Email: {}", 
                    userInfo.getId(), userInfo.getEmail() != null ? "provided" : "not provided");
            return Optional.of(userInfo);
            
        } catch (RestClientException e) {
            log.error("Error verifying Facebook access token: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error verifying Facebook access token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Verify Facebook access token by checking it with Facebook's debug endpoint.
     * This provides additional validation.
     * 
     * @param accessToken Facebook access token
     * @return true if token is valid, false otherwise
     */
    public boolean isValidToken(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Use Facebook debug endpoint to verify token
            String debugUrl = String.format("https://graph.facebook.com/debug_token?input_token=%s&access_token=%s",
                    accessToken.trim(), accessToken.trim());
            
            FacebookDebugResponse response = restTemplate.getForObject(debugUrl, FacebookDebugResponse.class);
            
            if (response != null && response.getData() != null) {
                boolean isValid = response.getData().isValid();
                if (isValid && appId != null && !appId.isEmpty()) {
                    // Verify app ID matches
                    return appId.equals(response.getData().getAppId());
                }
                return isValid;
            }
            
            return false;
        } catch (Exception e) {
            log.warn("Error validating Facebook token via debug endpoint: {}", e.getMessage());
            // Fallback to basic verification via user info endpoint
            return verifyAccessToken(accessToken).isPresent();
        }
    }
    
    /**
     * Data class for Facebook user information from Graph API.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FacebookUserInfo {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("first_name")
        private String firstName;
        
        @JsonProperty("last_name")
        private String lastName;
        
        @JsonProperty("picture")
        private FacebookPicture picture;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FacebookPicture {
            @JsonProperty("data")
            private PictureData data;
            
            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class PictureData {
                @JsonProperty("url")
                private String url;
            }
        }
    }
    
    /**
     * Data class for Facebook debug token response.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FacebookDebugResponse {
        @JsonProperty("data")
        private DebugData data;
        
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class DebugData {
            @JsonProperty("app_id")
            private String appId;
            
            @JsonProperty("is_valid")
            private boolean isValid;
            
            @JsonProperty("user_id")
            private String userId;
        }
    }
}

