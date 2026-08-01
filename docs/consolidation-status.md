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
- ouverture des secteurs atomique entre économie et exploration ;
- récompenses météoriques atomiques entre inventaire et état d’événement, avec reprise des anciennes phases `COMMITTING` ;
- logique de production, simulation et sauvegarde extraite de `ManufacturingPlanetScreen` vers `ManufacturingCoordinator` ;
- les actions de production sont calculées sur un état candidat, sauvegardées, puis publiées en mémoire uniquement après succès ;
- un échec de sauvegarde annule réellement la vente, la collecte, le raffinage, l’assemblage ou l’installation de technologie ;
- tests de non-mutation, de reprise et de non-duplication ajoutés ;
- routage initial isolé dans un résolveur pur et testé ;
- erreurs de chargement importantes enregistrées au lieu d’être toutes absorbées silencieusement ;
- Android Lint intégré aux contrôles locaux et au préflight release.

## Délibérément non traité dans cette passe

- musiques et sons définitifs ;
- modèles 3D, textures, icônes et VFX finaux ;
- refonte graphique des écrans ;
- publication Google Play et validations sur appareils physiques.

## Priorités en cours

1. **Terminé** — extraire la production hors de `ManufacturingPlanetScreen`.
2. **Terminé** — sécuriser les mutations en cas d’échec de sauvegarde.
3. **Terminé** — étendre les transactions multi-slots aux secteurs, transferts et récompenses météoriques.
4. Centraliser les textes affichés.
5. Mutualiser les codecs versionnés.
6. Renforcer les tests Android et interface.
7. Raccorder les publicités contextuelles.
8. Préparer les performances et la gestion des futurs assets.
9. Uniformiser les diagnostics locaux.
10. Renforcer la validation release.

## Dette restante

- séparer progressivement le renderer et le calcul de layout des écrans de production et d’événement ;
- utiliser le journal pour toute nouvelle récompense qui modifie plusieurs slots ;
- remplacer progressivement les codecs textuels artisanaux par une sérialisation partagée et versionnée ;
- ajouter des tests instrumentés Android du cycle de vie, du consentement et des formats paysage ;
- raccorder les offres publicitaires contextuelles au retour hors ligne et aux météorites.
