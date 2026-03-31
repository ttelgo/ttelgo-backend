package com.tiktel.ttelgo.auth.application;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingCustomerSignupStore {

    public static final int OTP_TTL_MINUTES = 5;

    private final ConcurrentHashMap<String, PendingSignup> byEmail = new ConcurrentHashMap<>();

    public void put(String email, String username, String passwordHash) {
        String key = normalizeEmail(email);
        byEmail.put(key, new PendingSignup(username, passwordHash, LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES)));
    }

    public Optional<PendingSignup> getValid(String email) {
        String key = normalizeEmail(email);
        PendingSignup pending = byEmail.get(key);
        if (pending == null) {
            return Optional.empty();
        }
        if (pending.expiresAt().isBefore(LocalDateTime.now())) {
            byEmail.remove(key);
            return Optional.empty();
        }
        return Optional.of(pending);
    }

    public void remove(String email) {
        byEmail.remove(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record PendingSignup(String username, String passwordHash, LocalDateTime expiresAt) {}
}
