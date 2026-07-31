# Ferrum Delta 1.0 — Validation du contenu

## Budget livré

Le manifeste `assets/data/ferrum-delta-v1.json` et `FerrumDeltaContentFactory` constituent la source mesurable de Ferrum Delta.

| Domaine | Volume |
|---|---:|
| Secteurs | 14 |
| Gisements | 34 |
| Ressources brutes | 9 |
| Matériaux raffinés | 9 |
| Composants | 24 |
| Technologies | 14 |
| Modules | 24 |
| Bâtiments | 15 |
| Missions principales | 42 |
| Missions secondaires | 36 |
| Missions de maîtrise ou collection | 20 |
| Contrats | 12 |
| Exploits | 8 |
| Événements facultatifs | 12 |
| Entrées de Codex | 120 |
| Collections | 10 |
| Jalons narratifs | 5 |
| Transmissions NOVA | 12 |

Les 24 modules sont répartis en 10 standards, 8 améliorés, 4 avancés et 2 exceptionnels. Les ensembles Forge et Survey conservent chacun suffisamment de pièces pour leurs synergies.

## Garanties structurelles

Le validateur refuse le contenu lorsque :

- un budget n’est pas atteint ;
- un identifiant est dupliqué ;
- une dépendance de secteur, technologie ou mission forme un cycle ;
- une recette référence une entrée inconnue ;
- un secteur ne possède pas de nouveauté stratégique ;
- une ressource brute ne possède pas deux gisements et une source garantie ;
- une ressource obligatoire dépend uniquement d’un événement ;
- un événement devient obligatoire ;
- une collection ou un jalon narratif référence une entrée inexistante.

## Simulations

Le simulateur utilise 32 000 points de progression et des profils configurables.

| Profil | Sans publicité | Avec bonus maximal |
|---|---:|---:|
| Très actif | 22 jours | 18 jours |
| Régulier | 32 jours | 27 jours |
| Occasionnel | 50 jours | 44 jours |

Les critères de publication sont évalués sur le parcours sans publicité. Chaque profil atteint les 14 secteurs et aucune dépendance ne reste bloquée.

## Compatibilité

Les identifiants historiques du vertical slice restent présents, notamment les ressources initiales, les six premiers secteurs, les deux premières technologies, les composants de base et les ressources rares déjà sauvegardées.

Les fichiers actifs `sectors.json` et `narrative.json` conservent leur version de contenu précédente afin que les slots existants puissent être normalisés sans migration destructive.

## Validation Android restante

La simulation ne remplace pas une partie réelle. Une nouvelle sauvegarde doit encore être parcourue sur appareil pour mesurer les temps, les interactions tactiles, la lisibilité du volume complet et la stabilité en qualité faible.
