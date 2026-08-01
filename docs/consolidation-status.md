# Miner Space — Consolidation technique

## Corrigé

- build et version reproductibles ;
- transactions multi-slots et rollback des actions ;
- coordinateurs production, exploration, transfert, météorites, retour hors ligne et publicités contextuelles ;
- catalogue typé de textes et codecs versionnés communs ;
- tests paysage et Android ;
- registre d’assets à références comptées ;
- groupes de durée de vie `CORE_UI`, `FERRUM`, `CRYOS`, `FRONTIER`, `ROBOTS` et `AUDIO` ;
- annulation du chargement ou déchargement lorsque la dernière référence disparaît ;
- estimation mémoire par asset et par ensemble chargé ;
- budgets LOW, MEDIUM, HIGH et profil appareil à faible mémoire ;
- limites explicites pour robots visibles, particules, traînées, modèles, textures et shaders ;
- moniteur circulaire de temps de frame sans allocation pendant `record()`.

## Intégration des futurs assets

Les fichiers ne doivent pas être chargés directement depuis un écran. Chaque fichier final recevra un `GameAssetDescriptor` reprenant l’identifiant et le chemin définis dans `docs/asset-production-pack.md`. Un écran acquiert son groupe à l’entrée et le libère à la sortie.

Le backend concret devra :

- charger les textures, sons et musiques de manière asynchrone ;
- enregistrer un chargeur GLB compatible avant l’ajout des modèles `.glb` ;
- annuler un fichier encore en file lorsque `unload()` est appelé ;
- ne jamais dépasser `RuntimePerformanceBudget.assetMemoryBudgetBytes` ;
- choisir le profil faible mémoire lorsque l’appareil le nécessite.

Aucun son, modèle, texture ou VFX factice n’a été ajouté.

## Priorités

1. **Terminé** — coordinateur de production.
2. **Terminé** — rollback sur sauvegarde échouée.
3. **Terminé** — transactions multi-slots.
4. **Terminé** — catalogue de textes.
5. **Terminé** — codecs versionnés communs.
6. **Terminé** — tests interface et Android.
7. **Terminé** — publicités contextuelles.
8. **Terminé** — budgets de performance et gestion de durée de vie des futurs assets.
9. Uniformiser les diagnostics locaux.
10. Renforcer la validation release.
