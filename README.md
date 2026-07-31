# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 13 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module et activité Android en `sensorLandscape` ;
- économie déterministe, raffinage, assemblage, technologies et sauvegarde hors ligne ;
- carte active de Ferrum Delta étendue à 14 secteurs ;
- robots, spécialisations, modules, synergies et automatisation ;
- tutoriel, missions, contrats, Codex, collections et archives NOVA ;
- direction artistique procédurale, réglages de qualité, vibrations et audio essentiel ;
- catalogue Ferrum Delta 1.0 conforme au budget de `docs/content-v1.md` ;
- simulations déterministes des profils très actif, régulier et occasionnel ;
- aucun workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, interruptions système réelles, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Contenu Ferrum Delta 1.0

Le manifeste `assets/data/ferrum-delta-v1.json` et `FerrumDeltaContentFactory` décrivent notamment :

- 14 secteurs et 34 gisements ;
- 9 ressources brutes, 9 raffinées et 24 composants ;
- 14 technologies, 24 modules et deux ensembles complets ;
- 42 missions principales, 36 secondaires et 20 de maîtrise ou collection ;
- 12 contrats, 8 exploits et 12 événements facultatifs ;
- 120 entrées de Codex, 10 collections, 5 jalons narratifs et 12 transmissions ;
- trois profils de simulation terminant respectivement en 22, 32 et 50 jours.

Les identifiants du vertical slice restent conservés dans le catalogue final.

## Modules

- `androidApp` : activité Android, cycle de vie, audio et services de plateforme ;
- `game` : production, exploration, événements, robots, stratégie, missions, archives et présentation ;
- `domain` : règles économiques, progression, narration et validation du contenu ;
- `data` : contenu, factories, chargeurs, codecs, migrations et snapshots alternés ;
- `simulation` : simulation active, hors ligne et campagne Ferrum Delta ;
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

Pour valider l’étape 13 sur appareil :

1. démarrer depuis une nouvelle sauvegarde ;
2. vérifier les jalons de début : extraction, raffinage, robot, secteur et transformation visuelle ;
3. ouvrir progressivement les 14 secteurs ;
4. parcourir les 12 transmissions NOVA ;
5. confirmer qu’aucune ressource obligatoire ne dépend d’un événement ;
6. tester une sauvegarde ancienne et vérifier les identifiants historiques ;
7. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
8. mesurer le mode qualité faible sur l’appareil cible ;
9. confirmer l’absence de blocage permanent ou de duplication ;
10. comparer les durées observées aux simulations 22, 32 et 50 jours.
