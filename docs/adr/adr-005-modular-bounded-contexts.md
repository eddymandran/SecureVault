# ADR-005 : Organisation modulaire par bounded context (identity / vault / shared)

## Statut
Accepté

## Contexte

Le projet contient au moins deux domaines métier bien distincts :
- **identity** : authentification, gestion des utilisateurs
- **vault** : coffres de secrets chiffrés, partage, permissions

Une première tentative de code avait tout regroupé sous un seul
package `domain` (`domain/model`, `domain/port`, `domain/service`),
mélangeant les classes des deux domaines au même niveau. Ça posait
plusieurs problèmes concrets :
- Rien n'empêchait techniquement une classe `identity` d'importer une
  classe `vault`, ou l'inverse — risque de couplage caché.
- Impossible de faire évoluer un domaine sans risquer d'impacter l'autre.
- Un nouveau développeur ne pouvait pas voir, juste en lisant l'arbre
  de fichiers, où s'arrête un domaine et où commence l'autre.

## Décision

**Premier niveau de package = le bounded context** (le domaine métier),
**deuxième niveau = la couche hexagonale** :

com.securevault.backend
  identity/
    domain/model/          → entités et value objects du domaine
    domain/exception/       → exceptions métier spécifiques à identity
    port/in/                 → interfaces des use cases (ce qu'on peut demander à identity)
    port/out/                → interfaces vers l'extérieur (repository, hasher, jwt signer...)
    service/                  → implémentation des use cases
    infrastructure/            → adapters concrets (JPA, BCrypt, JWT...)

  vault/
    domain/model/
    port/in/, port/out/
    service/
    infrastructure/

  shared/
    domain/UserId.java
    domain/exception/DomainException.java

**Règle de référence entre agrégats de bounded contexts différents :
toujours par ID, jamais par objet.**

Exemple : `Vault` a un champ `ownerId` de type `UserId`, pas un champ
`owner` de type `User`. `Vault` ne connaît donc jamais l'objet `User`
complet — juste un identifiant opaque. Pour retrouver les infos d'un
owner, on passe explicitement par `UserRepository.findById(...)`,
jamais par navigation objet automatique (pas de `@ManyToOne` direct
entre agrégats de domaines différents).

Pourquoi : ça évite le couplage bidirectionnel entre `identity` et
`vault`, réduit la taille des objets chargés en mémoire (pas de
cascade JPA ramenant des données non nécessaires), et anticipe
l'évolution vers un modèle de partage de secrets plus complexe
(many-to-many avec permissions), qui aurait cassé une relation
objet directe.

**Ce qui va dans `shared/` — deux catégories différentes, à ne pas
confondre :**

1. **Concepts métier réellement partagés entre plusieurs domaines**
   (ex : `UserId`, utilisé à la fois par `identity` pour désigner un
   `User` et par `vault` pour désigner un `ownerId`). Critère : les
   deux domaines parlent du même concept du monde réel.
2. **Mécanismes techniques transverses**, vides de sens métier
   (ex : `DomainException`, classe abstraite dont héritent toutes les
   exceptions métier de tous les domaines, pour permettre un traitement
   centralisé par un seul `@ControllerAdvice`). Critère : pas de lien
   de sens métier entre domaines, juste un contrat technique commun.

Rien n'est ajouté à `shared/` par anticipation spéculative — seulement
face à un besoin déjà identifié concrètement (principe YAGNI). C'est
pour cette même raison qu'un package `application/` vide, créé par
anticipation sans cas d'usage clair, a été supprimé du projet plutôt
que d'être conservé "au cas où".

## Conséquences

**Positif :**
- Impossible de créer un couplage caché entre `identity` et `vault`
  sans que ce soit visible dans les imports.
- Un nouveau développeur retrouve instantanément la frontière entre
  domaines juste en lisant l'arborescence.
- Prépare le terrain pour une éventuelle règle automatisée (ArchUnit,
  Phase 3) qui interdirait explicitement en CI tout import direct
  entre `identity` et `vault` hors `shared`.

**Négatif / compromis acceptés :**
- Un peu plus de verbosité : récupérer les infos d'un `User` depuis
  `vault` nécessite un appel repository explicite plutôt qu'une
  navigation objet directe.
- Discipline requise pour ne pas re-mélanger les domaines au fil du
  temps — la structure ne s'auto-applique pas sans vigilance humaine
  tant qu'aucun outil automatisé (type ArchUnit) ne la fait respecter.

## Dette technique identifiée
- Pas encore de test automatisé (ArchUnit ou équivalent) vérifiant
  que `identity` et `vault` ne s'importent jamais mutuellement — à
  prévoir en Phase 3 (CI/CD).