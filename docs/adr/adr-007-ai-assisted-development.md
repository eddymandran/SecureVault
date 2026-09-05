# ADR-007 : Usage d'un assistant IA (Claude) dans le développement

## Statut
Accepté

## Contexte
Projet mené en solo, sans revue de code par des pairs ni sparring technique disponible en interne. Le besoin de challenger les choix d'architecture, de détecter les angles morts (notamment sécurité) et d'accélérer les tâches à faible valeur ajoutée (documentation, boilerplate, debugging) justifie l'intégration d'un assistant IA dans le flux de travail.

## Décision
Claude (Anthropic) intervient comme partenaire de discussion sur l'ensemble du projet, du cadrage initial aux choix d'implémentation : identification du sujet et de sa pertinence pédagogique, choix de stack, options d'architecture, revue de code, débogage, rédaction et relecture de documentation. Le fonctionnement type : une problématique ou un besoin est posé, plusieurs options sont discutées avec Claude, et la décision finale — ce qui est retenu, ce qui est écarté, comment c'est mis en œuvre — m'appartient.

Ce qui ne varie pas, quel que soit le sujet abordé en discussion :
- La décision finale est prise par moi, jamais déléguée
- Chaque choix retenu est validé par la mise en pratique : implémentation, tests, passage CI/CD (build, tests unitaires, SonarCloud) avant merge
- La responsabilité du résultat — sécurité, comportement en production, dette technique — reste entière, indépendamment de l'origine de la discussion ayant mené au choix

## Conséquences

**Positives**
- Sparring technique disponible sur l'ensemble du cycle, y compris en amont (cadrage, choix de sujet, stack) là où un développeur solo n'a habituellement personne à qui challenger ces choix
- Vélocité accrue sur les tâches répétitives (boilerplate, tests, documentation)
- Détection plus rapide d'incohérences ou d'oublis (ex. cas limites non testés)

**Négatives / points de vigilance**
- Risque de code halluciné ou de vulnérabilité introduite silencieusement (dépendance obsolète, faille OWASP) → nécessite une relecture critique systématique, pas une confiance par défaut
- Risque de sur-dépendance si la relecture devient superficielle → discipline de test et de revue maintenue quelle que soit l'origine du choix
- Le code généré ou co-conçu avec l'IA doit être soumis aux mêmes contrôles qualité/sécurité (SAST prévu) que le reste du projet, sans traitement de faveur ni suspicion excessive