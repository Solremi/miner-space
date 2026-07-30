# Miner Space — Plan de développement complet

> Document directeur du projet. Il décrit la vision, le gameplay, l’économie, l’architecture Kotlin, la direction visuelle, la monétisation et l’ordre de développement.

## 1. Vision du jeu

**Miner Space** est un jeu mobile de gestion, d’automatisation et de progression spatiale en vue 2.5D. Le joueur exploite une planète, découvre des gisements, extrait des matériaux bruts, les raffine, fabrique des composants puis assemble des technologies spatiales de plus en plus avancées.

Chaque technologie améliore directement la capacité d’exploitation : vitesse de minage, capacité de stockage, nombre de files de production, portée des scanners, efficacité des robots, accès à des gisements rares et ouverture de nouvelles zones.

La première planète doit proposer environ **30 jours de progression raisonnable** pour un joueur régulier. Une fois les objectifs majeurs terminés, le joueur construit un vaisseau et part vers une nouvelle planète. Il recommence avec un nouvel environnement, de nouvelles ressources et de nouvelles contraintes, tout en conservant des améliorations permanentes et un multiplicateur de progression.

Le jeu ne possède pas de fin définitive. Chaque planète constitue un cycle complet, mais le voyage continue ensuite vers de nouveaux mondes, des frontières procédurales, des anomalies et des objectifs interplanétaires. Le joueur doit toujours disposer d’un prochain territoire, d’une nouvelle spécialisation, d’une collection à compléter ou d’un projet spatial à poursuivre.

### Positionnement

- Jeu mobile Android en Kotlin.
- Gestion et automation accessibles, sans contrôles complexes.
- Carte 2.5D manipulable au doigt : déplacement, zoom, sélection des gisements et bâtiments.
- Sessions courtes de 2 à 8 minutes, avec progression hors ligne.
- Progression longue, lisible et régulière.
- Monétisation principalement par publicités récompensées.
- Direction artistique immersive, propre et spectaculaire.
- Aucun travail sonore dans les premières étapes ; le système audio sera préparé mais non alimenté.
- Progression interplanétaire extensible, sans écran de fin définitif.
- Mélange de gestion passive, d’optimisation et de courtes activités interactives facultatives.

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

### 2.6 Des choix qui transforment réellement la partie

Le joueur doit pouvoir construire une base différente de celle d’un autre joueur. Les spécialisations, modules et synergies doivent modifier les priorités, les bâtiments dominants, les robots utilisés et l’apparence générale de l’installation.

Aucune spécialisation ne doit être objectivement supérieure dans toutes les situations. Chaque orientation doit apporter un avantage clair, une contrainte compréhensible et plusieurs moyens de compensation.

### 2.7 Une surprise rare mais mémorable

Les ressources exceptionnelles, pluies de météorites, anomalies et découvertes narratives doivent casser la routine sans empêcher la progression normale. Une découverte rare doit être identifiable immédiatement par son animation, sa présentation et son utilité unique.

### 2.8 Une satisfaction permanente

Chaque action importante doit produire un retour perceptible. Le joueur ne doit jamais appuyer sur un bouton et constater uniquement qu’un nombre a changé. Les effets doivent rester rapides, lisibles, désactivables si nécessaire et compatibles avec les appareils modestes.

## 3. Boucles de gameplay

### 3.1 Boucle de session courte

1. Récupérer la production terminée.
2. Vérifier les stocks et les missions.
3. Relancer les robots de raffinage et d’assemblage.
4. Améliorer un robot, une technologie ou un bâtiment.
5. Débloquer ou inspecter un nouveau gisement.
6. Choisir les priorités de production.
7. Participer éventuellement à une activité courte : pluie de météorites, anomalie ou récupération de fragments.
8. Quitter avec des tâches longues actives.

Durée cible : 2 à 8 minutes.

### 3.2 Boucle quotidienne

1. Collecter la progression hors ligne.
2. Terminer plusieurs contrats.
3. Réorganiser les files de production.
4. Dépenser les SpaceDollars.
5. Débloquer une zone ou une technologie.
6. Atteindre un jalon de mission principale.
7. Vérifier le Codex, les découvertes rares et la maîtrise des robots.
8. Préparer les productions longues pour la nuit.

### 3.3 Boucle de planète

1. Installer la base.
2. Exploiter les ressources communes.
3. Construire les premiers robots.
4. Automatiser le raffinage.
5. Découvrir les ressources rares.
6. Construire des composants avancés.
7. Choisir une spécialisation industrielle.
8. Déployer les grandes technologies planétaires.
9. Découvrir une partie du mystère de la planète.
10. Terminer les missions principales.
11. Construire le vaisseau de départ.
12. Choisir les bonus permanents et partir vers la planète suivante.

### 3.4 Boucle interplanétaire sans fin définitive

1. Terminer une planète ou atteindre son seuil de départ.
2. Conserver les collections, plans, robots vétérans et bonus permanents autorisés.
3. Choisir une destination parmi plusieurs mondes ou anomalies.
4. Découvrir de nouvelles règles environnementales.
5. Adapter la spécialisation, les modules et les chaînes de production.
6. Agrandir le réseau interplanétaire et débloquer des technologies globales.
7. Accéder progressivement à des planètes conçues manuellement puis à des frontières générées à partir de règles contrôlées.
8. Continuer tant que le joueur souhaite optimiser, collectionner et explorer.

Le jeu ne doit jamais afficher « jeu terminé ». La fin d’une planète ouvre toujours un nouvel horizon.

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

### 4.5 Ressources exceptionnellement rares

Ces ressources ne sont pas nécessaires à la progression principale. Elles servent à produire des modules uniques, compléter le Codex, améliorer des robots vétérans ou construire des technologies interplanétaires.

| Ressource exceptionnelle | Source principale | Usage |
|---|---|---|
| Ferrite prismatique | Extraction critique très rare | Modules de structure et apparences holographiques |
| Cœur météorique | Pluie de météorites | Modules énergétiques et projets orbitaux |
| Diamant xénon instable | Raffinage exceptionnel | Accélérateurs quantiques à risque contrôlé |
| Matière noire solidifiée | Anomalie profonde | Technologies interplanétaires |
| Minerai vivant | Planète biologique future | Robots adaptatifs |
| Fragment d’archive ancienne | Ruines et événements narratifs | Déblocage de données, plans et éléments du mystère |
| Cristal chronal | Frontières avancées | Réduction limitée de certaines durées |

Règles :

- aucune ressource exceptionnelle indispensable ne dépend d’un tirage aléatoire pur ;
- une protection contre la malchance augmente progressivement la probabilité après plusieurs événements sans découverte ;
- les probabilités internes doivent être configurables et testables ;
- les ressources exceptionnelles ne sont jamais vendues automatiquement ;
- leur obtention déclenche une présentation spécifique ;
- elles restent rares même après plusieurs planètes ;
- aucune publicité ne garantit directement une ressource exceptionnelle.

### 4.6 Règle de lisibilité

Chaque recette doit afficher :

- les entrées nécessaires ;
- les stocks possédés ;
- le temps de production ;
- le robot utilisé ;
- le résultat ;
- l’usage principal du résultat ;
- les synergies de modules actives ;
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
- Chance de variante exceptionnelle.
- Compatibilités de modules.

### 5.3 Formule de production proposée

```text
productionParMinute = rendementBase
                       × multiplicateurExtracteur
                       × multiplicateurRobot
                       × multiplicateurModules
                       × multiplicateurSynergies
                       × multiplicateurSpécialisation
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

- un identifiant permanent ;
- un nom généré ou choisi ;
- un numéro de série ;
- un niveau ;
- une vitesse ;
- une efficacité énergétique ;
- une capacité ;
- un nombre d’emplacements de module ;
- une apparence évolutive ;
- une spécialisation optionnelle à partir d’un certain niveau ;
- un niveau de maîtrise ;
- des statistiques historiques.

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

### 6.4 Robots reconnaissables et vétérans

Chaque robot important doit être identifiable autrement que par son niveau.

Informations affichées :

- nom et numéro de série ;
- type et spécialisation ;
- date de construction ;
- quantité totale extraite, raffinée, assemblée ou transportée ;
- événement rare auquel il a participé ;
- meilleur record ;
- niveau de maîtrise ;
- modules équipés ;
- apparence actuelle.

La maîtrise progresse lentement en utilisant réellement le robot. Elle débloque de petits bonus, des animations, des marques visuelles et éventuellement une capacité passive.

Exemples de traits :

- Précis : légère réduction des pertes.
- Endurant : meilleure efficacité hors ligne.
- Rapide : bonus de vitesse mais consommation supérieure.
- Stable : résiste mieux aux contraintes environnementales.
- Prospecteur : améliore légèrement la détection de fragments rares.

Les traits ne doivent jamais rendre un robot inutilisable. Ils créent des préférences et des histoires personnelles, pas une loterie punitive.

Lors d’un changement de planète, le joueur peut conserver au moins un robot vétéran, son noyau ou son plan mémoriel. Il n’existe pas de mort permanente imposée.

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
- Laboratoire de modules.
- Chambre du Codex.

### 7.3 Structures finales

- Réacteur à fusion.
- Raffinerie quantique.
- Chantier du vaisseau.
- Relais orbital.
- Arche de colonisation.
- Observatoire des anomalies.

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
- Fabrication et amélioration de modules.
- Réorientation d’une spécialisation après le délai gratuit prévu.

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
- Les ressources exceptionnelles ont surtout une valeur d’usage et de collection, pas une simple valeur marchande.

### 8.4 Prévention des blocages

Le joueur doit toujours pouvoir se relever d’une mauvaise décision. Prévoir :

- un petit marché de secours ;
- la revente partielle des constructions ;
- des contrats simples renouvelables ;
- une production minimale garantie ;
- aucun objet indispensable définitivement consommable sans avertissement ;
- une réinitialisation gratuite de la spécialisation après un changement majeur de planète ;
- un démontage de module restituant une partie importante des matériaux.

## 9. Système de production et files d’attente

### 9.1 Types de tâches

- Extraction continue.
- Raffinage par lot.
- Assemblage unitaire ou par lot.
- Construction de bâtiment.
- Recherche technologique.
- Analyse de secteur.
- Fabrication de module.
- Analyse de fragment exceptionnel.

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
- Missions narratives : découvrir des archives, réparer un robot ou analyser une anomalie.
- Missions de collection : compléter une famille du Codex sans imposer une échéance courte.

### 10.2 Découpage indicatif

#### Jours 1 à 3 — Installation

- Tutoriel interactif.
- Première extraction de ferrite.
- Premier raffinage.
- Vente initiale.
- Déblocage du deuxième gisement.
- Construction du premier robot RF.
- Première amélioration visible de la base.
- Première transmission mystérieuse très courte.

#### Jours 4 à 7 — Première automation

- Files de raffinage.
- Cuivre et cobalt.
- Premier robot AS.
- Premiers composants.
- Ouverture de deux secteurs.
- Introduction des contrats.
- Première technologie installée.
- Déblocage du Codex.

#### Jours 8 à 14 — Expansion industrielle

- Titane.
- Robot logistique ou transport automatisé.
- Scanner amélioré.
- Plusieurs chaînes concurrentes.
- Choix entre rendement, stockage et vitesse.
- Construction du hangar à drones.
- Première spécialisation de base.
- Premiers modules et synergies simples.
- Première pluie de météorites guidée.

#### Jours 15 à 21 — Haute technologie

- Iridium et xénon.
- Technologies spécialisées.
- Raffinerie avancée.
- Événements de secteurs.
- Déblocage des zones profondes.
- Début du chantier spatial.
- Découverte d’une ressource exceptionnellement rare garantie par une mission.
- Premier robot atteignant un statut vétéran.

#### Jours 22 à 30+ — Départ planétaire

- Résidu d’antimatière.
- Réacteur à fusion.
- Moteur interplanétaire.
- Arche de colonisation.
- Missions finales longues.
- Résolution partielle du mystère local.
- Choix des bonus permanents.
- Choix du robot vétéran ou du noyau conservé.
- Départ vers la planète suivante.

### 10.3 Règles d’équilibrage des missions

- Toujours proposer au moins trois objectifs parallèles.
- Éviter une attente unique bloquant tout le jeu.
- Alterner production, exploration, amélioration, collection et vente.
- Montrer les prérequis futurs sans révéler tout le contenu.
- Récompenser les jalons avec une animation et une transformation visuelle.
- Ne pas imposer les événements interactifs pour terminer la campagne principale.
- Garantir les premières découvertes importantes afin d’introduire les systèmes sans frustration.

## 11. Planètes et prestige

### 11.1 Principe

Le départ vers une nouvelle planète agit comme un prestige scénarisé. Le joueur abandonne une partie de l’infrastructure locale, mais conserve des gains permanents.

Le prestige n’est pas une fin et ne doit jamais être présenté comme un effacement. Il constitue un transfert vers un nouveau terrain de jeu dont les règles changent réellement.

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
- Codex et collections.
- Archives narratives.
- Robots vétérans ou noyaux mémoriels autorisés.
- Ressources interplanétaires protégées.

### 11.4 Récompense de transfert

Le joueur gagne des **Noyaux Stellaires** en fonction de ses performances :

```text
noyauxGagnés = basePlanète
               + bonusMissions
               + bonusTechnologies
               + bonusEfficacité
               + bonusCollection
               + bonusMaîtriseRobots
```

Les Noyaux Stellaires permettent de choisir des améliorations permanentes :

- +10 % de vitesse d’extraction.
- +10 % de vitesse de raffinage.
- +10 % de vitesse d’assemblage.
- +15 % de capacité hors ligne.
- +1 emplacement de file au départ.
- Scanner initial amélioré.
- Réduction du coût des premiers secteurs.
- Emplacement de module initial supplémentaire.
- Bonus de maîtrise pour les robots vétérans.

Le multiplicateur global annoncé au joueur peut commencer à **×2** sur la deuxième planète, mais il doit être distribué entre plusieurs systèmes afin d’éviter de casser l’économie.

### 11.5 Planètes qui changent réellement les règles

Chaque planète doit modifier au minimum :

- une contrainte environnementale ;
- une ressource ou famille de ressources ;
- une chaîne de fabrication ;
- une règle énergétique ou logistique ;
- un type d’événement ;
- une partie de la direction artistique ;
- une information narrative importante.

Changer uniquement les couleurs, les coûts et les multiplicateurs est interdit pour une nouvelle planète majeure.

#### Planète 1 — Ferrum Delta

- Désert minéral rouge et sombre.
- Tempêtes de poussière légères.
- Ressources équilibrées.
- Planète tutorielle complète.
- Spécialisations introduites progressivement.
- Ruines discrètes révélant le premier fragment d’archive.

#### Planète 2 — Cryos IX

- Monde glacé bleu, fissures lumineuses et cristaux.
- Gisements plus riches mais extraction ralentie par le froid.
- Gestion thermique introduite.
- Nouvelles recettes cristallines.
- La chaleur des raffineries peut alimenter un réseau thermique.
- Certains fragments météoritiques doivent être récupérés avant congélation.
- Modules cryogéniques et synergies thermiques.

#### Planète 3 — Vulkaris

- Monde volcanique.
- Production énergétique facilitée.
- Robots soumis à la surchauffe.
- Alliages rares et zones instables.
- Refroidissement actif et choix de priorité énergétique.
- Éruptions révélant temporairement des ressources profondes.
- Modules résistants à la chaleur.

#### Planètes futures

- Monde océanique : plateformes mobiles, corrosion et extraction sous-marine.
- Planète biologique : ressources vivantes, croissance et adaptation des robots.
- Monde à faible gravité : logistique orbitale et fragments difficiles à capturer.
- Planète obscure : énergie rare, visibilité réduite et détection par impulsions.
- Monde fracturé : secteurs flottants reliés par téléportation.

La première version commercialisable doit contenir au minimum Ferrum Delta complète et Cryos IX suffisamment complète pour que le prestige ne mène pas à un écran vide.

### 11.6 Absence d’end game définitif

La progression à long terme repose sur trois couches :

1. **Planètes principales conçues manuellement** avec identité, narration et mécaniques propres.
2. **Frontière interplanétaire** composée de mondes générés à partir de règles contrôlées, de modificateurs et de familles visuelles validées.
3. **Progression permanente** : Codex, robots vétérans, technologies interplanétaires, spécialisations avancées et collection de ressources exceptionnelles.

Les mondes procéduraux ne doivent pas remplacer les planètes principales. Ils prolongent la durée de vie entre les ajouts de contenu conçus manuellement.

Le système doit permettre d’ajouter de nouvelles planètes, ressources, événements et règles par données, sans réécrire le cœur du jeu.

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
- Pendant une pluie de météorites : toucher ou glisser sur les fragments sans bloquer le déplacement général hors événement.

### 12.3 Contraintes caméra

- Limites strictes pour ne jamais sortir de la carte.
- Inertie légère, mais arrêt rapide.
- Zoom minimal montrant une zone suffisamment large.
- Zoom maximal permettant d’admirer les machines.
- Aucun geste ne doit entrer en conflit avec les boutons du HUD.
- Cibles tactiles d’au moins 48 dp.
- Caméra recentrée intelligemment au début d’un événement interactif, sans déplacement brutal.

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
- Entrée atmosphérique des météorites.
- Traînées lumineuses et impacts localisés.
- Aura visuelle spécifique aux ressources exceptionnelles.
- Marques d’usure et détails évolutifs sur les robots vétérans.
- Identité visuelle distincte pour chaque spécialisation de base.

### 13.3 Règles de performance visuelle

- Particules regroupées et plafonnées.
- Niveau de détail selon la distance caméra.
- Ombres dynamiques limitées aux éléments importants.
- Textures regroupées dans des atlas.
- Modèles instanciés pour les éléments répétés.
- Mode qualité : faible, moyen, élevé.
- Objectif : 60 FPS sur appareils moyens, mode 30 FPS stable sur appareils faibles.
- Nombre de météorites et fragments adapté au mode qualité.
- Aucun effet indispensable à la compréhension ne dépend uniquement de la transparence ou d’une couleur.

### 13.4 Son

Aucun son ne sera produit pendant les premières phases. Préparer néanmoins :

- un `AudioService` abstrait ;
- des identifiants d’événements sonores ;
- des réglages musique, effets et vibration ;
- aucun asset audio et aucun travail de mixage avant la phase dédiée.

### 13.5 Matrice de satisfaction visuelle et haptique

| Action | Retour minimal |
|---|---|
| Collecter une production | Mouvement de la ressource, compteur animé et confirmation courte |
| Lancer une tâche | Robot qui s’active, indicateur de file et impulsion visuelle |
| Améliorer un robot | Comparaison avant/après, transformation visible et vibration facultative |
| Ouvrir un secteur | Onde lumineuse, disparition progressive du brouillard et caméra contrôlée |
| Trouver une ressource rare | Arrêt visuel bref, halo, carte de découverte et entrée du Codex |
| Compléter une collection | Animation de page, récompense et progression permanente |
| Choisir une spécialisation | Transformation de la base, nouvelle signalétique et aperçu des effets |
| Départ planétaire | Construction finale visible, regroupement des robots et séquence de décollage |

Les animations doivent être courtes lors des actions répétitives. Les grandes animations sont réservées aux jalons importants.

## 14. UX et écrans

### 14.1 HUD principal

Le HUD doit rester compact :

- barre supérieure : SpaceDollars, énergie, stockage et ressource suivie ;
- coin supérieur : paramètres ;
- côté gauche : mission principale et objectifs ;
- côté droit : raccourcis production, robots et carte ;
- partie basse : panneau contextuel de l’élément sélectionné ;
- bouton central discret : recentrer sur la base ;
- indicateur d’événement actif discret mais visible ;
- accès au Codex sans surcharger le HUD.

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
12. Spécialisations.
13. Modules et synergies.
14. Codex et collections.
15. Archives narratives.
16. Prestige et choix de planète.
17. Paramètres.
18. Consentement publicitaire et confidentialité.

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
- Les synergies actives et inactives sont expliquées sans jargon.
- Une spécialisation affiche toujours ses avantages, contraintes et coût de changement.
- Les ressources exceptionnelles ne peuvent pas être détruites ou vendues par erreur sans confirmation.
- Les séquences de satisfaction répétitives peuvent être accélérées ou désactivées.

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
9. Équiper un premier module simple.
10. Participer à une pluie de météorites guidée.

Chaque étape bloque uniquement les actions susceptibles de perdre le joueur, pas toute l’interface.

## 15. Monétisation Google Ads

### 15.1 Format principal : publicité récompensée

Récompenses possibles :

- terminer une tâche courte ;
- réduire de 25 % une tâche longue ;
- doubler la production hors ligne ;
- obtenir un contrat premium temporaire ;
- scanner gratuitement une anomalie ;
- récupérer un coffre de matériaux standards ;
- activer un boost de production limité dans le temps ;
- prolonger légèrement une pluie de météorites déjà commencée, sans garantir de ressource exceptionnelle ;
- envoyer un drone orbital récupérer quelques fragments manqués.

### 15.2 Règles de protection du gameplay

- Aucune publicité obligatoire pour progresser.
- Récompense annoncée avant le lancement.
- Récompense accordée uniquement après validation du SDK.
- Limites quotidiennes par catégorie.
- Temps minimum entre deux propositions.
- Aucun bouton trompeur.
- Pas de publicité pendant les premières minutes du tutoriel.
- Pas de bannière permanente sur la carte principale.
- Aucune ressource exceptionnelle garantie directement par une publicité.
- Une absence de publicité disponible ne doit jamais bloquer l’action principale.

### 15.3 Monétisation intégrée au monde

Les propositions publicitaires utilisent un habillage cohérent avec l’univers, tout en affichant clairement qu’il s’agit d’une publicité récompensée.

Exemples :

- **Transmission commerciale orbitale** : débloque un contrat premium temporaire.
- **Capsule de ravitaillement sponsorisée** : fournit des matériaux standards.
- **Relais de compression temporelle** : réduit une durée précise.
- **Drone de récupération orbital** : récupère une partie de fragments manqués.
- **Balise d’analyse externe** : scanne une anomalie gratuitement.

Règles d’interface :

- mention « publicité récompensée » visible avant validation ;
- récompense exacte affichée en quantité et en durée ;
- bouton de refus aussi lisible que le bouton d’acceptation ;
- aucune fausse urgence ;
- aucune animation imitant une récompense déjà gagnée ;
- aucune publicité proposée pendant une découverte narrative ou une grande animation.

### 15.4 Interstitiels

À éviter au MVP. Si ajoutés plus tard :

- uniquement après un changement naturel d’écran ;
- jamais après chaque collecte ;
- fréquence fortement limitée ;
- désactivés pendant le tutoriel ;
- jamais en remplacement d’une publicité récompensée promise.

### 15.5 Intégration

- AdMob côté Android.
- Interface `RewardedAdsService` injectée dans le cœur du jeu.
- Identifiants de test en debug.
- Identifiants réels uniquement dans la configuration release.
- Gestion des échecs réseau sans bloquer l’utilisateur.
- Consentement via Google User Messaging Platform selon la zone.
- Journalisation des récompenses pour éviter les doubles attributions.
- Habillage diégétique séparé du service publicitaire afin de pouvoir changer de fournisseur.

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
- `SpecializationScreen`.
- `ModuleScreen`.
- `CodexScreen`.
- `ArchiveScreen`.
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
RobotMasteryState
RobotTraitDefinition
InventoryState
RecipeDefinition
TechnologyDefinition
ProductionJob
MissionDefinition
MissionProgress
ContractState
PrestigeState
SpecializationState
SpecializationDefinition
ModuleInstance
ModuleDefinition
ModuleSetDefinition
CodexState
CodexEntryDefinition
RareDiscoveryState
NarrativeArchiveState
MeteorShowerEventState
PlanetRuleDefinition
AdRewardLedger
GameSettings
```

### 17.2 Définitions statiques et états dynamiques

Séparer strictement :

- les **définitions** : recettes, coûts, temps, textes, modèles, icônes, modules, spécialisations, règles planétaires ;
- les **états** : quantités, niveaux, tâches, missions terminées, secteurs ouverts, collections, robots vétérans.

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

### 17.4 Exemple de module

```json
{
  "id": "meteor_core_drill_mk1",
  "slot": "DRILL_HEAD",
  "rarity": "EXCEPTIONAL",
  "compatibleRobotTypes": ["EX"],
  "stats": {
    "miningSpeedPercent": 12,
    "rareFragmentChanceBasisPoints": 35
  },
  "setId": "meteor_set",
  "setPieces": 1
}
```

### 17.5 Quantités

Utiliser des entiers longs (`Long`) pour les ressources et la monnaie. Éviter les nombres flottants pour les stocks et les coûts. Les multiplicateurs peuvent être calculés avec `BigDecimal` ou une représentation fixe lorsque la précision devient importante.

Les probabilités utilisent des points de base entiers afin d’être testables et déterministes dans les simulations.

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
- Sauvegarde immédiate des ressources exceptionnelles et résultats d’événements.
- Identifiants idempotents pour les fragments récupérés.

### 18.3 Serveur

Le MVP peut fonctionner avec une sauvegarde locale. Concevoir toutefois les interfaces afin d’ajouter ultérieurement :

- sauvegarde cloud ;
- synchronisation multi-appareils ;
- validation de récompenses ;
- événements distants ;
- équilibrage distant ;
- nouvelles planètes et règles chargées à distance après validation de version.

Aucun serveur n’est nécessaire pour valider le cœur de jeu initial.

### 18.4 Anti-triche raisonnable

- Détection des grands changements d’horloge.
- Horodatage de la dernière session valide.
- Plafond de progression hors ligne.
- Journal local des publicités récompensées.
- Journal idempotent des événements interactifs.
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
- Coffre protégé pour ressources exceptionnelles.
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

#### Modules

- Emplacements supplémentaires.
- Amélioration contrôlée.
- Recyclage efficace.
- Synergies avancées.

#### Archives

- Décryptage des fragments.
- Analyse des ruines.
- Cartographie des signaux.
- Technologie de la civilisation disparue.

### 20.2 Choix

Certaines technologies doivent proposer des choix temporaires ou réversibles, par exemple :

- vitesse élevée contre consommation ;
- rendement matière contre durée ;
- stockage contre valeur de vente ;
- production spécialisée contre polyvalence.

Les choix permanents sont réservés au prestige et doivent être clairement confirmés.

## 21. Spécialisations réelles de la base

### 21.1 Déblocage

La première spécialisation est proposée après que le joueur maîtrise l’extraction, le raffinage, l’assemblage et l’énergie. Elle ne doit pas apparaître dans les premières minutes.

Le joueur peut consulter toutes les branches avant de choisir. Une période d’essai ou un changement gratuit est proposé lors de l’introduction.

### 21.2 Empire industriel

Identité : production de masse, machines imposantes et flux continus.

Avantages :

- files plus longues ;
- production par lot améliorée ;
- coûts unitaires réduits ;
- meilleur rendement sur les contrats standards.

Contraintes :

- forte consommation énergétique ;
- moins efficace sur les ressources exceptionnellement rares ;
- infrastructure plus coûteuse à déplacer.

### 21.3 Laboratoire quantique

Identité : faible volume, haute valeur et technologies complexes.

Avantages :

- bonus sur les cristaux, diamants et technologies avancées ;
- meilleures chances de résultats de qualité supérieure ;
- analyse plus efficace des anomalies.

Contraintes :

- productions plus longues ;
- recettes coûteuses ;
- dépendance à des ressources rares.

### 21.4 Réseau robotique

Identité : nombreux robots coordonnés et automatisation flexible.

Avantages :

- maîtrise des robots plus rapide ;
- bonus de coordination ;
- files automatiques et priorités avancées ;
- transfert facilité d’un robot vétéran.

Contraintes :

- coûts de fabrication et d’entretien plus élevés ;
- dépendance aux modules de processeur ;
- efficacité réduite si l’énergie est instable.

### 21.5 Exploitation pionnière

Identité : exploration rapide, scanner puissant et bases légères.

Avantages :

- secteurs moins coûteux ;
- événements et anomalies détectés plus tôt ;
- meilleure récupération de fragments météoritiques ;
- déplacement interplanétaire facilité.

Contraintes :

- capacité de stockage inférieure ;
- production industrielle moins performante ;
- gisements exploités plus rapidement.

### 21.6 Règles d’équilibrage

- chaque spécialisation possède au moins deux styles de sous-construction ;
- aucune branche ne dépasse durablement les autres sur tous les indicateurs ;
- les avantages sont visibles dans la base et les robots ;
- le changement est possible avec un coût raisonnable et un délai, jamais avec une monnaie premium obligatoire ;
- une nouvelle planète peut encourager une autre spécialisation sans rendre le choix précédent invalide ;
- les bonus sont pilotés par données et couverts par le simulateur économique.

## 22. Modules et synergies

### 22.1 Emplacements de modules

Les robots peuvent recevoir progressivement :

- tête ou outil principal ;
- moteur ;
- batterie ;
- refroidissement ;
- processeur ;
- châssis ;
- module spécial.

Le nombre d’emplacements dépend du niveau et du type de robot. Le joueur ne doit pas gérer sept emplacements dès le début.

### 22.2 Niveaux de rareté

- Standard : bonus simple et lisible.
- Amélioré : deux statistiques cohérentes.
- Avancé : bonus plus spécialisé.
- Exceptionnel : effet unique ou appartenance à un ensemble rare.

Il n’existe pas de coffre payant aléatoire. Les modules sont fabriqués, découverts, gagnés par mission ou reconstruits à partir de fragments.

### 22.3 Synergies d’ensemble

Exemple d’ensemble météorique :

- 2 pièces : +8 % de vitesse lors des événements de météorites.
- 3 pièces : un fragment manqué peut être récupéré automatiquement.
- 4 pièces : faible chance d’obtenir un Cœur météorique supplémentaire, soumise à un plafond.

Exemple d’ensemble Cryos :

- 2 pièces : réduction du malus de froid.
- 3 pièces : la chaleur produite est partiellement recyclée.
- 4 pièces : immunité temporaire après une surcharge thermique réussie.

### 22.4 Fabrication et amélioration

- recettes connues et prévisibles ;
- amélioration par paliers ;
- coût affiché avant validation ;
- démontage restituant une partie des matériaux ;
- verrouillage manuel pour éviter la destruction accidentelle ;
- comparaison directe entre module équipé et module sélectionné ;
- filtres par robot, slot, planète et synergie.

### 22.5 Garde-fous

- trois ou quatre statistiques visibles maximum par module ;
- aucun module ne multiplie seul la production de manière disproportionnée ;
- les synergies ne doivent pas imposer un unique équipement optimal ;
- les modules anciens peuvent rester utiles sur certaines planètes ;
- les ressources exceptionnelles améliorent des effets particuliers plutôt que de simples multiplicateurs gigantesques.

## 23. Pluie de météorites et récupération de fragments

### 23.1 Objectif

La pluie de météorites constitue l’activité interactive principale validée. Elle donne au joueur une action courte et satisfaisante pendant que les chaînes automatisées continuent de fonctionner.

Elle reste facultative : ignorer l’événement ne bloque aucune mission principale et ne détruit aucune ressource existante.

### 23.2 Déroulement

1. Un signal orbital annonce une pluie prochaine.
2. Une zone de la carte est mise en évidence.
3. Plusieurs météorites traversent l’atmosphère avec des trajectoires lisibles.
4. Les impacts libèrent des fragments pendant une courte durée.
5. Le joueur touche ou balaie les fragments pour les récupérer.
6. Un robot ou drone proche peut récupérer automatiquement une petite partie des fragments manqués.
7. Un résumé affiche la quantité, la rareté, les entrées de Codex et les éventuels records.

Durée cible : 45 à 90 secondes.

### 23.3 Types de fragments

- Fragment métallique : matériaux communs.
- Fragment cristallin : matériaux raffinables rares.
- Fragment énergétique : batteries et technologies.
- Fragment d’archive : narration et Codex.
- Cœur météorique : ressource exceptionnellement rare.

### 23.4 Difficulté et accessibilité

- trajectoires suffisamment lentes sur petits écrans ;
- tailles tactiles adaptées ;
- mode d’assistance augmentant les zones de collecte ;
- aucun besoin de réflexes extrêmes ;
- pas de pénalité pour un fragment raté ;
- contraste, formes et traînées distinctes selon la rareté ;
- pause ou simplification si une fenêtre système interrompt l’événement ;
- densité adaptée aux performances de l’appareil.

### 23.5 Fréquence

- première pluie scénarisée et garantie pendant la première semaine ;
- événements suivants semi-aléatoires avec fenêtre d’apparition contrôlée ;
- délai minimum entre deux événements ;
- possibilité de stocker une alerte courte si l’utilisateur ouvre le jeu dans la fenêtre ;
- aucune obligation de se connecter à une heure exacte ;
- les pluies manquées ne cassent pas les séries quotidiennes.

### 23.6 Récompenses et publicité

Une publicité récompensée peut :

- prolonger légèrement l’événement ;
- envoyer un drone récupérer quelques fragments manqués ;
- doubler uniquement les fragments standards, avec plafond.

Elle ne peut pas garantir ni doubler directement un Cœur météorique ou une ressource exceptionnelle majeure.

## 24. Codex et collection permanente

### 24.1 Catégories

- minerais bruts ;
- matériaux raffinés ;
- composants ;
- technologies ;
- modules ;
- robots et modèles ;
- planètes ;
- anomalies ;
- ressources exceptionnelles ;
- archives narratives ;
- événements observés ;
- records personnels.

### 24.2 Niveaux d’entrée

1. **Découvert** : l’objet a été vu ou obtenu.
2. **Analysé** : une quantité ou mission d’étude est terminée.
3. **Maîtrisé** : le joueur l’a utilisé dans plusieurs chaînes ou situations.
4. **Complet** : variantes, données et objectifs associés terminés.

### 24.3 Récompenses

- apparences ;
- hologrammes décoratifs ;
- titres ;
- petites améliorations permanentes plafonnées ;
- plans de modules ;
- informations narratives ;
- visualisation détaillée des modèles 3D.

La collection survit à tous les changements de planète.

### 24.4 UX du Codex

- progression globale et par planète ;
- silhouettes pour les éléments non découverts sans révéler leur nom ;
- filtres simples ;
- bouton permettant de localiser la prochaine source possible ;
- aucune notification rouge permanente pour les entrées impossibles à compléter ;
- animation particulière lors d’une entrée exceptionnelle.

## 25. Histoire légère et mystérieuse

### 25.1 Principe narratif

Le joueur est accompagné par une intelligence de bord appelée provisoirement **NOVA**. Elle aide à installer la base, mais découvre progressivement que plusieurs planètes ont déjà été exploitées selon des schémas impossibles à expliquer.

La narration doit rester légère : messages courts, transmissions, ruines visibles et fragments de données. Elle ne doit jamais interrompre une session pendant plusieurs minutes.

### 25.2 Mystère central

Des structures anciennes relient les planètes. Elles semblent avoir été construites par une civilisation qui utilisait les ressources non pour produire des armes, mais pour déplacer ou préserver des mondes entiers.

NOVA ne possède pas toutes ses données. Certains fragments suggèrent qu’elle pourrait provenir de cette ancienne technologie.

### 25.3 Diffusion du récit

- transmissions de 1 à 3 phrases ;
- changements visibles sur la carte ;
- fragments d’archive dans les météorites ;
- robots abandonnés ;
- anomalies analysables ;
- séquences de départ planétaire ;
- pages optionnelles du Codex pour les joueurs intéressés.

### 25.4 Structure par planète

Chaque planète apporte :

- une question narrative ;
- trois à cinq découvertes courtes ;
- une révélation partielle ;
- un nouvel indice vers la planète suivante ;
- aucune résolution définitive empêchant l’extension future.

### 25.5 Règles d’écriture

- textes courts et compréhensibles ;
- pas d’exposition massive ;
- aucune obligation de lire pour gérer la base ;
- résumé accessible des découvertes ;
- ton mystérieux mais non horrifique ;
- narration conçue pour rester extensible sans contradiction majeure.

## 26. Contrats et événements

### 26.1 Contrats

Chaque contrat précise :

- produit demandé ;
- quantité ;
- durée ;
- récompense ;
- difficulté ;
- éventuel bonus de série.

Trois contrats disponibles en permanence : simple, rentable et ambitieux.

### 26.2 Événements de carte

À introduire après stabilisation du MVP :

- pluie de météorites et récupération interactive de fragments ;
- panne de scanner ;
- surcharge énergétique ;
- marchand orbital ;
- anomalie cristalline ;
- robot abandonné à réparer ;
- signal d’archive ancienne ;
- gisement exceptionnel temporaire.

Les événements ne doivent pas punir lourdement une absence.

## 27. Rétention sans frustration

### 27.1 Récompense de retour

L’écran de retour affiche :

- durée d’absence ;
- production réalisée ;
- tâches terminées ;
- gisements épuisés ;
- progression des robots ;
- nouvelles entrées de Codex ;
- stockage perdu faute de place, le cas échéant ;
- possibilité facultative de doubler une partie de la production standard par publicité.

### 27.2 Objectifs visibles

- Mission principale toujours accessible.
- Trois objectifs secondaires maximum sur le HUD.
- Jalon journalier.
- Progression de planète en pourcentage.
- Aperçu du prochain grand déblocage.
- Prochaine étape du Codex épinglée facultativement.
- Progression du robot vétéran suivi.

### 27.3 Notifications locales

Désactivables individuellement :

- tâche importante terminée ;
- stockage plein ;
- contrat proche de l’expiration ;
- énergie rétablie ;
- mission principale réalisable ;
- événement météoritique dans une fenêtre souple ;
- analyse rare terminée.

Aucune notification promotionnelle agressive.

## 28. Équilibrage initial

### 28.1 Durées indicatives

| Niveau de recette | Durée de base |
|---|---:|
| Raffinage commun | 15 s à 2 min |
| Raffinage rare | 5 à 30 min |
| Composant basique | 1 à 10 min |
| Composant avancé | 15 min à 2 h |
| Technologie majeure | 2 à 12 h |
| Élément final de planète | 8 à 24 h |

Les tâches longues apparaissent seulement lorsque le joueur dispose de plusieurs files et objectifs parallèles.

### 28.2 Courbe de coûts

- Améliorations fréquentes : coefficient 1,35 à 1,55.
- Robots : coefficient 1,60 à 1,75.
- Secteurs : coefficient autour de 1,55 avec ajustement manuel.
- Technologies majeures : coûts définis manuellement.
- Modules : croissance plus faible que les robots, mais matériaux spécialisés.
- Changement de spécialisation : coût plafonné et réinitialisation gratuite lors de certaines transitions.

### 28.3 Temps jusqu’au prestige

Objectif de test :

- Joueur très actif : 18 à 25 jours.
- Joueur régulier : 25 à 40 jours.
- Joueur occasionnel : 40 à 60 jours.

La publicité récompensée peut accélérer, mais ne doit pas diviser ce temps par plus de deux sans usage intensif.

### 28.4 Simulation des systèmes ajoutés

Le simulateur doit comparer :

- rendement de chaque spécialisation ;
- combinaisons de modules ;
- fréquence des synergies dominantes ;
- taux d’obtention des ressources exceptionnelles ;
- fréquence et valeur des pluies de météorites ;
- progression du Codex ;
- gain réel des publicités ;
- impact des règles propres à chaque planète.

## 29. Analytique produit et confidentialité

Même sans outil analytique au départ, prévoir des événements internes anonymisés pouvant être activés plus tard :

- tutoriel terminé ;
- premier raffinage ;
- premier robot ;
- premier secteur ;
- première spécialisation ;
- module équipé ;
- synergie activée ;
- pluie de météorites commencée et terminée ;
- fragment récupéré par catégorie, sans journal individuel excessif ;
- ressource exceptionnelle découverte ;
- entrée de Codex complétée ;
- robot devenu vétéran ;
- jour de progression ;
- mission bloquante ;
- prestige ;
- publicité proposée, lancée et récompensée ;
- abandon d’une production.

Ne jamais enregistrer le contenu personnel de l’utilisateur. Documenter précisément les données envoyées par AdMob et les obligations de consentement avant publication.

## 30. Tests

### 30.1 Tests unitaires du domaine

- Calcul des productions.
- Coûts d’amélioration.
- Réservation et remboursement des ressources.
- Fin de tâche en ligne et hors ligne.
- Missions.
- Prestige.
- Saturation de stockage.
- Déficit énergétique.
- Récompenses publicitaires idempotentes.
- Bonus et contraintes de chaque spécialisation.
- Compatibilité des modules.
- Activation et désactivation des synergies.
- Protection contre la malchance des ressources exceptionnelles.
- Progression du Codex.
- Maîtrise et traits des robots.
- Modificateurs planétaires.

### 30.2 Tests d’intégration

- Sauvegarde et restauration.
- Migration de version.
- Retour après plusieurs heures.
- Plusieurs tâches terminées simultanément.
- Gisement épuisé pendant une absence.
- Publicité réussie, annulée ou indisponible.
- Interruption d’une pluie de météorites.
- Attribution idempotente des fragments.
- Changement de spécialisation.
- Équipement et démontage de modules.
- Transfert d’un robot vétéran entre planètes.
- Conservation du Codex après prestige.

### 30.3 Tests visuels et appareils

Formats minimaux :

- 320 × 640 portrait.
- 640 × 320 paysage.
- 390 × 844 portrait.
- 844 × 390 paysage.
- Tablettes Android.
- Écrans avec encoche et barres système.

Même si le jeu est principalement conçu en paysage, les écrans système, menus et erreurs doivent rester utilisables dans toutes les orientations prévues. Décider avant production si le gameplay sera verrouillé en paysage.

Tester spécifiquement la récupération de fragments sur les petits écrans et avec les options d’accessibilité activées.

### 30.4 Tests de performance

- 100 gisements affichés.
- 50 robots ou drones visibles.
- Particules simultanées.
- Pluie de météorites au niveau de densité maximal autorisé.
- Zoom maximal et minimal.
- Retour après 24 heures hors ligne.
- Sauvegarde volumineuse avec plusieurs planètes et collections.
- Appareil Android à faible mémoire.

## 31. Roadmap de développement

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

### Étape 7 — Prototype de plaisir interactif

- Première pluie de météorites.
- Récupération tactile de fragments.
- Résumé de récompense.
- Assistance tactile.
- Événement interrompable et sauvegardé.
- Un fragment rare garanti pour le test.
- Mesure de fluidité et de compréhension.

**Livrable :** activité de 45 à 90 secondes amusante, facultative et rejouable.

Cette étape doit être validée avant de produire une grande quantité d’événements.

### Étape 8 — Robots et automation avancée

- Niveaux.
- Modules de base.
- Plusieurs files.
- Robot logistique.
- Priorités énergétiques.
- Évolutions visuelles.
- Identité, nom et statistiques des robots.
- Premier niveau de maîtrise.

**Livrable :** système d’automatisation profond et robots reconnaissables.

### Étape 9 — Spécialisations, modules et synergies

- Quatre spécialisations principales.
- Écran de comparaison.
- Changement contrôlé de spécialisation.
- Slots de modules.
- Fabrication, amélioration et démontage.
- Deux ensembles de synergie complets.
- Simulateur d’équilibrage.

**Livrable :** deux parties pouvant suivre des stratégies nettement différentes.

### Étape 10 — Missions, contrats, Codex et tutoriel

- Missions principales.
- Missions secondaires.
- Contrats.
- Tutoriel interactif.
- Récompenses.
- Progression de planète.
- Codex permanent.
- Premières collections.

**Livrable :** première semaine de contenu jouable guidé avec objectifs de collection.

### Étape 11 — Narration légère et ressources exceptionnelles

- NOVA et transmissions courtes.
- Archives.
- Ruines et anomalies.
- Ressources exceptionnellement rares.
- Protection contre la malchance.
- Première découverte rare scénarisée.
- Premier robot vétéran.

**Livrable :** mystère compréhensible et découvertes mémorables sans interrompre la gestion.

### Étape 12 — Direction artistique, satisfaction et effets

- Modèles définitifs de la première planète.
- Textures.
- Éclairage.
- Particules.
- Shaders.
- Animations des robots.
- Effets de météorites.
- Matrice de retours visuels et haptiques.
- Qualité graphique réglable.

**Livrable :** vertical slice représentative de la qualité finale et de la sensation de jeu.

### Étape 13 — Contenu de 30 jours

- Toutes les recettes Ferrum Delta.
- Tous les secteurs.
- Technologies finales.
- Missions sur 30 jours.
- Courbe économique.
- Événements variés.
- Collections de la planète.
- Chapitre narratif complet.
- Tests accélérés de simulation.
- Ajustements anti-blocage.

**Livrable :** première planète complète.

### Étape 14 — Prestige et deuxième planète

- Construction du vaisseau.
- Écran de transfert.
- Noyaux Stellaires.
- Bonus permanents.
- Robot vétéran transférable.
- Cryos IX.
- Gestion thermique.
- Modules et ressources propres à Cryos.
- Conservation et réinitialisation correctes.

**Livrable :** cycle complet entre deux planètes dont les règles sont réellement différentes.

### Étape 15 — Progression interplanétaire sans fin définitive

- Carte stellaire extensible.
- Séparation planètes principales et frontière procédurale.
- Modificateurs planétaires pilotés par données.
- Technologies interplanétaires.
- Sauvegarde multi-planètes.
- Génération contrôlée de secteurs et objectifs.
- Garde-fous contre les combinaisons impossibles.

**Livrable :** progression pouvant continuer après le contenu scénarisé disponible sans afficher de fin définitive.

### Étape 16 — Publicités récompensées intégrées au monde

- AdMob debug.
- UMP.
- Service abstrait.
- Récompenses idempotentes.
- Limites et délais.
- Habillage orbital cohérent.
- Tests d’échec.
- Écran de confidentialité.

**Livrable :** monétisation facultative, claire, intégrée à l’univers et non bloquante.

### Étape 17 — Finition et publication

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

## 32. Ordre recommandé des branches

```text
main
├── setup/project-foundation
├── feature/planet-map-prototype
├── feature/core-economy
├── feature/refining-system
├── feature/assembly-system
├── feature/save-offline-progress
├── feature/sectors-exploration
├── feature/meteor-shower-prototype
├── feature/robot-identity-automation
├── feature/specializations
├── feature/modules-synergies
├── feature/missions-tutorial
├── feature/codex-collections
├── feature/narrative-archives
├── feature/rare-resources
├── feature/visual-satisfaction
├── content/ferrum-delta
├── feature/planet-prestige
├── content/cryos-ix
├── feature/infinite-frontier
├── feature/rewarded-ads
└── release/1.0
```

Une fonctionnalité doit être fusionnée uniquement lorsqu’elle est jouable, testée et documentée. Éviter les branches contenant plusieurs systèmes indépendants.

## 33. Définition de terminé

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
- sa documentation est mise à jour ;
- son retour visuel est compréhensible ;
- elle reste utilisable sans publicité ;
- elle ne rend pas une spécialisation ou un module obligatoire ;
- elle respecte la conservation des collections permanentes.

## 34. MVP, version 1.0 et évolutions

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

### Prototype de plaisir

- Une pluie de météorites.
- Récupération de fragments.
- Un fragment rare.
- Un robot nommé.
- Un premier module.
- Une animation de récompense complète.

Objectif : vérifier que le jeu offre une activité plaisante au-delà de l’attente et de la collecte.

### Vertical slice

- Direction artistique quasi définitive.
- Une heure de contenu.
- Une technologie spectaculaire.
- Un secteur à débloquer.
- Une mission complète.
- Une spécialisation testable.
- Une petite collection.
- Une transmission narrative.
- Effets et HUD représentatifs.

Objectif : valider l’expérience, la lisibilité, l’ambiance et l’identité du jeu.

### Version 1.0

- Ferrum Delta complète.
- Cryos IX jouable après prestige avec règles différentes.
- Environ 30 jours de progression sur la première planète.
- Robots, automation, missions et contrats.
- Pluies de météorites.
- Spécialisations.
- Modules et synergies.
- Codex permanent.
- Ressources exceptionnelles.
- Histoire légère et mystérieuse.
- Robots reconnaissables et vétérans.
- Publicités récompensées intégrées au monde.
- Sauvegarde locale robuste.
- Paramètres graphiques et accessibilité.
- Début d’une progression interplanétaire sans écran de fin définitif.

### Après la version 1.0

- Sauvegarde cloud.
- Nouvelles planètes manuelles.
- Enrichissement de la frontière procédurale.
- Nouveaux événements de météorites et anomalies.
- Nouveaux ensembles de modules.
- Nouvelles spécialisations avancées.
- Événements saisonniers sobres.
- Personnalisation de la base.
- Nouveaux robots.
- Contrats communautaires.
- Suite du mystère de NOVA.
- Système audio complet.
- Achats intégrés éventuels uniquement après analyse de la rétention.

## 35. Risques principaux

### Trop de contenu avant validation du plaisir

**Réponse :** construire d’abord une chaîne courte mais complète, puis le prototype de pluie de météorites, et les tester avant de produire le contenu de 30 jours.

### Économie bloquante ou trop lente

**Réponse :** configurations externes, simulateur d’équilibrage et plusieurs objectifs parallèles.

### Interface surchargée

**Réponse :** HUD compact, panneaux contextuels et tests sur petits écrans dès le prototype.

### Rendu 3D trop coûteux

**Réponse :** low-poly, caméra fixe, LOD, instancing, qualité réglable et budgets stricts.

### Publicités trop présentes

**Réponse :** récompenses facultatives, limites quotidiennes, présentation claire et absence de bannière permanente.

### Sauvegarde locale manipulable

**Réponse :** plafonds, contrôle des horodatages et architecture permettant un serveur ultérieur, sans sacrifier le mode hors ligne.

### Première planète trop longue à produire

**Réponse :** systèmes pilotés par données, réutilisation des mécaniques, variations environnementales et production de contenu après le vertical slice.

### Trop de systèmes simultanés

**Réponse :** introduction progressive. Le joueur découvre d’abord la production, puis les modules, ensuite les spécialisations, le Codex et enfin les systèmes interplanétaires.

### Une spécialisation dominante

**Réponse :** simulations comparatives, tests sur plusieurs horizons de temps et correctifs pilotés par configuration.

### Modules illisibles ou trop nombreux

**Réponse :** peu de statistiques, slots progressifs, filtres clairs, comparaison directe et absence de génération aléatoire incontrôlée.

### Frontière infinie répétitive

**Réponse :** combiner modificateurs de règles, objectifs, ressources, événements et familles visuelles ; conserver les planètes principales conçues manuellement comme contenu premium de progression.

### Ressources rares frustrantes

**Réponse :** protection contre la malchance, premières découvertes scénarisées et aucune dépendance obligatoire à un tirage aléatoire pur.

## 36. Priorité immédiate

La première tâche de développement après ce document doit être l’initialisation du projet et un prototype de carte 2.5D comprenant uniquement :

- une caméra orthographique ;
- une petite planète low-poly ;
- une base ;
- trois gisements ;
- déplacement et zoom tactiles ;
- sélection d’un gisement ;
- un panneau affichant son nom, sa réserve et son rendement.

Aucune publicité, aucun système complexe de mission, aucune spécialisation, aucun module et aucun contenu de 30 jours ne doit être développé avant que cette manipulation de carte soit fluide, lisible et agréable.

Après validation de la carte et de la chaîne brute → raffinée → composant, la priorité suivante est le prototype de pluie de météorites afin de vérifier que Miner Space propose une activité interactive satisfaisante en plus de sa progression automatisée.
