package com.tiktel.ttelgo.security;

import com.tiktel.ttelgo.user.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * UserPrincipal implements Spring Security's UserDetails interface.
 * This class wraps the User domain entity and provides Spring Security with
 * the necessary user information for authentication and authorization.
 */
public class UserPrincipal implements UserDetails {
    
    private final Long id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    
    public UserPrincipal(Long id, String email, String password, 
                        Collection<? extends GrantedAuthority> authorities, boolean enabled) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }
    
    /**
     * Create UserPrincipal from User entity.
     * Maps UserRole enum to Spring Security authorities (ROLE_USER, ROLE_ADMIN, ROLE_SUPER_ADMIN).
     */
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        
        return new UserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getPassword() != null ? user.getPassword() : "", // Password may be null for OTP-based auth
            authorities,
            true // Enabled by default - can be extended with isActive flag if needed
        );
    }
    
    public Long getId() {
        return id;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    public String getEmail() {
        return email;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

