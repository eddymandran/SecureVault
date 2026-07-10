package com.securevault.backend.vault.infrastructure.persistence;

import com.securevault.backend.vault.domain.model.Vault;
import org.springframework.stereotype.Component;

@Component
public class VaultMapper {

    public VaultJpaEntity toJpaEntity(Vault vault) {
        return new VaultJpaEntity(
                vault.getId(),
                vault.getOwnerId(),
                vault.getName(),
                vault.getDescription(),
                vault.getCreatedAt(),
                vault.getUpdatedAt()
        );
    }

    public Vault toDomain(VaultJpaEntity entity) {
        return Vault.reconstitute(
                entity.getId(),
                entity.getOwnerId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}