package com.tiktel.ttelgo.auth.infrastructure.adapter;

import com.tiktel.ttelgo.auth.application.port.SessionRepositoryPort;
import com.tiktel.ttelgo.auth.domain.Session;
import com.tiktel.ttelgo.auth.infrastructure.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SessionRepositoryAdapter implements SessionRepositoryPort {
    
    private final SessionRepository sessionRepository;
    
    @Autowired
    public SessionRepositoryAdapter(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }
    
    @Override
    public Session save(Session session) {
        return sessionRepository.save(session);
    }
    
    @Override
    public Optional<Session> findByToken(String token) {
        return sessionRepository.findByToken(token);
    }
    
    @Override
    public Optional<Session> findByRefreshToken(String refreshToken) {
        return sessionRepository.findByRefreshToken(refreshToken);
    }
    
    @Override
    public List<Session> findByUserId(Long userId) {
        return sessionRepository.findByUserId(userId);
    }
    
    @Override
    public void delete(Session session) {
        sessionRepository.delete(session);
    }
}

