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
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for verifying Apple Sign-In identity tokens.
 * Apple uses JWT tokens signed with RSA keys that are fetched from Apple's public key endpoint.
 */
@Service
@Slf4j
public class AppleOAuthService {
    
    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private final String clientId;
    private final RestTemplate restTemplate;
    
    public AppleOAuthService(@Value("${apple.oauth.client-id:}") String clientId) {
        this.clientId = clientId;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Verify Apple identity token.
     * 
     * @param identityToken Apple identity token string
     * @return Optional containing JWTClaimsSet if valid, empty otherwise
     */
    public Optional<JWTClaimsSet> verifyIdentityToken(String identityToken) {
        try {
            // Parse the JWT token
            SignedJWT signedJWT = SignedJWT.parse(identityToken);
            
            // Get the key ID from the token header
            JWSHeader header = signedJWT.getHeader();
            String keyId = header.getKeyID();
            
            if (keyId == null) {
                log.warn("Apple identity token missing key ID");
                return Optional.empty();
            }
            
            // Fetch Apple's public keys
            JWKSet jwkSet = fetchApplePublicKeys();
            if (jwkSet == null) {
                log.error("Failed to fetch Apple public keys");
                return Optional.empty();
            }
            
            // Find the matching key
            JWK jwk = jwkSet.getKeyByKeyId(keyId);
            if (jwk == null) {
                log.warn("Apple public key not found for key ID: {}", keyId);
                return Optional.empty();
            }
            
            // Convert to RSA public key
            RSAKey rsaKey = jwk.toRSAKey();
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            
            // Verify the token signature
            RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                log.warn("Apple identity token signature verification failed");
                return Optional.empty();
            }
            
            // Get claims
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            
            // Verify audience (client ID)
            List<String> audiences = claims.getAudience();
            if (audiences == null || !audiences.contains(clientId)) {
                log.warn("Apple identity token audience mismatch. Expected: {}, Got: {}", clientId, audiences);
                return Optional.empty();
            }
            
            // Verify issuer
            String issuer = claims.getIssuer();
            if (issuer == null || !issuer.equals("https://appleid.apple.com")) {
                log.warn("Apple identity token issuer mismatch. Expected: https://appleid.apple.com, Got: {}", issuer);
                return Optional.empty();
            }
            
            // Verify expiration
            if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new java.util.Date())) {
                log.warn("Apple identity token has expired");
                return Optional.empty();
            }
            
            return Optional.of(claims);
            
        } catch (ParseException e) {
            log.error("Error parsing Apple identity token: {}", e.getMessage(), e);
            return Optional.empty();
        } catch (JOSEException e) {
            log.error("Error verifying Apple identity token: {}", e.getMessage(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error verifying Apple identity token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Fetch Apple's public keys from Apple's endpoint.
     * 
     * @return JWKSet containing Apple's public keys
     */
    @SuppressWarnings("unchecked")
    private JWKSet fetchApplePublicKeys() {
        try {
            Map<String, Object> response = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL, Map.class);
            if (response == null || !response.containsKey("keys")) {
                log.error("Invalid response from Apple public keys endpoint");
                return null;
            }
            
            List<Map<String, Object>> keys = (List<Map<String, Object>>) response.get("keys");
            if (keys == null || keys.isEmpty()) {
                log.error("No keys found in Apple public keys response");
                return null;
            }
            
            return JWKSet.parse(response);
        } catch (Exception e) {
            log.error("Error fetching Apple public keys: {}", e.getMessage(), e);
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

