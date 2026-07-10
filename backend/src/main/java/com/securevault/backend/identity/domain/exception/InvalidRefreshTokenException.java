package com.securevault.backend.identity.domain.exception;

import com.securevault.backend.shared.domain.exception.DomainException;

public class InvalidRefreshTokenException extends DomainException {
    public InvalidRefreshTokenException() {
        super("Refresh token is invalid or expired");
    }
}