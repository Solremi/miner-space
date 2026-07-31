# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 11 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module et activité Android en `sensorLandscape` ;
- carte 2.5D, économie déterministe, raffinage, assemblage et technologies ;
- sauvegarde alternée, migrations et progression hors ligne ;
- secteurs, brouillard, événements et ressources rares ;
- robots, files, maîtrise, traits et logistique automatisée ;
- spécialisations, modules et synergies ;
- tutoriel, missions, contrats, Codex et collections ;
- NOVA, transmissions courtes, ruines, anomalies et archives ;
- protection contre la malchance et attribution rare idempotente ;
- premier robot vétéran conservant son identité et ses statistiques ;
- aucun workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, interruptions système réelles, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Modules

- `androidApp` : activité Android, cycle de vie et adaptateurs de plateforme ;
- `game` : production, exploration, événements, robots, stratégie, missions et archives ;
- `domain` : économie, production, exploration, robots, progression et narration ;
- `data` : contenu JSON, codecs, migrations et snapshots alternés ;
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

Pour valider l’étape 11 sur appareil :

1. ouvrir `ARCHIVES` depuis la scène principale ;
2. vérifier que la production reste entièrement utilisable sans ouvrir la narration ;
3. lire les transmissions courtes puis consulter leur résumé ;
4. analyser chaque anomalie jusqu’au seuil de garantie configuré ;
5. fermer de force avant et après l’écriture d’une découverte rare ;
6. confirmer l’absence de perte ou de duplication après reprise ;
7. terminer le protocole vétéran et vérifier Aster, son numéro et ses statistiques ;
8. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. vérifier que les chapitres impossibles ne sont pas affichés ;
10. confirmer l’absence d’overflow et de blocage de la gestion principale.
