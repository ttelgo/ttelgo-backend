package com.tiktel.ttelgo.auth.application;

import com.tiktel.ttelgo.auth.api.dto.AuthResponse;
import com.tiktel.ttelgo.auth.api.dto.OtpRequest;
import com.tiktel.ttelgo.auth.api.dto.OtpVerifyRequest;
import com.tiktel.ttelgo.auth.application.port.OtpServicePort;
import com.tiktel.ttelgo.auth.application.port.SessionRepositoryPort;
import com.tiktel.ttelgo.auth.application.port.UserPort;
import com.tiktel.ttelgo.auth.domain.OtpToken;
import com.tiktel.ttelgo.auth.domain.Session;
import com.tiktel.ttelgo.auth.infrastructure.repository.OtpTokenRepository;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.security.JwtTokenProvider;
import com.tiktel.ttelgo.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
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
                    .orElseThrow(() -> new BusinessException("Invalid or expired OTP"));
        } else if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            otpToken = otpTokenRepository
                    .findByPhoneAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                            request.getPhone(), request.getOtp(), now)
                    .orElseThrow(() -> new BusinessException("Invalid or expired OTP"));
        } else {
            throw new BusinessException("Email or phone is required");
        }
        
        // Check attempts
        if (otpToken.getAttempts() >= otpToken.getMaxAttempts()) {
            throw new BusinessException("Maximum OTP attempts exceeded");
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
            throw new BusinessException("User not found. Please register first.");
        }
        
        // Generate tokens
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        
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
                        .build())
                .build();
    }
    
    private String generateReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("Invalid refresh token");
        }
        
        Session session = sessionRepositoryPort.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException("Session not found"));
        
        if (!session.getIsActive() || session.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token expired");
        }
        
        User user = userPort.findByEmail(jwtTokenProvider.getEmailFromToken(refreshToken))
                .orElseThrow(() -> new BusinessException("User not found"));
        
        // Generate new tokens
        String newToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        
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
}

