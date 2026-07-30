# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 6 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module séparant Android, rendu, domaine, données et simulation ;
- activité Android en `sensorLandscape` ;
- carte 2.5D avec déplacement, pincement, limites et HUD respectant les zones sûres ;
- économie entière et déterministe : extraction, collecte et vente ;
- raffinage avec `RF-01`, réservation, file persistante, annulation et collecte ;
- assemblage avec `AS-01`, composants et technologies installables ;
- multiplicateurs appliqués dans l’ordre officiel avec un seul arrondi final ;
- deux snapshots locaux alternés avec checksum, migrations et progression hors ligne ;
- carte d’exploration avec six secteurs, brouillard, scanner, coûts et prérequis ;
- trois gisements rares garantis, mission active et centrage automatique ;
- ouverture visuelle légère compatible avec le mode qualité faible ;
- aucun workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, fermeture forcée réelle, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Modules

- `androidApp` : activité Android, cycle de vie et adaptateurs de plateforme ;
- `game` : LibGDX/KTX, carte de production, exploration, HUD et écran de retour ;
- `domain` : économie, raffinage, assemblage, technologies et règles de secteurs ;
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

Pour valider l’étape 6 sur appareil :

1. ouvrir la carte avec le bouton `SECTEURS` ;
2. scanner puis ouvrir la Crête cuivrée et les Plaines cristallines ;
3. vérifier que les coûts et prérequis restent toujours visibles ;
4. confirmer que les SpaceDollars et composants sont réellement consommés ;
5. installer les technologies nécessaires puis ouvrir les secteurs profonds ;
6. vérifier les trois gisements rares garantis ;
7. utiliser `MISSION` pour centrer les Profondeurs xénon puis les Ruines d’archive ;
8. fermer et relancer l’application pour vérifier la persistance des secteurs ;
9. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
10. confirmer une ouverture fluide sans overflow en mode qualité faible.
