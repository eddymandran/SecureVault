package com.securevault.backend.domain.service;

import com.securevault.backend.domain.model.Vault;
import com.securevault.backend.domain.port.out.VaultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VaultService — tests unitaires")
class VaultServiceTest {

    @Mock
    private VaultRepository vaultRepository;

    private VaultService vaultService;

    @BeforeEach
    void setUp() {
        // Injection manuelle — pas de Spring, c'est un test unitaire pur
        vaultService = new VaultService(vaultRepository);
    }

    @Test
    @DisplayName("createVault — doit créer et sauvegarder un vault avec les bonnes données")
    void should_create_vault_with_correct_data() {
        // ARRANGE
        UUID ownerId = UUID.randomUUID();
        String name = "Mon vault perso";
        String description = "Mes credentials perso";

        when(vaultRepository.save(any(Vault.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        Vault result = vaultService.createVault(ownerId, name, description);

        // ASSERT
        assertThat(result.getId()).isNotNull();
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findById — doit retourner le vault quand il existe")
    void should_return_vault_when_found() {
        // ARRANGE
        UUID vaultId = UUID.randomUUID();
        Vault existing = Vault.create(UUID.randomUUID(), "Test", "desc");

        when(vaultRepository.findById(vaultId)).thenReturn(Optional.of(existing));

        // ACT
        Vault result = vaultService.findById(vaultId);

        // ASSERT
        assertThat(result).isEqualTo(existing);
    }

    @Test
    @DisplayName("findById — doit lever une exception quand le vault n'existe pas")
    void should_throw_when_vault_not_found() {
        // ARRANGE
        UUID vaultId = UUID.randomUUID();
        when(vaultRepository.findById(vaultId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> vaultService.findById(vaultId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(vaultId.toString());
    }

    @Test
    @DisplayName("rename — doit mettre à jour le nom et updatedAt")
    void should_rename_vault() {
        // ARRANGE
        Vault vault = Vault.create(UUID.randomUUID(), "Ancien nom", "desc");

        // ACT
        vault.rename("Nouveau nom");

        // ASSERT
        assertThat(vault.getName()).isEqualTo("Nouveau nom");
        assertThat(vault.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("rename — doit rejeter un nom vide")
    void should_reject_blank_name() {
        // ARRANGE
        Vault vault = Vault.create(UUID.randomUUID(), "Mon vault", "desc");

        // ACT & ASSERT
        assertThatThrownBy(() -> vault.rename("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }
}