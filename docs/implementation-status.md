# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 9

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation, production, sauvegarde hors ligne, exploration, événement météorique, flotte robotique, spécialisations, modules et synergies.

## Étape 10 — Missions, contrats, tutoriel et Codex

Statut : **implémentée dans le code et vérifiée par contrôles Kotlin locaux, validation Gradle et Android restante**.

Éléments présents :

- accès `MISSIONS` depuis la scène principale ;
- tutoriel progressif de sept phases, persistant et non bloquant ;
- reprise exacte de la phase courante après fermeture ;
- missions principales, secondaires et exploits ;
- graphe de prérequis acyclique ;
- plusieurs objectifs parallèles dès l’introduction ;
- objectif sélectionnable et sauvegardé ;
- trois contrats simultanés : simple, rentable et ambitieux ;
- rotation déterministe des variantes de contrats ;
- livraison consommant réellement les ressources ;
- récompenses en SpaceDollars attribuées une seule fois ;
- Codex permanent de ressources, industrie, exploration, robots et stratégie ;
- entrées impossibles totalement masquées ;
- quatre collections avec récompense unique ;
- onglets objectifs, contrats et Codex ;
- quatre lignes maximum pour préserver la lisibilité ;
- cibles tactiles de 48 unités et respect des zones sûres ;
- sauvegarde séparée `progression` avec snapshots alternés et checksum ;
- contenu versionné `0.10.0` dans `assets/data/progression.json` ;
- compensation économique si la sauvegarde de progression échoue.

Contrôles déjà effectués hors Android :

- compilation Kotlin du moteur de progression ;
- compilation Kotlin du chargeur JSON et du codec ;
- validation JSON du contenu `0.10.0` ;
- simulation des sept phases du tutoriel ;
- vérification d’au moins trois objectifs parallèles ;
- livraison d’un contrat puis rejet d’une seconde livraison de la même occurrence ;
- vérification de la consommation exacte des stocks ;
- filtrage des entrées de Codex impossibles ;
- réclamation unique d’une collection ;
- aller-retour complet de la sauvegarde de progression.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. parcourir le tutoriel depuis une nouvelle sauvegarde ;
5. fermer de force au milieu d’une phase et pendant une récompense ;
6. vérifier plusieurs cycles de contrats ;
7. tester les collections et le filtrage du Codex ;
8. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. vérifier la navigation tactile et les zones sûres ;
10. confirmer l’absence d’overflow, de duplication et de blocage permanent.
