# Miner Space — UX, paysage et accessibilité

## 1. Orientation

- Le gameplay est verrouillé en `sensorLandscape`.
- Les deux sens paysage sont pris en charge.
- Formats de référence : 640 × 320 et 844 × 390.
- Formats de contrôle : 960 × 540, 1280 × 720, tablettes 4:3 et écrans pliables.
- Le portrait n’est pas un format de gameplay.
- Les écrans de consentement, confidentialité, erreur et restauration restent utilisables si le système les affiche en portrait.

## 2. Viewport et zones sûres

- Utiliser une résolution logique de référence, jamais des coordonnées d’écran fixes.
- Respecter les insets Android, encoches, barres de navigation et coins arrondis.
- Les panneaux peuvent se compacter, se superposer ou devenir des tiroirs.
- Aucun contenu indispensable ne doit se trouver sous une zone système.
- Le monde 3D remplit l’espace restant ; le HUD ne réduit pas excessivement la zone de jeu.

## 3. HUD principal

### Barre supérieure

- SpaceDollars ;
- énergie ;
- stockage ;
- ressource épinglée ;
- paramètres.

### Zone gauche

- mission principale ;
- deux objectifs secondaires maximum visibles ;
- accès à la liste complète.

### Zone droite

- production ;
- robots ;
- technologies ;
- carte des secteurs ;
- événement actif.

### Zone basse

- panneau contextuel de l’élément sélectionné ;
- hauteur limitée ;
- bouton de fermeture clair ;
- actions principales accessibles au pouce.

Sur 640 × 320, les raccourcis secondaires sont regroupés dans un bouton unique et le panneau bas utilise un état compact.

## 4. Navigation

- Une action fréquente : 2 pressions maximum.
- Bouton Base toujours accessible.
- Bouton Mission centre la cible active.
- Retour Android ferme d’abord le panneau supérieur, puis demande confirmation uniquement pour quitter le jeu.
- Aucun empilement incontrôlé de fenêtres.
- Une modale critique à la fois.

## 5. Écrans obligatoires

- chargement ;
- menu principal ;
- carte ;
- gisement ;
- production ;
- robots ;
- inventaire ;
- technologies ;
- missions ;
- contrats ;
- secteurs ;
- spécialisations ;
- modules ;
- Codex ;
- archives ;
- transfert planétaire ;
- paramètres ;
- accessibilité ;
- consentement publicitaire ;
- confidentialité ;
- crédits et licences ;
- restauration de sauvegarde ;
- erreur de chargement ;
- migration ;
- absence d’espace de stockage ;
- reprise après crash.

## 6. Règles d’information

Une recette affiche toujours :

- entrées ;
- quantités possédées ;
- quantité réservée ;
- durée ;
- robot ;
- énergie ;
- résultat ;
- valeur de vente ;
- synergies ;
- prérequis ;
- raison du blocage ;
- bouton pour localiser la ressource manquante.

Une amélioration affiche :

- valeur actuelle ;
- valeur future ;
- coût ;
- temps ;
- effet visuel attendu ;
- éventuelle contrainte nouvelle.

## 7. Accessibilité

Options indépendantes :

- taille de texte : 100 %, 115 %, 130 % ;
- contraste élevé ;
- palettes adaptées aux principaux daltonismes ;
- formes et icônes en complément des couleurs ;
- réduction des animations ;
- réduction des flashs ;
- désactivation des vibrations ;
- zones tactiles agrandies ;
- vitesse réduite pour événements interactifs ;
- collecte assistée des fragments ;
- volume séparé musique, ambiance et effets ;
- jeu entièrement compréhensible sans son.

Les textes agrandis peuvent augmenter la hauteur des panneaux ou activer un défilement interne, mais ne provoquent jamais un overflow global.

## 8. Pluie de météorites

- zone de jeu clairement délimitée ;
- trajectoires lisibles ;
- fragments d’au moins 48 dp en mode assistance ;
- toucher ou glisser ;
- pas de pénalité pour fragment manqué ;
- couleurs doublées par formes et traînées ;
- pause lors de perte de focus ;
- HUD économique simplifié pendant l’événement ;
- bouton Quitter l’événement sans perte des fragments déjà collectés.

## 9. Satisfaction et feedback

Chaque action importante possède :

1. confirmation immédiate ;
2. changement visible dans le monde ou le panneau ;
3. mise à jour animée du compteur ;
4. vibration facultative adaptée ;
5. message seulement si nécessaire.

Les actions répétitives utilisent des animations de moins d’une seconde. Les jalons majeurs peuvent être plus longs et passables après leur première lecture.

## 10. Tutoriel

- tutoriel contextuel, pas de longue page de texte ;
- une seule notion nouvelle à la fois ;
- reprise possible après fermeture ;
- possibilité de revoir les étapes ;
- possibilité d’ignorer les explications secondaires ;
- premières actions garanties : extraction, raffinage, robot, secteur, module, météorites ;
- aucune publicité avant la fin du noyau du tutoriel.

## 11. Critères d’acceptation

Pour chaque écran :

- capture validée en 640 × 320 ;
- capture validée en 844 × 390 ;
- test avec texte à 130 % ;
- test avec insets simulés ;
- aucune cible inférieure à 48 dp ;
- aucun texte tronqué sans mécanisme de lecture ;
- aucune information dépendant uniquement de la couleur ;
- navigation Retour cohérente ;
- action principale identifiable en moins de quelques secondes lors d’un test utilisateur.
