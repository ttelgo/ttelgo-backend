package com.tiktel.ttelgo.auth.api;

import com.tiktel.ttelgo.auth.api.dto.AppleLoginRequest;
import com.tiktel.ttelgo.auth.api.dto.AuthResponse;
import com.tiktel.ttelgo.auth.api.dto.EmailOtpRequest;
import com.tiktel.ttelgo.auth.api.dto.EmailOtpVerifyRequest;
import com.tiktel.ttelgo.auth.api.dto.FacebookLoginRequest;
import com.tiktel.ttelgo.auth.api.dto.GoogleLoginRequest;
import com.tiktel.ttelgo.auth.api.dto.LoginRequest;
import com.tiktel.ttelgo.auth.api.dto.OtpRequest;
import com.tiktel.ttelgo.auth.api.dto.OtpVerifyRequest;
import com.tiktel.ttelgo.auth.api.dto.RegisterRequest;
import com.tiktel.ttelgo.auth.application.AuthService;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<String>> requestOtp(@RequestBody OtpRequest request) {
        authService.requestOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully"));
    }
    
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody OtpRequest request) {
        request.setPurpose("REGISTER");
        authService.requestOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Registration OTP sent. Please verify to complete registration."));
    }
    
    @PostMapping("/admin/register")
    public ResponseEntity<ApiResponse<AuthResponse>> adminRegister(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerWithPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginWithPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        log.info("Google login request received");
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/apple")
    public ResponseEntity<ApiResponse<AuthResponse>> appleLogin(@Valid @RequestBody AppleLoginRequest request) {
        log.info("Apple login request received");
        AuthResponse response = authService.appleLogin(request.getIdentityToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/facebook")
    public ResponseEntity<ApiResponse<AuthResponse>> facebookLogin(@Valid @RequestBody FacebookLoginRequest request) {
        log.info("Facebook login request received");
        AuthResponse response = authService.facebookLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/email/send-otp")
    public ResponseEntity<ApiResponse<String>> sendEmailOtp(@Valid @RequestBody EmailOtpRequest request) {
        log.info("Email OTP send request received for: {}", request.getEmail());
        authService.sendEmailOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully"));
    }
    
    @PostMapping("/email/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmailOtp(@Valid @RequestBody EmailOtpVerifyRequest request) {
        log.info("Email OTP verify request received for: {}", request.getEmail());
        AuthResponse response = authService.verifyEmailOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        log.info("Logout request received");
        
        String token = authHeader != null && authHeader.startsWith("Bearer ") 
                ? authHeader.substring(7) 
                : authHeader;
        
        authService.logout(token, httpRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
    
    /**
     * One-time endpoint to create initial admin user.
     * This should be disabled in production or protected with a secret key.
     * Email: admin@ttelgo.com, Password: Admin@123456
     */
    @PostMapping("/admin/create-initial")
    public ResponseEntity<ApiResponse<String>> createInitialAdmin() {
        try {
            authService.createInitialAdminUser();
            return ResponseEntity.ok(ApiResponse.success("Initial admin user created successfully. Email: admin@ttelgo.com, Password: Admin@123456"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create admin user: " + e.getMessage()));
        }
    }
    
    // DTO for refresh token request
    public static class RefreshTokenRequest {
        private String refreshToken;
        
        public String getRefreshToken() {
            return refreshToken;
        }
        
        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}

