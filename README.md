# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les fondations de l’étape 0 et le prototype de carte de l’étape 1 sont implémentés :

- projet Gradle multi-module ;
- séparation stricte entre domaine, données, simulation, rendu et Android ;
- activité de jeu en `sensorLandscape` ;
- services de plateforme abstraits ;
- carte 2.5D légère avec caméra orthographique ;
- déplacement tactile et zoom par pincement ;
- caméra bornée aux limites de la carte ;
- base temporaire et trois gisements sélectionnables ;
- bouton de recentrage sur la base ;
- HUD compact et responsive utilisant les zones sûres de l’écran ;
- écran d’erreur fatal minimal ;
- variantes Android `debug` et `release` ;
- aucun workflow ni CI/CD.

Les critères nécessitant un appareil Android — installation, comportement tactile réel, contrôle visuel en 640 × 320 et 844 × 390, rotation et mesure des FPS — restent à valider manuellement.

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

Pour valider l’étape 1 sur appareil :

1. tester le pan et le pincement dans les deux sens paysage ;
2. vérifier que la caméra ne montre jamais l’extérieur de la carte ;
3. sélectionner la base et chacun des trois gisements à plusieurs niveaux de zoom ;
4. contrôler le HUD et les encoches en 640 × 320 et 844 × 390 ;
5. vérifier le bouton `BASE` après déplacement et zoom ;
6. confirmer 60 FPS sur l’appareil cible moyen.
