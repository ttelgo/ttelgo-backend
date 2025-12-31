package com.tiktel.ttelgo.auth.infrastructure.repository;

import com.tiktel.ttelgo.auth.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByToken(String token);
    Optional<Session> findByRefreshToken(String refreshToken);
    List<Session> findByUserId(Long userId);
    List<Session> findByUserIdAndIsActiveTrue(Long userId);
    void deleteByUserId(Long userId);
}

