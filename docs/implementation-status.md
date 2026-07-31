# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 14

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation, économie, sauvegarde hors ligne, Ferrum Delta, événements, robots, stratégie, missions, Codex, narration, présentation, prestige et boucle Cryos IX.

## Étape 15 — Frontière interplanétaire 1.0

Statut : **génération contrôlée, validation automatique, carte stellaire et sauvegarde multi-planètes implémentées ; validation Gradle complète et Android restante**.

### Contenu

- manifeste `assets/data/interplanetary-frontier.json` version `1.0.0` ;
- trois familles visuelles : volcanique, cristalline et épave ;
- huit modèles de secteurs préconstruits par famille ;
- douze modificateurs avec familles autorisées, exclusions et capacités requises ;
- six objectifs couvrant extraction, raffinage, réseau, événement, artefact et construction ;
- trois difficultés : 2, 4 et 7 jours ciblés ;
- cinq à sept secteurs par monde ;
- récompenses permanentes, cosmétiques ou de collection.

### Garanties de génération

- graine principale persistée ;
- index de génération persistant ;
- définition complète de chaque monde sauvegardée ;
- chaîne linéaire de secteurs toujours réalisable ;
- capacité de l’objectif garantie dans les secteurs sélectionnés ;
- compatibilité famille/modificateurs vérifiée ;
- paires de modificateurs incompatibles rejetées ;
- aucune répétition immédiate de la même famille avec les mêmes modificateurs ;
- trois mondes incomplets maximum ;
- aucune mention de fin définitive du jeu.

### Sauvegarde et reprise

- slot séparé `frontier` ;
- plusieurs mondes conservés simultanément ;
- monde sélectionné et dernière signature sauvegardés ;
- progression, actions, statut et dates conservés ;
- récompenses permanentes, cosmétiques et de collection persistées ;
- retour automatique sur la frontière lorsqu’un monde actif existe après fermeture.

### Contrôles effectués hors Android

- compilation Kotlin du modèle, du générateur, du moteur et du codec ;
- compilation syntaxique des nouveaux écrans et de la navigation avec API simulées ;
- validation JSON du manifeste ;
- génération locale de 10 000 mondes ;
- zéro combinaison impossible ;
- zéro chaîne cassée ;
- zéro répétition immédiate ;
- test des trois difficultés ;
- limite de trois mondes actifs ;
- stabilisation d’un monde et attribution unique de sa récompense ;
- aller-retour exact d’une sauvegarde multi-planètes.

### Validation Android restante

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. ouvrir la frontière depuis une sauvegarde Cryos IX terminée ;
5. interrompre et reprendre plusieurs mondes ;
6. vérifier les récompenses et archives ;
7. tester 640 × 320 et 844 × 390 dans les deux sens paysage ;
8. mesurer les FPS en qualité faible ;
9. vérifier l’absence d’overflow et de chargement perceptible ;
10. confirmer l’absence de combinaison impossible sur appareil.
