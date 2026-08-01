# Miner Space — Consolidation technique

## Corrigé

- build et version reproductibles depuis un clone neuf ;
- contrôles locaux, Android Lint, tests paysage et instrumentation Android ;
- journal multi-slots avec reprise idempotente ;
- transfert planétaire, secteurs et météorites atomiques ;
- coordinateur de production avec rollback réel ;
- catalogue typé de textes et primitives communes de codecs ;
- coordinateur générique de publicité récompensée contextuelle ;
- `scopeId` obligatoire pour les offres `RETURN` et `EVENT` ;
- refus d’une seconde récompense pour le même retour ou événement ;
- doublement facultatif du retour hors ligne, plafonné à huit heures ;
- prolongation météorique facultative de quinze secondes ;
- consommation du jeton et état gameplay enregistrés dans la même transaction ;
- reprise automatique lorsqu’une publicité est validée avant une interruption.

## Règles publicitaires

- aucune publicité obligatoire ;
- aucun interstitiel ;
- aucune publicité pendant le tutoriel, une transmission ou une animation majeure ;
- la récompense exacte est connue avant le lancement ;
- une fermeture ou une indisponibilité ne consomme aucune limite ;
- les offres de retour et d’événement ne sont utilisables qu’une fois pour leur `scopeId` ;
- le gameplay normal reste disponible en cas de refus, d’absence de réseau ou de SDK indisponible.

## Non traité ici

- sons, musiques, modèles, textures et VFX finaux ;
- tests sur le parc physique complet ;
- opérations externes Play Console.

## Priorités

1. **Terminé** — coordinateur de production.
2. **Terminé** — rollback sur sauvegarde échouée.
3. **Terminé** — transactions multi-slots.
4. **Terminé** — catalogue de textes.
5. **Terminé** — codecs versionnés communs.
6. **Terminé** — tests interface et Android.
7. **Terminé** — publicités contextuelles hors ligne et météorites.
8. Préparer les performances et futurs assets.
9. Uniformiser les diagnostics locaux.
10. Renforcer la validation release.
