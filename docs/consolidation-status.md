# Miner Space — Consolidation technique

## Corrigé

- version du projet centralisée dans `gradle.properties` ;
- lanceurs Gradle Linux, macOS et Windows capables de démarrer depuis un clone neuf sans installation globale ;
- téléchargement Gradle vérifié par SHA-256 ;
- tâche locale `qualityCheck` et scripts multiplateformes ;
- suppression de la dépendance d’exécution `game → simulation` ;
- tests JUnit activés dans `game` ;
- journal binaire de transactions multi-slots avec préparation, reprise et application idempotente ;
- reprise d’une transaction incomplète avant tout chargement de sauvegarde ou d’écran ;
- transfert Ferrum Delta → Cryos IX déplacé hors de l’écran vers un coordinateur testable ;
- routage initial isolé dans un résolveur pur et testé ;
- erreurs de chargement importantes enregistrées au lieu d’être toutes absorbées silencieusement ;
- Android Lint intégré aux contrôles locaux et au préflight release ;
- chargement, validation et sauvegarde de l’exploration sortis de `SectorExplorationScreen` ;
- ouverture de secteur enregistrée comme une transaction unique entre `primary` et `exploration` ;
- interactions d’exploration bloquées lorsqu’une transaction doit être reprise ;
- tests ajoutés pour l’engagement conjoint des coûts, du secteur et de la ressource rare.

## Délibérément non traité dans ces passes

- musiques et sons définitifs ;
- modèles 3D, textures, icônes et VFX finaux ;
- refonte graphique des écrans ;
- publication Google Play et validations sur appareils physiques.

## Dette restante

- extraire la simulation, la persistance et les actions de `ManufacturingPlanetScreen` ;
- séparer progressivement les renderers et modèles de vue des écrans LibGDX ;
- étendre le journal multi-slots aux autres opérations qui modifient plusieurs sauvegardes ;
- centraliser tous les textes affichés avant une éventuelle traduction ;
- remplacer progressivement les codecs textuels artisanaux par une sérialisation partagée et versionnée ;
- ajouter des tests instrumentés Android du cycle de vie, du consentement et des formats paysage ;
- raccorder les offres publicitaires contextuelles au retour hors ligne et aux météorites.
