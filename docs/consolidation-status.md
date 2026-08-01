# Miner Space — Consolidation technique

## Priorités traitées

1. **Production extraite de l’écran** — `ManufacturingCoordinator` possède les moteurs, l’état, la simulation et la persistance.
2. **Rollback réel** — une action de production n’est publiée en mémoire qu’après sauvegarde réussie.
3. **Transactions multi-slots** — transfert planétaire, secteurs, météorites, robots, stratégie, missions et consommation publicitaire sont reprenables et idempotents.
4. **Textes centralisés** — catalogue typé français, paramètres contrôlés et mappings de messages gameplay.
5. **Codecs mutualisés** — lecteur, writer et collections versionnés communs, sans casser les formats existants.
6. **Tests interface et Android** — layouts paysage compacts, zones sûres, manifeste, orientation, sauvegarde système et trafic clair.
7. **Publicités contextuelles** — retour hors ligne et prolongation météorique avec `scopeId`, limites et consommation atomique.
8. **Performance et futurs assets** — registre à références comptées, groupes de durée de vie, budgets qualité et moniteur de frames.
9. **Diagnostics locaux** — tampon borné, empreintes privées et contexte technique intégré au rapport de crash local.
10. **Validation release** — contrôle du dépôt, modèle privé de preuves et décision Go/No-Go reproductible.
11. **Transactions avancées** — améliorations de robots, transferts logistiques, spécialisations, modules, missions, contrats et collections sont engagés via le journal multi-slots.

## Commandes locales

```sh
sh scripts/quality-check.sh
sh scripts/device-check.sh
sh scripts/release-readiness.sh --repository-only
sh scripts/release-preflight.sh
sh scripts/release-readiness.sh
```

Les équivalents PowerShell sont disponibles dans `scripts/`.

## État réel

Le dépôt possède maintenant les garde-fous nécessaires pour poursuivre le développement proprement. Les sons, musiques, modèles 3D, textures et VFX définitifs restent volontairement absents et pourront être intégrés via le registre d’assets lorsque les fichiers seront prêts.

La publication reste **NO-GO** tant que les preuves externes suivantes ne sont pas complètes : clé et bundle signés, installation sur appareils, migrations réelles, matrice paysage/accessibilité, performance faible appareil, consentement, test fermé, absence de crash/ANR, politique HTTPS, visuels et formulaires Play Console.

Aucun workflow, aucune CI/CD et aucun déploiement automatique n’est présent ou requis par cette consolidation.
