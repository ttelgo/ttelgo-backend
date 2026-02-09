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
 * Service for verifying Google ID tokens using Google's tokeninfo endpoint.
 * Uses: https://oauth2.googleapis.com/tokeninfo?id_token={idToken}
 */
@Service
@Slf4j
public class GoogleTokenInfoService {
    
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private final String clientId;
    private final RestTemplate restTemplate;
    
    public GoogleTokenInfoService(@Value("${google.oauth.client-id:}") String clientId) {
        this.clientId = clientId;
        this.restTemplate = new RestTemplate();
        log.info("GoogleTokenInfoService initialized with clientId: {}", 
                clientId != null && !clientId.isEmpty() ? "configured" : "not configured");
    }
    
    /**
     * Verify Google ID token using tokeninfo endpoint.
     * 
     * @param idToken Google ID token string
     * @return Optional containing TokenInfoResponse if valid, empty otherwise
     */
    public Optional<TokenInfoResponse> verifyIdToken(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            log.warn("Google ID token is null or empty");
            return Optional.empty();
        }
        
        try {
            String url = String.format("%s?id_token=%s", GOOGLE_TOKENINFO_URL, idToken.trim());
            log.debug("Verifying Google token with tokeninfo endpoint");
            
            TokenInfoResponse response = restTemplate.getForObject(url, TokenInfoResponse.class);
            
            if (response == null) {
                log.warn("Invalid response from Google tokeninfo endpoint");
                return Optional.empty();
            }
            
            // Validate audience (client ID) if configured
            if (clientId != null && !clientId.isEmpty() && response.getAudience() != null) {
                if (!clientId.equals(response.getAudience())) {
                    log.warn("Google token audience mismatch. Expected: {}, Got: {}", 
                            clientId, response.getAudience());
                    return Optional.empty();
                }
            }
            
            // Validate issuer
            if (response.getIssuer() == null || 
                (!response.getIssuer().equals("https://accounts.google.com") && 
                 !response.getIssuer().equals("accounts.google.com"))) {
                log.warn("Invalid Google token issuer: {}", response.getIssuer());
                return Optional.empty();
            }
            
            log.info("Google token verified successfully. Email: {}, Subject: {}", 
                    response.getEmail() != null ? "provided" : "not provided", 
                    response.getSubject());
            return Optional.of(response);
            
        } catch (RestClientException e) {
            log.error("Error verifying Google ID token: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error verifying Google ID token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Data class for Google tokeninfo response.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenInfoResponse {
        @JsonProperty("sub")
        private String subject; // Google user ID (providerId)
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("email_verified")
        private Boolean emailVerified;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("given_name")
        private String givenName;
        
        @JsonProperty("family_name")
        private String familyName;
        
        @JsonProperty("picture")
        private String picture;
        
        @JsonProperty("aud")
        private String audience; // Client ID
        
        @JsonProperty("iss")
        private String issuer; // Should be "https://accounts.google.com" or "accounts.google.com"
        
        @JsonProperty("exp")
        private Long expirationTime;
        
        @JsonProperty("iat")
        private Long issuedAt;
    }
}

