# 🔐 SecureVault

Application web de gestion de secrets et de credentials (mots de passe, clés API, tokens), développée en autodidacte comme terrain d'entraînement DevSecOps : architecture propre, CI/CD, conteneurisation, puis sécurité applicative et déploiement cloud.

## 🎯 Pourquoi ce projet

Le choix du sujet, la gestion de secrets, est volontaire : il impose une rigueur sécurité de bout en bout (chiffrement, contrôle d'accès, audit des accès). Trois axes travaillés en parallèle : maturité de développement (architecture hexagonale, tests, clean code), pratiques DevOps/Cloud (CI/CD, conteneurisation, Azure), et sécurité applicative (chiffrement, gestion des secrets, OWASP).

## 🤝 Méthode de travail

Projet développé en solo, en dialogue constant avec Claude (Anthropic) : cadrage du sujet, choix de stack, options d'architecture, revue de code, débogage, rédaction de documentation. Les choix retenus sont validés par la mise en pratique — implémentation, tests, CI/CD — avant merge, et documentés via ADR (voir `ADR-007`) au même titre que les autres décisions du projet.

## 🧱 État d'avancement

**✅ En place**
- Architecture hexagonale du backend (domain / application / infrastructure), séparation stricte ports/adapters
- Première feature métier `Vault` : entité de domaine pure, service, ports in/out, tests unitaires (pattern AAA)
- Choix techniques documentés via ADR (Architecture Decision Records) versionnés
- Pipeline CI/CD Jenkins en Pipeline as Code (Jenkinsfile versionné, déclenché depuis GitHub) : build et tests Maven en container éphémère, publication des rapports JUnit
- Analyse de qualité de code via SonarCloud (quality gate)
- Dockerfile multi-stage pour le backend (build JDK, exécution sur JRE Alpine, utilisateur non-root) et docker-compose pour l'environnement de dev local (Spring Boot + PostgreSQL)
- Homelab Proxmox comme environnement de recette avant la cible de déploiement Azure

**🚧 À venir**
- Authentification JWT + OAuth2 (Azure AD / Google)
- Chiffrement AES-256 des coffres, gestion des secrets via Azure Key Vault
- Contrôles OWASP, Content Security Policy, audit log des accès
- Pipeline avec SAST/DAST, déploiement cible sur Azure App Service / AKS
- Observabilité (Azure Monitor, alerting) et durcissement final

## 🧰 Stack technique

**Backend** : Java 21 · Spring Boot · Maven · PostgreSQL
**Frontend** : Angular (standalone)
**Ops** : Docker · Jenkins (Pipeline as Code) · SonarCloud · Proxmox (homelab) → Azure (cible)

## 📫 Contact

[LinkedIn](https://www.linkedin.com/in/eddymandran/) · dev.eddy.mdn@proton.me
