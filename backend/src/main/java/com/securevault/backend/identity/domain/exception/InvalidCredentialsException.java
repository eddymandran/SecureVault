package com.securevault.backend.identity.domain.exception;

import com.securevault.backend.shared.domain.exception.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}