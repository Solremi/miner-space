# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 4 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module séparant Android, rendu, domaine, données et simulation ;
- activité Android en `sensorLandscape` ;
- carte 2.5D avec déplacement, pincement, limites et HUD respectant les zones sûres ;
- ressources, réserves, stocks et SpaceDollars en `Long` ;
- extraction continue, collecte et vente atomiques ;
- robot raffineur `RF-01`, recettes, file persistante, annulation et collecte ;
- robot assembleur `AS-01` et file de fabrication persistante ;
- composants `component_power_cell` et `component_sensor_array` ;
- technologies installables avec prérequis et comparaison avant/après ;
- chaîne complète brute → raffinée → composant → technologie ;
- multiplicateurs fixes appliqués dans l’ordre officiel avec un arrondi final inférieur ;
- sauvegarde locale atomique de l’économie, des files et des technologies installées ;
- aucun workflow ni CI/CD.

Les critères nécessitant Android — compilation APK, installation, comportement tactile réel, contrôle visuel en 640 × 320 et 844 × 390, rotation et mesure des FPS — restent à valider manuellement.

## Modules

- `androidApp` : activité Android et adaptateurs de plateforme ;
- `game` : LibGDX/KTX, carte, entrées et HUD ;
- `domain` : règles économiques, raffinage, assemblage et technologies ;
- `data` : sauvegarde locale et chargement des définitions JSON ;
- `simulation` : horloges et simulation économique accélérée ;
- `shared` : identifiants, résultats et journalisation ;
- `assets` : ressources et données versionnées.

## Préparer le wrapper Gradle

Sous Windows :

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap-wrapper.ps1
```

Sous Linux ou macOS :

```sh
sh scripts/bootstrap-wrapper.sh
```

## Vérifications manuelles

Aucun workflow n’est configuré. Après génération du wrapper :

```sh
./gradlew :domain:test :shared:test :data:test :simulation:test
./gradlew :androidApp:assembleDebug
```

Pour valider l’étape 4 sur appareil :

1. extraire et collecter les ressources brutes ;
2. produire et collecter les matériaux raffinés ;
3. fabriquer les deux composants avec `AS-01` ;
4. fabriquer, collecter puis installer `tech_extraction_protocol` ;
5. vérifier le passage de 360 à 432 unités par minute dans la comparaison ;
6. confirmer que `tech_quantum_sorting` reste verrouillée avant le premier nœud ;
7. fabriquer et installer la seconde technologie, puis vérifier le cumul à 486 unités par minute ;
8. fermer de force l’application pendant une tâche AS et vérifier sa restauration ;
9. contrôler le HUD en 640 × 320 et 844 × 390 dans les deux sens paysage ;
10. confirmer l’absence d’overflow et la stabilité des FPS.
