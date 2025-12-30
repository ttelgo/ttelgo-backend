package com.tiktel.ttelgo.auth.application.port;

import com.tiktel.ttelgo.auth.domain.Session;

import java.util.List;
import java.util.Optional;

public interface SessionRepositoryPort {
    Session save(Session session);
    Optional<Session> findByToken(String token);
    Optional<Session> findByRefreshToken(String refreshToken);
    List<Session> findByUserId(Long userId);
    void delete(Session session);
}

