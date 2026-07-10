package com.securevault.backend.vault.port.in;

import com.securevault.backend.vault.domain.model.Vault;
import java.util.UUID;

public interface VaultUseCase {
    Vault createVault(UUID ownerId, String name, String description);
    Vault findById(UUID vaultId);

}