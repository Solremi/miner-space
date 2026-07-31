# Miner Space — Spécialisations, modules et synergies

## Objectif

L’étape 9 ajoute des choix stratégiques lisibles sans tirage aléatoire payant. Les spécialisations, modules et ensembles utilisent uniquement des données versionnées et des transactions économiques déterministes.

## Spécialisations

Quatre spécialisations sont disponibles :

- **Industrie** : extraction et raffinage renforcés, assemblage et logistique légèrement réduits ;
- **Logistique** : transfert et stockage renforcés, rendement direct plus faible ;
- **Recherche** : assemblage et raffinage renforcés, extraction réduite ;
- **Prospection** : découverte rare et extraction renforcées, production industrielle plus lente.

Le premier choix utilise un essai gratuit. Les changements suivants coûtent 900 SpaceDollars et respectent un délai de six heures. Aucune spécialisation ne domine simultanément extraction, raffinage, assemblage et logistique.

## Modules

Huit modules sont fabriqués avec des matériaux connus. Aucun module n’est obtenu par coffre ou tirage aléatoire.

Les robots possèdent :

- un emplacement aux niveaux 1 et 2 ;
- deux emplacements aux niveaux 3 et 4 ;
- trois emplacements au niveau 5.

Un module ne peut être équipé que sur un robot à la fois. Chaque module affiche au maximum quatre statistiques. Les modules possèdent trois niveaux dans ce vertical slice.

## Ensembles

Deux ensembles complets sont disponibles :

- **Forge** : foreuse, cœur thermique et châssis renforcé ;
- **Survey** : optique, capteur quantique et lentille d’archive.

Chaque ensemble possède un bonus à deux pièces et un bonus supplémentaire à trois pièces. Les bonus sont recalculés à partir des modules réellement équipés.

## Démontage

Le démontage retire définitivement l’instance et restitue 70 % des matériaux de fabrication, arrondis à l’entier inférieur. Les SpaceDollars de fabrication et d’amélioration ne sont pas remboursés.

## Comparateur

L’écran stratégique compare uniquement quatre indicateurs :

1. extraction ;
2. vitesse de raffinage ;
3. vitesse d’assemblage ;
4. logistique.

Le comparateur montre l’état actuel et l’aperçu de la spécialisation sélectionnée avant validation.

## Persistance

La progression stratégique utilise le slot `strategy`, protégé par les snapshots alternés et checksums existants. Les dépenses économiques sont écrites avant l’état stratégique ; une erreur d’écriture déclenche une compensation vers l’état précédent.
