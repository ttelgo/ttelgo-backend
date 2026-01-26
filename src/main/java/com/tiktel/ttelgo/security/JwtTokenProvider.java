package com.tiktel.ttelgo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret:your-secret-key-change-in-production-min-256-bits}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}") // 24 hours default
    private Long expiration;
    
    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private Long refreshExpiration;
    
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
    
    /**
     * Generate JWT token with custom expiration time.
     * Used for OAuth providers that require longer token expiry (e.g., 7 days for Google OAuth).
     * 
     * @param userId User ID
     * @param email User email
     * @param role User role (USER, ADMIN, SUPER_ADMIN)
     * @param userType User type (CUSTOMER, VENDOR, ADMIN)
     * @param expirationMillis Custom expiration time in milliseconds
     * @return JWT access token with custom expiration
     */
    public String generateTokenWithCustomExpiration(Long userId, String email, String role, 
                                                     String userType, Long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        claims.put("email", email);
        claims.put("user_type", userType != null ? userType : "CUSTOMER");
        
        // Map USER role to ROLE_CUSTOMER for customers
        String jwtRole = "USER".equals(role) && "CUSTOMER".equals(userType) 
            ? "ROLE_CUSTOMER" 
            : role;
        claims.put("roles", jwtRole);
        claims.put("type", "access");
        
        return createToken(claims, email, expirationMillis);
    }
    
    /**
     * Generate JWT token with full user information and custom expiration time.
     * Used for OAuth providers that require longer token expiry (e.g., 7 days for Google OAuth).
     * 
     * @param userId User ID
     * @param email User email
     * @param phone User phone
     * @param firstName User first name
     * @param lastName User last name
     * @param role User role (USER, ADMIN, SUPER_ADMIN)
     * @param userType User type (CUSTOMER, VENDOR, ADMIN)
     * @param isEmailVerified Email verification status
     * @param isPhoneVerified Phone verification status
     * @param expirationMillis Custom expiration time in milliseconds
     * @return JWT access token with user information and custom expiration
     */
    public String generateTokenWithUserInfoAndCustomExpiration(Long userId, String email, String phone, 
                                                                String firstName, String lastName, 
                                                                String role, String userType,
                                                                Boolean isEmailVerified, Boolean isPhoneVerified,
                                                                Long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        claims.put("email", email);
        if (phone != null) {
            claims.put("phone", phone);
        }
        if (firstName != null) {
            claims.put("first_name", firstName);
        }
        if (lastName != null) {
            claims.put("last_name", lastName);
        }
        claims.put("user_type", userType != null ? userType : "CUSTOMER");
        
        // Map USER role to ROLE_CUSTOMER for customers
        String jwtRole = "USER".equals(role) && "CUSTOMER".equals(userType) 
            ? "ROLE_CUSTOMER" 
            : role;
        claims.put("roles", jwtRole);
        claims.put("type", "access");
        
        if (isEmailVerified != null) {
            claims.put("is_email_verified", isEmailVerified);
        }
        if (isPhoneVerified != null) {
            claims.put("is_phone_verified", isPhoneVerified);
        }
        
        return createToken(claims, email, expirationMillis);
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
            throw new RuntimeException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Malformed token", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Token is empty", e);
        }
    }
    
    public Boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return true;
        } catch (Exception e) {
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

