# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- une étape de roadmap correspond à un commit distinct ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique ;
- vérifications locales ou manuelles uniquement.

## Étape 0 — Fondation du projet

Statut : **implémentée dans le code, validation sur appareil réel restante**.

Éléments présents :

- Gradle Kotlin multi-module : `androidApp`, `game`, `domain`, `data`, `simulation`, `shared` ;
- Android, Kotlin, LibGDX et KTX déclarés ;
- variantes `debug` et `release` ;
- activité Android configurée avec `sensorLandscape` ;
- conservation de l’activité lors du changement de sens paysage ;
- domaine sans dépendance Android ni LibGDX ;
- contrats de services de plateforme ;
- adaptateurs Android initiaux et remplacements neutres ;
- scène LibGDX responsive minimale ;
- écran fatal minimal ;
- données et identifiants versionnés ;
- tests JVM initiaux pour les modules purs.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle ;
2. synchroniser le projet ;
3. exécuter les tests JVM ;
4. assembler l’APK debug ;
5. démarrer hors connexion sur un appareil réel ;
6. vérifier paysage gauche et paysage droit ;
7. vérifier les formats 640 × 320 et 844 × 390.

L’étape 1 ne doit commencer qu’après correction des éventuels problèmes de synchronisation, compilation ou démarrage détectés pendant cette validation.
