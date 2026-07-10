package com.securevault.backend.identity.domain.model;

import com.securevault.backend.shared.UserId;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class RefreshToken {

    private final RefreshTokenId id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;

    public RefreshToken(RefreshTokenId id, UserId userId, String tokenHash, Instant expiresAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.revokedAt = null;
    }

    public boolean isValid(Clock clock) {
        return revokedAt == null && Instant.now(clock).isBefore(expiresAt);
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public RefreshTokenId getId() { return id; }
    public UserId getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public boolean isRevoked() { return revokedAt != null; }
}