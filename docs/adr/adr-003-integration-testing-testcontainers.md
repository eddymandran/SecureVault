# ADR-003 : Tests d'intégration avec Testcontainers

## Statut
Acceptée

## Contexte
La couche persistence (VaultJpaEntity, VaultPersistenceAdapter) doit être validée contre une vraie base PostgreSQL, pas des mocks, pour détecter de vrais problèmes
de mapping JPA/SQL — un mock ne peut pas révéler une erreur de type de colonne ou de contrainte SQL mal définie.

## Décision
Utiliser Testcontainers avec un conteneur PostgreSQL unique ("singleton"), partagé par toutes les classes de test d'intégration, via une classe de base commune `AbstractIntegrationTest`.

## Détails d'implémentation clés

- **Pattern Singleton Container** : le conteneur est démarré une seule fois dans un bloc `static`, jamais arrêté explicitement (Ryuk s'en charge  automatiquement à la fermeture de la JVM). Utiliser `@Testcontainers`/`@Container` au niveau de chaque classe redémarre un conteneur à chaque fois, ce qui entre
  en conflit avec le cache de contexte Spring et provoque des connexions `DataSource` obsolètes pointant vers des conteneurs déjà arrêtés.
- **`@ServiceConnection`** : Spring Boot configure automatiquement la `DataSource` à partir du conteneur en cours d'exécution — plus besoin de
  `@DynamicPropertySource` manuel.
- **`ddl-auto: create-drop`** : uniquement dans `src/test/resources/application.yml`, jamais dans `src/main/resources`. Le schéma est régénéré à partir des entités
  JPA à chaque exécution de test — acceptable pour une base de test jetable, jamais pour la production (Flyway/Liquibase prévus en Phase 2).

## Problème d'environnement connu (juillet 2026)

Docker Engine 29 a relevé sa version minimale d'API supportée à 1.40+.
Les versions de Testcontainers antérieures à 2.0.2 retombent, dans certains chemins de négociation, sur une version d'API codée en dur à 1.32, provoquant :

**_client version 1.32 is too old. Minimum supported API version is 1.40_**

**Contournement en place** : `src/test/resources/docker-java.properties` avec `api.version=1.40` force le client à ignorer la négociation automatique.

**Suite à prévoir** : monter vers Testcontainers 2.0.x une fois la compatibilité avec `spring-boot-testcontainers` et Spring Boot 4.0.6 vérifiée. Suivre via https://github.com/testcontainers/testcontainers-java (issues #11210, #11235).

## Conséquences
- Les tests d'intégration sont plus lents (~15-20s pour le démarrage du conteneur) mais valident un comportement réel.
- Nécessite Docker disponible dans chaque environnement exécutant `mvn verify` (dev local, Jenkins). Documenté dans homelab-infra.