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

- ressources, réserves, stocks et monnaie en `Long` ;
- extraction continue calculée en secondes entières ;
- calculs fixes en millionièmes et arrondi inférieur ;
- collecte et vente atomiques ;
- séquence de transaction monotone ;
- définitions versionnées dans `assets/data/core-economy.json` ;
- chargeur JSON refusant les nombres décimaux ;
- simulateur accéléré et test de 86 400 secondes ;
- HUD affichant monnaie, stocks, réserve, collecte et vitesse.

## Étape 3 — Raffinage

Statut : **implémentée dans le code et vérifiée par contrôles Kotlin locaux, validation Gradle et Android restante**.

Éléments présents :

- robot raffineur `robot_rf_01` visible et sélectionnable ;
- recettes `recipe_iron_ingot` et `recipe_copper_plate` ;
- ressources raffinées `refined_iron_ingot` et `refined_copper_plate` ;
- file persistante de quatre tâches ;
- réservation atomique des ingrédients au lancement ;
- horodatages absolus permettant de reprendre une tâche après fermeture forcée ;
- statuts `QUEUED`, `RUNNING` et `READY_TO_COLLECT` ;
- annulation avec remboursement de 100 % avant 10 %, 80 % jusqu’à 90 %, puis 0 % ;
- zone de remboursement persistante si le stockage est plein ;
- sortie terminée conservée dans la tâche tant que le stockage ne peut pas la recevoir ;
- collecte idempotente empêchant une double attribution ;
- sauvegarde locale atomique par fichier temporaire puis remplacement ;
- snapshot de l’économie, des gisements, de la file, des réservations et des remboursements ;
- sauvegarde immédiate après lancement, annulation, collecte et vente ;
- sauvegarde au passage en arrière-plan et autosauvegarde périodique ;
- choix de recette, lancement, relance, collecte et annulation depuis le HUD ;
- relance de la recette courante en deux pressions maximum ;
- effet visuel pulsé et barre de progression sur RF-01.

Contrôles déjà effectués hors Android :

- compilation syntaxique du moteur de raffinage avec les dépendances de domaine simulées ;
- compilation syntaxique de l’écran avec les API LibGDX simulées ;
- scénario lancement → fin → collecte ;
- rejet d’une seconde collecte du même produit ;
- contrôle des remboursements à 100 %, 80 % et 0 % ;
- contrôle du maintien d’un résultat terminé lorsque le stockage est plein ;
- aller-retour complet snapshot → fichier → restauration d’une tâche active ;
- correction de la replanification afin qu’annuler une tâche en attente ne réinitialise pas la tâche active.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle ;
2. synchroniser le projet ;
3. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
4. assembler et installer l’APK debug ;
5. lancer plusieurs recettes et vérifier l’ordre de file ;
6. forcer la fermeture pendant une tâche puis rouvrir l’application ;
7. tester les trois paliers de remboursement ;
8. tester un stockage de sortie plein puis libéré ;
9. contrôler le HUD en 640 × 320 et 844 × 390 dans les deux sens paysage ;
10. confirmer l’absence d’overflow et 60 FPS sur l’appareil cible.
