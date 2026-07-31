# Miner Space

Jeu Android en Kotlin basé sur LibGDX et KTX.

## État

Les étapes 0 à 16 de la roadmap sont implémentées dans le code :

- architecture Gradle multi-module et activité Android en `sensorLandscape` ;
- économie déterministe, raffinage, assemblage, technologies et sauvegarde hors ligne ;
- campagne Ferrum Delta, robots, stratégies, missions, Codex et archives NOVA ;
- direction artistique procédurale, réglages de qualité, vibrations et audio essentiel ;
- prestige planétaire, transfert reprenable et boucle Cryos IX ;
- frontière interplanétaire contrôlée et sauvegarde multi-planètes ;
- publicités récompensées facultatives, consentement UMP et transactions idempotentes ;
- aucun workflow ni CI/CD.

Les validations nécessitant Android — compilation APK, installation, interruptions système réelles, formats 640 × 320 et 844 × 390 et mesure des FPS — restent manuelles.

## Contenu principal

Ferrum Delta 1.0 comprend 14 secteurs, 34 gisements, 9 ressources brutes, 9 raffinées, 24 composants, 14 technologies, 24 modules, 98 missions structurées, 120 entrées de Codex et 12 transmissions NOVA.

Cryos IX ajoute une boucle fondée sur l’énergie, la chaleur, le froid et cinq nœuds thermiques. La frontière interplanétaire génère des mondes à partir de secteurs préconstruits, avec trois familles visuelles, douze modificateurs, six objectifs et trois difficultés.

## Publicités récompensées

Le manifeste `assets/data/rewarded-advertising.json` et `RewardedAdvertisingContentFactory` définissent huit offres :

- relais temporel ;
- doublement du retour hors ligne ;
- capsule de matériaux standards ;
- contrat premium ;
- balise d’analyse ;
- drone météoritique ;
- prolongation météoritique ;
- boost orbital.

Le slot `rewarded_ads` enregistre les demandes `PREPARED`, les callbacks `SDK_REWARDED`, les récompenses `COMMITTED`, les plafonds, les délais, les portées retour/événement et les droits persistants. Un callback dupliqué ne peut pas attribuer deux fois la récompense.

Les identifiants de test Google sont utilisés par défaut. Pour une version distribuée, fournir :

```properties
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
ADMOB_REWARDED_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```

## Modules

- `androidApp` : activité Android, AdMob, UMP, audio et services de plateforme ;
- `game` : écrans, présentation, transfert, Cryos IX, frontière et transmission orbitale ;
- `domain` : économie, progression, prestige, génération et protocole publicitaire ;
- `data` : contenu, factories, codecs, migrations et snapshots alternés ;
- `simulation` : simulations de campagne et validation des mondes ;
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

Pour valider l’étape 16 sur appareil :

1. démarrer avec les identifiants publicitaires de test ;
2. tester consentement requis, refusé et non requis ;
3. vérifier la réouverture des préférences de confidentialité ;
4. tester indisponibilité réseau, fermeture et échec d’affichage ;
5. provoquer un callback dupliqué et confirmer une seule récompense ;
6. interrompre après `SDK_REWARDED` puis vérifier la reprise ;
7. atteindre les plafonds d’offre et le plafond global de dix ;
8. confirmer qu’aucune action normale n’est désactivée ;
9. vérifier l’absence d’offre pendant tutoriel, narration et grande animation ;
10. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage.
