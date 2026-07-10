package com.securevault.backend.identity.port.in;

public interface LogoutUseCase {
    void logout(String rawRefreshToken);

}
