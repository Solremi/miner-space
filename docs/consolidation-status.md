# Miner Space — Consolidation technique

## Corrigé

- build et version reproductibles depuis un clone neuf ;
- contrôles locaux, Android Lint et préflight release ;
- journal multi-slots avec reprise idempotente ;
- transfert planétaire, secteurs et météorites atomiques ;
- coordinateur de production avec rollback réel sur échec de sauvegarde ;
- catalogue typé de textes français ;
- primitives communes de codecs versionnés ;
- politique de layout paysage testable sans LibGDX ;
- tests JVM pour `640 × 320`, `844 × 390`, zones sûres et cibles tactiles de 48 unités ;
- tests Android instrumentés du manifeste : sauvegarde système, trafic clair, orientation et export de l’activité ;
- scripts locaux `device-check.sh` et `device-check.ps1`.

## Exécuter les tests avec appareil

Un appareil ou émulateur Android déverrouillé doit être visible par `adb`.

```sh
sh scripts/device-check.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/device-check.ps1
```

Ces scripts n’installent aucun workflow et ne déploient rien sur Google Play. Ils exécutent les tests du module `game`, installent temporairement la variante debug sur l’appareil et lancent `connectedDebugAndroidTest`.

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
6. **Terminé** — tests de layout et instrumentation Android de base.
7. Raccorder les publicités contextuelles.
8. Préparer les performances et futurs assets.
9. Uniformiser les diagnostics locaux.
10. Renforcer la validation release.
