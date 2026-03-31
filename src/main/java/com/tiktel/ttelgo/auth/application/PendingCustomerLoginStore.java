package com.tiktel.ttelgo.auth.application;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingCustomerLoginStore {

    private static final int OTP_TTL_MINUTES = 5;
    private final ConcurrentHashMap<String, LocalDateTime> byEmail = new ConcurrentHashMap<>();

    public void put(String email) {
        byEmail.put(normalizeEmail(email), LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
    }

    public boolean isValid(String email) {
        String key = normalizeEmail(email);
        LocalDateTime expiry = byEmail.get(key);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(LocalDateTime.now())) {
            byEmail.remove(key);
            return false;
        }
        return true;
    }

    public void remove(String email) {
        byEmail.remove(normalizeEmail(email));
    }

    public Optional<LocalDateTime> getExpiry(String email) {
        return Optional.ofNullable(byEmail.get(normalizeEmail(email)));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
