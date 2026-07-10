package com.securevault.backend.identity.domain.exception;

import com.securevault.backend.shared.domain.exception.DomainException;

public class AccountDisabledException extends DomainException {
    public AccountDisabledException() {
        super("Account disabled");
    }
}
