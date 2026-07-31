# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 8

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation, carte, économie, production, sauvegarde hors ligne, exploration, météorites et automatisation robotique.

## Étape 9 — Spécialisations, modules et synergies

Statut : **implémentée dans le code et couverte par tests JVM, validation Gradle et Android restante**.

Éléments présents :

- accès `STRATÉGIE` depuis la scène principale ;
- quatre spécialisations : Industrie, Logistique, Recherche et Prospection ;
- bonus et contraintes explicites pour chaque spécialisation ;
- essai initial gratuit ;
- changement suivant à 900 SpaceDollars avec délai de six heures ;
- comparateur avant/après limité à extraction, raffinage, assemblage et logistique ;
- huit modules à fabrication déterministe ;
- coûts connus en matériaux et SpaceDollars ;
- aucune boîte ou récompense aléatoire payante ;
- un emplacement de module aux niveaux robot 1 et 2 ;
- deux emplacements aux niveaux 3 et 4 ;
- trois emplacements au niveau 5 ;
- équipement exclusif d’une instance sur un seul robot ;
- trois niveaux par module dans le vertical slice ;
- maximum de quatre statistiques visibles par module ;
- démontage restituant 70 % des matériaux, arrondi à l’entier inférieur ;
- ensemble Forge complet avec bonus à deux et trois pièces ;
- ensemble Survey complet avec bonus à deux et trois pièces ;
- calcul des synergies uniquement à partir des modules réellement équipés ;
- sauvegarde séparée `strategy` protégée par snapshots alternés et checksum ;
- compensation économique si l’écriture de la stratégie échoue ;
- contenu versionné `0.9.0` dans `assets/data/specializations-modules.json`.

Contrôles prévus ou couverts hors Android :

- compilation Kotlin du moteur stratégique ;
- chargement du contenu JSON entier uniquement ;
- aller-retour du codec de sauvegarde ;
- essai gratuit puis changement payant et temporisé ;
- fabrication, équipement, amélioration et démontage ;
- limites progressives des emplacements ;
- activation des deux ensembles à deux et trois pièces ;
- vérification que chaque spécialisation possède au moins un compromis ;
- vérification qu’aucune spécialisation ne domine plus de deux indicateurs visibles ;
- absence de valeur négative après transaction.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. parcourir toutes les spécialisations sur une sauvegarde réelle ;
5. fabriquer et équiper les huit modules ;
6. tester les ensembles Forge et Survey ;
7. fermer de force pendant une fabrication, un équipement et un démontage ;
8. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. vérifier la lisibilité des quatre indicateurs ;
10. confirmer l’absence d’overflow, de duplication et de blocage économique.
