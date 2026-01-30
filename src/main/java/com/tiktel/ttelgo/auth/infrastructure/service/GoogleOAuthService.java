package com.tiktel.ttelgo.auth.infrastructure.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * Service for verifying Google OAuth ID tokens.
 */
@Service
@Slf4j
public class GoogleOAuthService {
    
    private final GoogleIdTokenVerifier verifier;
    
    public GoogleOAuthService(@Value("${google.oauth.client-id:}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
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
}

