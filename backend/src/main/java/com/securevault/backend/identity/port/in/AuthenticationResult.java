package com.securevault.backend.identity.port.in;

import java.time.Instant;

public record AuthenticationResult(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt
) {}