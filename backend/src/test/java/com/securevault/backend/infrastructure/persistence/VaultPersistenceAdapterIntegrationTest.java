package com.securevault.backend.infrastructure.persistence;

import com.securevault.backend.AbstractIntegrationTest;
import com.securevault.backend.domain.model.Vault;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VaultPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {


    @Autowired
    private VaultPersistenceAdapter adapter;

    @Test
    void should_save_and_retrieve_vault() {
        // Arrange
        Vault vault = Vault.create(UUID.randomUUID(), "My first vault", "Personal secrets");

        // Act
        Vault saved = adapter.save(vault);
        Optional<Vault> found = adapter.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(vault.getId());
        assertThat(found.get().getName()).isEqualTo("My first vault");
    }
}