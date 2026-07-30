# Miner Space — Frontière interplanétaire

## 1. Objectif

La frontière prolonge la progression après les planètes scénarisées disponibles. Elle ne remplace pas les planètes principales conçues manuellement.

La version 1.0 utilise une génération contrôlée à partir d’éléments validés, pas une génération totalement libre.

## 2. Graine et persistance

Chaque monde possède :

- `worldSeed` ;
- famille visuelle ;
- niveau de difficulté ;
- secteurs sélectionnés ;
- ressources ;
- modificateurs ;
- objectifs ;
- récompenses ;
- date de création ;
- état de progression.

La graine est persistée dès la présentation du monde. Recharger la sauvegarde recrée exactement le même monde.

## 3. Familles visuelles 1.0

Trois familles :

1. désert minéral ;
2. monde glacé ;
3. monde fracturé sombre.

Chaque famille dispose de :

- 8 modèles de secteurs préconstruits ;
- 2 palettes ;
- 2 variantes d’éclairage ;
- 3 ensembles de décors ;
- 1 événement principal ;
- ressources autorisées et interdites.

## 4. Modificateurs 1.0

Douze modificateurs validés, par exemple :

- gisements riches mais petits ;
- extraction lente et raffinage rapide ;
- stockage réduit ;
- forte production solaire ;
- énergie instable ;
- robots plus efficaces hors ligne ;
- secteurs coûteux mais ressources rares ;
- météorites fréquentes ;
- anomalies nombreuses ;
- contrats très rentables mais exigeants ;
- logistique lente ;
- modules d’une famille renforcés.

Chaque modificateur déclare :

- tags requis ;
- tags interdits ;
- difficulté ;
- impact économique ;
- texte joueur ;
- paramètres numériques.

## 5. Compatibilités

Le générateur interdit notamment :

- énergie trop faible sans source alternative ;
- stockage très faible avec logistique très lente ;
- ressource obligatoire absente ;
- objectif nécessitant une technologie impossible à débloquer ;
- malus planétaire annulant totalement une spécialisation ;
- plusieurs modificateurs donnant une progression quasi nulle ;
- récompense rare sans moyen de compléter l’objectif.

Une matrice de compatibilité est testée automatiquement.

## 6. Construction d’un monde

1. choisir la difficulté ;
2. choisir une famille différente du monde précédent si possible ;
3. choisir une taille ;
4. sélectionner les secteurs ;
5. garantir la chaîne de ressources minimale ;
6. choisir 2 modificateurs en difficulté normale, 3 en difficile ;
7. générer les objectifs ;
8. simuler la progression ;
9. rejeter le monde s’il échoue ;
10. persister la graine et présenter le monde.

## 7. Chaîne minimale garantie

Tout monde doit permettre :

- une ressource structurelle ;
- une ressource conductrice ;
- une source énergétique ;
- une recette de raffinage ;
- un composant de robot ;
- une amélioration de scanner ou d’accès ;
- un objectif final réalisable.

Les ressources peuvent changer de nom ou de variante, mais leurs rôles économiques restent couverts.

## 8. Objectifs générés

Six modèles 1.0 :

- atteindre un rendement ;
- produire une technologie ;
- ouvrir un nombre de secteurs ;
- compléter une collection locale ;
- stabiliser une contrainte énergétique ;
- analyser une anomalie finale.

Un monde possède :

- 1 objectif principal ;
- 3 à 6 objectifs secondaires ;
- 1 objectif de maîtrise optionnel ;
- aucun objectif imposant une publicité.

## 9. Difficulté et durée

- normale : 2 à 3 jours ;
- avancée : 3 à 5 jours ;
- extrême : 5 à 7 jours.

La difficulté augmente par complexité et contraintes, pas seulement par multiplication des coûts.

## 10. Récompenses

- Noyaux Stellaires plafonnés ;
- fragments de modules ;
- apparences ;
- entrées de Codex ;
- plans interplanétaires ;
- amélioration permanente faible ;
- aucun multiplicateur infini non contrôlé.

## 11. Anti-répétition

Le générateur conserve l’historique des cinq derniers mondes et évite :

- même famille deux fois de suite ;
- même paire de modificateurs ;
- même objectif principal ;
- mêmes secteurs dans le même ordre ;
- même récompense majeure répétée.

## 12. Retour et abandon

- Le joueur peut abandonner un monde après une période minimale sans perdre ses collections permanentes.
- L’abandon ne donne pas la récompense finale.
- Une confirmation affiche précisément ce qui est conservé et perdu.
- La version 1.0 conserve le dernier monde terminé en lecture seule dans l’historique.
- Le retour libre sur toutes les anciennes bases est postérieur à la 1.0.

## 13. Validation automatique

Avant présentation, une simulation vérifie :

- chaîne de fabrication réalisable ;
- aucune ressource obligatoire absente ;
- aucun coût négatif ou débordement ;
- durée estimée dans la plage ;
- au moins trois objectifs parallèles ;
- aucune combinaison interdite ;
- récompense cohérente avec la difficulté.

Le test de build génère au moins 10 000 mondes et exige zéro monde impossible.

## 14. Extension future

Ajouter une nouvelle famille ne doit demander que :

- secteurs ;
- règles visuelles ;
- ressources autorisées ;
- événements ;
- modificateurs ;
- tests de compatibilité ;
- données de localisation.

Aucune modification du moteur économique central ne doit être nécessaire.
