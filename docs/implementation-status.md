# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 5

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation multi-module, carte 2.5D, économie entière, raffinage, assemblage, technologies, snapshots alternés, migrations et progression hors ligne.

## Étape 6 — Secteurs et exploration

Statut : **implémentée dans le code et vérifiée par contrôles Kotlin locaux, validation Gradle et Android restante**.

Éléments présents :

- couche d’exploration accessible depuis la scène de production ;
- six secteurs de vertical slice pour Ferrum Delta ;
- secteur initial ouvert et cinq secteurs à scanner puis débloquer ;
- brouillard distinguant clairement secteur inconnu, scanné et ouvert ;
- scanner de niveau 1 à 3 dérivé des technologies installées ;
- coûts réels en SpaceDollars ;
- prérequis de secteurs, technologies et composants ;
- consommation atomique des SpaceDollars et composants lors de l’ouverture ;
- compensation automatique si l’une des deux sauvegardes échoue ;
- budget et prérequis visibles avant ouverture ;
- raison stratégique affichée pour chaque secteur ;
- trois gisements rares garantis : ferrite prismatique, cristal xénon et fragment d’archive ;
- ressources rares non vendables automatiquement ;
- mission active persistée ;
- bouton `MISSION` recentrant la caméra sur la cible ;
- double toucher recentrant le secteur sélectionné ;
- pan, pincement, limites strictes et bouton de retour vers la production ;
- ouverture visuelle par masque rétractable sans particules ni allocation par image ;
- sauvegarde séparée `exploration` bénéficiant des snapshots alternés et checksums de l’étape 5 ;
- contenu versionné `0.6.0` dans `assets/data/sectors.json` ;
- chemin de déblocage acyclique empêchant un secteur indispensable de devenir définitivement inaccessible.

Contrôles déjà effectués hors Android :

- compilation Kotlin du moteur d’exploration ;
- compilation Kotlin du chargeur JSON et du codec de progression ;
- compilation syntaxique de l’écran avec les API LibGDX simulées ;
- validation JSON des quatre fichiers de contenu `0.6.0` ;
- ouverture séquentielle de tous les secteurs avec prérequis suffisants ;
- rejet d’une seconde ouverture du même secteur ;
- vérification des niveaux de scanner et de l’arbre technologique ;
- vérification des dépenses cumulées et des trois découvertes rares ;
- aller-retour complet de la sauvegarde d’exploration.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. parcourir tous les secteurs depuis une nouvelle sauvegarde ;
5. vérifier la consommation réelle des coûts après retour en production ;
6. fermer de force pendant une ouverture puis relancer ;
7. tester le centrage mission, le pan et le pincement ;
8. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. activer le mode qualité faible et vérifier l’ouverture fluide ;
10. confirmer l’absence d’overflow, de duplication et de secteur bloqué définitivement.
