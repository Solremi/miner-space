# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 5 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module séparant Android, rendu, domaine, données et simulation ;
- activité Android en `sensorLandscape` ;
- carte 2.5D avec déplacement, pincement, limites et HUD respectant les zones sûres ;
- économie entière et déterministe : extraction, collecte et vente ;
- raffinage avec `RF-01`, réservation, file persistante, annulation et collecte ;
- assemblage avec `AS-01`, composants et technologies installables ;
- multiplicateurs appliqués dans l’ordre officiel avec un seul arrondi final ;
- deux snapshots locaux alternés avec checksum et remplacement atomique ;
- migration des schémas de sauvegarde 1, 2 et 3 ;
- restauration automatique du dernier snapshot valide après corruption ;
- progression hors ligne plafonnée à 8 heures au départ ;
- écran de retour récapitulant extraction, productions terminées, blocages et plafonnement ;
- protection contre les retours d’horloge et les gains excessifs ;
- aucun workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, fermeture forcée réelle, redémarrage du téléphone, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Modules

- `androidApp` : activité Android, cycle de vie et adaptateurs de plateforme ;
- `game` : LibGDX/KTX, carte, HUD et écran de retour ;
- `domain` : économie, raffinage, assemblage et technologies ;
- `data` : chargement JSON, migrations, codecs et snapshots alternés ;
- `simulation` : simulation active, accélérée et hors ligne ;
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

Pour valider l’étape 5 sur appareil :

1. quitter pendant l’extraction, puis revenir après 1 minute ;
2. tester des absences de 8 heures et 24 heures, la seconde devant être plafonnée à 8 heures ;
3. forcer la fermeture pendant une tâche RF et une tâche AS ;
4. redémarrer le téléphone puis vérifier les stocks et les files ;
5. simuler un gisement épuisé et un stockage plein hors ligne ;
6. modifier l’heure vers l’arrière puis vers l’avant et vérifier l’absence de gain excessif ;
7. corrompre le snapshot le plus récent et vérifier la restauration de l’alterné ;
8. vérifier l’écran de retour en 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. confirmer l’absence de duplication, de valeur négative et d’overflow ;
10. confirmer la stabilité des FPS et l’absence de gel pendant la sauvegarde.
