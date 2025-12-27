package com.tiktel.ttelgo.auth.infrastructure.repository;

import com.tiktel.ttelgo.auth.domain.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByEmailAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(String email, String otpCode, LocalDateTime now);
    Optional<OtpToken> findByPhoneAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(String phone, String otpCode, LocalDateTime now);
    List<OtpToken> findByEmailAndIsUsedFalse(String email);
    List<OtpToken> findByPhoneAndIsUsedFalse(String phone);
    void deleteByExpiresAtBefore(LocalDateTime now);
}

