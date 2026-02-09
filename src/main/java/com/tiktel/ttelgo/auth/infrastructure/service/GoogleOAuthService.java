package com.tiktel.ttelgo.auth.infrastructure.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Service for verifying Google OAuth ID tokens.
 */
@Service
@Slf4j
public class GoogleOAuthService {
    
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    
    private final GoogleIdTokenVerifier verifier;
    private final RestTemplate restTemplate;
    
    public GoogleOAuthService(@Value("${google.oauth.client-id:}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Verify and parse Google ID token.
     * 
     * @param idToken Google ID token string
     * @return Optional containing GoogleIdToken if valid, empty otherwise
     */
    public Optional<GoogleIdToken> verifyIdToken(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token != null) {
                return Optional.of(token);
            }
            log.warn("Google ID token verification failed: token is null");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error verifying Google ID token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Extract email from Google ID token.
     * 
     * @param token Verified GoogleIdToken
     * @return Email address or null
     */
    public String getEmail(GoogleIdToken token) {
        if (token == null || token.getPayload() == null) {
            return null;
        }
        return token.getPayload().getEmail();
    }
    
    /**
     * Extract name from Google ID token.
     * 
     * @param token Verified GoogleIdToken
     * @return Name or null
     */
    public String getName(GoogleIdToken token) {
        if (token == null || token.getPayload() == null) {
            return null;
        }
        return (String) token.getPayload().get("name");
    }
    
    /**
     * Extract picture URL from Google ID token.
     * 
     * @param token Verified GoogleIdToken
     * @return Picture URL or null
     */
    public String getPictureUrl(GoogleIdToken token) {
        if (token == null || token.getPayload() == null) {
            return null;
        }
        return (String) token.getPayload().get("picture");
    }
    
    /**
     * Extract given name (first name) from Google ID token.
     * 
     * @param token Verified GoogleIdToken
     * @return Given name or null
     */
    public String getGivenName(GoogleIdToken token) {
        if (token == null || token.getPayload() == null) {
            return null;
        }
        return (String) token.getPayload().get("given_name");
    }
    
    /**
     * Extract family name (last name) from Google ID token.
     * 
     * @param token Verified GoogleIdToken
     * @return Family name or null
     */
    public String getFamilyName(GoogleIdToken token) {
        if (token == null || token.getPayload() == null) {
            return null;
        }
        return (String) token.getPayload().get("family_name");
    }
    
    /**
     * Extract subject (user ID) from Google ID token.
     * This is the providerId for Google users.
     * 
     * @param token Verified GoogleIdToken
     * @return Subject (user ID) or null
     */
    public String getSubject(GoogleIdToken token) {
        if (token == null || token.getPayload() == null) {
            return null;
        }
        return token.getPayload().getSubject();
    }
    
    /**
     * Extract email verification status from Google ID token.
     * 
     * @param token Verified GoogleIdToken
     * @return true if email is verified, false otherwise
     */
    public Boolean getEmailVerified(GoogleIdToken token) {
        if (token == null || token.getPayload() == null) {
            return false;
        }
        Object emailVerified = token.getPayload().get("email_verified");
        if (emailVerified instanceof Boolean) {
            return (Boolean) emailVerified;
        }
        // Default to false if not present or not a boolean
        return false;
    }
    
    /**
     * Verify Google ID token using tokeninfo endpoint.
     * This method uses the Google tokeninfo API endpoint as specified in requirements.
     * 
     * @param idToken Google ID token string
     * @return Optional containing token info map if valid, empty otherwise
     */
    public Optional<Map<String, Object>> verifyIdTokenViaTokenInfo(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            log.warn("Google ID token is null or empty");
            return Optional.empty();
        }
        
        try {
            String url = GOOGLE_TOKENINFO_URL + idToken.trim();
            log.debug("Verifying Google token via tokeninfo endpoint");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenInfo = restTemplate.getForObject(url, Map.class);
            
            if (tokenInfo == null || tokenInfo.isEmpty()) {
                log.warn("Invalid response from Google tokeninfo endpoint");
                return Optional.empty();
            }
            
            // Check for error in response
            if (tokenInfo.containsKey("error")) {
                log.warn("Google tokeninfo returned error: {}", tokenInfo.get("error"));
                return Optional.empty();
            }
            
            log.info("Google token verified successfully via tokeninfo endpoint");
            return Optional.of(tokenInfo);
            
        } catch (Exception e) {
            log.error("Error verifying Google ID token via tokeninfo: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Extract email from tokeninfo response.
     * 
     * @param tokenInfo Token info map from Google tokeninfo endpoint
     * @return Email address or null
     */
    public String getEmailFromTokenInfo(Map<String, Object> tokenInfo) {
        if (tokenInfo == null) {
            return null;
        }
        Object email = tokenInfo.get("email");
        return email != null ? email.toString() : null;
    }
    
    /**
     * Extract name from tokeninfo response.
     * 
     * @param tokenInfo Token info map from Google tokeninfo endpoint
     * @return Name or null
     */
    public String getNameFromTokenInfo(Map<String, Object> tokenInfo) {
        if (tokenInfo == null) {
            return null;
        }
        Object name = tokenInfo.get("name");
        return name != null ? name.toString() : null;
    }
    
    /**
     * Extract subject (user ID) from tokeninfo response.
     * 
     * @param tokenInfo Token info map from Google tokeninfo endpoint
     * @return Subject (user ID) or null
     */
    public String getSubjectFromTokenInfo(Map<String, Object> tokenInfo) {
        if (tokenInfo == null) {
            return null;
        }
        Object sub = tokenInfo.get("sub");
        return sub != null ? sub.toString() : null;
    }
    
    /**
     * Extract email verification status from tokeninfo response.
     * 
     * @param tokenInfo Token info map from Google tokeninfo endpoint
     * @return true if email is verified, false otherwise
     */
    public Boolean getEmailVerifiedFromTokenInfo(Map<String, Object> tokenInfo) {
        if (tokenInfo == null) {
            return false;
        }
        Object emailVerified = tokenInfo.get("email_verified");
        if (emailVerified instanceof Boolean) {
            return (Boolean) emailVerified;
        }
        if (emailVerified instanceof String) {
            return Boolean.parseBoolean((String) emailVerified);
        }
        return false;
    }
}

