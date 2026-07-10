package com.securevault.backend.identity.port.in;

public interface AuthenticateUserUseCase {
    AuthenticationResult authenticate(String email, String rawPassword);

}
