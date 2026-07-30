# Miner Space — Monétisation

## 1. Principes

- Le jeu est entièrement jouable sans publicité.
- Les publicités récompensées sont le format principal.
- Aucune bannière permanente sur la carte.
- Aucun interstitiel dans la version 1.0, sauf décision ultérieure après tests de rétention.
- La récompense exacte est affichée avant lecture.
- Le bouton de refus est aussi lisible que le bouton d’acceptation.
- Une publicité ne garantit jamais une ressource exceptionnellement rare majeure.

## 2. Offres de la version 1.0

| Offre | Récompense | Limite quotidienne | Délai minimal |
|---|---|---:|---:|
| Relais temporel | -25 % sur une tâche en cours, maximum 2 h retirées | 5 | 10 min |
| Production hors ligne | Double jusqu’à 8 h de production standard | 1 | par retour |
| Capsule sponsorisée | Matériaux standards équivalents à 10 min de production | 3 | 20 min |
| Contrat premium | Un contrat à coefficient maximal 1,75 | 2 | 30 min |
| Balise d’analyse | Analyse gratuite d’une anomalie standard | 2 | 30 min |
| Drone météoritique | Récupère jusqu’à 25 % des fragments standards manqués | 2 | par événement |
| Prolongation météoritique | +15 secondes, une seule fois | 1 | par événement |
| Boost orbital | +25 % sur une catégorie pendant 15 min | 2 | 60 min |

Plafond global conseillé : 10 publicités récompensées engagées par jour. Ce plafond est piloté par configuration et peut être réduit après test.

## 3. Déblocage progressif

- Aucune publicité pendant les premières minutes.
- Le doublement hors ligne apparaît après la première absence significative.
- Le relais temporel apparaît après la première tâche de plus de 5 minutes.
- Les offres météoritiques apparaissent après la pluie guidée.
- Les contrats premium apparaissent après introduction des contrats standards.
- Une seule offre est mise en avant à la fois.

## 4. Habillage dans l’univers

- Transmission commerciale orbitale.
- Capsule de ravitaillement sponsorisée.
- Relais de compression temporelle.
- Drone de récupération orbital.
- Balise d’analyse externe.

L’habillage ne doit jamais masquer la mention « publicité récompensée ».

## 5. Règles d’attribution

- Un identifiant unique est créé avant ouverture du SDK.
- La récompense est préparée mais non appliquée.
- Le callback de récompense passe l’offre à `SDK_REWARDED`.
- Une transaction atomique applique la récompense.
- L’état `COMMITTED` est sauvegardé.
- Tout callback dupliqué est ignoré.
- Une fermeture après callback mais avant animation reprend l’animation, pas la récompense.

## 6. Échecs et hors connexion

- Si aucune publicité n’est disponible, afficher une explication courte et fermer l’offre.
- Ne jamais désactiver l’action normale.
- Ne pas consommer la limite en cas d’échec ou d’annulation avant récompense.
- Aucun bouton répété agressivement après un échec.
- Les offres non pertinentes sont cachées hors connexion.

## 7. Consentement et confidentialité

- Utiliser UMP lorsque nécessaire.
- Conserver la preuve de l’état de consentement localement.
- Permettre la réouverture des préférences publicitaires dans les paramètres.
- Afficher la politique de confidentialité avant publication.
- Documenter les données collectées par le SDK publicitaire.
- Ne pas mélanger les données de gameplay personnelles avec le ciblage publicitaire.

## 8. Équilibrage

- La publicité modérée ne doit pas réduire de plus de 25 % le temps normal jusqu’au prestige.
- Un usage intensif ne doit pas diviser ce temps par plus de deux.
- Les récompenses sont comparées à la production réelle du joueur, pas à une valeur fixe devenue obsolète.
- Les boosts ne se multiplient pas entre eux sans plafond.
- Les fragments rares majeurs, Noyaux Stellaires et objets narratifs ne sont pas directement vendus par publicité.

## 9. Analytique minimale

Événements anonymisés :

- offre affichée ;
- offre refusée ;
- publicité lancée ;
- publicité échouée ;
- récompense SDK ;
- récompense engagée ;
- type de récompense ;
- progression du joueur au moment de l’offre.

Aucun identifiant publicitaire n’est stocké dans la sauvegarde de gameplay.

## 10. Tests

- SDK indisponible ;
- hors connexion ;
- fermeture pendant publicité ;
- callback dupliqué ;
- rotation paysage au retour ;
- limite quotidienne ;
- changement de jour ;
- consentement refusé ;
- récompense avec stockage plein ;
- boost déjà actif ;
- publicité pendant un événement ou une narration : l’offre doit être empêchée.
