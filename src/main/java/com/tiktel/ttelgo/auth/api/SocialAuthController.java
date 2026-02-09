package com.tiktel.ttelgo.auth.api;

import com.tiktel.ttelgo.auth.api.dto.AuthResponse;
import com.tiktel.ttelgo.auth.api.dto.SocialLoginRequest;
import com.tiktel.ttelgo.auth.application.SocialAuthService;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Social Login Authentication.
 * Provides endpoints for Google, Facebook, and Apple login.
 * 
 * All endpoints accept unified request format:
 * {
 *   "idToken": "SOCIAL_PROVIDER_ID_TOKEN"
 * }
 * 
 * @author Spring Boot Team
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class SocialAuthController {
    
    private final SocialAuthService socialAuthService;
    
    /**
     * Google Sign-In endpoint.
     * Verifies Google ID token and returns JWT access token.
     * 
     * POST /api/auth/google
     * Request Body: { "idToken": "GOOGLE_ID_TOKEN" }
     * 
     * @param request Social login request containing Google ID token
     * @return AuthResponse with JWT access token, refresh token, and user details
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Google login request received");
        AuthResponse response = socialAuthService.googleLogin(request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Facebook Sign-In endpoint.
     * Verifies Facebook access token and returns JWT access token.
     * 
     * POST /api/auth/facebook
     * Request Body: { "idToken": "FACEBOOK_ACCESS_TOKEN" }
     * 
     * @param request Social login request containing Facebook access token
     * @return AuthResponse with JWT access token, refresh token, and user details
     */
    @PostMapping("/facebook")
    public ResponseEntity<ApiResponse<AuthResponse>> facebookLogin(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Facebook login request received");
        AuthResponse response = socialAuthService.facebookLogin(request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Apple Sign-In endpoint.
     * Verifies Apple identity token (JWT) and returns JWT access token.
     * 
     * POST /api/auth/apple
     * Request Body: { "idToken": "APPLE_IDENTITY_TOKEN" }
     * 
     * @param request Social login request containing Apple identity token
     * @return AuthResponse with JWT access token, refresh token, and user details
     */
    @PostMapping("/apple")
    public ResponseEntity<ApiResponse<AuthResponse>> appleLogin(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Apple login request received");
        AuthResponse response = socialAuthService.appleLogin(request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
