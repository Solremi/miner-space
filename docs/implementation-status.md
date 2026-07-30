# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étape 0 — Fondation du projet

Statut : **implémentée dans le code, validation Android restante**.

Éléments présents :

- Gradle Kotlin multi-module : `androidApp`, `game`, `domain`, `data`, `simulation`, `shared` ;
- Android, Kotlin, LibGDX et KTX déclarés ;
- variantes `debug` et `release` ;
- activité Android configurée avec `sensorLandscape` ;
- domaine sans dépendance Android ni LibGDX ;
- contrats de services de plateforme ;
- écran fatal minimal et données versionnées.

## Étape 1 — Prototype de carte 2.5D

Statut : **implémentée dans le code, validation visuelle et tactile sur appareil restante**.

Éléments présents :

- caméra orthographique ;
- déplacement tactile et zoom par pincement ;
- caméra bornée aux limites de la carte ;
- base et trois gisements sélectionnables ;
- recentrage sur la base ;
- HUD séparé du monde et adapté aux zones sûres.

## Étape 2 — Économie minimale

Statut : **implémentée dans le code et vérifiée par compilation Kotlin locale, validation Gradle et Android restante**.

Éléments présents :

- `ResourceDefinition`, `DepositDefinition`, `EconomyState` et inventaire en `Long` ;
- réserves de gisement et zones de collecte bornées ;
- extraction continue calculée en secondes entières depuis l’horloge monotone ;
- multiplicateurs fixes en millionièmes et arrondi final inférieur ;
- collecte atomique avec rejet d’une collecte vide ;
- vente atomique et ajout contrôlé de SpaceDollars ;
- séquence de transaction monotone ;
- définitions versionnées dans `assets/data/core-economy.json` ;
- chargeur JSON entier refusant les nombres décimaux ;
- simulateur accéléré avec collecte et vente automatiques ;
- tests JVM pour la non-duplication, les arrondis, le chargement JSON et 24 heures de simulation ;
- HUD affichant monnaie, stocks, réserve, collecte et vitesse ;
- actions tactiles `COLLECTER` et `VENDRE`.

Contrôles déjà effectués hors Android :

- compilation des sources Kotlin pures `domain`, `data` et `simulation` ;
- exécution d’un scénario extraction → collecte → vente ;
- rejet d’une seconde collecte sur une zone vide ;
- simulation de 86 400 secondes sans erreur d’invariant, valeur négative ou dépassement.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle ;
2. synchroniser le projet ;
3. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
4. assembler l’APK debug ;
5. vérifier l’extraction au fil du temps réel ;
6. tester collecte et vente en 640 × 320 et 844 × 390 ;
7. vérifier paysage gauche et paysage droit ;
8. confirmer l’absence d’overflow et 60 FPS sur l’appareil cible.
