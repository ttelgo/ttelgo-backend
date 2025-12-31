package com.tiktel.ttelgo.auth.infrastructure.adapter;

import com.tiktel.ttelgo.auth.application.port.UserPort;
import com.tiktel.ttelgo.user.application.port.UserRepositoryPort;
import com.tiktel.ttelgo.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPortAdapter implements UserPort {
    
    private final UserRepositoryPort userRepositoryPort;
    
    @Autowired
    public UserPortAdapter(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepositoryPort.findByEmail(email);
    }
    
    @Override
    public Optional<User> findByEmailIgnoreCase(String email) {
        return userRepositoryPort.findByEmailIgnoreCase(email);
    }
    
    @Override
    public Optional<User> findByPhone(String phone) {
        return userRepositoryPort.findByPhone(phone);
    }
    
    @Override
    public Optional<User> findByReferralCode(String referralCode) {
        return userRepositoryPort.findByReferralCode(referralCode);
    }
    
    @Override
    public User save(User user) {
        return userRepositoryPort.save(user);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepositoryPort.existsByEmail(email);
    }
    
    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return userRepositoryPort.existsByEmailIgnoreCase(email);
    }
    
    @Override
    public boolean existsByPhone(String phone) {
        return userRepositoryPort.existsByPhone(phone);
    }
}

