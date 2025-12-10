package com.tiktel.ttelgo.user.application.port;

import com.tiktel.ttelgo.user.domain.User;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    User save(User user);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}

