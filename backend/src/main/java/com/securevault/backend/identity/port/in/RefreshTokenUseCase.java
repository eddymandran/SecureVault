package com.securevault.backend.identity.port.in;

public interface RefreshTokenUseCase {
    AuthenticationResult refresh(String rawRefreshToken);

}
