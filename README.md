# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 17 de la roadmap sont implémentées dans le dépôt :

- économie déterministe, fabrication, sauvegarde hors ligne et restauration ;
- Ferrum Delta, robots, stratégie, missions, Codex et archives NOVA ;
- prestige, Cryos IX et frontière interplanétaire multi-planètes ;
- présentation procédurale et publicités récompensées facultatives ;
- texte 100–130 %, contraste, modes de couleurs, réduction des animations et des flashes ;
- configuration release 1.0, signature externe, R8, sécurité réseau et journal de panne local ;
- politique, crédits, fiche Play Store, Sécurité des données et plans de test/déploiement ;
- aucun workflow ni CI/CD.

Une passe de consolidation ajoute un lanceur Gradle reproductible, une version unique du projet, des contrôles locaux de qualité, un journal de transactions multi-slots et une reprise bloquante avant le chargement des écrans.

Les musiques, sons définitifs, modèles 3D et textures ne sont pas encore intégrés. Leur inventaire de production se trouve dans `docs/asset-production-pack.md`.

La version reste **NO-GO pour publication** tant que les validations sur appareil, la clé réelle, l’URL HTTPS de confidentialité, les captures, le test fermé et les formulaires Play Console ne sont pas terminés. Voir `docs/release-checklist.md`.

## Démarrage depuis un nouveau clone

Le dépôt ne dépend pas d’une installation globale de Gradle. Les lanceurs lisent la version et le SHA-256 depuis `gradle/wrapper/gradle-wrapper.properties`, téléchargent la distribution dans `.gradle-bootstrap/`, vérifient son intégrité puis exécutent Gradle.

Linux/macOS :

```sh
sh ./gradlew test
```

Windows :

```powershell
.\gradlew.bat test
```

Pour générer ensuite un wrapper Gradle standard avec son JAR :

```sh
sh scripts/bootstrap-wrapper.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap-wrapper.ps1
```

## Vérifications locales

Linux/macOS :

```sh
sh scripts/quality-check.sh
```

Windows :

```powershell
powershell -ExecutionPolicy Bypass -File scripts/quality-check.ps1
```

La tâche `qualityCheck` exécute les vérifications JVM de tous les modules, les tests du module `game`, les tests Android debug et Android Lint. Aucun workflow ou déploiement n’est lancé.

## Version 1.0

La version est définie une seule fois dans `gradle.properties` :

```properties
MINER_SPACE_VERSION=1.0.0
MINER_SPACE_VERSION_CODE=100
```

- applicationId : `fr.solremi.minerspace` ;
- orientation : paysage capteur ;
- sauvegarde : locale, snapshots alternés, journal multi-slots, sauvegarde Android désactivée ;
- monétisation : publicités récompensées uniquement, facultatives.

## Préparer un build release

Fournir par propriétés Gradle ou variables d’environnement :

```properties
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
ADMOB_REWARDED_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
PRIVACY_POLICY_URL=https://example.com/privacy
SUPPORT_EMAIL=support@example.com
RELEASE_STORE_FILE=/chemin/miner-space-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

`validateReleaseConfiguration` bloque les identifiants de test, une politique non HTTPS, un contact absent, une signature incomplète ou une divergence de version.

```sh
sh scripts/release-preflight.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/release-preflight.ps1
```

Le préflight exécute les tests, Android Lint release et construit le bundle signé sans workflow ni déploiement automatique.

## Documents

- `docs/release-checklist.md` : état GO/NO-GO ;
- `docs/closed-test-plan.md` : test interne et fermé ;
- `docs/release-rollout-and-rollback.md` : publication et correctif ;
- `docs/save-compatibility-matrix.md` : migrations ;
- `docs/play-store-listing-fr.md` : fiche et captures ;
- `docs/data-safety.md` : brouillon Sécurité des données ;
- `docs/asset-production-pack.md` : sons, objets, VFX, UI et assets marketing à produire ;
- `assets/legal/privacy-policy-fr.md` : politique intégrée ;
- `assets/legal/third-party-notices.md` : licences et crédits.
