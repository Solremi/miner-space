# Miner Space — Consolidation technique

## Corrigé

- build et version reproductibles depuis un clone neuf ;
- contrôles locaux, Android Lint et préflight release ;
- journal binaire multi-slots avec reprise idempotente ;
- transfert planétaire, ouverture de secteur et récompenses météoriques atomiques ;
- production extraite de l’écran et publiée uniquement après sauvegarde réussie ;
- catalogue typé de textes français et mappings communs ;
- infrastructure commune `VersionedFieldReader` / `VersionedFieldWriter` ;
- encodage partagé des ensembles de `GameId` et quantités ;
- codecs exploration et présentation migrés sans changement de format ;
- rejet uniforme des doublons, champs inconnus et types invalides ;
- tests de texte, transaction, non-mutation et sérialisation.

## Règle de sauvegarde

Tout nouveau codec textuel doit utiliser `VersionedFieldWriter`, `VersionedFieldReader` et `SaveFieldCollections`. Les formats existants restent lisibles ; une hausse de `FORMAT_VERSION` n’est nécessaire que si la structure enregistrée change réellement.

## Non traité ici

- sons, musiques, modèles, textures et VFX finaux ;
- refonte visuelle complète ;
- opérations externes Play Console et tests physiques.

## Priorités

1. **Terminé** — coordinateur de production.
2. **Terminé** — rollback sur échec de sauvegarde.
3. **Terminé** — transactions multi-slots.
4. **Terminé** — catalogue de textes.
5. **Terminé** — primitives communes de codecs versionnés.
6. Renforcer les tests Android et interface.
7. Raccorder les publicités contextuelles.
8. Préparer les performances et futurs assets.
9. Uniformiser les diagnostics locaux.
10. Renforcer la validation release.
