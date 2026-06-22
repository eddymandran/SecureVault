package com.securevault.backend.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Vault {

    private final UUID id;
    private final UUID ownerId;
    private String name;
    private String description;
    private final Instant createdAt;
    private Instant updatedAt;

    // Constructeur de création — appelé quand on crée un nouveau vault
    public static Vault create(UUID ownerId, String name, String description) {
        return new Vault(
                UUID.randomUUID(),
                ownerId,
                name,
                description,
                Instant.now(),
                Instant.now()
        );
    }

    // Constructeur de reconstitution — appelé depuis la BDD
    public static Vault reconstitute(
            UUID id, UUID ownerId, String name,
            String description, Instant createdAt, Instant updatedAt) {
        return new Vault(id, ownerId, name, description, createdAt, updatedAt);
    }

    private Vault(UUID id, UUID ownerId, String name,
                  String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Comportement métier
    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Vault name cannot be blank");
        }
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    // Getters uniquement — pas de setters publics
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}