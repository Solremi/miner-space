# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 8 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module séparant Android, rendu, domaine, données et simulation ;
- activité Android en `sensorLandscape` ;
- carte 2.5D, économie déterministe, raffinage, assemblage et technologies ;
- snapshots alternés, migrations et progression hors ligne ;
- six secteurs avec brouillard, scanner, coûts, missions et gisements rares ;
- pluie de météorites avec récupération tactile, assistance et reprise idempotente ;
- quatre familles de robots identifiables avec nom et numéro de série ;
- cinq niveaux, trois files maximum, quatre paliers de maîtrise et cinq traits non punitifs ;
- robot logistique transférant réellement les productions en attente selon une priorité ;
- évolution visuelle et densité réglable jusqu’à 50 robots ou drones ;
- aucun workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, interruptions système réelles, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Modules

- `androidApp` : activité Android, cycle de vie et adaptateurs de plateforme ;
- `game` : production, exploration, événements, flotte robotique, HUD et écrans de retour ;
- `domain` : économie, production, exploration, événements et automatisation robotique ;
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

Pour valider l’étape 8 sur appareil :

1. ouvrir `ROBOTS` depuis la scène principale ;
2. vérifier les noms, numéros de série, traits, maîtrises et statistiques des quatre familles ;
3. vendre des ressources puis améliorer RF ou AS jusqu’au niveau 3 et vérifier le passage à deux files ;
4. ajouter plusieurs tâches, ouvrir le gestionnaire et confirmer leur redistribution parallèle ;
5. laisser les gisements produire, puis vérifier le transfert automatique de LG-01 ;
6. changer la priorité logistique et observer l’ordre des ressources transférées ;
7. passer les qualités faible, moyenne et élevée et confirmer 18, 32 et 50 unités visibles ;
8. fermer puis relancer l’application pour vérifier la persistance complète de la flotte ;
9. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
10. confirmer l’absence d’overflow et une fréquence d’images stable avec 50 unités.
