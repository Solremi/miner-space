# Miner Space — État de la refactorisation

## Décision

La refactorisation structurelle préalable à l’intégration des assets définitifs est terminée sur `main`.

Le projet reste volontairement sans workflow, sans CI/CD et sans déploiement automatique. Les validations sont locales et manuelles.

## Organisation des modules

Les responsabilités de haut niveau restent séparées :

- `androidApp` : intégration Android et services de plateforme ;
- `game` : rendu LibGDX, écrans, entrées et présentation ;
- `domain` : règles métier pures ;
- `data` : contenu, sauvegardes, migrations et transactions ;
- `simulation` : équilibrage, campagne et progression hors ligne ;
- `shared` : identifiants, diagnostics, textes et types partagés ;
- `assets` : données, localisation, mentions légales et futurs fichiers visuels ou audio.

## Organisation du module game

Les écrans ne sont plus regroupés dans un dossier unique. Ils sont rangés par fonctionnalité : Ferrum, météorites, robots, stratégie, missions, narration, réglages, prestige, Cryos, frontière, monétisation et fonctions communes.

Ferrum est séparé en :

```text
game/ferrum/
├── input/
├── model/
├── presentation/
├── scene/
├── screen/
├── text/
└── ui/
```

`FerrumCommandScreen` est désormais un orchestrateur. La caméra, les gestes, les actions, le modèle de HUD, le rendu du HUD, les textes et l’état d’écran sont dans des composants distincts.

Les écrans Stratégie, Missions et Météorites possèdent également des fichiers dédiés pour le layout, le texte ou le rendu. Aucun écran actif ne doit dépasser 22 000 octets sans justification et extraction préalable.

## Suppressions

Les anciennes variantes non routées ont été retirées :

- `EmptyPlanetScreen` ;
- `FerrumVerticalSliceScreen` ;
- `GameplayHubScreen` ;
- `ManufacturingPlanetScreen` ;
- `PlanetScreen` ;
- `PresentationGameplayScreen` ;
- `SectorExplorationScreen`.

Git conserve leur historique ; aucune copie `legacy` n’est nécessaire dans le code actif.

## Domaine

Les principaux fichiers monolithiques sont divisés en définitions, état et moteur sans changer les packages ni les noms publics :

- assemblage ;
- raffinage ;
- robots ;
- progression ;
- publicités récompensées ;
- événement météorique ;
- stratégie ;
- catalogue Ferrum Delta.

Cette séparation maintient la compatibilité des imports et des codecs tout en limitant les responsabilités par fichier.

## État et textes Ferrum

Le singleton mutable `FerrumColonyVisualState` a été supprimé. Le stade de la colonie est calculé depuis `ManufacturingGameState`, conservé dans `FerrumScreenState` et transmis explicitement au rendu.

Les textes Ferrum sont centralisés dans `FerrumTextCatalog`. Les destinations, stades, conseils, états de sauvegarde, recettes et messages de production ne sont plus dispersés dans l’écran principal.

## Garde-fous locaux

`scripts/source-safety-check.py` contrôle notamment :

- l’absence de `.github/workflows` ;
- la pureté du domaine ;
- la présence des transactions et mécanismes de rollback ;
- l’absence des anciens écrans et monolithes ;
- l’absence d’état visuel Ferrum global ;
- la séparation des composants Ferrum ;
- la limite de taille des écrans actifs ;
- l’utilisation du parseur JSON partagé ;
- les corrections Gradle Android nécessaires.

## Validation restante

La structure est prête pour les assets définitifs. La compilation Gradle complète, les tests JVM, l’installation Android et les tests sur appareils réels doivent encore être exécutés localement avant toute publication.
