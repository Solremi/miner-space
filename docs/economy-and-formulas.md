# Miner Space — Économie et formules

## 1. Principes

- Les stocks, monnaies, réserves et quantités utilisent `Long`.
- Les probabilités utilisent des points de base : 10 000 = 100 %.
- Les multiplicateurs utilisent une représentation fixe en millionièmes : 1 000 000 = ×1.
- Aucun calcul critique ne dépend d’un `Float` ou `Double` non contrôlé.
- Toutes les valeurs viennent de fichiers de configuration versionnés.

## 2. Ordre officiel des calculs

Les bonus d’une même catégorie sont d’abord additionnés, puis les catégories sont multipliées dans l’ordre suivant :

```text
base
× robot
× modules
× synergies
× spécialisation
× technologies
× planète
× événement
× prestige
```

Formule :

```text
productionThéorique = floor(
  rendementBase
  × robotMultiplier
  × moduleMultiplier
  × synergyMultiplier
  × specializationMultiplier
  × technologyMultiplier
  × planetMultiplier
  × eventMultiplier
  × prestigeMultiplier
)
```

Règles :

- arrondi final à l’entier inférieur ;
- résultat minimum de 1 uniquement si la tâche est valide et que le rendement de base est supérieur à 0 ;
- plafond de réduction du temps : 80 % hors effet narratif temporaire ;
- aucun bonus unique supérieur à ×2 en version 1.0 ;
- les bonus publicitaires sont appliqués en dernier et plafonnés séparément.

## 3. Extraction

```text
quantitéExtraite = min(
  réserveDisponible,
  capacitéTransportDisponible,
  capacitéStockageDisponible,
  productionThéorique × tempsActif
)
```

- Un gisement ne devient jamais négatif.
- Si le stockage est plein, l’extraction ralentit puis s’arrête avec une explication visible.
- Un robot affecté à un gisement épuisé passe en attente et ne change pas automatiquement de cible sans règle explicite.
- Les variantes exceptionnelles sont tirées uniquement sur une unité réellement produite.

## 4. Raffinage et assemblage

Une tâche réserve ses entrées au lancement.

```text
finPrévue = début + duréeBase × duréeMultipliers
```

- Une annulation standard restitue 100 % avant 10 % d’avancement, puis 80 % jusqu’à 90 %, puis aucun remboursement après achèvement.
- Une recette peut définir une autre règle, affichée avant lancement.
- Si le stockage de sortie est plein, le produit terminé reste dans une zone de collecte liée à la tâche.
- Changer les modules après lancement ne modifie pas rétroactivement une tâche, sauf règle de recette explicite.
- Les bonus sont figés dans un `ProductionSnapshot` au lancement.

## 5. Prix de vente

```text
valeurEntrées = somme(quantitéEntrée × valeurUnitaireEntrée)
valeurTemps = duréeBaseSecondes × valeurSecondeCatégorie
prixBase = valeurEntrées + valeurTemps
prixVente = floor(prixBase × rareté × qualité × contrat)
```

Garde-fous :

- vendre brut rapporte moins que raffiner sur un horizon raisonnable ;
- fabriquer un composant est plus rentable mais immobilise un robot ;
- une technologie obligatoire n’est pas systématiquement plus rentable à vendre qu’à installer ;
- les ressources exceptionnelles ne sont jamais proposées à la vente rapide ;
- un contrat ne dépasse pas 2,5 fois la valeur standard en version 1.0.

## 6. Coûts

Valeurs de départ :

- amélioration fréquente : coefficient 1,35 à 1,55 ;
- robot : coefficient 1,60 à 1,75 ;
- secteur : coefficient de base 1,55 avec ajustement manuel ;
- module : coefficient 1,30 à 1,50 plus matériaux spécialisés ;
- technologie majeure : coût manuel validé par simulation.

```text
coûtNiveau = ceil(coûtBase × coefficient^(niveau - 1))
```

Les coûts dépassant la plage de progression prévue doivent être définis manuellement.

## 7. Énergie

L’énergie distingue puissance et capacité.

```text
puissanceProduite = somme(générateurs actifs)
puissanceDemandée = somme(consommateurs actifs)
énergieStockée = valeur courante des batteries
capacitéBatterie = capacité maximale
```

À chaque pas de simulation :

1. la production instantanée alimente la demande ;
2. le surplus charge les batteries dans la limite de la vitesse de charge ;
3. le déficit utilise les batteries dans la limite de la vitesse de décharge ;
4. le déficit restant réduit les consommateurs selon leur priorité ;
5. la vitesse minimale d’un système alimenté partiellement est affichée.

Priorités par défaut :

1. fonctions de sécurité et sauvegarde ;
2. missions épinglées ;
3. extraction ;
4. raffinage ;
5. assemblage ;
6. recherche.

Cryos IX ajoute chaleur produite, chaleur demandée, stockage thermique et pertes thermiques.

## 8. Progression hors ligne

La simulation hors ligne utilise des segments d’événements et non une simple multiplication moyenne.

```text
tempsSimulé = min(tempsAbsent, capacitéHorsLigne)
```

Le moteur avance jusqu’au prochain événement :

- fin de tâche ;
- saturation de stockage ;
- épuisement de gisement ;
- batterie vide ou pleine ;
- fin d’un boost ;
- changement de priorité planifié.

Il applique ensuite le nouvel état jusqu’à la fin de la période.

Plafonds :

- 8 heures au départ ;
- améliorable jusqu’à 24 heures ;
- aucune production après saturation ;
- aucune ressource exceptionnelle générée hors ligne par une activité interactive manquée ;
- les tâches terminées restent collectables.

## 9. Ressources exceptionnelles

Chaque source possède :

- probabilité de base ;
- compteur de protection contre la malchance ;
- seuil d’augmentation ;
- probabilité maximale ;
- remise à zéro après succès.

Exemple :

```text
chance = min(chanceBase + échecsConsécutifs × incrément, chanceMax)
```

- La première ressource exceptionnelle obligatoire pour expliquer le système est accordée par mission.
- Aucune publicité ne garantit une ressource exceptionnelle majeure.
- Le résultat aléatoire est enregistré avec un identifiant d’événement avant l’animation.

## 10. Simulateur d’équilibrage

Le simulateur exécute au minimum :

- joueur très actif ;
- joueur régulier ;
- joueur occasionnel ;
- zéro publicité ;
- publicité modérée ;
- chaque spécialisation ;
- plusieurs combinaisons de modules ;
- 60 jours de jeu ;
- transfert vers Cryos IX.

Alertes automatiques :

- stock ou monnaie négative ;
- progression bloquée plus de 24 heures sans objectif parallèle ;
- spécialisation dominante sur tous les indicateurs ;
- recette jamais rentable ni nécessaire ;
- ressource obligatoire sans source garantie ;
- temps de prestige hors plages cibles.
