package com.tiktel.ttelgo.auth.infrastructure.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for verifying Apple Sign-In identity tokens.
 * Apple uses JWT tokens signed with RSA keys that are fetched from Apple's public key endpoint.
 * 
 * This service implements production-grade security practices:
 * - Caches Apple public keys to reduce API calls
 * - Validates JWT signature using Apple's public keys (JWKS)
 * - Validates issuer (https://appleid.apple.com)
 * - Validates audience (bundle id/client id)
 * - Validates token expiration
 */
@Service
@Slf4j
public class AppleOAuthService {
    
    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final long CACHE_TTL_SECONDS = 3600; // Cache keys for 1 hour
    
    private final String clientId;
    private final RestTemplate restTemplate;
    
    // In-memory cache for Apple public keys with TTL
    private JWKSet cachedJwkSet;
    private Instant cacheExpiry;
    private final Object cacheLock = new Object();
    
    public AppleOAuthService(@Value("${apple.oauth.client-id:}") String clientId) {
        this.clientId = clientId;
        this.restTemplate = new RestTemplate();
        log.info("AppleOAuthService initialized with clientId: {}", clientId != null && !clientId.isEmpty() ? "configured" : "not configured");
    }
    
    /**
     * Verify Apple identity token with production-grade security validation.
     * 
     * Validates:
     * - JWT signature using Apple public keys (JWKS)
     * - Issuer (https://appleid.apple.com)
     * - Audience (bundle id/client id)
     * - Token expiration
     * 
     * @param identityToken Apple identity token string (JWT)
     * @return Optional containing JWTClaimsSet if valid, empty otherwise
     * @throws BusinessException with specific error codes for different failure scenarios
     */
    public Optional<JWTClaimsSet> verifyIdentityToken(String identityToken) {
        if (identityToken == null || identityToken.trim().isEmpty()) {
            log.warn("Apple identity token is null or empty");
            return Optional.empty();
        }
        
        try {
            // Parse the JWT token
            SignedJWT signedJWT = SignedJWT.parse(identityToken);
            
            // Get the key ID from the token header
            JWSHeader header = signedJWT.getHeader();
            String keyId = header.getKeyID();
            
            if (keyId == null) {
                log.warn("Apple identity token missing key ID in header");
                return Optional.empty();
            }
            
            // Fetch Apple's public keys (with caching)
            JWKSet jwkSet = getApplePublicKeys();
            if (jwkSet == null) {
                log.error("Failed to fetch Apple public keys from JWKS endpoint");
                return Optional.empty();
            }
            
            // Find the matching key by key ID
            JWK jwk = jwkSet.getKeyByKeyId(keyId);
            if (jwk == null) {
                log.warn("Apple public key not found for key ID: {}", keyId);
                return Optional.empty();
            }
            
            // Convert to RSA public key
            RSAKey rsaKey = jwk.toRSAKey();
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            
            // Verify the token signature using RSA public key
            RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                log.warn("Apple identity token signature verification failed for key ID: {}", keyId);
                return Optional.empty();
            }
            
            // Get claims
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            
            // Validate issuer (must be https://appleid.apple.com)
            String issuer = claims.getIssuer();
            if (issuer == null || !issuer.equals(APPLE_ISSUER)) {
                log.warn("Apple identity token issuer mismatch. Expected: {}, Got: {}", APPLE_ISSUER, issuer);
                return Optional.empty();
            }
            
            // Validate audience (must match client ID/bundle ID)
            if (clientId == null || clientId.isEmpty()) {
                log.error("Apple OAuth client ID not configured");
                return Optional.empty();
            }
            
            List<String> audiences = claims.getAudience();
            if (audiences == null || !audiences.contains(clientId)) {
                log.warn("Apple identity token audience mismatch. Expected: {}, Got: {}", clientId, audiences);
                return Optional.empty();
            }
            
            // Validate expiration
            if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new java.util.Date())) {
                log.warn("Apple identity token has expired. Expiration: {}", claims.getExpirationTime());
                return Optional.empty();
            }
            
            log.debug("Apple identity token verified successfully. Subject: {}, Issuer: {}", 
                    claims.getSubject(), claims.getIssuer());
            return Optional.of(claims);
            
        } catch (ParseException e) {
            log.error("Error parsing Apple identity token: {}", e.getMessage());
            return Optional.empty();
        } catch (JOSEException e) {
            log.error("JOSE error verifying Apple identity token: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error verifying Apple identity token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Get Apple's public keys with caching.
     * Keys are cached for 1 hour to reduce API calls to Apple's JWKS endpoint.
     * 
     * @return JWKSet containing Apple's public keys, or null if fetch fails
     */
    @SuppressWarnings("unchecked")
    private JWKSet getApplePublicKeys() {
        // Check cache first
        synchronized (cacheLock) {
            if (cachedJwkSet != null && cacheExpiry != null && Instant.now().isBefore(cacheExpiry)) {
                log.debug("Using cached Apple public keys");
                return cachedJwkSet;
            }
        }
        
        // Cache miss or expired - fetch from Apple
        log.info("Fetching Apple public keys from JWKS endpoint");
        try {
            Map<String, Object> response = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, Map.class);
            if (response == null || !response.containsKey("keys")) {
                log.error("Invalid response from Apple public keys endpoint: response is null or missing 'keys'");
                return null;
            }
            
            List<Map<String, Object>> keys = (List<Map<String, Object>>) response.get("keys");
            if (keys == null || keys.isEmpty()) {
                log.error("No keys found in Apple public keys response");
                return null;
            }
            
            JWKSet jwkSet = JWKSet.parse(response);
            
            // Update cache
            synchronized (cacheLock) {
                cachedJwkSet = jwkSet;
                cacheExpiry = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
                log.info("Apple public keys cached successfully. Cache expires at: {}", cacheExpiry);
            }
            
            return jwkSet;
        } catch (RestClientException e) {
            log.error("Error fetching Apple public keys from endpoint: {}", e.getMessage());
            // Return cached keys if available (even if expired) as fallback
            synchronized (cacheLock) {
                if (cachedJwkSet != null) {
                    log.warn("Using expired cached Apple public keys due to fetch failure");
                    return cachedJwkSet;
                }
            }
            return null;
        } catch (ParseException e) {
            log.error("Error parsing Apple public keys response: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching Apple public keys: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extract email from Apple identity token.
     * 
     * @param claims Verified JWTClaimsSet
     * @return Email address or null
     */
    public String getEmail(JWTClaimsSet claims) {
        if (claims == null) {
            return null;
        }
        try {
            return claims.getStringClaim("email");
        } catch (ParseException e) {
            log.warn("Error extracting email from Apple token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract subject (user ID) from Apple identity token.
     * This is the providerId for Apple users.
     * 
     * @param claims Verified JWTClaimsSet
     * @return Subject (user ID) or null
     */
    public String getSubject(JWTClaimsSet claims) {
        if (claims == null) {
            return null;
        }
        return claims.getSubject();
    }
    
    /**
     * Extract name from Apple identity token (only available on first login).
     * 
     * @param claims Verified JWTClaimsSet
     * @return Name object with firstName and lastName, or null
     */
    @SuppressWarnings("unchecked")
    public Name getName(JWTClaimsSet claims) {
        if (claims == null) {
            return null;
        }
        try {
            Object nameClaim = claims.getClaim("name");
            if (nameClaim == null) {
                return null;
            }
            if (nameClaim instanceof Map) {
                Map<String, Object> nameMap = (Map<String, Object>) nameClaim;
                String firstName = nameMap.get("firstName") != null ? nameMap.get("firstName").toString() : null;
                String lastName = nameMap.get("lastName") != null ? nameMap.get("lastName").toString() : null;
                return new Name(firstName, lastName);
            }
            return null;
        } catch (Exception e) {
            log.warn("Error extracting name from Apple token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Data class for Apple user name.
     */
    public static class Name {
        private final String firstName;
        private final String lastName;
        
        public Name(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public String getFullName() {
            if (firstName == null && lastName == null) {
                return null;
            }
            if (firstName == null) {
                return lastName;
            }
            if (lastName == null) {
                return firstName;
            }
            return firstName + " " + lastName;
        }
    }
}

