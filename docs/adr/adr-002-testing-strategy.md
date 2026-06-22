# ADR-002 : Stratégie de tests

## Statut
Accepté — 22/06/2026

## Contexte
L'architecture hexagonale permet de tester le domaine sans infrastructure.
On veut ancrer des conventions de tests claires dès le début du projet.

## Décision

### Types de tests
- **Tests unitaires** : domaine pur, zéro Spring, zéro BDD — rapides
- **Tests d'intégration** : couche infrastructure avec Testcontainers (à venir)
- **Tests end-to-end** : à définir en phase ultérieure

### Pattern AAA
Chaque test suit obligatoirement la structure Arrange / Act / Assert :
```java
// ARRANGE — prépare le contexte
// ACT — exécute l'action
// ASSERT — vérifie le résultat
```

### Conventions
- `@DisplayName` obligatoire — les rapports JUnit doivent être lisibles
- Nommage : `should_<résultat>_when_<condition>`
- Pas de `@SpringBootTest` dans les tests unitaires du domaine

## Conséquences
✅ Tests unitaires démarrent en millisecondes  
✅ Rapports JUnit lisibles dans Jenkins  
✅ Conventions uniformes sur tout le projet