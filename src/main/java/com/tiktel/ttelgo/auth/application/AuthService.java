package com.tiktel.ttelgo.auth.application;

import com.tiktel.ttelgo.auth.api.dto.AuthResponse;
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
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.security.JwtTokenProvider;
import com.tiktel.ttelgo.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    
    @Autowired
    public AuthService(
            UserPort userPort,
            OtpServicePort otpServicePort,
            OtpTokenRepository otpTokenRepository,
            SessionRepositoryPort sessionRepositoryPort,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder) {
        this.userPort = userPort;
        this.otpServicePort = otpServicePort;
        this.otpTokenRepository = otpTokenRepository;
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional
    public void requestOtp(OtpRequest request) {
        String otp = otpServicePort.generateOtp();
        String purpose = request.getPurpose() != null ? request.getPurpose() : "LOGIN";
        
        // Invalidate previous unused OTPs
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            otpTokenRepository.findByEmailAndIsUsedFalse(request.getEmail())
                    .forEach(otpTokenRepository::delete);
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            otpTokenRepository.findByPhoneAndIsUsedFalse(request.getPhone())
                    .forEach(otpTokenRepository::delete);
        }
        
        // Create new OTP token
        OtpToken otpToken = OtpToken.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .otpCode(otp)
                .purpose(purpose)
                .isUsed(false)
                .attempts(0)
                .maxAttempts(3)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        
        otpTokenRepository.save(otpToken);
        
        // Send OTP
        otpServicePort.sendOtp(request.getEmail(), request.getPhone(), otp, purpose);
    }
    
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        LocalDateTime now = LocalDateTime.now();
        
        OtpToken otpToken = null;
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            otpToken = otpTokenRepository
                    .findByEmailAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                            request.getEmail(), request.getOtp(), now)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OTP, "Invalid or expired OTP"));
        } else if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            otpToken = otpTokenRepository
                    .findByPhoneAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                            request.getPhone(), request.getOtp(), now)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OTP, "Invalid or expired OTP"));
        } else {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Email or phone is required");
        }
        
        // Check attempts
        if (otpToken.getAttempts() >= otpToken.getMaxAttempts()) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED, "Maximum OTP attempts exceeded");
        }
        
        // Mark as used
        otpToken.setIsUsed(true);
        otpTokenRepository.save(otpToken);
        
        // Find or create user
        User user = null;
        if (otpToken.getEmail() != null && !otpToken.getEmail().isEmpty()) {
            user = userPort.findByEmail(otpToken.getEmail())
                    .orElse(null);
        } else if (otpToken.getPhone() != null && !otpToken.getPhone().isEmpty()) {
            user = userPort.findByPhone(otpToken.getPhone())
                    .orElse(null);
        }
        
        // If user doesn't exist and purpose is REGISTER, create new user
        if (user == null && "REGISTER".equals(otpToken.getPurpose())) {
            user = User.builder()
                    .email(otpToken.getEmail())
                    .phone(otpToken.getPhone())
                    .isEmailVerified(otpToken.getEmail() != null)
                    .isPhoneVerified(otpToken.getPhone() != null)
                    .referralCode(generateReferralCode())
                    .role(User.UserRole.USER)
                    .build();
            user = userPort.save(user);
        } else if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found. Please register first.");
        }
        
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
    public void logout(String token) {
        Session session = sessionRepositoryPort.findByToken(token)
                .orElse(null);
        if (session != null) {
            session.setIsActive(false);
            sessionRepositoryPort.save(session);
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
}

