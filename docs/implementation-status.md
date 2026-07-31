# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 7

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation, carte, économie, production, sauvegarde hors ligne, exploration et pluie de météorites.

## Étape 8 — Robots et automatisation avancée

Statut : **implémentée dans le code et vérifiée par contrôles Kotlin locaux, validation Gradle et Android restante**.

Éléments présents :

- accès `ROBOTS` depuis la scène principale ;
- quatre familles EX, RF, AS et LG ;
- identités initiales Aster, Rhea, Kestrel et Nox ;
- numéro de série stable et unique par robot ;
- cinq niveaux fonctionnels par famille ;
- une file aux niveaux 1 et 2, deux files aux niveaux 3 et 4, trois files au niveau 5 ;
- redistribution des tâches RF et AS encore en attente sur les files disponibles ;
- tâches déjà actives ou terminées jamais redémarrées ;
- robot logistique transférant réellement les quantités en attente vers l’inventaire ;
- conservation exacte des ressources pendant les transferts ;
- respect des capacités de stockage ;
- cinq priorités : équilibrée, mission, désengorgement, ressource rare et valeur ;
- quatre paliers de maîtrise ;
- cinq traits uniquement bénéfiques ;
- statistiques de travail et de temps actif ;
- trois paliers d’évolution visuelle ;
- qualité faible à 18 unités, moyenne à 32 et élevée à 50 ;
- positions visuelles pré-calculées et rendu par formes simples ;
- sauvegarde séparée `robots` avec snapshots alternés et checksum ;
- contenu versionné `0.8.0` dans `assets/data/robots.json` ;
- coûts d’amélioration configurables en SpaceDollars ;
- compensation économique si la sauvegarde robotique d’une amélioration échoue.

Contrôles déjà effectués hors Android :

- compilation Kotlin du moteur robotique ;
- compilation Kotlin du chargeur JSON et du codec ;
- compilation syntaxique de l’écran et de la navigation avec les API LibGDX simulées ;
- validation du fichier `robots.json` ;
- vérification des quatre identités uniques ;
- vérification des cinq niveaux et des trois files ;
- planification de six tâches sur trois files en deux vagues ;
- simulation logistique avec conservation exacte des ressources ;
- vérification des limites de stockage ;
- vérification que chaque trait reste au-dessus de la capacité de base ;
- progression de maîtrise jusqu’au niveau expert ;
- vérification de 50 unités en qualité élevée ;
- aller-retour complet de la sauvegarde robotique.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. améliorer chaque famille sur une sauvegarde réelle ;
5. tester plusieurs tâches RF et AS avant et après déblocage des files ;
6. vérifier plusieurs priorités du robot LG avec stockage presque plein ;
7. fermer de force pendant un transfert et une amélioration ;
8. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. tester les trois niveaux de qualité ;
10. confirmer une fréquence stable avec 50 unités et l’absence d’overflow.
