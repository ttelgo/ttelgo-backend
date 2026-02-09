package com.tiktel.ttelgo.auth.application;

import com.tiktel.ttelgo.auth.api.dto.AuthResponse;
import com.tiktel.ttelgo.auth.application.port.SessionRepositoryPort;
import com.tiktel.ttelgo.auth.application.port.UserPort;
import com.tiktel.ttelgo.auth.domain.Session;
import com.tiktel.ttelgo.auth.infrastructure.service.AppleOAuthService;
import com.tiktel.ttelgo.auth.infrastructure.service.FacebookOAuthService;
import com.tiktel.ttelgo.auth.infrastructure.service.GoogleOAuthService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.security.JwtTokenProvider;
import com.tiktel.ttelgo.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Service for Social Login Authentication (Google, Facebook, Apple).
 * Implements token verification and user creation/lookup for social providers.
 * 
 * @author Spring Boot Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SocialAuthService {
    
    private final UserPort userPort;
    private final SessionRepositoryPort sessionRepositoryPort;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleOAuthService googleOAuthService;
    private final FacebookOAuthService facebookOAuthService;
    private final AppleOAuthService appleOAuthService;
    
    /**
     * Google Sign-In authentication.
     * Verifies Google ID token using tokeninfo endpoint and creates/logs in user.
     * 
     * @param idToken Google ID token
     * @return AuthResponse with JWT access token, refresh token, and user details
     */
    @Transactional
    public AuthResponse googleLogin(String idToken) {
        log.info("Google login request received");
        
        // Verify Google ID token using tokeninfo endpoint
        Optional<Map<String, Object>> tokenInfoOpt = googleOAuthService.verifyIdTokenViaTokenInfo(idToken);
        
        if (tokenInfoOpt.isEmpty()) {
            log.warn("Google login failed: Invalid Google ID token");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid Google ID token");
        }
        
        Map<String, Object> tokenInfo = tokenInfoOpt.get();
        
        // Extract user information from tokeninfo response
        String email = googleOAuthService.getEmailFromTokenInfo(tokenInfo);
        String name = googleOAuthService.getNameFromTokenInfo(tokenInfo);
        String providerId = googleOAuthService.getSubjectFromTokenInfo(tokenInfo);
        Boolean emailVerified = googleOAuthService.getEmailVerifiedFromTokenInfo(tokenInfo);
        
        // Validate required fields
        if (email == null || email.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Email not found in Google token");
        }
        
        if (providerId == null || providerId.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Subject (providerId) not found in Google token");
        }
        
        if (emailVerified == null || !emailVerified) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Google email not verified");
        }
        
        log.info("Google token verified successfully: email={}, providerId={}", email, providerId);
        
        // Find or create user
        User user = userPort.findByEmailIgnoreCase(email).orElse(null);
        
        if (user == null) {
            // Create new user
            user = User.builder()
                    .email(email)
                    .name(name != null ? name : email)
                    .provider(User.AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .isEmailVerified(true)
                    .referralCode(generateUniqueReferralCode())
                    .role(User.UserRole.USER)
                    .userType(User.UserType.CUSTOMER)
                    .build();
            user = userPort.save(user);
            log.info("Created new user via Google OAuth: email={}, providerId={}", email, providerId);
        } else {
            // Update existing user
            if (user.getProvider() == null || user.getProvider() != User.AuthProvider.GOOGLE) {
                user.setProvider(User.AuthProvider.GOOGLE);
            }
            if (providerId != null && (user.getProviderId() == null || user.getProviderId().isEmpty())) {
                user.setProviderId(providerId);
            }
            if (name != null && (user.getName() == null || user.getName().isEmpty())) {
                user.setName(name);
            }
            user.setIsEmailVerified(true);
            user = userPort.save(user);
            log.info("User logged in via Google OAuth: email={}, id={}", email, user.getId());
        }
        
        return generateAuthResponse(user);
    }
    
    /**
     * Facebook Sign-In authentication.
     * Verifies Facebook access token using Graph API and creates/logs in user.
     * 
     * @param accessToken Facebook access token
     * @return AuthResponse with JWT access token, refresh token, and user details
     */
    @Transactional
    public AuthResponse facebookLogin(String accessToken) {
        log.info("Facebook login request received");
        
        // Verify Facebook access token using Graph API
        Optional<FacebookOAuthService.FacebookUserInfo> facebookUserOpt = 
                facebookOAuthService.verifyAccessToken(accessToken);
        
        if (facebookUserOpt.isEmpty()) {
            log.warn("Facebook login failed: Invalid Facebook access token");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, 
                    "Invalid Facebook access token. Please ensure the token is valid and not expired.");
        }
        
        FacebookOAuthService.FacebookUserInfo facebookUser = facebookUserOpt.get();
        
        // Extract user information from Facebook response
        String providerId = facebookUser.getId();
        String email = facebookUser.getEmail();
        String name = facebookUser.getName();
        String firstName = facebookUser.getFirstName();
        String lastName = facebookUser.getLastName();
        
        // Validate required fields
        if (providerId == null || providerId.isEmpty()) {
            log.warn("Facebook login failed: User ID not found in Facebook response");
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "User ID not found in Facebook response");
        }
        
        log.info("Facebook token verified successfully: providerId={}, email={}", 
                providerId, email != null ? "provided" : "not provided");
        
        // Find or create user
        User user = null;
        
        // First, try to find by providerId (most reliable for Facebook)
        user = userPort.findByProviderAndProviderId(User.AuthProvider.FACEBOOK, providerId).orElse(null);
        
        // If not found by providerId, try by email (if provided)
        if (user == null && email != null && !email.trim().isEmpty()) {
            user = userPort.findByEmailIgnoreCase(email.trim()).orElse(null);
        }
        
        if (user == null) {
            // Create new user
            String userEmail;
            if (email != null && !email.trim().isEmpty()) {
                userEmail = email.trim().toLowerCase();
            } else {
                // Facebook may not provide email - use placeholder
                userEmail = providerId + "@facebook.private";
                log.info("Facebook email not provided, using placeholder: {}", userEmail);
            }
            
            user = User.builder()
                    .email(userEmail)
                    .name(name != null ? name : (userEmail.contains("@facebook.private") ? "Facebook User" : userEmail))
                    .firstName(firstName)
                    .lastName(lastName)
                    .provider(User.AuthProvider.FACEBOOK)
                    .providerId(providerId)
                    .isEmailVerified(email != null && !email.trim().isEmpty())
                    .referralCode(generateUniqueReferralCode())
                    .role(User.UserRole.USER)
                    .userType(User.UserType.CUSTOMER)
                    .build();
            user = userPort.save(user);
            log.info("Created new user via Facebook OAuth: email={}, providerId={}", userEmail, providerId);
        } else {
            // Update existing user
            boolean updated = false;
            
            if (user.getProvider() == null || user.getProvider() != User.AuthProvider.FACEBOOK) {
                user.setProvider(User.AuthProvider.FACEBOOK);
                updated = true;
            }
            if (providerId != null && (user.getProviderId() == null || user.getProviderId().isEmpty())) {
                user.setProviderId(providerId);
                updated = true;
            }
            if (name != null && (user.getName() == null || user.getName().isEmpty())) {
                user.setName(name);
                updated = true;
            }
            if (firstName != null && (user.getFirstName() == null || user.getFirstName().isEmpty())) {
                user.setFirstName(firstName);
                updated = true;
            }
            if (lastName != null && (user.getLastName() == null || user.getLastName().isEmpty())) {
                user.setLastName(lastName);
                updated = true;
            }
            if (email != null && !email.trim().isEmpty() && 
                (user.getEmail() == null || user.getEmail().endsWith("@facebook.private"))) {
                user.setEmail(email.trim().toLowerCase());
                user.setIsEmailVerified(true);
                updated = true;
            }
            
            if (updated) {
                user = userPort.save(user);
            }
            log.info("User logged in via Facebook OAuth: email={}, id={}", user.getEmail(), user.getId());
        }
        
        return generateAuthResponse(user);
    }
    
    /**
     * Apple Sign-In authentication.
     * Verifies Apple identity token (JWT) and creates/logs in user.
     * 
     * @param identityToken Apple identity token (JWT)
     * @return AuthResponse with JWT access token, refresh token, and user details
     */
    @Transactional
    public AuthResponse appleLogin(String identityToken) {
        log.info("Apple login request received");
        
        // Verify Apple identity token (JWT verification)
        Optional<JWTClaimsSet> appleTokenOpt = appleOAuthService.verifyIdentityToken(identityToken);
        
        if (appleTokenOpt.isEmpty()) {
            log.warn("Apple login failed: Invalid Apple identity token");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid Apple identity token");
        }
        
        JWTClaimsSet appleToken = appleTokenOpt.get();
        
        // Extract user information from Apple token
        String email = appleOAuthService.getEmail(appleToken);
        String sub = appleOAuthService.getSubject(appleToken);
        AppleOAuthService.Name name = appleOAuthService.getName(appleToken);
        
        if (sub == null || sub.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Subject (providerId) not found in Apple token");
        }
        
        log.info("Apple token verified successfully: email={}, providerId={}", 
                email != null ? "provided" : "not provided", sub);
        
        // Find or create user
        User user = null;
        
        if (email != null && !email.isEmpty()) {
            user = userPort.findByEmailIgnoreCase(email).orElse(null);
        }
        
        if (user == null) {
            user = userPort.findByProviderAndProviderId(User.AuthProvider.APPLE, sub).orElse(null);
        }
        
        String fullName = null;
        String firstName = null;
        String lastName = null;
        if (name != null) {
            fullName = name.getFullName();
            firstName = name.getFirstName();
            lastName = name.getLastName();
        }
        
        if (user == null) {
            // Create new user
            if (email == null || email.isEmpty()) {
                email = sub + "@apple.private";
            }
            
            user = User.builder()
                    .email(email)
                    .name(fullName != null ? fullName : email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .provider(User.AuthProvider.APPLE)
                    .providerId(sub)
                    .isEmailVerified(email != null && !email.endsWith("@apple.private"))
                    .referralCode(generateUniqueReferralCode())
                    .role(User.UserRole.USER)
                    .userType(User.UserType.CUSTOMER)
                    .build();
            user = userPort.save(user);
            log.info("Created new user via Apple Sign-In: email={}, providerId={}", email, sub);
        } else {
            // Update existing user
            if (user.getProvider() == null || user.getProvider() != User.AuthProvider.APPLE) {
                user.setProvider(User.AuthProvider.APPLE);
            }
            if (sub != null && (user.getProviderId() == null || user.getProviderId().isEmpty())) {
                user.setProviderId(sub);
            }
            if (fullName != null && (user.getName() == null || user.getName().isEmpty())) {
                user.setName(fullName);
            }
            if (firstName != null && (user.getFirstName() == null || user.getFirstName().isEmpty())) {
                user.setFirstName(firstName);
            }
            if (lastName != null && (user.getLastName() == null || user.getLastName().isEmpty())) {
                user.setLastName(lastName);
            }
            if (email != null && !email.endsWith("@apple.private") &&
                (user.getEmail() == null || user.getEmail().endsWith("@apple.private"))) {
                user.setEmail(email);
                user.setIsEmailVerified(true);
            }
            user = userPort.save(user);
            log.info("User logged in via Apple Sign-In: email={}, id={}", email, user.getId());
        }
        
        return generateAuthResponse(user);
    }
    
    /**
     * Generate JWT tokens and create session for authenticated user.
     * 
     * @param user Authenticated user
     * @return AuthResponse with JWT access token, refresh token, and user details
     */
    private AuthResponse generateAuthResponse(User user) {
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String userType = user.getUserType() != null ? user.getUserType().name() : "CUSTOMER";
        
        Long accessTokenExpiration = jwtTokenProvider.getCustomerAccessExpiration();
        
        // Generate access token
        String accessToken = jwtTokenProvider.generateTokenWithUserInfoAndCustomExpiration(
                user.getId(), 
                user.getEmail(), 
                user.getPhone(), 
                user.getFirstName(), 
                user.getLastName(),
                role, 
                userType, 
                user.getIsEmailVerified(), 
                user.getIsPhoneVerified(), 
                accessTokenExpiration
        );
        
        // Generate refresh token
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), role);
        
        // Create session
        Session session = Session.builder()
                .userId(user.getId())
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(accessTokenExpiration / 1000))
                .refreshExpiresAt(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build();
        sessionRepositoryPort.save(session);
        
        // Build user full name
        String userFullName = user.getName();
        if (userFullName == null || userFullName.isEmpty()) {
            if (user.getFirstName() != null || user.getLastName() != null) {
                userFullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                              (user.getFirstName() != null && user.getLastName() != null ? " " : "") +
                              (user.getLastName() != null ? user.getLastName() : "");
            } else {
                userFullName = user.getEmail();
            }
        }
        
        // Build and return response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(userFullName)
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phone(user.getPhone())
                        .isEmailVerified(user.getIsEmailVerified())
                        .isPhoneVerified(user.getIsPhoneVerified())
                        .role(role)
                        .build())
                .build();
    }
    
    /**
     * Generate unique referral code for new users.
     * 
     * @return Unique referral code
     */
    private String generateUniqueReferralCode() {
        String code;
        int maxAttempts = 10;
        int attempts = 0;
        
        do {
            code = "REF" + System.currentTimeMillis() % 1000000;
            attempts++;
            if (attempts >= maxAttempts) {
                code = "REF" + System.nanoTime() % 1000000;
                break;
            }
        } while (userPort.findByReferralCode(code).isPresent());
        
        return code;
    }
}
