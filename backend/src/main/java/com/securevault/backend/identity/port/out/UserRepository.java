package com.securevault.backend.identity.port.out;

import com.securevault.backend.identity.domain.model.Email;
import com.securevault.backend.identity.domain.model.User;
import com.securevault.backend.shared.UserId;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(Email email);
    Optional<User> findById(UserId id);
}
