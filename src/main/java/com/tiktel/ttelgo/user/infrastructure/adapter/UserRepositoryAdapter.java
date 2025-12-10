package com.tiktel.ttelgo.user.infrastructure.adapter;

import com.tiktel.ttelgo.user.application.port.UserRepositoryPort;
import com.tiktel.ttelgo.user.domain.User;
import com.tiktel.ttelgo.user.infrastructure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {
    
    private final UserRepository userRepository;
    
    @Autowired
    public UserRepositoryAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }
    
    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }
}

