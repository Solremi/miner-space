# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 17 de la roadmap sont implémentées dans le dépôt :

- économie déterministe, fabrication, sauvegarde hors ligne et restauration ;
- Ferrum Delta, robots, stratégie, missions, Codex et archives NOVA ;
- prestige, Cryos IX et frontière interplanétaire multi-planètes ;
- présentation procédurale, audio essentiel et publicités récompensées facultatives ;
- texte 100–130 %, contraste, modes de couleurs, réduction des animations et des flashes ;
- configuration release 1.0, signature externe, R8, sécurité réseau et journal de panne local ;
- politique, crédits, fiche Play Store, Sécurité des données et plans de test/déploiement ;
- aucun workflow ni CI/CD.

La version reste **NO-GO pour publication** tant que les validations sur appareil, la clé réelle, l’URL HTTPS de confidentialité, les captures, le test fermé et les formulaires Play Console ne sont pas terminés. Voir `docs/release-checklist.md`.

## Version 1.0

- applicationId : `fr.solremi.minerspace` ;
- versionCode : `100` ;
- versionName : `1.0.0` ;
- orientation : paysage capteur ;
- sauvegarde : locale, snapshots alternés, sauvegarde Android désactivée ;
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

`validateReleaseConfiguration` bloque les identifiants de test, une politique non HTTPS, un contact absent ou une signature incomplète.

```sh
sh scripts/release-preflight.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/release-preflight.ps1
```

Ces scripts exécutent les tests et construisent le bundle release sans workflow ni déploiement automatique.

## Documents

- `docs/release-checklist.md` : état GO/NO-GO ;
- `docs/closed-test-plan.md` : test interne et fermé ;
- `docs/release-rollout-and-rollback.md` : publication et correctif ;
- `docs/save-compatibility-matrix.md` : migrations ;
- `docs/play-store-listing-fr.md` : fiche et captures ;
- `docs/data-safety.md` : brouillon Sécurité des données ;
- `assets/legal/privacy-policy-fr.md` : politique intégrée ;
- `assets/legal/third-party-notices.md` : licences et crédits.
