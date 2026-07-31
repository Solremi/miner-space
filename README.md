# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 10 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module et activité Android en `sensorLandscape` ;
- carte 2.5D, économie déterministe, raffinage, assemblage et technologies ;
- snapshots alternés, migrations et progression hors ligne ;
- secteurs avec brouillard, scanner, missions locales et ressources rares ;
- pluie de météorites tactile, assistance et reprise idempotente ;
- quatre familles de robots, niveaux, files, maîtrise, traits et logistique ;
- quatre spécialisations, huit modules et deux ensembles de synergie ;
- tutoriel reprenable en sept phases ;
- missions principales, secondaires et exploits ;
- trois contrats simultanés et déterministes ;
- Codex permanent, collections et objectifs visibles ;
- aucun workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, interruptions système réelles, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Modules

- `androidApp` : activité Android, cycle de vie et adaptateurs de plateforme ;
- `game` : production, exploration, événements, robots, stratégie, missions et Codex ;
- `domain` : économie, production, exploration, événements, robots, stratégies et progression ;
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

Pour valider l’étape 10 sur appareil :

1. ouvrir `MISSIONS` depuis la scène principale ;
2. avancer dans plusieurs phases du tutoriel, fermer puis vérifier la reprise exacte ;
3. confirmer qu’au moins trois objectifs restent actifs après l’introduction ;
4. réclamer une mission et vérifier la récompense unique ;
5. livrer les trois catégories de contrats et vérifier la consommation des stocks ;
6. ouvrir le Codex et confirmer que les entrées impossibles ne sont pas marquées ;
7. compléter puis réclamer une collection une seule fois ;
8. fermer de force pendant une récompense puis vérifier l’absence de duplication ;
9. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
10. confirmer l’absence d’overflow et la lisibilité des objectifs.
