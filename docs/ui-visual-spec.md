# Miner Space — Spécification visuelle UI / HUD

## 1. Objectif

Définir l’apparence et l’organisation de l’interface afin d’obtenir une UX paysage claire sur mobile.

## 2. Orientation et formats

- Gameplay verrouillé en paysage (`sensorLandscape`).
- Références principales : `640 × 320` et `844 × 390`.
- Références secondaires : `960 × 540`, `1280 × 720`, tablettes.

## 3. Principes UI

- Une information importante = un emplacement stable.
- L’écran principal reste centré sur la carte.
- Les panneaux doivent pouvoir se replier.
- Le joueur ne doit jamais perdre la vue de la base plus de quelques secondes.
- Les overlays doivent être rapides à ouvrir et fermer.

## 4. Layout du HUD principal

## 4.1 Barre supérieure

Contenu :

- SpaceDollars ;
- énergie ;
- stockage ;
- ressource suivie ;
- bouton paramètres.

Style :

- bande sombre translucide ;
- coins légèrement arrondis ;
- pictogrammes à gauche, valeur à droite ;
- animation brève lors des changements.

## 4.2 Colonne gauche

- mission principale ;
- jusqu’à 2 objectifs secondaires ;
- progression de planète ;
- bouton d’accès aux missions complètes.

Sur `640 × 320`, la colonne gauche devient plus compacte et l’objectif secondaire 2 peut passer dans un état replié.

## 4.3 Colonne droite

Raccourcis verticaux :

- production ;
- robots ;
- technologies ;
- inventaire ;
- Codex ;
- carte des secteurs.

Règles :

- icône circulaire ou rectangle arrondi ;
- badge discret si action utile ;
- pas d’animation permanente parasite.

## 4.4 Bas d’écran

Le panneau contextuel inférieur affiche l’élément sélectionné :

- gisement ;
- robot ;
- bâtiment ;
- mission ciblée.

Comportement :

- mode compact par défaut ;
- extension par glissement ou bouton ;
- action principale toujours visible.

## 4.5 Centre inférieur

- bouton recentrer la base ;
- bouton centrer mission ;
- indicateur d’événement discret.

## 5. Écrans / panneaux clés

## 5.1 Panneau gisement

Doit afficher :

- nom ;
- type de ressource ;
- réserve ;
- rendement ;
- niveau requis ;
- coût de déblocage ;
- bonus / malus ;
- action principale.

## 5.2 Panneau raffinage

- recettes épinglées ;
- filtres ;
- files en cours ;
- bouton « produire à nouveau » ;
- temps restant lisible ;
- ressource manquante localisable.

## 5.3 Panneau robots

Chaque carte robot doit montrer :

- nom ;
- type ;
- niveau ;
- maîtrise ;
- modules ;
- spécialisation ;
- action rapide.

## 5.4 Panneau spécialisation

Chaque spécialisation a besoin de :

- illustration ou emblème ;
- résumé en une phrase ;
- 3 à 4 avantages ;
- 2 à 3 contraintes ;
- aperçu visuel de la base ;
- bouton d’essai / choix / changement.

## 5.5 Panneau modules

- filtres par type de robot ;
- comparaison gauche/droite ;
- synergies actives ;
- verrouillage manuel ;
- rareté lisible ;
- action équiper / améliorer / démonter.

## 5.6 Codex

Le Codex doit être très satisfaisant visuellement.

Prévoir :

- catégories ;
- progression ;
- silhouettes ;
- carte de découverte ;
- affichage 3D ou illustré simplifié si possible.

## 5.7 Offre de publicité récompensée

Le panneau doit utiliser une intégration diégétique :

- transmission orbitale ;
- capsule sponsorisée ;
- relais temporel.

Mais il doit surtout être clair :

- mention “publicité récompensée” ;
- récompense exacte ;
- bouton refuser ;
- bouton accepter.

## 6. Style visuel des composants UI

## 6.1 Boutons

Trois niveaux :

- primaire ;
- secondaire ;
- discret / utilitaire.

Règles :

- boutons primaires lumineux ;
- boutons secondaires plus sobres ;
- états pressés nets ;
- état désactivé compréhensible.

## 6.2 Icônes

- géométriques ;
- simples ;
- cohérentes ;
- reconnaissables à petite taille ;
- compatibles avec contours clairs ou fond sombre.

## 6.3 Cartes et panneaux

- fond semi-opaque ;
- bord lumineux fin ;
- hiérarchie interne stricte ;
- coin haut : titre ;
- centre : contenu ;
- bas : action principale.

## 6.4 Feedbacks

Prévoir pour l’UI :

- variation de taille au clic ;
- halo discret ;
- vibration optionnelle ;
- compteur animé ;
- confirmation visuelle en 200–350 ms.

## 7. Responsive paysage

## 7.1 640 × 320

- priorité absolue à l’action principale ;
- texte court ;
- certains labels raccourcis ;
- panneaux repliables ;
- une seule sous-action visible à la fois si nécessaire.

## 7.2 844 × 390 et plus

- davantage d’informations persistantes ;
- meilleure visibilité des objectifs ;
- raccourcis plus confortables ;
- panneaux plus riches.

## 8. Accessibilité visuelle

- taille de texte réglable ;
- contraste élevé ;
- daltonisme ;
- icônes + couleurs ;
- zones tactiles généreuses ;
- réduction des animations ;
- confirmation pour actions à risque.

## 9. Maquettes à produire plus tard

À dériver depuis ce document :

1. HUD principal 640 × 320.
2. HUD principal 844 × 390.
3. panneau gisement.
4. panneau raffinage.
5. panneau robots.
6. panneau spécialisation.
7. panneau modules.
8. Codex.
9. offre publicitaire.
10. écran de transfert planétaire.
