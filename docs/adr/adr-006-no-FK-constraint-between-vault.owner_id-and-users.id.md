# ADR-006 : Pas de contrainte FK entre `vaults.owner_id` et `users.id`

## Statut
Accepté

## Contexte

Le module `vault` référence un propriétaire via `Vault.ownerId`
(type `UserId`, cf. ADR-005 — référence par ID, jamais par objet,
entre bounded contexts différents).

Cette règle a été prise au niveau du code applicatif, mais la
question se repose à l'identique au niveau du schéma SQL au moment
d'écrire les migrations Flyway : faut-il une contrainte
`FOREIGN KEY (owner_id) REFERENCES users(id)` sur la table `vaults` ?

Une FK classique semble être une évidence en SQL relationnel — elle
garantit qu'aucun vault ne peut exister avec un propriétaire
inexistant. Mais l'ajouter reviendrait à recréer, au niveau base de
données, exactement le couplage qu'ADR-005 a supprimé au niveau code :
les deux modules `identity` et `vault` devraient alors partager un
schéma cohérent et co-déployé, alors que rien dans le code ne les
oblige à vivre dans la même base.

`refresh_tokens.user_id`, à l'inverse, référence bien `users.id` avec
une FK classique — mais `refresh_tokens` vit *dans* `identity`, donc
le couplage y est interne au même bounded context, pas entre deux
bounded contexts différents.

## Décision

`vaults.owner_id` reste un simple `UUID NOT NULL`, sans contrainte
`FOREIGN KEY` vers `users.id`.

L'intégrité référentielle (garantir qu'un `ownerId` correspond à un
utilisateur réel) devient une responsabilité applicative, portée par
`vault`, et non plus une garantie automatique de la base de données.

C'est la même règle qu'ADR-005 (référence par ID, jamais par objet),
simplement appliquée un niveau plus bas : jusqu'au schéma SQL, pas
seulement jusqu'au code Java.

## Conséquences

**Positif :**
- `vault` et `identity` restent découplés jusqu'au niveau du schéma —
  cohérent avec ADR-005, pas juste au niveau des classes Java.
- Chaque module peut faire évoluer ses migrations Flyway (`V1`
  identity, `V2` vault) indépendamment, sans jamais risquer une
  contrainte croisée qui casserait l'autre module.
- Prépare le terrain pour une éventuelle séparation physique des deux
  modules en bases ou services distincts, sans migration disruptive.

**Négatif / compromis acceptés :**
- Postgres ne peut plus empêcher la création d'un vault avec un
  `ownerId` qui ne correspond à aucun utilisateur — cette garantie
  doit être assurée explicitement côté applicatif (`VaultService`),
  pas encore implémentée à ce stade.
- Aucune suppression en cascade automatique : si un utilisateur était
  un jour supprimé (aujourd'hui seul `disable()` existe, pas de
  suppression), ses vaults deviendraient orphelins silencieusement.
- Discipline requise : ce choix n'est pas auto-documenté par le schéma
  lui-même — sans cet ADR, un futur développeur pourrait "corriger"
  l'absence de FK en pensant réparer un oubli.

## Dette technique identifiée
- Pas de vérification applicative que `ownerId` correspond à un
  utilisateur existant au moment de la création d'un vault — à
  ajouter dans `VaultService.createVault`, potentiellement via un
  nouveau port `vault/port/out` (ex. `UserExistenceChecker`).
- Pas de mécanisme de nettoyage des vaults orphelins en cas de
  suppression d'un utilisateur — non urgent tant que `User` ne peut
  être que désactivé, jamais supprimé. À traiter si une fonctionnalité
  de suppression de compte est introduite.
- Comme pour ADR-005 : pas encore de test ArchUnit (ou équivalent)
  vérifiant qu'aucune FK croisée n'est réintroduite par erreur dans
  une future migration — à prévoir Phase 3 (CI/CD), potentiellement
  dans le même effort que la règle d'imports croisés déjà notée dans
  ADR-005.