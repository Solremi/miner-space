# Miner Space — Plan de développement complet

> Document directeur du projet. Il décrit la vision, le gameplay, l’économie, l’architecture Kotlin, la direction visuelle, la monétisation et l’ordre de développement.

## 1. Vision du jeu

**Miner Space** est un jeu mobile de gestion, d’automatisation et de progression spatiale en vue 2.5D. Le joueur exploite une planète, découvre des gisements, extrait des matériaux bruts, les raffine, fabrique des composants puis assemble des technologies spatiales de plus en plus avancées.

Chaque technologie améliore directement la capacité d’exploitation : vitesse de minage, capacité de stockage, nombre de files de production, portée des scanners, efficacité des robots, accès à des gisements rares et ouverture de nouvelles zones.

La première planète doit proposer environ **30 jours de progression raisonnable** pour un joueur régulier. Une fois les objectifs majeurs terminés, le joueur construit un vaisseau et part vers une nouvelle planète. Il recommence avec un nouvel environnement, de nouvelles ressources et de nouvelles contraintes, tout en conservant des améliorations permanentes et un multiplicateur de progression.

### Positionnement

- Jeu mobile Android en Kotlin.
- Gestion et automation accessibles, sans contrôles complexes.
- Carte 2.5D manipulable au doigt : déplacement, zoom, sélection des gisements et bâtiments.
- Sessions courtes de 2 à 8 minutes, avec progression hors ligne.
- Progression longue, lisible et régulière.
- Monétisation principalement par publicités récompensées.
- Direction artistique immersive, propre et spectaculaire.
- Aucun travail sonore dans les premières étapes ; le système audio sera préparé mais non alimenté.

## 2. Piliers de conception

### 2.1 Une chaîne de transformation satisfaisante

Chaque ressource doit traverser plusieurs états visibles :

1. Gisement découvert.
2. Minerai brut extrait.
3. Minerai transporté et stocké.
4. Matériau raffiné sous forme de cristal, lingot ou diamant énergétique.
5. Composant spatial basique assemblé.
6. Module technologique avancé construit.
7. Technologie installée produisant un avantage concret.

Le joueur doit voir les matériaux circuler et comprendre immédiatement ce qui bloque sa progression.

### 2.2 Une automation qui se construit progressivement

Au départ, le joueur intervient souvent. Ensuite, il construit des robots spécialisés, améliore leurs performances, augmente les files d’attente et automatise progressivement son complexe.

L’objectif n’est pas de supprimer le gameplay, mais de déplacer les décisions : le joueur passe de l’action manuelle à l’optimisation des priorités, des flux et des investissements.

### 2.3 Une carte qui donne envie d’explorer

La planète n’est pas un simple menu. C’est une carte vivante comprenant :

- une base centrale ;
- des gisements visibles ou cachés ;
- des secteurs verrouillés ;
- des reliefs et éléments environnementaux ;
- des zones rares ;
- des événements visuels ;
- des routes logistiques ;
- des structures construites par le joueur.

Chaque extension de territoire doit changer visuellement la carte et révéler un nouvel objectif.

### 2.4 Des améliorations visibles

Une amélioration ne doit pas uniquement modifier un nombre. Elle doit, lorsque possible :

- ajouter une animation ;
- transformer le modèle du bâtiment ou du robot ;
- augmenter le nombre de drones visibles ;
- intensifier un faisceau, une lumière ou un effet de particules ;
- ouvrir une nouvelle fonction ;
- modifier la carte ou les flux logistiques.

### 2.5 Une progression sans mur artificiel

Le jeu doit ralentir graduellement, mais ne jamais devenir incompréhensible. Le joueur doit toujours connaître :

- son objectif actuel ;
- la ressource manquante ;
- la meilleure action possible ;
- le temps restant ;
- les alternatives disponibles.

## 3. Boucles de gameplay

### 3.1 Boucle de session courte

1. Récupérer la production terminée.
2. Vérifier les stocks et les missions.
3. Relancer les robots de raffinage et d’assemblage.
4. Améliorer un robot, une technologie ou un bâtiment.
5. Débloquer ou inspecter un nouveau gisement.
6. Choisir les priorités de production.
7. Quitter avec des tâches longues actives.

Durée cible : 2 à 8 minutes.

### 3.2 Boucle quotidienne

1. Collecter la progression hors ligne.
2. Terminer plusieurs contrats.
3. Réorganiser les files de production.
4. Dépenser les SpaceDollars.
5. Débloquer une zone ou une technologie.
6. Atteindre un jalon de mission principale.
7. Préparer les productions longues pour la nuit.

### 3.3 Boucle de planète

1. Installer la base.
2. Exploiter les ressources communes.
3. Construire les premiers robots.
4. Automatiser le raffinage.
5. Découvrir les ressources rares.
6. Construire des composants avancés.
7. Déployer les grandes technologies planétaires.
8. Terminer les missions principales.
9. Construire le vaisseau de départ.
10. Choisir les bonus permanents et partir vers la planète suivante.

## 4. Structure des ressources

Les noms ci-dessous constituent une première base de travail. Toutes les valeurs devront être stockées dans des fichiers de configuration afin de permettre l’équilibrage sans modifier le code.

### 4.1 Matériaux bruts

| Ressource | Rareté | Usage principal | Première apparition |
|---|---:|---|---|
| Ferrite | Commune | Structures, châssis, plaques | Secteur initial |
| Silicate | Commune | Verre spatial, optique, capteurs | Secteur initial |
| Carbone dense | Commune | Composites, isolants, filtres | Secteur initial |
| Cuivre stellaire | Peu commune | Conducteurs, moteurs, circuits | Début de progression |
| Cobalt | Peu commune | Batteries, alliages résistants | Début intermédiaire |
| Titane | Rare | Structures avancées et blindages | Milieu de planète |
| Iridium | Rare | Catalyseurs et moteurs avancés | Milieu avancé |
| Cristal de xénon | Épique | Énergie, scanner, propulsion | Secteurs profonds |
| Résidu d’antimatière | Légendaire | Technologies finales | Fin de planète |

### 4.2 Matériaux raffinés

Les matériaux raffinés sont représentés par des formes premium et très lisibles : diamants énergétiques, lingots, plaques, cristaux ou cellules.

| Produit raffiné | Entrées principales | Fonction |
|---|---|---|
| Plaque de ferrite | Ferrite | Construction générale |
| Cristal de silice | Silicate | Optique et scanners |
| Fibre de carbone | Carbone dense | Composites légers |
| Bobine conductrice | Cuivre stellaire | Circuits et moteurs |
| Cellule cobalt | Cobalt | Stockage énergétique |
| Alliage titane | Titane + ferrite | Châssis avancés |
| Catalyseur iridium | Iridium | Production haute technologie |
| Diamant xénon | Cristal de xénon | Énergie quantique |
| Cellule d’antimatière | Résidu d’antimatière + diamant xénon | Propulsion finale |

### 4.3 Composants spatiaux basiques

- Tête de forage renforcée.
- Châssis de robot.
- Bras manipulateur.
- Module de stockage.
- Batterie compacte.
- Panneau solaire orbital.
- Capteur optique.
- Unité de refroidissement.
- Circuit de navigation.
- Capsule logistique.

### 4.4 Technologies avancées

- Drone minier autonome.
- Scanner orbital.
- Raffinerie quantique.
- Réseau logistique automatisé.
- Réacteur à fusion.
- Extracteur profond.
- Relais de téléportation.
- Bouclier atmosphérique.
- Noyau d’intelligence robotique.
- Moteur interplanétaire.
- Arche de colonisation.

### 4.5 Règle de lisibilité

Chaque recette doit afficher :

- les entrées nécessaires ;
- les stocks possédés ;
- le temps de production ;
- le robot utilisé ;
- le résultat ;
- l’usage principal du résultat ;
- le bouton permettant de localiser la ressource manquante.

## 5. Extraction et gisements

### 5.1 États d’un gisement

1. **Inconnu** : masqué par le brouillard ou représenté par une anomalie.
2. **Détecté** : type ou rareté approximative visible.
3. **Analysé** : ressource, quantité, rendement et coût de déblocage connus.
4. **Déverrouillé** : achat effectué en SpaceDollars.
5. **Exploité** : robot ou extracteur affecté.
6. **Épuisé** : production arrêtée ; possibilité de recyclage ou de forage profond.
7. **Régénéré** : option tardive utilisant une technologie planétaire.

### 5.2 Propriétés d’un gisement

- Identifiant unique.
- Type de ressource.
- Réserve totale.
- Rendement par minute.
- Dureté.
- Profondeur.
- Niveau de scanner requis.
- Coût de déblocage.
- Taille visuelle.
- Bonus ou malus local.
- Emplacement sur la carte.
- État actuel.

### 5.3 Formule de production proposée

```text
productionParMinute = rendementBase
                       × multiplicateurExtracteur
                       × multiplicateurRobot
                       × multiplicateurTechnologies
                       × multiplicateurPlanète
                       × multiplicateurÉvénement
```

La production réelle est limitée par :

- la capacité du gisement ;
- la capacité de stockage ;
- la disponibilité énergétique ;
- le niveau de l’extracteur ;
- les éventuelles contraintes de la planète.

### 5.4 Déblocage des secteurs

La carte est découpée en secteurs. Chaque secteur contient plusieurs gisements et un élément remarquable.

Le déblocage demande :

- un montant de SpaceDollars ;
- parfois un niveau de scanner ;
- parfois une mission principale terminée ;
- pour les secteurs avancés, un composant spécifique.

Formule initiale :

```text
coûtSecteur = coûtBasePlanète × 1,55^indexSecteur × coefficientRareté
```

Cette formule devra être plafonnée et ajustée manuellement sur les secteurs narratifs.

## 6. Robots spécialisés

### 6.1 Familles principales

#### Robot extracteur EX

- Affecté aux gisements.
- Augmente le rendement.
- Débloque les minerais plus durs.
- Peut recevoir une tête de forage et une batterie.

#### Robot raffineur RF

- Transforme les minerais bruts.
- Possède une ou plusieurs files de raffinage.
- Réduit le temps de traitement.
- Améliore le rendement matière à haut niveau.

#### Robot assembleur AS

- Produit les composants et technologies.
- Débloque les recettes par niveau.
- Peut produire plusieurs exemplaires par lot.
- Réduit les temps d’assemblage.

#### Robot logistique LG

- Transporte les ressources entre les sites.
- Augmente la capacité de transfert.
- Réduit les blocages de stockage.
- Devient utile lorsque plusieurs zones sont ouvertes.

Le MVP peut commencer avec EX, RF et AS. Le robot logistique sera introduit lorsque les flux visuels et les distances deviennent importantes.

### 6.2 Amélioration des robots

Chaque robot possède :

- un niveau ;
- une vitesse ;
- une efficacité énergétique ;
- une capacité ;
- un nombre d’emplacements de module ;
- une apparence évolutive ;
- une spécialisation optionnelle à partir d’un certain niveau.

Exemple de formule :

```text
vitesseNiveau = vitesseBase × (1 + 0,12 × (niveau - 1))
coûtNiveau = coûtBase × 1,70^(niveau - 1)
```

Le temps d’une recette ne doit jamais descendre sous 20 % de son temps de base, sauf bonus exceptionnel de prestige.

### 6.3 Spécialisations tardives

- RF Thermique : plus rapide sur les métaux.
- RF Cristallin : bonus sur les cristaux et diamants.
- AS Mécanique : bonus sur les composants basiques.
- AS Quantique : bonus sur les technologies avancées.
- EX Profond : accès aux réserves souterraines.
- EX Rapide : rendement élevé mais consommation énergétique supérieure.

## 7. Bâtiments et base principale

### 7.1 Structures initiales

- Centre de commandement.
- Stockage brut.
- Stockage raffiné.
- Atelier robotique.
- Raffinerie.
- Assembleur.
- Générateur solaire.
- Terminal commercial.
- Scanner de surface.

### 7.2 Structures intermédiaires

- Hangar à drones.
- Réseau de batteries.
- Centre de recherche.
- Station de contrats.
- Scanner orbital.
- Dépôt logistique avancé.

### 7.3 Structures finales

- Réacteur à fusion.
- Raffinerie quantique.
- Chantier du vaisseau.
- Relais orbital.
- Arche de colonisation.

### 7.4 Placement

Pour limiter la complexité, les structures principales utilisent d’abord des emplacements prédéfinis autour de la base. Le placement libre peut être ajouté après validation du cœur de jeu.

Cette décision évite :

- les problèmes de collision ;
- les cartes illisibles ;
- les bâtiments bloqués ;
- une interface de placement trop lourde ;
- un coût de développement élevé avant validation du gameplay.

## 8. SpaceDollars et économie

### 8.1 Sources de SpaceDollars

- Vente de matériaux bruts.
- Vente de matériaux raffinés.
- Vente de composants.
- Vente de technologies excédentaires.
- Contrats commerciaux.
- Missions principales.
- Découvertes rares.
- Récompenses quotidiennes modérées.
- Publicités récompensées optionnelles.

### 8.2 Dépenses principales

- Déblocage des secteurs.
- Achat de licences de technologie.
- Construction de bâtiments.
- Amélioration des stockages.
- Réparation ou entretien événementiel.
- Achat d’emplacements de production avec monnaie de jeu.
- Analyse avancée des gisements.

### 8.3 Prix de vente

Le prix doit refléter le temps et la complexité, sans rendre inutile la fabrication avancée.

```text
prixProduit = somme(valeurEntrées)
              × coefficientTransformation
              × coefficientRareté
              × coefficientContrat
```

Principes :

- La vente brute est immédiate mais peu rentable.
- Le raffinage augmente la valeur par unité de temps.
- L’assemblage augmente encore la valeur, mais immobilise des robots.
- Les contrats offrent un prix supérieur pour encourager la diversification.
- Les technologies nécessaires à la progression ne doivent pas être systématiquement plus rentables à vendre qu’à installer.

### 8.4 Prévention des blocages

Le joueur doit toujours pouvoir se relever d’une mauvaise décision. Prévoir :

- un petit marché de secours ;
- la revente partielle des constructions ;
- des contrats simples renouvelables ;
- une production minimale garantie ;
- aucun objet indispensable définitivement consommable sans avertissement.

## 9. Système de production et files d’attente

### 9.1 Types de tâches

- Extraction continue.
- Raffinage par lot.
- Assemblage unitaire ou par lot.
- Construction de bâtiment.
- Recherche technologique.
- Analyse de secteur.

### 9.2 Données d’une tâche

```kotlin
data class ProductionJob(
    val id: String,
    val recipeId: String,
    val robotId: String,
    val quantity: Int,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val status: JobStatus,
    val reservedInputs: Map<String, Long>
)
```

### 9.3 Règles importantes

- Les ingrédients sont réservés au lancement de la tâche.
- Une annulation rembourse une partie ou la totalité selon le type de tâche.
- Le résultat attend dans une zone de collecte si le stockage est plein.
- Le jeu recalcule l’avancement à partir d’horodatages, sans minuterie active permanente.
- Toutes les files sont persistées immédiatement.
- Une tâche terminée hors ligne doit être récupérable au prochain lancement.

### 9.4 Progression hors ligne

Durée initiale : 8 heures, améliorable jusqu’à 24 heures.

```text
productionHorsLigne = min(tempsAbsent, capacitéHorsLigne)
                       × rendementMoyenValide
```

Le calcul doit simuler les limitations essentielles : réserve du gisement, stockage et énergie. Il ne doit pas produire une quantité infinie lorsqu’un maillon est bloqué.

## 10. Missions et progression sur 30 jours

La durée réelle dépendra du rythme de jeu. L’objectif est qu’un joueur régulier termine la première planète en 25 à 40 jours sans achat obligatoire.

### 10.1 Types de missions

- Missions principales : structurent la progression planétaire.
- Missions de secteur : liées à une zone spécifique.
- Contrats commerciaux : produire et livrer une quantité donnée.
- Missions de maîtrise : améliorer un robot ou optimiser une chaîne.
- Missions quotidiennes : objectifs courts et variés.
- Exploits : objectifs permanents donnant des récompenses cosmétiques ou de prestige.

### 10.2 Découpage indicatif

#### Jours 1 à 3 — Installation

- Tutoriel interactif.
- Première extraction de ferrite.
- Premier raffinage.
- Vente initiale.
- Déblocage du deuxième gisement.
- Construction du premier robot RF.
- Première amélioration visible de la base.

#### Jours 4 à 7 — Première automation

- Files de raffinage.
- Cuivre et cobalt.
- Premier robot AS.
- Premiers composants.
- Ouverture de deux secteurs.
- Introduction des contrats.
- Première technologie installée.

#### Jours 8 à 14 — Expansion industrielle

- Titane.
- Robot logistique ou transport automatisé.
- Scanner amélioré.
- Plusieurs chaînes concurrentes.
- Choix entre rendement, stockage et vitesse.
- Construction du hangar à drones.

#### Jours 15 à 21 — Haute technologie

- Iridium et xénon.
- Technologies spécialisées.
- Raffinerie avancée.
- Événements de secteurs.
- Déblocage des zones profondes.
- Début du chantier spatial.

#### Jours 22 à 30+ — Départ planétaire

- Résidu d’antimatière.
- Réacteur à fusion.
- Moteur interplanétaire.
- Arche de colonisation.
- Missions finales longues.
- Choix des bonus permanents.
- Départ vers la planète suivante.

### 10.3 Règles d’équilibrage des missions

- Toujours proposer au moins trois objectifs parallèles.
- Éviter une attente unique bloquant tout le jeu.
- Alterner production, exploration, amélioration et vente.
- Montrer les prérequis futurs sans révéler tout le contenu.
- Récompenser les jalons avec une animation et une transformation visuelle.

## 11. Planètes et prestige

### 11.1 Principe

Le départ vers une nouvelle planète agit comme un prestige scénarisé. Le joueur abandonne une partie de l’infrastructure locale, mais conserve des gains permanents.

### 11.2 Éléments réinitialisés

- Gisements locaux.
- Secteurs débloqués.
- Bâtiments planétaires.
- Stocks courants, sauf réserve protégée éventuelle.
- Contrats locaux.
- Monnaie locale au-delà d’un montant de transfert limité.

### 11.3 Éléments conservés

- Plans technologiques découverts.
- Niveau du noyau d’intelligence.
- Bonus de prestige.
- Cosmétiques.
- Exploits.
- Multiplicateur interplanétaire.
- Capacités de départ débloquées.

### 11.4 Récompense de transfert

Le joueur gagne des **Noyaux Stellaires** en fonction de ses performances :

```text
noyauxGagnés = basePlanète
               + bonusMissions
               + bonusTechnologies
               + bonusEfficacité
```

Les Noyaux Stellaires permettent de choisir des améliorations permanentes :

- +10 % de vitesse d’extraction.
- +10 % de vitesse de raffinage.
- +10 % de vitesse d’assemblage.
- +15 % de capacité hors ligne.
- +1 emplacement de file au départ.
- Scanner initial amélioré.
- Réduction du coût des premiers secteurs.

Le multiplicateur global annoncé au joueur peut commencer à **×2** sur la deuxième planète, mais il doit être distribué entre plusieurs systèmes afin d’éviter de casser l’économie.

### 11.5 Premières planètes proposées

#### Planète 1 — Ferrum Delta

- Désert minéral rouge et sombre.
- Tempêtes de poussière légères.
- Ressources équilibrées.
- Planète tutorielle complète.

#### Planète 2 — Cryos IX

- Monde glacé bleu, fissures lumineuses et cristaux.
- Gisements plus riches mais extraction ralentie par le froid.
- Gestion thermique introduite.
- Nouvelles recettes cristallines.

#### Planète 3 — Vulkaris

- Monde volcanique.
- Production énergétique facilitée.
- Robots soumis à la surchauffe.
- Alliages rares et zones instables.

La première version commercialisable doit contenir au minimum Ferrum Delta complète et Cryos IX suffisamment complète pour que le prestige ne mène pas à un écran vide.

## 12. Carte 2.5D et contrôles

### 12.1 Recommandation technique

Utiliser une véritable scène 3D à caméra orthographique fixe, avec modèles low-poly et interface 2D. Le rendu reste lisible comme un jeu isométrique tout en permettant :

- reliefs ;
- éclairage dynamique ;
- ombres ;
- particules ;
- rotations légères ;
- zoom fluide ;
- effets de profondeur ;
- variations de planète.

### 12.2 Contrôles tactiles

- Glisser à un doigt : déplacer la caméra.
- Pincer : zoomer ou dézoomer.
- Toucher court : sélectionner un élément.
- Double toucher sur une mission : centrer la cible.
- Maintenir : afficher les informations rapides.
- Bouton Base : recentrer la caméra.
- Bouton Mission : centrer l’objectif actif.

### 12.3 Contraintes caméra

- Limites strictes pour ne jamais sortir de la carte.
- Inertie légère, mais arrêt rapide.
- Zoom minimal montrant une zone suffisamment large.
- Zoom maximal permettant d’admirer les machines.
- Aucun geste ne doit entrer en conflit avec les boutons du HUD.
- Cibles tactiles d’au moins 48 dp.

### 12.4 Brouillard et découverte

Les zones non débloquées sont couvertes par :

- brume volumétrique légère ;
- grille holographique ;
- silhouettes de relief ;
- signaux de scanner ;
- halo indiquant les secteurs achetables.

## 13. Direction artistique et ambiance

### 13.1 Style

- Science-fiction industrielle élégante.
- Low-poly détaillé mais léger.
- Volumes simples, silhouettes fortes.
- Matériaux métal sombre, verre, néons et cristaux.
- Palette spécifique par planète.
- Interface holographique lisible, sans surcharge.

### 13.2 Effets prioritaires

- Faisceaux de forage.
- Étincelles et fragments lors de l’extraction.
- Matière circulant dans des conduits.
- Cristallisation pendant le raffinage.
- Bras robotisés animés pendant l’assemblage.
- Traînées des drones.
- Hologrammes de construction.
- Onde lumineuse à l’ouverture d’un secteur.
- Transformation visuelle lors d’une amélioration.
- Éclairage pulsé sur les ressources rares.
- Décollage spectaculaire du vaisseau.

### 13.3 Règles de performance visuelle

- Particules regroupées et plafonnées.
- Niveau de détail selon la distance caméra.
- Ombres dynamiques limitées aux éléments importants.
- Textures regroupées dans des atlas.
- Modèles instanciés pour les éléments répétés.
- Mode qualité : faible, moyen, élevé.
- Objectif : 60 FPS sur appareils moyens, mode 30 FPS stable sur appareils faibles.

### 13.4 Son

Aucun son ne sera produit pendant les premières phases. Préparer néanmoins :

- un `AudioService` abstrait ;
- des identifiants d’événements sonores ;
- des réglages musique, effets et vibration ;
- aucun asset audio et aucun travail de mixage avant la phase dédiée.

## 14. UX et écrans

### 14.1 HUD principal

Le HUD doit rester compact :

- barre supérieure : SpaceDollars, énergie, stockage et ressource suivie ;
- coin supérieur : paramètres ;
- côté gauche : mission principale et objectifs ;
- côté droit : raccourcis production, robots et carte ;
- partie basse : panneau contextuel de l’élément sélectionné ;
- bouton central discret : recentrer sur la base.

### 14.2 Écrans essentiels

1. Chargement et initialisation.
2. Carte principale.
3. Détail d’un gisement.
4. Raffinage.
5. Assemblage.
6. Robots.
7. Inventaire.
8. Technologies.
9. Missions.
10. Contrats et terminal commercial.
11. Carte des secteurs.
12. Prestige et choix de planète.
13. Paramètres.
14. Consentement publicitaire et confidentialité.

### 14.3 Principes UX

- Maximum deux actions pour relancer une production fréquente.
- Bouton « Produire à nouveau » après collecte.
- Possibilité d’épingler une recette.
- Localisation immédiate d’une ressource manquante.
- Comparaison avant/après pour chaque amélioration.
- Temps affichés dans un format humain.
- Notifications internes regroupées.
- Pas de pop-up publicitaire pendant une action importante.
- Les boutons de publicité indiquent précisément la récompense.

### 14.4 Tutoriel

Tutoriel intégré au jeu, sans longues pages de texte :

1. Déplacer la caméra.
2. Sélectionner le premier gisement.
3. Lancer l’extraction.
4. Récupérer la ferrite.
5. Raffiner une plaque.
6. Vendre ou utiliser la plaque.
7. Construire le premier robot.
8. Ouvrir le premier secteur.

Chaque étape bloque uniquement les actions susceptibles de perdre le joueur, pas toute l’interface.

## 15. Monétisation Google Ads

### 15.1 Format principal : publicité récompensée

Récompenses possibles :

- terminer une tâche courte ;
- réduire de 25 % une tâche longue ;
- doubler la production hors ligne ;
- obtenir un contrat premium temporaire ;
- scanner gratuitement une anomalie ;
- récupérer un coffre de matériaux ;
- activer un boost de production limité dans le temps.

### 15.2 Règles de protection du gameplay

- Aucune publicité obligatoire pour progresser.
- Récompense annoncée avant le lancement.
- Récompense accordée uniquement après validation du SDK.
- Limites quotidiennes par catégorie.
- Temps minimum entre deux propositions.
- Aucun bouton trompeur.
- Pas de publicité pendant les premières minutes du tutoriel.
- Pas de bannière permanente sur la carte principale.

### 15.3 Interstitiels

À éviter au MVP. Si ajoutés plus tard :

- uniquement après un changement naturel d’écran ;
- jamais après chaque collecte ;
- fréquence fortement limitée ;
- désactivés pendant le tutoriel ;
- jamais en remplacement d’une publicité récompensée promise.

### 15.4 Intégration

- AdMob côté Android.
- Interface `RewardedAdsService` injectée dans le cœur du jeu.
- Identifiants de test en debug.
- Identifiants réels uniquement dans la configuration release.
- Gestion des échecs réseau sans bloquer l’utilisateur.
- Consentement via Google User Messaging Platform selon la zone.
- Journalisation des récompenses pour éviter les doubles attributions.

## 16. Architecture technique recommandée

### 16.1 Stack

- Kotlin.
- LibGDX pour le moteur de jeu.
- Extensions KTX pour une utilisation Kotlin idiomatique.
- Rendu 3D low-poly avec caméra orthographique.
- Scene2D pour le HUD et les menus de jeu.
- Kotlin Coroutines pour les opérations asynchrones hors boucle de rendu.
- Kotlin Serialization pour les configurations et sauvegardes.
- Room ou SQLDelight pour les données persistantes structurées.
- DataStore pour les préférences.
- WorkManager pour les notifications locales et travaux Android différés.
- AdMob et UMP dans le module Android.

Utiliser les dernières versions stables compatibles au moment de l’initialisation du projet. Ne pas verrouiller le plan sur des numéros de version susceptibles de devenir obsolètes.

### 16.2 Organisation Gradle

```text
miner-space/
├── androidApp/             # Launcher Android, cycle de vie, AdMob, UMP, notifications
├── game/                   # Écrans LibGDX, rendu, caméra, entrées, effets
├── domain/                 # Règles de jeu et cas d’usage sans dépendance Android
├── data/                   # Sauvegarde, configuration, repositories
├── shared/                 # Types communs, utilitaires, résultat, logs
├── assets/                 # Modèles, textures, shaders, données de gameplay
├── docs/                   # Documentation de design et d’équilibrage
├── build.gradle.kts
├── settings.gradle.kts
└── plan.md
```

### 16.3 Architecture logique

```text
UI / Game Screens
        ↓
ViewModels ou Presenters de jeu
        ↓
Use Cases du domaine
        ↓
Repositories
        ↓
Sauvegarde locale / Configurations / Services Android
```

Le domaine ne dépend pas de LibGDX, d’Android ni d’AdMob. Les règles d’économie, les recettes, les missions et les calculs de temps doivent pouvoir être testés par de simples tests JVM.

### 16.4 Écrans LibGDX proposés

- `BootScreen`.
- `LoadingScreen`.
- `MainMenuScreen`.
- `PlanetScreen`.
- `ProductionScreen`.
- `RobotScreen`.
- `MissionScreen`.
- `TechnologyScreen`.
- `PlanetTransferScreen`.
- `SettingsScreen`.

Les panneaux secondaires peuvent être des overlays Scene2D au-dessus de `PlanetScreen` plutôt que des écrans séparés afin de conserver le contexte visuel.

## 17. Modèle de données

### 17.1 Entités principales

```text
PlayerProfile
PlanetState
SectorState
DepositState
BuildingState
RobotState
InventoryState
RecipeDefinition
TechnologyDefinition
ProductionJob
MissionDefinition
MissionProgress
ContractState
PrestigeState
AdRewardLedger
GameSettings
```

### 17.2 Définitions statiques et états dynamiques

Séparer strictement :

- les **définitions** : recettes, coûts, temps, textes, modèles, icônes ;
- les **états** : quantités, niveaux, tâches, missions terminées, secteurs ouverts.

Les définitions sont chargées depuis des fichiers JSON versionnés dans les assets. Les états sont sauvegardés localement.

### 17.3 Exemple de recette

```json
{
  "id": "refined_ferrite_plate",
  "category": "REFINING",
  "requiredRobotType": "RF",
  "requiredRobotLevel": 1,
  "inputs": {
    "raw_ferrite": 10
  },
  "outputs": {
    "ferrite_plate": 1
  },
  "baseDurationSeconds": 30,
  "energyCost": 2,
  "unlockTechnologyId": null
}
```

### 17.4 Quantités

Utiliser des entiers longs (`Long`) pour les ressources et la monnaie. Éviter les nombres flottants pour les stocks et les coûts. Les multiplicateurs peuvent être calculés avec `BigDecimal` ou une représentation fixe lorsque la précision devient importante.

## 18. Temps, sauvegarde et robustesse

### 18.1 Horloge

- Stocker les dates en UTC avec epoch milliseconds.
- Ne jamais faire confiance à une minuterie d’interface pour la logique.
- Recalculer les tâches à partir de `startedAt` et `duration`.
- Détecter les retours en arrière importants de l’horloge système.

### 18.2 Sauvegarde

- Sauvegarde automatique après chaque transaction importante.
- Sauvegarde périodique pendant une session.
- Copie de secours locale alternée.
- Migration versionnée du schéma.
- Détection de sauvegarde corrompue.
- Restauration de la dernière sauvegarde valide.

### 18.3 Serveur

Le MVP peut fonctionner avec une sauvegarde locale. Concevoir toutefois les interfaces afin d’ajouter ultérieurement :

- sauvegarde cloud ;
- synchronisation multi-appareils ;
- validation de récompenses ;
- événements distants ;
- équilibrage distant.

Aucun serveur n’est nécessaire pour valider le cœur de jeu initial.

### 18.4 Anti-triche raisonnable

- Détection des grands changements d’horloge.
- Horodatage de la dernière session valide.
- Plafond de progression hors ligne.
- Journal local des publicités récompensées.
- Aucune sanction agressive en cas d’anomalie ; plafonner le gain et informer discrètement.

## 19. Énergie et logistique

### 19.1 Énergie

L’énergie devient une contrainte d’optimisation à partir du milieu de la première semaine.

```text
énergieDisponible = productionGénérateurs + batteries
énergieDemandée = extraction + raffinage + assemblage + bâtiments
```

En cas de déficit :

- réduction progressive de la vitesse ;
- priorité configurable ;
- jamais d’arrêt incompréhensible sans explication.

### 19.2 Priorités

Le joueur peut choisir :

1. Missions.
2. Extraction.
3. Raffinage.
4. Assemblage.
5. Recherche.

Un mode automatique propose une configuration recommandée.

### 19.3 Stockage

Le stockage doit créer des décisions, sans devenir une nuisance permanente.

- Stockage séparé brut, raffiné et composants.
- Alerte avant saturation.
- Bouton de vente rapide configurable.
- Aucun matériau rare vendu automatiquement par défaut.
- Capacité augmentée par bâtiments et modules.

## 20. Technologies et arbre de progression

### 20.1 Branches

#### Extraction

- Forage renforcé.
- Extraction multiple.
- Forage profond.
- Exploitation des gisements rares.

#### Raffinage

- Catalyse améliorée.
- Réduction des pertes.
- Double file.
- Raffinage quantique.

#### Assemblage

- Bras supplémentaires.
- Production par lot.
- Plans avancés.
- Assemblage autonome.

#### Logistique

- Stockage.
- Drones rapides.
- Téléportation locale.
- Réseau planétaire.

#### Énergie

- Panneaux solaires.
- Batteries.
- Fusion.
- Cellules d’antimatière.

#### Exploration

- Scanner de surface.
- Scanner orbital.
- Analyse rare.
- Détection interplanétaire.

### 20.2 Choix

Certaines technologies doivent proposer des choix temporaires ou réversibles, par exemple :

- vitesse élevée contre consommation ;
- rendement matière contre durée ;
- stockage contre valeur de vente ;
- production spécialisée contre polyvalence.

Les choix permanents sont réservés au prestige et doivent être clairement confirmés.

## 21. Contrats et événements

### 21.1 Contrats

Chaque contrat précise :

- produit demandé ;
- quantité ;
- durée ;
- récompense ;
- difficulté ;
- éventuel bonus de série.

Trois contrats disponibles en permanence : simple, rentable et ambitieux.

### 21.2 Événements de carte

À introduire après stabilisation du MVP :

- pluie de météorites révélant un gisement temporaire ;
- panne de scanner ;
- surcharge énergétique ;
- marchand orbital ;
- anomalie cristalline ;
- robot abandonné à réparer.

Les événements ne doivent pas punir lourdement une absence.

## 22. Rétention sans frustration

### 22.1 Récompense de retour

L’écran de retour affiche :

- durée d’absence ;
- production réalisée ;
- tâches terminées ;
- gisements épuisés ;
- stockage perdu faute de place, le cas échéant ;
- possibilité facultative de doubler une partie de la production par publicité.

### 22.2 Objectifs visibles

- Mission principale toujours accessible.
- Trois objectifs secondaires maximum sur le HUD.
- Jalon journalier.
- Progression de planète en pourcentage.
- Aperçu du prochain grand déblocage.

### 22.3 Notifications locales

Désactivables individuellement :

- tâche importante terminée ;
- stockage plein ;
- contrat proche de l’expiration ;
- énergie rétablie ;
- mission principale réalisable.

Aucune notification promotionnelle agressive.

## 23. Équilibrage initial

### 23.1 Durées indicatives

| Niveau de recette | Durée de base |
|---|---:|
| Raffinage commun | 15 s à 2 min |
| Raffinage rare | 5 à 30 min |
| Composant basique | 1 à 10 min |
| Composant avancé | 15 min à 2 h |
| Technologie majeure | 2 à 12 h |
| Élément final de planète | 8 à 24 h |

Les tâches longues apparaissent seulement lorsque le joueur dispose de plusieurs files et objectifs parallèles.

### 23.2 Courbe de coûts

- Améliorations fréquentes : coefficient 1,35 à 1,55.
- Robots : coefficient 1,60 à 1,75.
- Secteurs : coefficient autour de 1,55 avec ajustement manuel.
- Technologies majeures : coûts définis manuellement.

### 23.3 Temps jusqu’au prestige

Objectif de test :

- Joueur très actif : 18 à 25 jours.
- Joueur régulier : 25 à 40 jours.
- Joueur occasionnel : 40 à 60 jours.

La publicité récompensée peut accélérer, mais ne doit pas diviser ce temps par plus de deux sans usage intensif.

## 24. Analytique produit et confidentialité

Même sans outil analytique au départ, prévoir des événements internes anonymisés pouvant être activés plus tard :

- tutoriel terminé ;
- premier raffinage ;
- premier robot ;
- premier secteur ;
- jour de progression ;
- mission bloquante ;
- prestige ;
- publicité proposée, lancée et récompensée ;
- abandon d’une production.

Ne jamais enregistrer le contenu personnel de l’utilisateur. Documenter précisément les données envoyées par AdMob et les obligations de consentement avant publication.

## 25. Tests

### 25.1 Tests unitaires du domaine

- Calcul des productions.
- Coûts d’amélioration.
- Réservation et remboursement des ressources.
- Fin de tâche en ligne et hors ligne.
- Missions.
- Prestige.
- Saturation de stockage.
- Déficit énergétique.
- Récompenses publicitaires idempotentes.

### 25.2 Tests d’intégration

- Sauvegarde et restauration.
- Migration de version.
- Retour après plusieurs heures.
- Plusieurs tâches terminées simultanément.
- Gisement épuisé pendant une absence.
- Publicité réussie, annulée ou indisponible.

### 25.3 Tests visuels et appareils

Formats minimaux :

- 320 × 640 portrait.
- 640 × 320 paysage.
- 390 × 844 portrait.
- 844 × 390 paysage.
- Tablettes Android.
- Écrans avec encoche et barres système.

Même si le jeu est principalement conçu en paysage, les écrans système, menus et erreurs doivent rester utilisables dans toutes les orientations prévues. Décider avant production si le gameplay sera verrouillé en paysage.

### 25.4 Tests de performance

- 100 gisements affichés.
- 50 robots ou drones visibles.
- Particules simultanées.
- Zoom maximal et minimal.
- Retour après 24 heures hors ligne.
- Sauvegarde volumineuse.
- Appareil Android à faible mémoire.

## 26. Roadmap de développement

Chaque étape doit être réalisée sur une branche dédiée et validée avant d’ouvrir la suivante. Aucun workflow ou CI/CD n’est nécessaire au démarrage.

### Étape 0 — Initialisation et documentation

- Créer le projet Gradle Kotlin.
- Ajouter les modules.
- Configurer LibGDX et KTX.
- Définir les conventions de code.
- Créer `README.md`, `CONTRIBUTING.md` et documentation technique.
- Préparer les environnements debug et release.
- Ajouter une scène de test vide.

**Livrable :** application Android qui ouvre une scène vide stable.

### Étape 1 — Prototype de carte 2.5D

- Caméra orthographique.
- Déplacement tactile.
- Zoom.
- Limites de carte.
- Sélection d’un objet.
- Base centrale temporaire.
- Trois gisements temporaires.
- HUD minimal.

**Livrable :** carte manipulable avec sélection fiable.

### Étape 2 — Domaine économique minimal

- Ressources.
- Inventaire.
- Gisements.
- Extraction.
- SpaceDollars.
- Vente.
- Configurations JSON.
- Tests unitaires.

**Livrable :** extraire et vendre une ressource dans une boucle complète.

### Étape 3 — Raffinage

- Robot RF.
- Recettes.
- File de production.
- Minuteurs persistants.
- Collecte.
- Affichage des blocages.
- Première animation de raffinage.

**Livrable :** minerai brut transformé en produit raffiné.

### Étape 4 — Assemblage

- Robot AS.
- Composants basiques.
- Technologies installables.
- Effets des technologies sur la production.
- Panneaux de détail.

**Livrable :** chaîne brute → raffiné → composant → amélioration.

### Étape 5 — Sauvegarde et hors ligne

- Base de données locale.
- Sauvegarde automatique.
- Calcul hors ligne.
- Écran de retour.
- Migrations.
- Protection contre corruption.

**Livrable :** progression fiable après fermeture de l’application.

### Étape 6 — Secteurs et exploration

- Carte segmentée.
- Brouillard.
- Scanner.
- Coûts de déblocage.
- Gisements rares.
- Centrage sur mission.
- Effet d’ouverture de secteur.

**Livrable :** exploration et expansion de la carte.

### Étape 7 — Robots et automation avancée

- Niveaux.
- Modules.
- Spécialisations.
- Plusieurs files.
- Robot logistique.
- Priorités énergétiques.
- Évolutions visuelles.

**Livrable :** système d’automatisation profond et compréhensible.

### Étape 8 — Missions, contrats et tutoriel

- Missions principales.
- Missions secondaires.
- Contrats.
- Tutoriel interactif.
- Récompenses.
- Progression de planète.

**Livrable :** première semaine de contenu jouable guidé.

### Étape 9 — Direction artistique et effets

- Modèles définitifs de la première planète.
- Textures.
- Éclairage.
- Particules.
- Shaders.
- Animations des robots.
- Qualité graphique réglable.

**Livrable :** vertical slice représentative de la qualité finale.

### Étape 10 — Contenu de 30 jours

- Toutes les recettes Ferrum Delta.
- Tous les secteurs.
- Technologies finales.
- Missions sur 30 jours.
- Courbe économique.
- Tests accélérés de simulation.
- Ajustements anti-blocage.

**Livrable :** première planète complète.

### Étape 11 — Prestige et deuxième planète

- Construction du vaisseau.
- Écran de transfert.
- Noyaux Stellaires.
- Bonus permanents.
- Cryos IX.
- Nouvelles contraintes.
- Conservation et réinitialisation correctes.

**Livrable :** cycle complet entre deux planètes.

### Étape 12 — Publicités récompensées

- AdMob debug.
- UMP.
- Service abstrait.
- Récompenses idempotentes.
- Limites et délais.
- Tests d’échec.
- Écran de confidentialité.

**Livrable :** monétisation facultative fonctionnelle et non bloquante.

### Étape 13 — Finition et publication

- Optimisation.
- Accessibilité.
- Icône et captures.
- Fiche Play Store.
- Politique de confidentialité.
- Tests fermés.
- Correction des crashs.
- Équilibrage final.
- Publication progressive.

**Livrable :** version 1.0 prête pour Google Play.

## 27. Ordre recommandé des branches

```text
main
├── setup/project-foundation
├── feature/planet-map-prototype
├── feature/core-economy
├── feature/refining-system
├── feature/assembly-system
├── feature/save-offline-progress
├── feature/sectors-exploration
├── feature/robot-automation
├── feature/missions-tutorial
├── feature/visual-polish
├── content/ferrum-delta
├── feature/planet-prestige
├── feature/rewarded-ads
└── release/1.0
```

Une fonctionnalité doit être fusionnée uniquement lorsqu’elle est jouable, testée et documentée. Éviter les branches contenant plusieurs systèmes indépendants.

## 28. Définition de terminé

Une fonctionnalité est terminée lorsque :

- son comportement principal fonctionne ;
- ses erreurs sont gérées ;
- son état est sauvegardé ;
- son interface est utilisable au tactile ;
- elle fonctionne après reprise hors ligne si nécessaire ;
- ses calculs critiques sont testés ;
- ses textes ne sont pas codés en dur dans la logique ;
- ses paramètres d’équilibrage sont configurables ;
- elle ne dégrade pas les performances ;
- sa documentation est mise à jour.

## 29. MVP, version 1.0 et évolutions

### MVP interne

- Une petite carte.
- Trois ressources brutes.
- Deux matériaux raffinés.
- Deux composants.
- Trois robots.
- Vente.
- Un secteur verrouillé.
- Sauvegarde locale.
- Progression hors ligne.

Objectif : valider que la chaîne de production est amusante.

### Vertical slice

- Direction artistique quasi définitive.
- Une heure de contenu.
- Une technologie spectaculaire.
- Un secteur à débloquer.
- Une mission complète.
- Effets et HUD représentatifs.

Objectif : valider l’expérience, la lisibilité et l’ambiance.

### Version 1.0

- Ferrum Delta complète.
- Cryos IX jouable après prestige.
- Environ 30 jours de progression sur la première planète.
- Robots, automation, missions et contrats.
- Publicités récompensées.
- Sauvegarde locale robuste.
- Paramètres graphiques et accessibilité.

### Après la version 1.0

- Sauvegarde cloud.
- Nouvelles planètes.
- Événements saisonniers sobres.
- Personnalisation de la base.
- Nouveaux robots.
- Contrats communautaires.
- Système audio complet.
- Achats intégrés éventuels uniquement après analyse de la rétention.

## 30. Risques principaux

### Trop de contenu avant validation du plaisir

**Réponse :** construire d’abord une chaîne courte mais complète et la tester.

### Économie bloquante ou trop lente

**Réponse :** configurations externes, simulateur d’équilibrage et plusieurs objectifs parallèles.

### Interface surchargée

**Réponse :** HUD compact, panneaux contextuels et tests sur petits écrans dès le prototype.

### Rendu 3D trop coûteux

**Réponse :** low-poly, caméra fixe, LOD, instancing, qualité réglable et budgets stricts.

### Publicités trop présentes

**Réponse :** récompenses facultatives, limites quotidiennes et absence de bannière permanente.

### Sauvegarde locale manipulable

**Réponse :** plafonds, contrôle des horodatages et architecture permettant un serveur ultérieur, sans sacrifier le mode hors ligne.

### Première planète trop longue à produire

**Réponse :** systèmes pilotés par données, réutilisation des mécaniques, variations environnementales et production de contenu après le vertical slice.

## 31. Priorité immédiate

La première tâche de développement après ce document doit être l’initialisation du projet et un prototype de carte 2.5D comprenant uniquement :

- une caméra orthographique ;
- une petite planète low-poly ;
- une base ;
- trois gisements ;
- déplacement et zoom tactiles ;
- sélection d’un gisement ;
- un panneau affichant son nom, sa réserve et son rendement.

Aucune publicité, aucun système complexe de mission et aucun contenu de 30 jours ne doit être développé avant que cette manipulation de carte soit fluide, lisible et agréable.
