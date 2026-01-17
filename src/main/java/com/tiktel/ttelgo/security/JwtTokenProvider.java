package com.tiktel.ttelgo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret:${JWT_SECRET:TtelGo2026SecureJWTSecretKeyForProductionUseMin256BitsRequiredForHS256Algorithm}}")
    private String secret;
    
    @Value("${jwt.expiration:2592000000}") // 30 days (1 month) default
    private Long expiration;
    
    @Value("${jwt.refresh-expiration:5184000000}") // 60 days (2 months) default
    private Long refreshExpiration;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        // Log secret length for debugging (don't log the actual secret)
        log.info("JWT Token Provider initialized. Secret length: {} characters", 
            secret != null ? secret.length() : 0);
        if (secret == null || secret.isEmpty() || secret.equals("your-secret-key-change-in-production-min-256-bits")) {
            log.warn("WARNING: JWT secret is using default value! This will cause token validation to fail!");
        }
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String generateToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);
        claims.put("type", "access");
        return createToken(claims, email, expiration);
    }
    
    public String generateRefreshToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);
        claims.put("type", "refresh");
        return createToken(claims, email, refreshExpiration);
    }
    
    /**
     * Legacy method for backward compatibility - uses USER role as default.
     * @deprecated Use generateToken(Long, String, String) instead.
     */
    @Deprecated
    public String generateToken(Long userId, String email) {
        return generateToken(userId, email, "USER");
    }
    
    /**
     * Legacy method for backward compatibility - uses USER role as default.
     * @deprecated Use generateRefreshToken(Long, String, String) instead.
     */
    @Deprecated
    public String generateRefreshToken(Long userId, String email) {
        return generateRefreshToken(userId, email, "USER");
    }
    
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        
        var builder = Jwts.builder();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        builder.subject(subject);
        builder.issuedAt(now);
        builder.expiration(expiryDate);
        builder.signWith(getSigningKey());
        
        return builder.compact();
    }
    
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }
    
    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    /**
     * Extract role from JWT token.
     * @param token JWT token
     * @return Role as string (e.g., "USER", "ADMIN", "SUPER_ADMIN")
     */
    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object role = claims.get("role");
        return role != null ? role.toString() : "USER"; // Default to USER if role not found
    }
    
    /**
     * Extract token type from JWT token (access or refresh).
     * @param token JWT token
     * @return Token type as string
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object type = claims.get("type");
        return type != null ? type.toString() : "access"; // Default to access if type not found
    }
    
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token expired", e);
            throw e;
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported token", e);
            throw e;
        } catch (MalformedJwtException e) {
            log.debug("Malformed token", e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.debug("Token is empty", e);
            throw e;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("JWT signature validation failed. This usually means the secret used to sign the token is different from the secret used to validate it.", e);
            throw new RuntimeException("Invalid token signature", e);
        } catch (Exception e) {
            log.error("Unexpected error parsing token", e);
            throw new RuntimeException("Token parsing failed", e);
        }
    }
    
    public Boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Token is empty or invalid: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}

