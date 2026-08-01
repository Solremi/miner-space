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
- transfert Ferrum Delta → Cryos IX, ouverture de secteur et récompenses météoriques atomiques ;
- logique de production, simulation et sauvegarde extraite de `ManufacturingPlanetScreen` ;
- publication de l’état de production uniquement après écriture réussie ;
- catalogue typé `GameTextKey` avec traduction française complète et paramètres contrôlés ;
- mappings communs des messages de production, exploration, météorites et publicité récompensée ;
- tests de couverture du catalogue, de non-mutation, de reprise et de non-duplication ;
- erreurs de chargement importantes enregistrées au lieu d’être toutes absorbées silencieusement ;
- Android Lint intégré aux contrôles locaux et au préflight release.

## Règle de texte

Tout nouveau texte utilisateur doit recevoir une clé dans `shared/text/GameTextCatalog.kt`. Les paramètres utilisent la syntaxe `{nom}` et doivent être fournis exactement. Le français reste la langue de lancement, mais le code n’est plus lié à une traduction unique.

Les anciennes chaînes purement décoratives des écrans seront déplacées lorsqu’un écran est retravaillé ; les messages fonctionnels et les nouvelles fonctionnalités doivent utiliser le catalogue dès maintenant.

## Délibérément non traité dans cette passe

- musiques et sons définitifs ;
- modèles 3D, textures, icônes et VFX finaux ;
- refonte graphique complète des écrans ;
- publication Google Play et validations sur appareils physiques.

## Priorités en cours

1. **Terminé** — extraire la production hors de `ManufacturingPlanetScreen`.
2. **Terminé** — sécuriser les mutations en cas d’échec de sauvegarde.
3. **Terminé** — étendre les transactions multi-slots.
4. **Terminé** — créer et imposer un catalogue de textes typé.
5. Mutualiser les codecs versionnés.
6. Renforcer les tests Android et interface.
7. Raccorder les publicités contextuelles.
8. Préparer les performances et la gestion des futurs assets.
9. Uniformiser les diagnostics locaux.
10. Renforcer la validation release.
