# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 2 de la roadmap sont implémentées dans le code :

- projet Gradle multi-module ;
- séparation stricte entre domaine, données, simulation, rendu et Android ;
- activité de jeu en `sensorLandscape` ;
- services de plateforme abstraits ;
- carte 2.5D légère avec caméra orthographique ;
- déplacement tactile, zoom par pincement et caméra bornée ;
- base et trois gisements sélectionnables ;
- HUD compact utilisant les zones sûres de l’écran ;
- ressources, inventaire, réserves et SpaceDollars en `Long` ;
- extraction continue fondée sur une horloge monotone ;
- collecte atomique sans duplication ;
- vente atomique des ressources vendables ;
- définitions économiques JSON versionnées ;
- calculs fixes en millionièmes et arrondis à l’entier inférieur ;
- simulateur économique accéléré et test de 24 heures ;
- écran d’erreur fatal minimal ;
- variantes Android `debug` et `release` ;
- aucun workflow ni CI/CD.

Les critères nécessitant Android — compilation APK, installation, comportement tactile réel, contrôle visuel en 640 × 320 et 844 × 390, rotation et mesure des FPS — restent à valider manuellement.

## Modules

- `androidApp` : activité Android et adaptateurs de plateforme ;
- `game` : LibGDX/KTX, rendu, écrans, entrées et HUD économique ;
- `domain` : contrats et règles économiques pures sans Android ni LibGDX ;
- `data` : sauvegarde initiale et chargement des définitions JSON ;
- `simulation` : horloges et simulation économique accélérée ;
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

Pour valider les étapes 1 et 2 sur appareil :

1. tester le pan et le pincement dans les deux sens paysage ;
2. vérifier que la caméra ne montre jamais l’extérieur de la carte ;
3. sélectionner les trois gisements et observer les réserves diminuer ;
4. collecter plusieurs fois et vérifier qu’une seconde collecte vide ne duplique rien ;
5. vendre depuis la base et vérifier le stock et les SpaceDollars ;
6. contrôler le HUD et les encoches en 640 × 320 et 844 × 390 ;
7. confirmer 60 FPS sur l’appareil cible moyen.
