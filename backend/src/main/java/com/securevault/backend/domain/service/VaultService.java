package com.securevault.backend.domain.service;

import com.securevault.backend.domain.model.Vault;
import com.securevault.backend.domain.port.in.VaultUseCase;
import com.securevault.backend.domain.port.out.VaultRepository;

import java.util.UUID;

public class VaultService implements VaultUseCase {

    private final VaultRepository vaultRepository;

    public VaultService(VaultRepository vaultRepository) {
        this.vaultRepository = vaultRepository;
    }

    @Override
    public Vault createVault(UUID ownerId, String name, String description) {
        Vault vault = Vault.create(ownerId, name, description);
        return vaultRepository.save(vault);
    }

    @Override
    public Vault findById(UUID vaultId) {
        return vaultRepository.findById(vaultId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vault not found: " + vaultId));
    }
}