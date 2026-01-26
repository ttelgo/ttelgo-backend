package com.tiktel.ttelgo.admin.auth.application;

import com.tiktel.ttelgo.admin.auth.api.dto.AdminAuthResponse;
import com.tiktel.ttelgo.admin.auth.api.dto.AdminLoginRequest;
import com.tiktel.ttelgo.admin.auth.api.dto.AdminRefreshRequest;
import com.tiktel.ttelgo.auth.application.port.SessionRepositoryPort;
import com.tiktel.ttelgo.auth.domain.Session;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.security.JwtTokenProvider;
import com.tiktel.ttelgo.security.audit.AuditService;
import com.tiktel.ttelgo.user.application.port.UserRepositoryPort;
import com.tiktel.ttelgo.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Admin authentication service.
 * Handles admin login, logout, and token refresh with email + password authentication.
 * WEB ONLY - no OTP authentication for admins.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminAuthService {
    
    private final UserRepositoryPort userRepositoryPort;
    private final SessionRepositoryPort sessionRepositoryPort;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    
    // Admin scopes based on role
    private static final List<String> ADMIN_SCOPES = Arrays.asList(
        "orders:read", "orders:write", "orders:update",
        "users:read", "users:write",
        "kyc:read", "kyc:approve", "kyc:reject",
        "vendors:read", "vendors:write",
        "esims:read", "esims:manage",
        "payments:read", "payments:refund",
        "dashboard:read"
    );
    
    private static final List<String> SUPPORT_SCOPES = Arrays.asList(
        "orders:read", "orders:update",
        "users:read",
        "kyc:read", "kyc:approve",
        "vendors:read",
        "esims:read",
        "payments:read",
        "dashboard:read"
    );
    
    private static final List<String> SUPER_ADMIN_SCOPES = Arrays.asList(
        "orders:read", "orders:write", "orders:update", "orders:delete",
        "users:read", "users:write", "users:delete",
        "kyc:read", "kyc:approve", "kyc:reject",
        "vendors:read", "vendors:write", "vendors:delete",
        "esims:read", "esims:manage", "esims:delete",
        "payments:read", "payments:refund",
        "dashboard:read", "dashboard:write",
        "admin:manage", "system:configure"
    );
    
    /**
     * Get admin scopes based on role.
     */
    private List<String> getScopesForRole(User.UserRole role) {
        if (role == User.UserRole.SUPER_ADMIN) {
            return SUPER_ADMIN_SCOPES;
        }
        if (role == User.UserRole.SUPPORT) {
            return SUPPORT_SCOPES;
        }
        return ADMIN_SCOPES;
    }
    
    /**
     * Admin login with email + password.
     * Validates admin user type and role, then generates JWT tokens.
     */
    @Transactional
    public AdminAuthResponse login(AdminLoginRequest request, HttpServletRequest httpRequest) {
        log.info("Admin login attempt for email: {}", request.getEmail());
        
        // Find user by email (case-insensitive)
        User user = userRepositoryPort.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Admin login failed: User not found with email: {}", request.getEmail());
                    String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
                    auditService.logFailedAction("ADMIN", request.getEmail(), "ADMIN_LOGIN", 
                            "USER", "User not found", ipAddress);
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
                });
        
        // Validate user is ADMIN type
        if (user.getUserType() != User.UserType.ADMIN) {
            log.warn("Admin login failed: User is not ADMIN type. Email: {}, UserType: {}", 
                    request.getEmail(), user.getUserType());
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
            auditService.logFailedAction("ADMIN", request.getEmail(), "ADMIN_LOGIN", 
                    "USER", "User is not ADMIN type", ipAddress);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }
        
        // Validate user has ADMIN, SUPER_ADMIN, or SUPPORT role
        if (user.getRole() != User.UserRole.ADMIN && 
            user.getRole() != User.UserRole.SUPER_ADMIN && 
            user.getRole() != User.UserRole.SUPPORT) {
            log.warn("Admin login failed: User does not have ADMIN or SUPPORT role. Email: {}, Role: {}", 
                    request.getEmail(), user.getRole());
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
            auditService.logFailedAction("ADMIN", request.getEmail(), "ADMIN_LOGIN", 
                    "USER", "User does not have ADMIN or SUPPORT role", ipAddress);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }
        
        // Check if user has a password set
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            log.warn("Admin login failed: Password not set for user: {}", user.getEmail());
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
            auditService.logFailedAction("ADMIN", request.getEmail(), "ADMIN_LOGIN", 
                    "USER", "Password not set", ipAddress);
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Password not set. Please contact administrator.");
        }
        
        // Verify password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.debug("Password verification for admin {}: {}", user.getEmail(), passwordMatches ? "MATCH" : "NO MATCH");
        
        if (!passwordMatches) {
            log.warn("Admin login failed: Invalid password for user: {}", user.getEmail());
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
            auditService.logFailedAction("ADMIN", request.getEmail(), "ADMIN_LOGIN", 
                    "USER", "Invalid password", ipAddress);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }
        
        log.info("Admin login successful for user: {} (Role: {})", user.getEmail(), user.getRole());
        
        // Get scopes based on role
        List<String> scopes = getScopesForRole(user.getRole());
        
        // Generate admin tokens with scopes
        String role = user.getRole().name();
        String accessToken = jwtTokenProvider.generateAdminToken(user.getId(), user.getEmail(), role, scopes);
        String refreshToken = jwtTokenProvider.generateAdminRefreshToken(user.getId(), user.getEmail(), role, scopes);
        
        // Create or update session
        Session session = sessionRepositoryPort.findByUserId(user.getId())
                .stream()
                .filter(s -> s.getIsActive())
                .findFirst()
                .orElse(null);
        
        // Use shorter expiration for admin tokens (12 hours)
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusHours(12);
        
        if (session != null) {
            // Update existing session
            session.setToken(accessToken);
            session.setRefreshToken(refreshToken);
            session.setExpiresAt(tokenExpiresAt);
            session.setRefreshExpiresAt(LocalDateTime.now().plusDays(7));
            session.setIsActive(true);
        } else {
            // Create new session
            session = Session.builder()
                    .userId(user.getId())
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(tokenExpiresAt)
                    .refreshExpiresAt(LocalDateTime.now().plusDays(7))
                    .isActive(true)
                    .build();
        }
        sessionRepositoryPort.save(session);
        
        // Log successful login
        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "unknown";
        auditService.logAudit("ADMIN", user.getEmail(), user.getId(), null, "ADMIN_LOGIN", 
                "USER", user.getId(), "Admin login successful", ipAddress, userAgent, true, null);
        
        // Build response
        return AdminAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(43200L) // 12 hours in seconds
                .admin(AdminAuthResponse.AdminUserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .userType(user.getUserType().name())
                        .scopes(scopes)
                        .build())
                .build();
    }
    
    /**
     * Refresh admin access token.
     */
    @Transactional
    public AdminAuthResponse refresh(AdminRefreshRequest request, HttpServletRequest httpRequest) {
        log.info("Admin refresh token request");
        
        String refreshToken = request.getRefreshToken();
        
        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token");
        }
        
        // Verify token type
        String tokenType = jwtTokenProvider.getTokenTypeFromToken(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid token type");
        }
        
        // Verify user type is ADMIN
        // Extract user_id from token
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
        
        if (user.getUserType() != User.UserType.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid token for admin user");
        }
        
        // Find session
        Session session = sessionRepositoryPort.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Session not found"));
        
        if (!session.getIsActive() || session.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Refresh token expired");
        }
        
        // Generate new tokens with scopes
        List<String> scopes = getScopesForRole(user.getRole());
        String role = user.getRole().name();
        String newAccessToken = jwtTokenProvider.generateAdminToken(user.getId(), user.getEmail(), role, scopes);
        String newRefreshToken = jwtTokenProvider.generateAdminRefreshToken(user.getId(), user.getEmail(), role, scopes);
        
        // Update session
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusHours(12);
        session.setToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(tokenExpiresAt);
        session.setRefreshExpiresAt(LocalDateTime.now().plusDays(7));
        sessionRepositoryPort.save(session);
        
        log.info("Admin token refreshed successfully for user: {}", user.getEmail());
        
        return AdminAuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(43200L) // 12 hours in seconds
                .admin(AdminAuthResponse.AdminUserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .userType(user.getUserType().name())
                        .scopes(scopes)
                        .build())
                .build();
    }
    
    /**
     * Admin logout - invalidate session.
     */
    @Transactional
    public void logout(String accessToken, HttpServletRequest httpRequest) {
        log.info("Admin logout request");
        
        Session session = sessionRepositoryPort.findByToken(accessToken)
                .orElse(null);
        
        if (session != null) {
            session.setIsActive(false);
            sessionRepositoryPort.save(session);
            
            // Log logout
            Long userId = session.getUserId();
            User user = userRepositoryPort.findById(userId).orElse(null);
            String email = user != null ? user.getEmail() : "unknown";
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
            String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "unknown";
            
            auditService.logAudit("ADMIN", email, userId, null, "ADMIN_LOGOUT", 
                    "USER", userId, "Admin logout", ipAddress, userAgent, true, null);
            
            log.info("Admin logout successful for user: {}", email);
        }
    }
}

