package com.securevault.backend.vault.infrastructure.persistence;

import com.securevault.backend.vault.domain.model.Vault;
import com.securevault.backend.vault.port.out.VaultRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class VaultPersistenceAdapter implements VaultRepository {

    private final VaultJpaRepository jpaRepository;
    private final VaultMapper mapper;

    public VaultPersistenceAdapter(VaultJpaRepository jpaRepository, VaultMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Vault save(Vault vault) {
        VaultJpaEntity entity = mapper.toJpaEntity(vault);
        VaultJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Vault> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
}