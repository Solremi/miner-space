# Frontière interplanétaire 1.0

## Périmètre

La frontière est disponible après la stabilisation de Cryos IX. Elle n’est jamais présentée comme une fin du jeu : chaque monde stabilisé ouvre la possibilité de générer une nouvelle route.

## Génération contrôlée

La génération assemble uniquement des secteurs préconstruits :

- 3 familles visuelles : volcanique, cristalline et épave ;
- 8 modèles de secteurs par famille ;
- 12 modificateurs validés ;
- 6 modèles d’objectifs ;
- 3 difficultés ciblant des boucles de 2, 4 et 7 jours.

Chaque monde possède une graine persistée, une famille, deux ou trois modificateurs compatibles, un objectif réalisable et une chaîne linéaire de cinq à sept secteurs. Le générateur rejette les paires incompatibles et garantit la capacité requise par l’objectif.

## Répétition et validation

La signature `famille + modificateurs triés` du monde précédent est conservée. La génération suivante ne peut pas reproduire immédiatement cette signature.

Le validateur contrôle :

- les références de secteurs, objectifs et modificateurs ;
- la compatibilité avec la famille visuelle ;
- les exclusions entre modificateurs ;
- la présence des capacités requises ;
- la chaîne de progression sans rupture ;
- les limites de durée et de difficulté.

Une simulation de 10 000 mondes doit produire zéro combinaison impossible et zéro répétition immédiate.

## Sauvegarde multi-planètes

Le slot `frontier` contient :

- la graine principale ;
- l’index de génération suivant ;
- tous les mondes découverts ;
- le monde sélectionné ;
- la définition complète de chaque monde ;
- la progression et le nombre d’actions ;
- les récompenses permanentes, cosmétiques et de collection ;
- la dernière signature générée.

Trois mondes incomplets peuvent être conservés simultanément. Les mondes stabilisés restent consultables dans les archives. Une fermeture reprend le monde actif depuis son dernier snapshot.

## Interface

L’écran affiche une carte stellaire, les nœuds récents, la famille, la difficulté, l’objectif, les modificateurs, la progression et la récompense. Les cinq actions principales utilisent des cibles de 48 unités : nouveau monde, sélection, action, archives et retour Cryos.
