# Miner Space — Secteurs et exploration

## 1. Portée de l’étape 6

Cette étape valide le système d’exploration avec six secteurs de Ferrum Delta. Le budget final de la version 1.0 reste fixé à quatorze secteurs ; les secteurs supplémentaires seront produits après validation du vertical slice.

## 2. États d’un secteur

Un secteur est dans l’un des états suivants :

1. **inconnu** : recouvert par le brouillard, seul son emplacement est visible ;
2. **scanné** : nom, intérêt, coût et prérequis sont révélés ;
3. **ouvert** : le terrain, la mission et les découvertes rares sont accessibles.

Le scan ne consomme aucune monnaie. Il exige le niveau de scanner et l’ouverture des secteurs précédents. L’ouverture consomme atomiquement les SpaceDollars et composants annoncés.

## 3. Secteurs du vertical slice

| Secteur | Coût | Scanner | Prérequis | Raison stratégique |
|---|---:|---:|---|---|
| Noyau Delta | 0 SD | 1 | aucun | base, fer et première chaîne |
| Crête cuivrée | 120 SD | 1 | Noyau Delta | cuivre pour plaques et piles |
| Plaines cristallines | 280 SD | 1 | Crête cuivrée | cristal pour capteurs et technologies |
| Passe logistique | 450 SD + 2 piles | 2 | Noyau Delta + protocole d’extraction | raccourci logistique et ferrite prismatique |
| Profondeurs xénon | 900 SD + 3 capteurs | 2 | Crête cuivrée + protocole d’extraction | ressource profonde et mission |
| Ruines d’archive | 1 400 SD + 2 piles + 2 capteurs | 3 | Xénon + Cristal + tri quantique | archive, mission et fragment garanti |

## 4. Scanner

Le scanner commence au niveau 1 :

- `tech_extraction_protocol` ajoute un niveau ;
- `tech_quantum_sorting` ajoute un niveau supplémentaire.

Le niveau requis est toujours affiché. Aucun secteur ne peut être payé avant d’avoir été scanné.

## 5. Gisements rares

Trois découvertes sont garanties et non aléatoires :

- ferrite prismatique dans la Passe logistique ;
- cristal xénon dans les Profondeurs xénon ;
- fragment d’archive dans les Ruines d’archive.

La ressource correspondante est attribuée une seule fois lors de l’ouverture. Elle est non vendable automatiquement et reste sauvegardée dans l’inventaire principal.

## 6. Missions et caméra

Le secteur de mission actif est persisté. Le bouton `MISSION` centre immédiatement la caméra sur sa zone. Un double toucher sur un secteur le centre également. Les limites de caméra empêchent toute sortie de carte.

## 7. Sauvegarde et transactions

La progression des secteurs utilise le slot `exploration`, séparé du slot économique principal. Les deux bénéficient des snapshots alternés, checksums et écritures atomiques.

Lors d’une ouverture :

1. les prérequis sont vérifiés sur l’état économique courant ;
2. le prochain état économique est écrit ;
3. le prochain état d’exploration est écrit ;
4. si la seconde écriture échoue, l’état économique précédent est réécrit.

Une ouverture déjà validée est idempotente et ne peut pas être payée deux fois.

## 8. Performance et accessibilité

L’ouverture utilise un masque rectangulaire rétractable. Aucun système de particules n’est requis, ce qui conserve un coût faible sur appareil modeste. Les informations importantes utilisent texte, forme et état, jamais uniquement la couleur. Les boutons mesurent au moins 48 unités et respectent les zones sûres.
