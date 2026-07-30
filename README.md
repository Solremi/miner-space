# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

L’étape 0 de la roadmap est initialisée :

- projet Gradle multi-module ;
- séparation stricte entre domaine, données, simulation, rendu et Android ;
- activité de jeu en `sensorLandscape` ;
- services de plateforme abstraits ;
- scène LibGDX responsive minimale ;
- écran d’erreur fatal minimal ;
- variantes Android `debug` et `release` ;
- aucun workflow ni CI/CD.

## Modules

- `androidApp` : activité Android et adaptateurs de plateforme ;
- `game` : LibGDX/KTX, rendu, écrans et entrées ;
- `domain` : contrats et règles pures sans Android ni LibGDX ;
- `data` : implémentations de stockage et dépôts ;
- `simulation` : horloges et simulation accélérée ;
- `shared` : résultats, identifiants et journalisation ;
- `assets` : données et ressources chargées à l’exécution.

## Préparer le wrapper Gradle

Le dépôt conserve un bootstrap texte vérifié par SHA-256. Sous Windows :

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap-wrapper.ps1
```

Sous Linux ou macOS :

```sh
sh scripts/bootstrap-wrapper.sh
```

Le script génère ensuite le wrapper Gradle officiel 9.5.0.

## Vérifications manuelles

Aucun workflow n’est configuré. Après génération du wrapper :

```sh
./gradlew :domain:test :shared:test :data:test :simulation:test
./gradlew :androidApp:assembleDebug
```

L’APK debug est produit dans `androidApp/build/outputs/apk/debug/`.
