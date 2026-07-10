package com.securevault.backend.identity.domain.exception;

public class AccountDisabledException extends RuntimeException {
  public AccountDisabledException(String message) {
    super(message);
  }
}
