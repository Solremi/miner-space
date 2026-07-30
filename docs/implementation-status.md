# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 4

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation multi-module, carte 2.5D, économie entière, raffinage, assemblage, composants, technologies, files persistantes et ordre officiel des multiplicateurs.

## Étape 5 — Sauvegarde et progression hors ligne

Statut : **implémentée dans le code et vérifiée par contrôles Kotlin locaux, validation Gradle et Android restante**.

Éléments présents :

- conteneur local structuré avec version, séquence, date UTC, taille et checksum CRC32 ;
- deux snapshots alternés `A` et `B` ;
- écriture dans un fichier temporaire, synchronisation disque et remplacement atomique ;
- conservation automatique de la copie précédente ;
- lecture du snapshot valide possédant la séquence la plus récente ;
- restauration automatique de l’alterné lorsqu’une copie est corrompue ;
- compatibilité avec l’ancien fichier unique `primary.msv` ;
- schéma de snapshot 3 ;
- migration depuis le schéma 1 de raffinage et le schéma 2 de fabrication ;
- normalisation vers les ressources, gisements, recettes et technologies du contenu courant ;
- suppression contrôlée des références devenues inconnues ;
- conservation des technologies uniquement lorsque leurs prérequis restent valides ;
- progression hors ligne fondée sur l’heure UTC persistée ;
- plafond initial de 8 heures, y compris lors d’une absence de 24 heures ou plus ;
- absence de gain lors d’un retour significatif de l’horloge ;
- extraction limitée par réserve, transport et stockage ;
- gisements épuisés sans valeur négative ;
- tâches RF et AS réconciliées jusqu’au temps effectivement simulé ;
- produits terminés maintenus dans leur file jusqu’à collecte ;
- rapport de retour indiquant absence, durée simulée, extraction, tâches terminées, épuisements, blocages, migration et restauration ;
- écran de retour responsive avec cible tactile de 48 unités ;
- sauvegarde réécrite après migration, récupération ou progression hors ligne ;
- cycle Android renforcé sur `onPause`, `onStop`, mémoire faible et passage de l’interface en arrière-plan.

Contrôles déjà effectués hors Android :

- compilation Kotlin du stockage alterné, du codec, du migrateur et du moteur hors ligne ;
- compilation syntaxique de l’écran de retour avec les API LibGDX simulées ;
- lecture et écriture de deux snapshots alternés ;
- corruption volontaire du snapshot le plus récent et récupération de la copie précédente ;
- migration des schémas 1 et 2 vers le schéma 3 ;
- simulation de 1 minute, 8 heures et 24 heures ;
- vérification que 24 heures produisent le même état que le plafond de 8 heures ;
- simulation d’un gisement épuisé ;
- simulation d’un stockage saturé ;
- simulation d’une horloge déplacée vers l’arrière sans gain.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. tester arrière-plan, fermeture forcée et redémarrage réel du téléphone ;
5. contrôler les absences de 1 minute, 8 heures et 24 heures ;
6. remplir le stockage et épuiser un gisement avant une absence ;
7. modifier manuellement l’heure du téléphone ;
8. vérifier la récupération après corruption sur les fichiers de l’application ;
9. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
10. confirmer l’absence d’overflow, de gel et de duplication.
