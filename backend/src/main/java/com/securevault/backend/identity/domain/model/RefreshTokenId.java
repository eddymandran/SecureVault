package com.securevault.backend.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

public record RefreshTokenId(UUID value) {

    public RefreshTokenId {
        Objects.requireNonNull(value, "RefreshTokenId value must not be null");
    }

    public static RefreshTokenId generate() {
        return new RefreshTokenId(UUID.randomUUID());
    }

    public static RefreshTokenId of(String value) {
        return new RefreshTokenId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
