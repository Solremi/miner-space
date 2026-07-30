# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 3 de la roadmap sont implémentées dans le code :

- projet Gradle multi-module ;
- séparation stricte entre domaine, données, simulation, rendu et Android ;
- activité de jeu en `sensorLandscape` ;
- services de plateforme abstraits ;
- carte 2.5D avec caméra orthographique, déplacement tactile et zoom borné ;
- base, trois gisements et raffineur RF-01 sélectionnables ;
- HUD compact utilisant les zones sûres de l’écran ;
- ressources, inventaire, réserves et SpaceDollars en `Long` ;
- extraction continue, collecte et vente atomiques ;
- robot RF avec deux recettes et file de quatre tâches ;
- ingrédients réservés au lancement ;
- annulation avec remboursement à 100 %, 80 % ou 0 % selon l’avancement ;
- produit terminé conservé si le stockage de sortie est plein ;
- relance d’une recette en deux pressions maximum ;
- sauvegarde locale atomique de l’économie et de la file RF ;
- restauration d’une tâche active après fermeture forcée ;
- définitions JSON versionnées et calculs économiques entiers ;
- simulateur économique accéléré et test de 24 heures ;
- effet visuel de raffinage actif ;
- écran d’erreur fatal minimal ;
- variantes Android `debug` et `release` ;
- aucun workflow ni CI/CD.

Les critères nécessitant Android — compilation APK, installation, comportement tactile réel, contrôle visuel en 640 × 320 et 844 × 390, rotation et mesure des FPS — restent à valider manuellement.

## Modules

- `androidApp` : activité Android, cycle de vie et adaptateurs de plateforme ;
- `game` : LibGDX/KTX, rendu, carte, entrées et HUD économique ;
- `domain` : règles économiques et de raffinage pures sans Android ni LibGDX ;
- `data` : stockage fichier local, snapshots et chargement JSON ;
- `simulation` : horloges et simulation économique accélérée ;
- `shared` : résultats, identifiants et journalisation ;
- `assets` : définitions économiques et recettes chargées à l’exécution.

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

Pour valider les étapes 1 à 3 sur appareil :

1. tester le pan et le pincement dans les deux sens paysage ;
2. vérifier la carte et le HUD en 640 × 320 et 844 × 390 ;
3. collecter les ressources des trois gisements puis vendre depuis la base ;
4. sélectionner RF-01, choisir chaque recette et lancer plusieurs tâches ;
5. forcer la fermeture pendant une tâche, rouvrir et vérifier la progression ;
6. annuler avant 10 %, entre 10 % et 90 %, puis après 90 % ;
7. remplir le stockage raffiné et vérifier que le produit terminé reste collectable ;
8. collecter puis relancer la même recette en deux pressions maximum ;
9. confirmer l’absence de duplication, d’overflow et de perte de ressources ;
10. confirmer 60 FPS sur l’appareil cible moyen.
