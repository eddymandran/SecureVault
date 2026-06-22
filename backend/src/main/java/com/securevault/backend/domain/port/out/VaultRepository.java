package com.securevault.backend.domain.port.out;

import com.securevault.backend.domain.model.Vault;
import java.util.Optional;
import java.util.UUID;

public interface VaultRepository {
    Vault save(Vault vault);
    Optional<Vault> findById(UUID id);
}