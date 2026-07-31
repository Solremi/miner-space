# Miner Space — Game design

Ce document présente les décisions de game design actives. La version détaillée élaborée avant la restructuration documentaire est conservée dans [`game-design-legacy.md`](game-design-legacy.md) pour référence.

La source de vérité actuelle est répartie ainsi :

- progression, étapes et critères : [`../roadmap.md`](../roadmap.md) ;
- volume de contenu : [`content-v1.md`](content-v1.md) ;
- économie et calculs : [`economy-and-formulas.md`](economy-and-formulas.md) ;
- sauvegarde et cycle de vie : [`save-and-lifecycle.md`](save-and-lifecycle.md) ;
- secteurs et exploration : [`sectors-exploration.md`](sectors-exploration.md) ;
- pluie de météorites : [`meteor-shower.md`](meteor-shower.md) ;
- robots et automatisation : [`robots-automation.md`](robots-automation.md) ;
- UX paysage et accessibilité : [`ux-accessibility.md`](ux-accessibility.md) ;
- frontière sans fin définitive : [`infinite-frontier.md`](infinite-frontier.md) ;
- direction artistique : [`art-direction-bible.md`](art-direction-bible.md) ;
- UI et HUD visuels : [`ui-visual-spec.md`](ui-visual-spec.md) ;
- production des assets : [`asset-production-pack.md`](asset-production-pack.md) ;
- prompts et références visuelles : [`visual-prompt-pack.md`](visual-prompt-pack.md).

## 1. Vision

Miner Space est un jeu mobile Android de gestion, automatisation, exploration et collection spatiale en 2.5D. Le joueur extrait des minerais, les raffine, assemble des composants, construit des technologies, automatise sa base et change de planète.

Le gameplay principal est en paysage. Les sessions durent généralement de 2 à 8 minutes, avec progression hors ligne.

## 2. Piliers

- chaîne de transformation visible et compréhensible ;
- automatisation progressive sans supprimer les choix ;
- carte manipulable et secteurs donnant envie d’explorer ;
- spécialisations réellement différentes ;
- modules et synergies sans loterie payante ;
- robots reconnaissables et transférables ;
- collection permanente ;
- ressources exceptionnellement rares avec protection contre la malchance ;
- histoire légère autour de NOVA ;
- planètes modifiant réellement les règles ;
- activité interactive principale : pluie de météorites ;
- feedback visuel, haptique et audio sur chaque action importante ;
- aucune fin définitive.

## 3. Boucles

### Session

1. collecter ;
2. vérifier les blocages ;
3. relancer les files ;
4. améliorer ;
5. explorer ;
6. participer éventuellement à un événement ;
7. préparer les productions longues.

### Planète

1. installer la base ;
2. exploiter ;
3. raffiner ;
4. assembler ;
5. automatiser ;
6. se spécialiser ;
7. découvrir les ressources rares et le récit ;
8. construire le vaisseau ;
9. transférer les gains permanents.

### Interplanétaire

1. choisir un monde ;
2. s’adapter à ses règles ;
3. compléter objectifs et collections ;
4. améliorer le réseau permanent ;
5. repartir vers une nouvelle destination.

## 4. Exploration

La planète est divisée en secteurs. Chaque secteur doit proposer au moins une raison stratégique d’être ouvert : nouvelle ressource, raccourci logistique, technologie, mission, archive ou découverte rare. Le joueur scanne d’abord la zone afin de voir son coût et ses prérequis, puis choisit de l’ouvrir.

Aucun secteur indispensable ne dépend d’un hasard pur ou d’un chemin irréversible. Les ressources rares nécessaires à l’explication du système sont garanties par un secteur ou une mission identifiée.

## 5. Pluie de météorites

L’événement est une activité facultative de 45 à 90 secondes. Le joueur récupère les fragments par toucher ou glissement, avec une assistance activable. La perte de fragments ne pénalise jamais la campagne.

Le prototype utilise une apparition rare garantie pour tester la présentation, la sauvegarde et le Codex. La récompense reste idempotente après une interruption système.

## 6. Robots et automatisation

Les quatre familles EX, RF, AS et LG possèdent une identité persistante. Les niveaux augmentent progressivement le nombre de files et transforment le châssis visible. Les priorités restent lisibles directement dans le gestionnaire de flotte.

Le robot logistique déplace réellement les ressources déjà produites, sans production implicite. Les traits et la maîtrise améliorent les performances, mais aucun trait n’applique un malus rendant un robot inutilisable.

## 7. Règles non négociables

- aucune publicité obligatoire ;
- aucune ressource obligatoire dépendant d’un hasard pur ;
- aucune nouvelle planète limitée à une recoloration ;
- aucune action fréquente nécessitant plus de deux pressions ;
- aucune perte définitive imprévisible ;
- aucune information importante dépendant uniquement de la couleur ;
- aucune fonctionnalité déclarée terminée sans test de sauvegarde, paysage et performance.

## 8. Priorité

Valider d’abord :

1. carte 2.5D paysage ;
2. chaîne économique minimale ;
3. sauvegarde hors ligne ;
4. secteurs et exploration ;
5. pluie de météorites ;
6. robots et spécialisations.

Le contenu de 30 jours n’est produit qu’après validation du vertical slice.
