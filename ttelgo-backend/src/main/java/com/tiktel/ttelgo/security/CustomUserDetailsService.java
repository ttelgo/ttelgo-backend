package com.tiktel.ttelgo.security;

import com.tiktel.ttelgo.user.application.port.UserRepositoryPort;
import com.tiktel.ttelgo.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user details by email for authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepositoryPort userRepositoryPort;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
        
        log.debug("Loading user by email: {}, role: {}", email, user.getRole());
        return UserPrincipal.create(user);
    }
    
    /**
     * Load user details by user ID.
     * Used when we have the user ID from JWT token.
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UsernameNotFoundException("User not found with ID: " + userId);
                });
        
        log.debug("Loading user by ID: {}, role: {}", userId, user.getRole());
        return UserPrincipal.create(user);
    }
}

