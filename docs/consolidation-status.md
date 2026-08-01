# Miner Space — Consolidation technique

## Corrigé

- build et version reproductibles ;
- transactions multi-slots et rollback des actions ;
- coordinateurs gameplay et publicités contextuelles ;
- catalogue de textes, codecs communs, tests paysage et Android ;
- registre d’assets, budgets de performance et mesure de frames ;
- diagnostics structurés à capacité fixe ;
- logger Android décoré sans modifier les appels métier ;
- empreinte stable du message plutôt que conservation du texte brut ;
- seule la classe d’exception est retenue, jamais son message ;
- ajout des 64 derniers diagnostics au rapport de crash local ;
- aucun envoi distant, aucun identifiant joueur et aucun contenu de sauvegarde.

## Format d’un diagnostic local

```text
horodatage | niveau | tag technique | empreinte du message | classe d’exception éventuelle
```

L’empreinte permet de rapprocher deux erreurs identiques sans stocker un chemin local, une valeur d’économie ou une chaîne fournie par l’utilisateur. Le rapport reste dans `files/crash-reports/last-crash.txt` et peut être supprimé avec les données de l’application.

## Priorités

1. **Terminé** — coordinateur de production.
2. **Terminé** — rollback sur sauvegarde échouée.
3. **Terminé** — transactions multi-slots.
4. **Terminé** — catalogue de textes.
5. **Terminé** — codecs versionnés communs.
6. **Terminé** — tests interface et Android.
7. **Terminé** — publicités contextuelles.
8. **Terminé** — performance et futurs assets.
9. **Terminé** — diagnostics locaux structurés et privés.
10. Renforcer la validation release.
