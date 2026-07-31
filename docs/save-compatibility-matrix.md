# Miner Space — Compatibilité des sauvegardes 1.0

## Principes

- snapshots alternés et checksum conservés ;
- le dernier snapshot valide peut remplacer un snapshot corrompu ;
- les schémas anciens sont décodés puis réécrits uniquement après validation ;
- aucun slot inconnu n’est supprimé automatiquement ;
- les données optionnelles invalides peuvent être réinitialisées sans toucher à l’économie principale.

## Matrice

| Slot | Format 1.0 | Compatibilité antérieure |
|---|---:|---|
| primary | codec courant | formats 1 à 3 pris en charge par ManufacturingSnapshotCodec |
| presentation | 2 | format 1 migré avec valeurs d’accessibilité sûres |
| sectors | 1 | normalisation des identifiants conservés |
| robots | 1 | état initial uniquement si aucun snapshot valide |
| strategy | 1 | modules et spécialisation normalisés |
| progression | 1 | tutoriel, missions, Codex et contrats conservés |
| narrative | 1 | transmissions, archives et récompenses en attente conservées |
| prestige | 1 | transfert préparé repris de manière idempotente |
| cryos_ix | 1 | état thermique complet conservé |
| frontier | 1 | graine et définitions complètes des mondes conservées |
| rewarded_ads | 1 | demandes SDK_REWARDED reprises sans duplication |

## Validation manuelle obligatoire

Conserver un échantillon réel de chaque version de test diffusée, installer la version 1.0 par-dessus, parcourir les écrans concernés, fermer de force l’application et comparer les totaux avant et après.
