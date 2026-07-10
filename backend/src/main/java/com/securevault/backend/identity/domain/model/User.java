package com.securevault.backend.identity.domain.model;

import com.securevault.backend.shared.UserId;
import com.securevault.backend.identity.port.out.PasswordHasher;

import java.time.Instant;
import java.util.Objects;

public class User {

    private final UserId id;
    private Email email;
    private HashedPassword password;
    private final Instant createdAt;
    private boolean enabled;

    public User(UserId id, Email email, HashedPassword password, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.password = Objects.requireNonNull(password);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.enabled = true;
    }

    public boolean verifyPassword(String rawPassword, PasswordHasher hasher) {
        if (!enabled) {
            return false;
        }
        return hasher.matches(rawPassword, password.value());
    }

    public void disable() {
        this.enabled = false;
    }

    public UserId getId() { return id; }
    public Email getEmail() { return email; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
}