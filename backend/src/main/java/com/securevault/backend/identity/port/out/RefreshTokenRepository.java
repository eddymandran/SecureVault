package com.securevault.backend.identity.port.out;

import com.securevault.backend.identity.domain.model.RefreshToken;
import com.securevault.backend.identity.domain.model.RefreshTokenId;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void revoke(RefreshTokenId id);
}
