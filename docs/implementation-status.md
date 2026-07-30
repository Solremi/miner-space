# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étape 0 — Fondation du projet

Statut : **implémentée dans le code, validation Android restante**.

Présent : architecture multi-module, Android `sensorLandscape`, services abstraits, écran fatal et données versionnées.

## Étape 1 — Prototype de carte 2.5D

Statut : **implémentée dans le code, validation visuelle et tactile sur appareil restante**.

Présent : caméra orthographique, pan, pincement, limites de carte, sélection, base, gisements, recentrage et HUD avec zones sûres.

## Étape 2 — Économie minimale

Statut : **implémentée dans le code, validation Gradle et Android restante**.

Présent : économie en `Long`, extraction continue, collecte et vente atomiques, SpaceDollars, JSON versionné et simulation de 86 400 secondes.

## Étape 3 — Raffinage

Statut : **implémentée dans le code, validation Gradle et Android restante**.

Présent : `RF-01`, recettes, réservation des ingrédients, file persistante, annulation avec remboursement, collecte idempotente, blocage de stockage et sauvegarde locale atomique.

## Étape 4 — Assemblage et technologies

Statut : **implémentée dans le code et vérifiée par contrôles Kotlin locaux, validation Gradle et Android restante**.

Éléments présents :

- robot assembleur `robot_as_01` visible et sélectionnable ;
- file AS persistante de quatre tâches ;
- réservation atomique des entrées au lancement ;
- statuts `QUEUED`, `RUNNING` et `READY_TO_COLLECT` ;
- résultats terminés conservés lorsque le stockage est plein ;
- composants `component_power_cell` et `component_sensor_array` ;
- recettes de technologies `assembly_tech_extraction_protocol` et `assembly_tech_quantum_sorting` ;
- technologies `tech_extraction_protocol` et `tech_quantum_sorting` ;
- arbre de déblocage imposant la première technologie avant la seconde ;
- installation consommant l’objet technologique fabriqué ;
- objets technologiques non vendables, empêchant leur vente accidentelle ;
- bonus d’extraction de 20 %, puis 15 % supplémentaires ;
- addition des bonus d’une même catégorie avant multiplication ;
- ordre officiel : base, robot, modules, synergies, spécialisation, technologies, planète, événement, prestige ;
- calcul entier par `BigInteger` et un seul arrondi final inférieur ;
- comparaison avant/après affichant 360 → 432 puis 432 → 486 unités par minute ;
- snapshot de l’économie, du raffinage, de la file AS et des technologies installées ;
- sauvegarde après fabrication, collecte et installation ;
- HUD permettant de sélectionner une recette, lancer, collecter et installer ;
- barre de progression et retour visuel sur `AS-01`.

Contrôles déjà effectués hors Android :

- compilation syntaxique des modules purs avec Kotlin ;
- compilation syntaxique de l’écran avec les API LibGDX simulées ;
- scénario matériau raffiné → composant → objet technologique → installation ;
- vérification du verrouillage du second nœud ;
- vérification du cumul des bonus technologiques ;
- vérification de l’ordre officiel des multiplicateurs et de l’arrondi final ;
- aller-retour complet du snapshot avec une tâche AS et une technologie installée ;
- validation syntaxique des fichiers JSON 0.4.0.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. parcourir la chaîne complète depuis une nouvelle sauvegarde ;
5. fermer de force pendant une tâche AS et vérifier sa reprise ;
6. tester un stockage de sortie plein ;
7. vérifier les comparaisons avant/après et les prérequis ;
8. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. confirmer l’absence d’overflow et la stabilité des FPS.
