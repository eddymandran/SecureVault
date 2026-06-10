# Architecture Hexagonale — Guide pour les nouveaux

## En une phrase

Le domaine métier ne sait pas qu'il tourne dans Spring,
ni qu'il y a une base de données.

## La métaphore

Pense à une télécommande universelle et une TV.

La télécommande (le domaine) définit les boutons : "sauvegarder un vault",
"récupérer un secret". Elle ne sait pas quelle TV répond.

La TV (l'infrastructure) implémente ces boutons à sa façon — aujourd'hui
PostgreSQL via JPA, demain potentiellement autre chose.

Spring est le modèle de TV. PostgreSQL aussi. Ce sont des détails
techniques interchangeables. Le domaine, lui, ne change pas.

## La règle d'or

Les dépendances vont toujours vers le centre, jamais vers l'extérieur.

**infrastructure → application → domain**

## Les trois couches

**`domain/`** — le cerveau, zéro dépendance externe

Contient les règles métier pures. Un vault a un nom et un propriétaire.
Un secret ne peut pas être vide. Ces règles sont vraies peu importe
le framework ou la base de données.

**`application/`** — l'orchestration

Contient les use cases. `CreateVaultUseCase` dit : vérifie que
l'utilisateur existe, crée le vault, notifie. Il orchestre sans
contenir de logique métier.

**`infrastructure/`** — les détails techniques

Spring, JPA, Azure, les controllers REST. Ce sont des adaptateurs
qui branchent le monde extérieur sur le domaine.

## Exemple concret

```java
// domain/port/out/ — l'interface que le domaine définit
public interface VaultRepository {
    Vault save(Vault vault);
    Optional<Vault> findById(UUID id);
}

// domain/service/ — logique métier pure, pas de Spring
public class VaultService {
    private final VaultRepository repository; // juste l'interface

    public Vault createVault(String name, User owner) {
        if (name == null || name.isBlank()) {
            throw new VaultNameEmptyException();
        }
        return repository.save(new Vault(name, owner));
    }
}

// infrastructure/persistence/ — l'implémentation JPA
@Repository
public class JpaVaultRepository implements VaultRepository {
    public Vault save(Vault vault) {
        // ici Spring Data JPA, Hibernate, etc.
    }
}
```

`VaultService` ne voit jamais `@Repository`, `@Entity`, ni Spring.
C'est du Java pur testable sans rien démarrer.

## Ce que tu ne dois jamais faire dans `domain/`

```java
import org.springframework.*; // ❌ Spring est infrastructure
import jakarta.persistence.*;  // ❌ JPA est infrastructure
```

## Pourquoi c'est important pour SecureVault

Ce projet manipule des secrets et credentials. Les règles de sécurité
(chiffrement obligatoire, contrôle des permissions, audit logging)
vivent dans le domaine. Elles sont ainsi testables, visibles, et
indépendantes de tout framework.

## Pour aller plus loin

- [Architecture Hexagonale — Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Ports & Adapters — Wikipedia](https://en.wikipedia.org/wiki/Hexagonal_architecture_(software))