package com.tiktel.ttelgo.auth.application;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.tiktel.ttelgo.auth.api.dto.AuthResponse;
import com.tiktel.ttelgo.auth.api.dto.GoogleLoginRequest;
import com.tiktel.ttelgo.auth.api.dto.LoginRequest;
import com.tiktel.ttelgo.auth.api.dto.OtpRequest;
import com.tiktel.ttelgo.auth.api.dto.OtpVerifyRequest;
import com.tiktel.ttelgo.auth.api.dto.RegisterRequest;
import com.tiktel.ttelgo.auth.application.port.OtpServicePort;
import com.tiktel.ttelgo.auth.application.port.SessionRepositoryPort;
import com.tiktel.ttelgo.auth.application.port.UserPort;
import com.tiktel.ttelgo.auth.domain.OtpToken;
import com.tiktel.ttelgo.auth.domain.Session;
import com.tiktel.ttelgo.auth.infrastructure.repository.OtpTokenRepository;
import com.tiktel.ttelgo.auth.infrastructure.service.GoogleOAuthService;
import com.tiktel.ttelgo.auth.infrastructure.service.AppleOAuthService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.security.JwtTokenProvider;
import com.tiktel.ttelgo.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthService {
    
    private final UserPort userPort;
    private final OtpServicePort otpServicePort;
    private final OtpTokenRepository otpTokenRepository;
    private final SessionRepositoryPort sessionRepositoryPort;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final GoogleOAuthService googleOAuthService;
    private final AppleOAuthService appleOAuthService;
    
    @Autowired
    public AuthService(
            UserPort userPort,
            OtpServicePort otpServicePort,
            OtpTokenRepository otpTokenRepository,
            SessionRepositoryPort sessionRepositoryPort,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            GoogleOAuthService googleOAuthService,
            AppleOAuthService appleOAuthService) {
        this.userPort = userPort;
        this.otpServicePort = otpServicePort;
        this.otpTokenRepository = otpTokenRepository;
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.googleOAuthService = googleOAuthService;
        this.appleOAuthService = appleOAuthService;
    }
    
    @Transactional
    public void requestOtp(OtpRequest request) {
        String otp = otpServicePort.generateOtp();
        String purpose = request.getPurpose() != null ? request.getPurpose() : "LOGIN";
        
        // Normalize email to lowercase for consistent storage and lookup
        String normalizedEmail = null;
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            normalizedEmail = request.getEmail().trim().toLowerCase();
            // Invalidate previous unused OTPs using normalized email
            otpTokenRepository.findByEmailAndIsUsedFalse(normalizedEmail)
                    .forEach(otpTokenRepository::delete);
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            String normalizedPhone = request.getPhone().trim();
            otpTokenRepository.findByPhoneAndIsUsedFalse(normalizedPhone)
                    .forEach(otpTokenRepository::delete);
        }
        
        // Hash OTP before storage for security
        String hashedOtp = passwordEncoder.encode(otp);
        log.debug("OTP generated: {} (length: {}), hashed and stored", otp, otp.length());
        
        // Create new OTP token with hashed OTP (store normalized email)
        OtpToken otpToken = OtpToken.builder()
                .email(normalizedEmail)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .otpCode(hashedOtp)
                .purpose(purpose)
                .isUsed(false)
                .attempts(0)
                .maxAttempts(3)
                .expiresAt(LocalDateTime.now().plusMinutes(5)) // OTP expires in 5 minutes
                .build();
        
        // Use saveAndFlush to immediately persist to database
        otpTokenRepository.saveAndFlush(otpToken);
        log.info("OTP token saved to database with id: {}, email: {}, expires at: {}", 
                otpToken.getId(), normalizedEmail, otpToken.getExpiresAt());
        
        // Send plain OTP to user (via email/SMS) - use original email for sending
        // Email sending failure won't break the flow - OTP is already saved
        otpServicePort.sendOtp(request.getEmail(), request.getPhone(), otp, purpose);
        log.info("OTP request completed for email: {}, purpose: {}", request.getEmail(), purpose);
    }
    
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        log.info("OTP verification request received for email={}, phone={}", 
                request.getEmail(), request.getPhone());
        
        try {
            // Validate request
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                log.warn("OTP verification failed: OTP is null or empty");
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "OTP is required");
            }
            
            if ((request.getEmail() == null || request.getEmail().trim().isEmpty()) && 
                (request.getPhone() == null || request.getPhone().trim().isEmpty())) {
                log.warn("OTP verification failed: Both email and phone are missing");
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Email or phone is required");
            }
            
            LocalDateTime now = LocalDateTime.now();
            
            // Normalize email to lowercase for consistent lookup (must match how it was stored)
            final String normalizedEmail = (request.getEmail() != null && !request.getEmail().isEmpty()) 
                    ? request.getEmail().trim().toLowerCase() 
                    : null;
            
            // Find OTP token by email or phone (not by OTP code, since it's hashed)
            OtpToken otpToken = null;
            if (normalizedEmail != null) {
                final String emailForLog = normalizedEmail; // For use in lambda
                log.debug("Looking up OTP tokens for normalized email: {}", normalizedEmail);
                List<OtpToken> tokens = otpTokenRepository.findByEmailAndIsUsedFalse(normalizedEmail);
                log.info("Found {} unused OTP tokens for email: {}", tokens.size(), normalizedEmail);
                
                if (tokens.isEmpty()) {
                    log.warn("No unused OTP tokens found for email: {}", normalizedEmail);
                    throw new BusinessException(ErrorCode.INVALID_OTP, "Invalid or expired OTP");
                }
                
                otpToken = tokens.stream()
                        .filter(token -> {
                            if (token.getExpiresAt() == null) {
                                log.warn("OTP token {} has null expiresAt", token.getId());
                                return false;
                            }
                            boolean isNotExpired = token.getExpiresAt().isAfter(now);
                            log.debug("OTP token {} expires at {}, now is {}, isNotExpired={}", 
                                    token.getId(), token.getExpiresAt(), now, isNotExpired);
                            return isNotExpired;
                        })
                        .findFirst()
                        .orElseThrow(() -> {
                            log.warn("OTP verification failed: No valid (non-expired) OTP found for email: {} (checked {} tokens, all expired)", 
                                    emailForLog, tokens.size());
                            return new BusinessException(ErrorCode.INVALID_OTP, "Invalid or expired OTP");
                        });
            } else if (request.getPhone() != null && !request.getPhone().isEmpty()) {
                final String normalizedPhone = request.getPhone().trim();
                log.debug("Looking up OTP tokens for phone: {}", normalizedPhone);
                List<OtpToken> tokens = otpTokenRepository.findByPhoneAndIsUsedFalse(normalizedPhone);
                log.info("Found {} unused OTP tokens for phone: {}", tokens.size(), normalizedPhone);
                
                if (tokens.isEmpty()) {
                    log.warn("No unused OTP tokens found for phone: {}", normalizedPhone);
                    throw new BusinessException(ErrorCode.INVALID_OTP, "Invalid or expired OTP");
                }
                
                otpToken = tokens.stream()
                        .filter(token -> {
                            if (token.getExpiresAt() == null) {
                                log.warn("OTP token {} has null expiresAt", token.getId());
                                return false;
                            }
                            boolean isNotExpired = token.getExpiresAt().isAfter(now);
                            log.debug("OTP token {} expires at {}, now is {}, isNotExpired={}", 
                                    token.getId(), token.getExpiresAt(), now, isNotExpired);
                            return isNotExpired;
                        })
                        .findFirst()
                        .orElseThrow(() -> {
                            log.warn("OTP verification failed: No valid (non-expired) OTP found for phone: {} (checked {} tokens, all expired)", 
                                    normalizedPhone, tokens.size());
                            return new BusinessException(ErrorCode.INVALID_OTP, "Invalid or expired OTP");
                        });
            } else {
                log.error("OTP verification failed: Both email and phone are null/empty");
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Email or phone is required");
            }
            
            if (otpToken == null) {
                log.error("OTP token is null after lookup - this should not happen");
                throw new BusinessException(ErrorCode.INVALID_OTP, "Invalid or expired OTP");
            }
        
        log.info("OTP token found for verification: id={}, email={}, phone={}, attempts={}/{}, expiresAt={}", 
                otpToken.getId(), otpToken.getEmail(), otpToken.getPhone(), 
                otpToken.getAttempts(), otpToken.getMaxAttempts(), otpToken.getExpiresAt());
        
        // Check attempts
        if (otpToken.getAttempts() >= otpToken.getMaxAttempts()) {
            log.warn("OTP verification failed: Maximum attempts exceeded for token id={}", otpToken.getId());
            throw new BusinessException(ErrorCode.OTP_EXPIRED, "Maximum OTP attempts exceeded");
        }
        
        // Validate OTP input
        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            log.warn("OTP verification failed: OTP is null or empty");
            throw new BusinessException(ErrorCode.INVALID_OTP, "OTP is required");
        }
        
        String providedOtp = request.getOtp().trim();
        log.info("Verifying OTP: providedOtp='{}' (length={}), token id={}, stored hash exists={}", 
                providedOtp, providedOtp.length(), otpToken.getId(), 
                otpToken.getOtpCode() != null && !otpToken.getOtpCode().isEmpty());
        
        // Check if stored OTP code exists
        if (otpToken.getOtpCode() == null || otpToken.getOtpCode().isEmpty()) {
            log.error("OTP token has no stored hash! token id={}", otpToken.getId());
            throw new BusinessException(ErrorCode.INVALID_OTP, "OTP verification failed - invalid token");
        }
        
        // Verify OTP using password encoder (compare plain OTP with hashed OTP)
        boolean isValid = otpToken.verifyOtp(providedOtp, passwordEncoder);
        log.info("OTP verification result: isValid={} for token id={}", isValid, otpToken.getId());
        
        if (!isValid) {
            // Increment attempts
            int newAttempts = otpToken.getAttempts() + 1;
            otpToken.setAttempts(newAttempts);
            otpTokenRepository.save(otpToken);
            log.warn("OTP verification failed: Invalid OTP '{}' for token id={}, attempts now={}/{}", 
                    providedOtp, otpToken.getId(), newAttempts, otpToken.getMaxAttempts());
            throw new BusinessException(ErrorCode.INVALID_OTP, 
                    String.format("Invalid OTP. Attempts remaining: %d", otpToken.getMaxAttempts() - newAttempts));
        }
        
        log.info("OTP verified successfully for token id={}", otpToken.getId());
        
        // Mark as used
        otpToken.setIsUsed(true);
        otpTokenRepository.save(otpToken);
        
        // Find or create user (IMPLICIT REGISTRATION)
        // Use case-insensitive email lookup for consistency
        User user = null;
        if (otpToken.getEmail() != null && !otpToken.getEmail().isEmpty()) {
            user = userPort.findByEmailIgnoreCase(otpToken.getEmail())
                    .orElse(null);
        } else if (otpToken.getPhone() != null && !otpToken.getPhone().isEmpty()) {
            user = userPort.findByPhone(otpToken.getPhone())
                    .orElse(null);
        }
        
        // IMPLICIT REGISTRATION: If user doesn't exist, create CUSTOMER user
        if (user == null) {
            // Ensure we have at least email or phone for user creation
            String userEmail = otpToken.getEmail();
            String userPhone = otpToken.getPhone();
            
            // Email is required in User entity, so use fallback if missing
            if (userEmail == null || userEmail.trim().isEmpty()) {
                if (userPhone != null && !userPhone.trim().isEmpty()) {
                    // Use phone as email placeholder if email is missing
                    userEmail = userPhone.trim() + "@phone.placeholder";
                    log.warn("OTP token has no email, using phone-based email placeholder: {}", userEmail);
                } else {
                    log.error("Cannot create user: Both email and phone are null in OTP token id={}", otpToken.getId());
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                            "Cannot create user account: Email or phone is required");
                }
            }
            
            // Ensure userEmail is not null at this point
            if (userEmail == null || userEmail.trim().isEmpty()) {
                log.error("userEmail is still null after validation for OTP token id={}", otpToken.getId());
                throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                        "Cannot create user account: Email is required");
            }
            
            // Name is required in User entity - use email as name (or phone if email is placeholder)
            String name;
            try {
                if (userEmail != null && userEmail.contains("@phone.placeholder")) {
                    // If email is a phone placeholder, use the original phone or email prefix as name
                    if (userPhone != null && !userPhone.trim().isEmpty()) {
                        name = userPhone.trim();
                    } else {
                        int atIndex = userEmail.indexOf("@");
                        name = atIndex > 0 ? userEmail.substring(0, atIndex) : userEmail;
                    }
                } else if (userEmail != null && userEmail.contains("@")) {
                    // Extract username part from email
                    int atIndex = userEmail.indexOf("@");
                    name = atIndex > 0 ? userEmail.substring(0, atIndex) : userEmail;
                } else {
                    // Fallback to email itself or "User"
                    name = userEmail != null && !userEmail.trim().isEmpty() ? userEmail.trim() : "User";
                }
            } catch (Exception e) {
                log.warn("Error extracting name from email, using fallback: {}", e.getMessage());
                name = "User";
            }
            
            // Ensure name is not null or empty, and not too long (database constraint)
            if (name == null || name.trim().isEmpty()) {
                name = "User";
            }
            name = name.trim();
            // Limit name length to prevent database issues (assuming max 255 chars)
            if (name.length() > 255) {
                name = name.substring(0, 255);
            }
            
            try {
                // Double-check user doesn't exist (race condition protection)
                String normalizedEmailForLookup = userEmail.trim().toLowerCase();
                user = userPort.findByEmailIgnoreCase(normalizedEmailForLookup).orElse(null);
                
                if (user == null && userPhone != null && !userPhone.trim().isEmpty()) {
                    user = userPort.findByPhone(userPhone.trim()).orElse(null);
                }
                
                if (user == null) {
                    user = User.builder()
                            .email(normalizedEmailForLookup) // Normalize email to lowercase
                            .phone(userPhone != null && !userPhone.trim().isEmpty() ? userPhone.trim() : null)
                            .name(name)
                            .isEmailVerified(otpToken.getEmail() != null && !otpToken.getEmail().trim().isEmpty())
                            .isPhoneVerified(userPhone != null && !userPhone.trim().isEmpty())
                            .referralCode(generateUniqueReferralCode())
                            .role(User.UserRole.USER)
                            .userType(User.UserType.CUSTOMER) // Set user type to CUSTOMER
                            .build();
                    user = userPort.save(user);
                    log.info("Implicit registration: Created CUSTOMER user with id={}, email={}, phone={}, name={}", 
                            user.getId(), user.getEmail(), user.getPhone(), user.getName());
                } else {
                    log.info("User already exists, using existing user: id={}, email={}", 
                            user.getId(), user.getEmail());
                }
            } catch (DataIntegrityViolationException e) {
                // Handle unique constraint violations (e.g., duplicate email/phone)
                log.warn("Data integrity violation during user creation: {}", e.getMessage());
                // Try to find the existing user
                String normalizedEmailForLookup = userEmail.trim().toLowerCase();
                user = userPort.findByEmailIgnoreCase(normalizedEmailForLookup).orElse(null);
                if (user == null && userPhone != null && !userPhone.trim().isEmpty()) {
                    user = userPort.findByPhone(userPhone.trim()).orElse(null);
                }
                if (user == null) {
                    log.error("Data integrity violation but user not found: {}", e.getMessage());
                    throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, 
                            "An account with this email or phone already exists. Please try logging in.");
                }
                log.info("User already exists (caught from constraint violation), using existing user: id={}", user.getId());
            } catch (Exception e) {
                log.error("Failed to create user during OTP verification: {}", e.getMessage(), e);
                log.error("Exception type: {}, cause: {}", e.getClass().getName(), 
                        e.getCause() != null ? e.getCause().getMessage() : "none");
                if (e.getCause() != null) {
                    log.error("Cause stack trace:", e.getCause());
                }
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                        "Failed to create user account. Please try again.");
            }
        }
        
        // Generate tokens with user role
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String userType = user.getUserType() != null ? user.getUserType().name() : "CUSTOMER";
        
        // Ensure we have email for JWT token generation (required as subject - cannot be null)
        String userEmail = user.getEmail();
        if (userEmail == null || userEmail.trim().isEmpty()) {
            // Fallback to phone or user ID if no email
            if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                userEmail = user.getPhone().trim() + "@phone.placeholder";
            } else {
                userEmail = "user" + user.getId() + "@ttelgo.com";
            }
            log.warn("User {} has no email, using fallback: {}", user.getId(), userEmail);
        } else {
            userEmail = userEmail.trim();
        }
        
        // Validate email is not null before JWT generation
        if (userEmail == null || userEmail.isEmpty()) {
            log.error("Cannot generate JWT token: user email is null for user id={}", user.getId());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Unable to generate authentication token. Please contact support.");
        }
        
        // Generate token with user information embedded for direct extraction
        String token;
        String refreshToken;
        try {
            token = jwtTokenProvider.generateTokenWithUserInfo(
                user.getId(), 
                user.getEmail() != null ? user.getEmail() : userEmail, 
                user.getPhone(),
                user.getFirstName(),
                user.getLastName(),
                role, 
                userType,
                user.getIsEmailVerified(),
                user.getIsPhoneVerified()
            );
            refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), userEmail, role);
        } catch (Exception e) {
            log.error("Failed to generate JWT tokens for user id={}, email={}: {}", 
                    user.getId(), userEmail, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Failed to generate authentication token. Please try again.");
        }
        
        // Create session
        Session session = Session.builder()
                .userId(user.getId())
                .token(token)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .refreshExpiresAt(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build();
        
        try {
            sessionRepositoryPort.save(session);
        } catch (Exception e) {
            log.error("Failed to save session for user id={}: {}", user.getId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Failed to create session. Please try again.");
        }
        
        // Build response
        AuthResponse response = AuthResponse.builder()
                .accessToken(token)  // Use accessToken for consistency
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .isEmailVerified(user.getIsEmailVerified())
                        .isPhoneVerified(user.getIsPhoneVerified())
                        .role(user.getRole() != null ? user.getRole().name() : "USER")
                        .build())
                .build();
        
        log.info("OTP verification successful for user id={}, email={}", user.getId(), user.getEmail());
        return response;
        
        } catch (BusinessException e) {
            // Re-throw business exceptions (they're already properly formatted)
            log.warn("OTP verification failed with business exception: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Catch any unexpected exceptions and convert to BusinessException
            log.error("Unexpected error during OTP verification", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "An error occurred during OTP verification. Please try again.");
        }
    }
    
    private String generateReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generate a unique referral code, retrying if collision occurs.
     */
    private String generateUniqueReferralCode() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            String code = generateReferralCode();
            if (userPort.findByReferralCode(code).isEmpty()) {
                return code;
            }
        }
        // Fallback: use longer UUID if collisions persist (extremely unlikely)
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String buildName(RegisterRequest request) {
        String first = request.getFirstName();
        String last = request.getLastName();
        if (first != null && !first.isBlank() && last != null && !last.isBlank()) {
            return (first.trim() + " " + last.trim()).trim();
        }
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        // Fallback to email if no name provided
        return request.getEmail();
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token");
        }
        
        Session session = sessionRepositoryPort.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Session not found"));
        
        if (!session.getIsActive() || session.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Refresh token expired");
        }
        
        User user = userPort.findByEmail(jwtTokenProvider.getEmailFromToken(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
        
        // Generate new tokens with user role
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String newToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), role);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), role);
        
        // Update session
        session.setToken(newToken);
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(LocalDateTime.now().plusDays(1));
        session.setRefreshExpiresAt(LocalDateTime.now().plusDays(7));
        sessionRepositoryPort.save(session);
        
        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .isEmailVerified(user.getIsEmailVerified())
                        .isPhoneVerified(user.getIsPhoneVerified())
                        .role(user.getRole() != null ? user.getRole().name() : "USER")
                        .build())
                .build();
    }
    
    @Transactional
    public void logout(String token, HttpServletRequest httpRequest) {
        log.info("Logout request received");
        
        Session session = sessionRepositoryPort.findByToken(token)
                .orElse(null);
        
        if (session != null) {
            session.setIsActive(false);
            sessionRepositoryPort.save(session);
            
            // Log logout with user information
            String email = "unknown";
            Long userId = session.getUserId();
            
            try {
                // Try to extract email from JWT token
                if (token != null) {
                    try {
                        if (jwtTokenProvider.validateToken(token)) {
                            email = jwtTokenProvider.getEmailFromToken(token);
                        }
                    } catch (Exception e) {
                        log.debug("Token validation failed during logout, token may be expired: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract email from token during logout: {}", e.getMessage());
            }
            
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
            String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "unknown";
            
            log.info("Logout successful for user: {} (ID: {}) from IP: {}", email, userId, ipAddress);
        } else {
            log.warn("Logout attempt with invalid or expired token");
        }
    }
    
    /**
     * Register a new user with email and password (for admin registration).
     */
    @Transactional
    public AuthResponse registerWithPassword(RegisterRequest request) {
        // Check if user already exists
        if (userPort.existsByEmailIgnoreCase(request.getEmail())) {
            log.info("Registration blocked: email already exists (case-insensitive): {}", request.getEmail());
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "User with this email already exists");
        }
        
        // Generate unique referral code (handle collisions)
        String referralCode = generateUniqueReferralCode();
        
        // Create new user with hashed password
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(buildName(request))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .isEmailVerified(false) // Email verification can be added later
                .referralCode(referralCode)
                .role(User.UserRole.USER) // Default role, can be updated to ADMIN later
                .build();
        
        user = userPort.save(user);
        
        // Generate tokens with user role
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), role);
        
        // Create session
        Session session = Session.builder()
                .userId(user.getId())
                .token(token)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .refreshExpiresAt(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build();
        sessionRepositoryPort.save(session);
        
        // Build response
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .isEmailVerified(user.getIsEmailVerified())
                        .isPhoneVerified(user.getIsPhoneVerified())
                        .role(user.getRole() != null ? user.getRole().name() : "USER")
                        .build())
                .build();
    }
    
    /**
     * Login with email and password (for admin login).
     */
    @Transactional
    public AuthResponse loginWithPassword(LoginRequest request) {
        // Find user by email (case-insensitive)
        User user = userPort.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found with email: {}", request.getEmail());
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
                });
        
        log.info("Login attempt for user: {} (ID: {})", user.getEmail(), user.getId());
        
        // Check if user has a password set
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            log.warn("Login failed: Password not set for user: {}", user.getEmail());
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Password not set. Please use OTP login or reset your password.");
        }
        
        // Verify password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.debug("Password verification for user {}: {}", user.getEmail(), passwordMatches ? "MATCH" : "NO MATCH");
        
        if (!passwordMatches) {
            log.warn("Login failed: Invalid password for user: {}", user.getEmail());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }
        
        log.info("Login successful for user: {} (Role: {})", user.getEmail(), user.getRole());
        
        // Generate tokens with user role
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), role);
        
        // Create or update session
        Session session = sessionRepositoryPort.findByUserId(user.getId())
                .stream()
                .filter(s -> s.getIsActive())
                .findFirst()
                .orElse(null);
        
        if (session != null) {
            // Update existing session
            session.setToken(token);
            session.setRefreshToken(refreshToken);
            session.setExpiresAt(LocalDateTime.now().plusDays(1));
            session.setRefreshExpiresAt(LocalDateTime.now().plusDays(7));
            session.setIsActive(true);
        } else {
            // Create new session
            session = Session.builder()
                    .userId(user.getId())
                    .token(token)
                    .refreshToken(refreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .refreshExpiresAt(LocalDateTime.now().plusDays(7))
                    .isActive(true)
                    .build();
        }
        sessionRepositoryPort.save(session);
        
        // Build response
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .isEmailVerified(user.getIsEmailVerified())
                        .isPhoneVerified(user.getIsPhoneVerified())
                        .role(user.getRole() != null ? user.getRole().name() : "USER")
                        .build())
                .build();
    }
    
    /**
     * Create initial admin user (one-time setup).
     * Email: admin@ttelgo.com, Password: Admin@123456
     */
    /**
     * Google OAuth login.
     * Verifies Google ID token and creates/finds user.
     * 
     * @param request Google login request with idToken
     * @return AuthResponse with JWT access token, refresh token, and user info
     */
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        log.info("Google login request received");
        
        // Verify Google ID token
        Optional<GoogleIdToken> googleTokenOpt = googleOAuthService.verifyIdToken(request.getIdToken());
        if (googleTokenOpt.isEmpty()) {
            log.warn("Google login failed: Invalid Google ID token");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid Google ID token");
        }
        
        GoogleIdToken googleToken = googleTokenOpt.get();
        
        // Extract user information from Google token
        String email = googleOAuthService.getEmail(googleToken);
        String name = googleOAuthService.getName(googleToken);
        String sub = googleOAuthService.getSubject(googleToken); // providerId
        String givenName = googleOAuthService.getGivenName(googleToken);
        String familyName = googleOAuthService.getFamilyName(googleToken);
        Boolean emailVerified = googleOAuthService.getEmailVerified(googleToken);
        
        // Validate required fields
        if (email == null || email.isEmpty()) {
            log.warn("Google login failed: Email not found in Google token");
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Email not found in Google token");
        }
        
        if (sub == null || sub.isEmpty()) {
            log.warn("Google login failed: Subject (providerId) not found in Google token");
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Subject (providerId) not found in Google token");
        }
        
        // Check email verification status - reject if email is not verified
        if (emailVerified == null || !emailVerified) {
            log.warn("Google login rejected: Email not verified for email={}", email);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Email not verified. Please verify your email with Google before signing in.");
        }
        
        log.info("Google token verified successfully: email={}, emailVerified={}, sub={}", email, emailVerified, sub);
        
        // Find or create user
        User user = userPort.findByEmailIgnoreCase(email).orElse(null);
        
        if (user == null) {
            // Create new user with Google provider
            user = User.builder()
                    .email(email.trim().toLowerCase())
                    .name(name != null ? name : email)
                    .firstName(givenName)
                    .lastName(familyName)
                    .provider(User.AuthProvider.GOOGLE)
                    .providerId(sub)
                    .isEmailVerified(true) // Google emails are verified
                    .referralCode(generateUniqueReferralCode())
                    .role(User.UserRole.USER)
                    .userType(User.UserType.CUSTOMER)
                    .build();
            user = userPort.save(user);
            log.info("Created new user via Google OAuth: email={}, name={}, providerId={}", email, name, sub);
        } else {
            // Update existing user with Google provider info if not set
            if (user.getProvider() == null || user.getProvider() != User.AuthProvider.GOOGLE) {
                user.setProvider(User.AuthProvider.GOOGLE);
            }
            if (sub != null && (user.getProviderId() == null || user.getProviderId().isEmpty())) {
                user.setProviderId(sub);
            }
            if (name != null && (user.getName() == null || user.getName().isEmpty())) {
                user.setName(name);
            }
            if (givenName != null && (user.getFirstName() == null || user.getFirstName().isEmpty())) {
                user.setFirstName(givenName);
            }
            if (familyName != null && (user.getLastName() == null || user.getLastName().isEmpty())) {
                user.setLastName(familyName);
            }
            // Mark email as verified since Google verified it
            user.setIsEmailVerified(true);
            user = userPort.save(user);
            log.info("User logged in via Google OAuth: email={}, id={}", email, user.getId());
        }
        
        // Generate JWT access token and refresh token
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String userType = user.getUserType() != null ? user.getUserType().name() : "CUSTOMER";
        
        // Generate access token with 24 hours expiry (86400000 milliseconds)
        Long twentyFourHoursInMillis = 86400000L;
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
            twentyFourHoursInMillis
        );
        
        // Generate refresh token with 7 days expiry
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), role);
        
        // Create or update session
        Session session = sessionRepositoryPort.findByUserId(user.getId())
                .stream()
                .filter(s -> s.getIsActive())
                .findFirst()
                .orElse(null);
        
        if (session != null) {
            // Update existing session
            session.setToken(accessToken);
            session.setRefreshToken(refreshToken);
            session.setExpiresAt(LocalDateTime.now().plusDays(1));
            session.setRefreshExpiresAt(LocalDateTime.now().plusDays(7));
            session.setIsActive(true);
        } else {
            // Create new session
            session = Session.builder()
                    .userId(user.getId())
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .refreshExpiresAt(LocalDateTime.now().plusDays(7))
                    .isActive(true)
                    .build();
        }
        sessionRepositoryPort.save(session);
        
        // Build response with accessToken, refreshToken, and user details
        String fullName = user.getName();
        if (fullName == null || fullName.isEmpty()) {
            // Construct fullName from firstName and lastName if name is not set
            if (user.getFirstName() != null || user.getLastName() != null) {
                fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                          (user.getFirstName() != null && user.getLastName() != null ? " " : "") +
                          (user.getLastName() != null ? user.getLastName() : "");
            } else {
                fullName = email; // Fallback to email if no name available
            }
        }
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(twentyFourHoursInMillis)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(fullName)
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phone(user.getPhone())
                        .isEmailVerified(user.getIsEmailVerified())
                        .isPhoneVerified(user.getIsPhoneVerified())
                        .role(role)
                        .build())
                .build();
    }
    
    @Transactional
    public void createInitialAdminUser() {
        String email = "admin@ttelgo.com";
        String password = "Admin@123456";
        
        // Check if admin user already exists
        Optional<User> existingUser = userPort.findByEmailIgnoreCase(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Update password and role if needed
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(User.UserRole.ADMIN);
            user.setName("Admin User");
            user.setFirstName("Admin");
            user.setLastName("User");
            user.setIsEmailVerified(true);
            userPort.save(user);
            log.info("Updated existing admin user: {}", email);
            return;
        }
        
        // Create new admin user
        String referralCode = generateUniqueReferralCode();
        User adminUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name("Admin User")
                .firstName("Admin")
                .lastName("User")
                .isEmailVerified(true)
                .referralCode(referralCode)
                .role(User.UserRole.ADMIN)
                .build();
        
        userPort.save(adminUser);
        log.info("Created initial admin user: {}", email);
    }
    
    /**
     * Apple Sign-In authentication.
     * Verifies Apple identity token and creates/logs in user.
     * 
     * Handles the case where email is only provided on first login.
     * Finds user by Apple user ID (sub) or email.
     * 
     * @param identityToken Apple identity token (JWT string)
     * @return AuthResponse with JWT access token, refresh token, and user details
     */
    @Transactional
    public AuthResponse appleLogin(String identityToken) {
        log.info("Apple login request received");
        
        // Validate input
        if (identityToken == null || identityToken.trim().isEmpty()) {
            log.warn("Apple login failed: identityToken is null or empty");
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Apple identity token is required");
        }
        
        // Verify Apple identity token (validates signature, issuer, audience, expiration)
        Optional<JWTClaimsSet> appleTokenOpt = appleOAuthService.verifyIdentityToken(identityToken);
        if (appleTokenOpt.isEmpty()) {
            log.warn("Apple login failed: Invalid Apple identity token");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, 
                    "Invalid Apple identity token. Please ensure the token is valid and not expired.");
        }
        
        JWTClaimsSet appleToken = appleTokenOpt.get();
        
        // Extract user information from Apple token
        String email = appleOAuthService.getEmail(appleToken);
        String sub = appleOAuthService.getSubject(appleToken); // Apple user ID (providerId)
        AppleOAuthService.Name name = appleOAuthService.getName(appleToken);
        
        // Validate required fields
        if (sub == null || sub.isEmpty()) {
            log.warn("Apple login failed: Subject (providerId) not found in Apple token");
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "Subject (Apple user ID) not found in Apple token");
        }
        
        log.info("Apple token verified successfully: sub={}, email={}, hasName={}", 
                sub, email != null ? "provided" : "not provided", name != null);
        
        // Extract name information (only available on first login)
        String fullName = null;
        String firstName = null;
        String lastName = null;
        if (name != null) {
            fullName = name.getFullName();
            firstName = name.getFirstName();
            lastName = name.getLastName();
        }
        
        // Find user by Apple user ID (sub) or email
        // Apple only provides email on first login, so we must check by providerId
        User user = null;
        
        // First, try to find by providerId (most reliable for Apple)
        user = userPort.findByProviderAndProviderId(User.AuthProvider.APPLE, sub).orElse(null);
        
        // If not found by providerId, try by email (if provided)
        if (user == null && email != null && !email.trim().isEmpty()) {
            user = userPort.findByEmailIgnoreCase(email.trim()).orElse(null);
        }
        
        // Create or update user
        if (user == null) {
            // Create new user with Apple provider
            // Handle case where email is not provided (subsequent logins)
            String userEmail;
            boolean isEmailVerified = false;
            
            if (email != null && !email.trim().isEmpty()) {
                userEmail = email.trim().toLowerCase();
                isEmailVerified = true; // Apple provides verified emails
            } else {
                // Apple may not provide email on subsequent logins
                // Use a placeholder email based on providerId
                userEmail = sub + "@apple.private";
                log.info("Apple email not provided, using placeholder: {}", userEmail);
            }
            
            user = User.builder()
                    .email(userEmail)
                    .name(fullName != null ? fullName : (userEmail.contains("@apple.private") ? "Apple User" : userEmail))
                    .firstName(firstName)
                    .lastName(lastName)
                    .provider(User.AuthProvider.APPLE)
                    .providerId(sub)
                    .isEmailVerified(isEmailVerified)
                    .referralCode(generateUniqueReferralCode())
                    .role(User.UserRole.USER)
                    .userType(User.UserType.CUSTOMER)
                    .build();
            user = userPort.save(user);
            log.info("Created new user via Apple Sign-In: id={}, email={}, providerId={}", 
                    user.getId(), userEmail, sub);
        } else {
            // Update existing user with Apple provider info if not set
            boolean updated = false;
            
            if (user.getProvider() == null || user.getProvider() != User.AuthProvider.APPLE) {
                user.setProvider(User.AuthProvider.APPLE);
                updated = true;
            }
            if (sub != null && (user.getProviderId() == null || user.getProviderId().isEmpty())) {
                user.setProviderId(sub);
                updated = true;
            }
            
            // Update name if provided (only on first login)
            if (fullName != null && (user.getName() == null || user.getName().isEmpty())) {
                user.setName(fullName);
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
            
            // Update email if it was a placeholder and we now have a real email
            if (email != null && !email.trim().isEmpty() && !email.endsWith("@apple.private")) {
                String normalizedEmail = email.trim().toLowerCase();
                if (user.getEmail() == null || user.getEmail().endsWith("@apple.private")) {
                    user.setEmail(normalizedEmail);
                    user.setIsEmailVerified(true);
                    updated = true;
                } else if (!user.getEmail().equalsIgnoreCase(normalizedEmail)) {
                    // Email changed - update it
                    user.setEmail(normalizedEmail);
                    user.setIsEmailVerified(true);
                    updated = true;
                }
            }
            
            if (updated) {
                user = userPort.save(user);
                log.info("Updated user via Apple Sign-In: id={}, email={}", user.getId(), user.getEmail());
            } else {
                log.info("User logged in via Apple Sign-In: id={}, email={}", user.getId(), user.getEmail());
            }
        }
        
        // Generate JWT access token and refresh token
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String userType = user.getUserType() != null ? user.getUserType().name() : "CUSTOMER";
        
        // Generate access token with 24 hours expiry (86400000 milliseconds)
        Long twentyFourHoursInMillis = 86400000L;
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
            twentyFourHoursInMillis
        );
        
        // Generate refresh token with 7 days expiry
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail(), role);
        
        // Create or update session
        Session session = sessionRepositoryPort.findByUserId(user.getId())
                .stream()
                .filter(s -> s.getIsActive())
                .findFirst()
                .orElse(null);
        
        if (session != null) {
            // Update existing session
            session.setToken(accessToken);
            session.setRefreshToken(refreshToken);
            session.setExpiresAt(LocalDateTime.now().plusDays(1));
            session.setRefreshExpiresAt(LocalDateTime.now().plusDays(7));
            session.setIsActive(true);
        } else {
            // Create new session
            session = Session.builder()
                    .userId(user.getId())
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .refreshExpiresAt(LocalDateTime.now().plusDays(7))
                    .isActive(true)
                    .build();
        }
        sessionRepositoryPort.save(session);
        
        // Build full name for response
        String userFullName = user.getName();
        if (userFullName == null || userFullName.isEmpty()) {
            // Construct fullName from firstName and lastName if name is not set
            if (user.getFirstName() != null || user.getLastName() != null) {
                userFullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                              (user.getFirstName() != null && user.getLastName() != null ? " " : "") +
                              (user.getLastName() != null ? user.getLastName() : "");
            } else {
                // Fallback to email or "Apple User"
                userFullName = user.getEmail() != null && !user.getEmail().endsWith("@apple.private") 
                        ? user.getEmail() 
                        : "Apple User";
            }
        }
        
        // Build response with accessToken, refreshToken, and complete user details
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(twentyFourHoursInMillis)
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
     * Send OTP to email for email-based authentication.
     * 
     * @param email Email address
     */
    @Transactional
    public void sendEmailOtp(String email) {
        log.info("Email OTP request received for: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Email is required");
        }
        
        email = email.trim().toLowerCase();
        
        // Generate and send OTP
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail(email);
        otpRequest.setPurpose("LOGIN");
        
        requestOtp(otpRequest);
        log.info("Email OTP sent successfully to: {}", email);
    }
    
    /**
     * Verify OTP for email-based authentication.
     * Creates user if not exists (provider = EMAIL).
     * 
     * @param email Email address
     * @param otp OTP code
     * @return AuthResponse with JWT token and user info
     */
    @Transactional
    public AuthResponse verifyEmailOtp(String email, String otp) {
        log.info("Email OTP verification request received for: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Email is required");
        }
        
        if (otp == null || otp.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "OTP is required");
        }
        
        email = email.trim().toLowerCase();
        otp = otp.trim();
        
        // Verify OTP
        OtpVerifyRequest verifyRequest = new OtpVerifyRequest();
        verifyRequest.setEmail(email);
        verifyRequest.setOtp(otp);
        
        // Find OTP token
        Optional<OtpToken> otpTokenOpt = otpTokenRepository.findByEmailAndIsUsedFalse(email)
                .stream()
                .findFirst();
        
        if (otpTokenOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "OTP not found or already used");
        }
        
        OtpToken otpToken = otpTokenOpt.get();
        
        // Check if OTP is expired
        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "OTP has expired");
        }
        
        // Check attempts
        if (otpToken.getAttempts() >= otpToken.getMaxAttempts()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Maximum OTP verification attempts exceeded");
        }
        
        // Verify OTP (compare hashed OTP)
        otpToken.setAttempts(otpToken.getAttempts() + 1);
        if (!passwordEncoder.matches(otp, otpToken.getOtpCode())) {
            otpTokenRepository.save(otpToken);
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Invalid OTP");
        }
        
        // Mark OTP as used
        otpToken.setIsUsed(true);
        otpTokenRepository.save(otpToken);
        
        // Find or create user
        User user = userPort.findByEmailIgnoreCase(email).orElse(null);
        
        if (user == null) {
            // Create new user with EMAIL provider
            user = User.builder()
                    .email(email)
                    .name(email) // Use email as name initially
                    .provider(User.AuthProvider.EMAIL)
                    .isEmailVerified(true) // OTP verification confirms email
                    .referralCode(generateUniqueReferralCode())
                    .role(User.UserRole.USER)
                    .userType(User.UserType.CUSTOMER)
                    .build();
            user = userPort.save(user);
            log.info("Created new user via Email OTP: email={}", email);
        } else {
            // Update existing user if needed
            if (user.getProvider() == null) {
                user.setProvider(User.AuthProvider.EMAIL);
            }
            user.setIsEmailVerified(true);
            user = userPort.save(user);
            log.info("User logged in via Email OTP: email={}, id={}", email, user.getId());
        }
        
        // Generate JWT token with 24 hours expiry
        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String userType = user.getUserType() != null ? user.getUserType().name() : "CUSTOMER";
        
        // Generate token with 24 hours expiry (86400000 milliseconds)
        Long twentyFourHoursInMillis = 86400000L;
        String token = jwtTokenProvider.generateTokenWithUserInfoAndCustomExpiration(
            user.getId(),
            user.getEmail(),
            user.getPhone(),
            user.getFirstName(),
            user.getLastName(),
            role,
            userType,
            user.getIsEmailVerified(),
            user.getIsPhoneVerified(),
            twentyFourHoursInMillis
        );
        
        // Build response matching requirements: { token, user: { id, email, fullName } }
        String fullName = user.getName();
        if (fullName == null || fullName.isEmpty()) {
            // Construct fullName from firstName and lastName if name is not set
            if (user.getFirstName() != null || user.getLastName() != null) {
                fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                          (user.getFirstName() != null && user.getLastName() != null ? " " : "") +
                          (user.getLastName() != null ? user.getLastName() : "");
            } else {
                fullName = user.getEmail(); // Fallback to email if no name available
            }
        }
        
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(twentyFourHoursInMillis)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(fullName) // Using name field for fullName
                        .build())
                .build();
    }
}

