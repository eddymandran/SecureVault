package com.securevault.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VaultJpaRepository extends JpaRepository<VaultJpaEntity, UUID> {
}