# ADR-004 : Stratégie d'authentification JWT (access + refresh token)

## Statut
Accepté

## Contexte

SecureVault gère des secrets sensibles (mots de passe, API keys, tokens).
L'authentification doit permettre à un utilisateur de rester connecté
sans se reconnecter en permanence, tout en limitant les risques si un
token venait à être volé (session détournée, XSS, log qui traîne...).

Un JWT classique ne peut pas être révoqué unilatéralement une fois émis
(il reste valide jusqu'à expiration, même si le serveur "change d'avis").
Il faut donc choisir une durée de vie qui équilibre confort utilisateur
et surface d'exposition en cas de vol.

## Décision

On utilise un système à deux tokens :

- **Access token** : JWT signé, durée de vie **10 minutes**. Envoyé à
  chaque requête API dans le header `Authorization`. Ne transite jamais
  vers un stockage persistant côté client (mémoire uniquement).
- **Refresh token** : chaîne aléatoire opaque (pas un JWT), durée de vie
  **30 jours**. Utilisé uniquement pour obtenir un nouvel access token
  via l'endpoint `/auth/refresh`.

**Algorithme de signature : RS256** (asymétrique), plutôt que HS256
(symétrique). Choix fait en anticipation de l'intégration OAuth2/OIDC
avec Azure AD et Google (Phase 2), qui utilisent eux-mêmes RS256 —
cohérence dès maintenant plutôt qu'une migration plus tard.

**Rotation des refresh tokens** : à chaque appel à `/auth/refresh`,
l'ancien refresh token est immédiatement révoqué et un nouveau est
émis. Un refresh token ne peut donc être utilisé qu'une seule fois.
Ça limite les dégâts si un refresh token est intercepté : dès que le
vrai utilisateur (ou l'attaquant) l'utilise une fois, l'autre partie
se retrouve avec un token révoqué et ne peut plus continuer à
l'utiliser silencieusement.

**Stockage du refresh token en base : uniquement son hash SHA-256**,
jamais en clair. Si la base de données fuite, un attaquant ne peut pas
récupérer directement des tokens utilisables.

**Vérification de l'état du compte à chaque refresh** : en plus de
valider le token lui-même (non expiré, non révoqué), on vérifie que le
compte utilisateur associé est toujours actif (`enabled == true`). Sans
cette vérification, désactiver un compte compromis n'empêcherait pas
les sessions déjà ouvertes de continuer à fonctionner via `/auth/refresh`
jusqu'à expiration du refresh token (jusqu'à 30 jours).

**Gestion des erreurs d'authentification** :
- Un email inconnu et un mot de passe invalide renvoient exactement le
  même message d'erreur (`InvalidCredentialsException`, "Invalid email
  or password"). Objectif : empêcher un attaquant de déduire, à partir
  du message d'erreur, quels emails sont enregistrés dans le système
  (attaque par énumération de comptes).
- À l'inverse, un compte désactivé lève une exception **distincte**
  (`AccountDisabledException`), volontairement différenciée. Cette
  fois l'attaquant possède déjà un token valide (donc un scénario de
  compromission avancé, pas une simple tentative à l'aveugle) — le
  vrai bénéfice de la distinction est côté serveur : elle permet de
  détecter et journaliser spécifiquement les tentatives d'accès sur
  un compte bloqué, utile pour le futur dashboard de monitoring des
  accès suspects.

**Secret de signature (clé privée RSA)** : stocké en variable
d'environnement locale (`.env`, gitignored) pour le développement.
Migration prévue vers Azure Key Vault en Phase 2 — voir dette
technique en fin de document.

## Conséquences

**Positif :**
- Fenêtre d'exploitation courte en cas de vol de l'access token (10 min)
- Refresh token révocable côté serveur (logout, désactivation de compte)
- Rotation empêche la réutilisation silencieuse d'un refresh token volé
- Cohérence d'algorithme avec la future intégration OAuth2/Azure AD

**Négatif / compromis acceptés :**
- Complexité accrue côté frontend : il faut un intercepteur HTTP capable
  de détecter un 401, appeler `/auth/refresh` silencieusement, puis
  rejouer la requête d'origine.
- Une table `refresh_tokens` supplémentaire en base, avec son propre
  cycle de vie (nettoyage des tokens expirés à prévoir en Phase 4).

## Dette technique identifiée
- Le secret RSA en `.env` local doit migrer vers Azure Key Vault avant
  tout déploiement en environnement partagé ou en production.
- Pas encore de mécanisme de nettoyage automatique des refresh tokens
  expirés en base (à prévoir, ex : job planifié ou requête au démarrage).