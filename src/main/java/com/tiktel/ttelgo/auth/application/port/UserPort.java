package com.tiktel.ttelgo.auth.application.port;

import com.tiktel.ttelgo.user.domain.User;

import java.util.Optional;

public interface UserPort {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByReferralCode(String referralCode);
    Optional<User> findByProviderId(String providerId);
    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);
    User save(User user);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);
}

