# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 6

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation multi-module, carte 2.5D, économie, raffinage, assemblage, technologies, sauvegarde hors ligne, secteurs et exploration.

## Étape 7 — Prototype de plaisir interactif

Statut : **implémentée dans le code et vérifiée par contrôles Kotlin locaux, validation Gradle et Android restante**.

Éléments présents :

- accès `MÉTÉORES` depuis la scène de production ;
- événement de 60 secondes, dans la plage prévue de 45 à 90 secondes ;
- 18 fragments actifs maximum ;
- apparition toutes les 1,4 seconde ;
- durée de présence de 6,5 secondes ;
- capture par toucher et glissement ;
- rayon de capture généreux ;
- assistance activée par défaut, désactivable, avec rayon élargi et récupération automatique des fragments anciens ;
- cœur météorique rare apparaissant à 30 secondes ;
- fragments standards et ressource rare non vendables automatiquement ;
- aucune récompense nécessaire à la campagne ;
- pause fondée sur le temps actif lors d’un arrière-plan ou d’une fermeture ;
- sauvegarde séparée `meteor_event` avec fragments, progression, assistance et Codex ;
- attribution en deux phases avec totaux attendus ;
- récupération après arrêt avant ou après l’écriture de l’inventaire sans duplication ;
- résumé de récompense ;
- Codex temporaire de trois entrées ;
- rendu par formes simples, traînées courtes et plafond strict ;
- HUD et commandes respectant les zones sûres et des cibles de 48 unités ;
- contenu versionné `0.7.0` dans `assets/data/meteor-event.json`.

Contrôles déjà effectués hors Android :

- compilation Kotlin du moteur d’événement ;
- compilation Kotlin du chargeur et du codec ;
- compilation syntaxique des écrans avec les API LibGDX simulées ;
- validation JSON des cinq fichiers de contenu `0.7.0` ;
- simulation complète de 60 secondes ;
- vérification permanente du plafond de 18 fragments ;
- vérification de la capture unique ;
- vérification de la récupération rare avec assistance ;
- aller-retour complet de la sauvegarde en cours d’événement ;
- arrêt simulé avant l’écriture de l’inventaire ;
- arrêt simulé après l’écriture de l’inventaire ;
- résultat identique et sans double crédit dans les deux cas.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. tester toucher et glissement sur téléphone ;
5. tester l’assistance activée et désactivée ;
6. interrompre par appel, verrouillage, arrière-plan et fermeture forcée ;
7. interrompre précisément pendant l’attribution ;
8. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. confirmer l’absence de masquage du HUD essentiel ;
10. confirmer la stabilité avec la densité maximale autorisée.
