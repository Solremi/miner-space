# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 9 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module séparant Android, rendu, domaine, données et simulation ;
- activité Android en `sensorLandscape` ;
- carte 2.5D, économie déterministe, raffinage, assemblage et technologies ;
- snapshots alternés, migrations et progression hors ligne ;
- six secteurs avec brouillard, scanner, coûts, missions et gisements rares ;
- pluie de météorites avec récupération tactile, assistance et reprise idempotente ;
- quatre familles de robots identifiables, cinq niveaux, maîtrise, traits et robot logistique ;
- quatre spécialisations avec compromis, essai gratuit et changement contrôlé ;
- huit modules fabriqués, améliorables, équipables et démontables ;
- deux ensembles complets avec bonus à deux et trois pièces ;
- comparateur stratégique limité à quatre indicateurs ;
- aucun coffre aléatoire payant, workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, interruptions système réelles, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Modules

- `androidApp` : activité Android, cycle de vie et adaptateurs de plateforme ;
- `game` : production, exploration, événements, flotte robotique, laboratoire stratégique, HUD et écrans de retour ;
- `domain` : économie, production, exploration, événements, robots et stratégies ;
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

Pour valider l’étape 9 sur appareil :

1. ouvrir `STRATÉGIE` depuis la scène principale ;
2. comparer les quatre spécialisations avant sélection ;
3. utiliser l’essai gratuit puis vérifier le coût et le délai du changement suivant ;
4. fabriquer un module Forge et un module Survey avec les stocks réels ;
5. équiper les modules sur plusieurs robots et vérifier les limites de 1, 2 puis 3 emplacements ;
6. compléter les bonus d’ensemble à deux puis trois pièces ;
7. améliorer un module jusqu’au niveau 3 ;
8. démonter un module et vérifier la restitution de 70 % des matériaux ;
9. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
10. confirmer l’absence d’overflow, de duplication et de spécialisation dominante.
