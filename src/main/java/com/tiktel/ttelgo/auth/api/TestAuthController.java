package com.tiktel.ttelgo.auth.api;

import com.tiktel.ttelgo.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * TEMPORARY Test Authentication Controller
 * FOR DEVELOPMENT/TESTING ONLY - Remove in production!
 * 
 * This controller provides a simple way to generate JWT tokens for API testing
 * without requiring a full user authentication system.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs (Test endpoints)")
public class TestAuthController {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * Generate a test JWT token for API testing
     * 
     * @param request Token generation request
     * @return JWT token response
     */
    @PostMapping("/test/token")
    @Operation(summary = "Generate test JWT token", 
               description = "FOR TESTING ONLY: Generate a JWT token with custom user ID and email")
    public ResponseEntity<TokenResponse> generateTestToken(@RequestBody TokenRequest request) {
        
        Long userId = request.getUserId() != null ? request.getUserId() : 1L;
        String email = request.getEmail() != null ? request.getEmail() : "test@ttelgo.com";
        String role = request.getRole() != null ? request.getRole() : "USER";
        
        // Generate JWT token
        String accessToken = jwtTokenProvider.generateToken(userId, email, role);
        
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, email, role);
        
        return ResponseEntity.ok(new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                2592000L, // 30 days (1 month) in seconds
                request.getUserId() != null ? request.getUserId() : 1L,
                request.getEmail() != null ? request.getEmail() : "test@ttelgo.com"
        ));
    }
    
    /**
     * Validate a JWT token
     * 
     * @param token The JWT token to validate
     * @return Validation result
     */
    @GetMapping("/test/validate")
    @Operation(summary = "Validate JWT token", 
               description = "Check if a JWT token is valid and extract its claims")
    public ResponseEntity<ValidationResponse> validateToken(@RequestParam String token) {
        
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        if (isValid) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            boolean isExpired = jwtTokenProvider.isTokenExpired(token);
            
            return ResponseEntity.ok(new ValidationResponse(
                    true,
                    "Token is valid",
                    userId,
                    email,
                    isExpired
            ));
        } else {
            return ResponseEntity.ok(new ValidationResponse(
                    false,
                    "Token is invalid or expired",
                    null,
                    null,
                    true
            ));
        }
    }
    
    @Data
    public static class TokenRequest {
        private Long userId;
        private String email;
        private String role; // Optional: USER, ADMIN, SUPER_ADMIN (defaults to USER)
    }
    
    @Data
    @RequiredArgsConstructor
    public static class TokenResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String tokenType;
        private final Long expiresIn;
        private final Long userId;
        private final String email;
    }
    
    @Data
    @RequiredArgsConstructor
    public static class ValidationResponse {
        private final boolean valid;
        private final String message;
        private final Long userId;
        private final String email;
        private final boolean expired;
    }
}

