package com.securevault.backend.identity.port.out;

import com.securevault.backend.shared.UserId;

import java.util.Optional;

public interface JwtSigner {
    String generateAccessToken(UserId userId);
    Optional<UserId> validateAndExtract(String token);
}
