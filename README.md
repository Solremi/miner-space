# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 14 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module et activité Android en `sensorLandscape` ;
- économie déterministe, raffinage, assemblage, technologies et sauvegarde hors ligne ;
- campagne Ferrum Delta, robots, stratégies, missions, Codex et archives NOVA ;
- direction artistique procédurale, réglages de qualité, vibrations et audio essentiel ;
- catalogue Ferrum Delta 1.0 et simulations de profils joueurs ;
- prestige planétaire, transfert reprenable et Noyaux Stellaires ;
- conservation du Codex, des archives, des bonus et d’un robot vétéran ;
- boucle Cryos IX avec énergie, chaleur, froid et réseau thermique ;
- aucun workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, interruptions système réelles, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Ferrum Delta 1.0

Le manifeste `assets/data/ferrum-delta-v1.json` et `FerrumDeltaContentFactory` décrivent notamment :

- 14 secteurs et 34 gisements ;
- 9 ressources brutes, 9 raffinées et 24 composants ;
- 14 technologies, 24 modules et deux ensembles complets ;
- 42 missions principales, 36 secondaires et 20 de maîtrise ou collection ;
- 120 entrées de Codex, 10 collections, 5 jalons narratifs et 12 transmissions ;
- trois profils de simulation terminant respectivement en 22, 32 et 50 jours.

## Prestige et Cryos IX

Le transfert Ferrum Delta → Cryos IX utilise un slot `prestige` séparé :

1. écriture d’une transaction `PREPARED` contenant les totaux permanents attendus ;
2. réinitialisation idempotente des slots planétaires Ferrum ;
3. création ou rapprochement du slot `cryos_ix` ;
4. rapprochement des Noyaux Stellaires et du robot vétéran ;
5. clôture du transfert.

Une interruption avant ou après chaque phase reprend le même transfert sans perte ni duplication.

Le manifeste `assets/data/cryos-ix.json` et `CryosIxContentFactory` décrivent :

- 6 secteurs et 16 gisements budgétés ;
- 4 ressources locales et 4 matériaux raffinés ;
- 8 recettes spécifiques, 5 technologies et 8 modules cryogéniques ;
- un ensemble thermique complet ;
- 12 missions principales, 10 secondaires et 3 événements froids ;
- 2 découvertes narratives et 30 entrées de Codex.

La boucle jouable demande d’installer une base, produire énergie et chaleur, extraire malgré le froid, construire cinq nœuds thermiques, ouvrir six secteurs, fabriquer un module et terminer l’objectif planétaire.

## Modules

- `androidApp` : activité Android, cycle de vie, audio et services de plateforme ;
- `game` : production, exploration, transfert, Cryos IX, présentation et écrans ;
- `domain` : économie, progression, prestige, règles thermiques et validation ;
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

Pour valider l’étape 14 sur appareil :

1. terminer Ferrum Delta et ouvrir `DÉPART` ;
2. vérifier que le chantier final et un robot à 6 000 points sont exigés ;
3. interrompre l’application après la préparation, après la réinitialisation et avant la clôture ;
4. vérifier que les Noyaux restent à 3 et que le transfert reprend automatiquement ;
5. confirmer la conservation du Codex, des archives, des bonus et de l’identité du vétéran ;
6. installer la base Cryos IX puis équilibrer énergie et chaleur ;
7. construire les cinq nœuds et ouvrir les six secteurs ;
8. fabriquer un module, installer trois technologies et terminer l’objectif majeur ;
9. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
10. confirmer l’absence d’overflow, de blocage ou de duplication.
