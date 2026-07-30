# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 7 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module séparant Android, rendu, domaine, données et simulation ;
- activité Android en `sensorLandscape` ;
- carte 2.5D avec déplacement, pincement, limites et HUD respectant les zones sûres ;
- économie entière et déterministe, raffinage, assemblage et technologies ;
- snapshots alternés, migrations et progression hors ligne ;
- six secteurs avec brouillard, scanner, coûts, missions et gisements rares ;
- pluie de météorites de 60 secondes accessible depuis la planète ;
- récupération par toucher ou glissement avec assistance activée par défaut ;
- fragments standards, cœur rare de test, résumé et Codex temporaire ;
- reprise exacte après interruption et attribution idempotente des récompenses ;
- aucun workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, interruptions système réelles, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Modules

- `androidApp` : activité Android, cycle de vie et adaptateurs de plateforme ;
- `game` : production, exploration, événement météorique, HUD et écrans de retour ;
- `domain` : règles économiques, industrielles, exploration et événements ;
- `data` : contenu JSON, migrations, codecs et snapshots alternés ;
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

Pour valider l’étape 7 sur appareil :

1. ouvrir `MÉTÉORES` depuis la scène de production ;
2. récupérer des fragments par toucher puis par glissement ;
3. désactiver et réactiver l’assistance ;
4. vérifier le cœur rare vers 30 secondes ;
5. passer en arrière-plan puis reprendre l’événement au même temps actif ;
6. forcer la fermeture pendant l’événement puis pendant l’attribution ;
7. vérifier l’absence de double récompense après chaque reprise ;
8. ouvrir le Codex temporaire et le résumé ;
9. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
10. confirmer la stabilité avec 18 fragments actifs et l’absence d’overflow.
