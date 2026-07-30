# Miner Space — Roadmap de développement

> Document maître du projet. Le game design détaillé est conservé dans [`docs/game-design.md`](docs/game-design.md). Les règles de calcul, le contenu, la technique, l’UX, la monétisation et la publication sont détaillés dans les documents spécialisés du dossier [`docs/`](docs/).

## 1. Décisions verrouillées

- Plateforme initiale : Android.
- Langage : Kotlin.
- Moteur : LibGDX avec KTX.
- Rendu : scène 3D low-poly avec caméra orthographique et interface 2D.
- Gameplay principal : **paysage uniquement**, avec orientation Android `sensorLandscape` pour accepter les deux sens du téléphone.
- Formats de référence gameplay : **640 × 320** et **844 × 390**.
- Formats complémentaires : tablettes, écrans pliables et écrans avec encoche.
- Les écrans système, consentement, confidentialité, restauration et erreurs restent adaptatifs même si le gameplay est verrouillé en paysage.
- Progression : extraction → raffinage → composants → technologies → planète suivante.
- Durée cible de Ferrum Delta : 25 à 40 jours pour un joueur régulier.
- Le jeu ne possède pas de fin définitive : planètes principales puis frontière interplanétaire contrôlée.
- Monétisation principale : publicités récompensées facultatives et intégrées à l’univers.
- Aucun workflow ou CI/CD ne doit être créé ou lancé au démarrage.

## 2. Documents de référence

| Document | Fonction |
|---|---|
| [`docs/game-design.md`](docs/game-design.md) | Vision, systèmes, progression, robots, planètes, Codex et narration |
| [`docs/content-v1.md`](docs/content-v1.md) | Budget exact de contenu de Ferrum Delta, Cryos IX et version 1.0 |
| [`docs/economy-and-formulas.md`](docs/economy-and-formulas.md) | Formules déterministes, énergie, arrondis, hors ligne et équilibrage |
| [`docs/technical-architecture.md`](docs/technical-architecture.md) | Modules Gradle, modèle de données, services et conventions techniques |
| [`docs/save-and-lifecycle.md`](docs/save-and-lifecycle.md) | Sauvegarde, transactions, reprise, migrations et cycle de vie Android |
| [`docs/ux-accessibility.md`](docs/ux-accessibility.md) | Paysage, HUD, écrans, responsive, accessibilité et critères UX |
| [`docs/monetization.md`](docs/monetization.md) | AdMob, plafonds, récompenses, consentement et garde-fous |
| [`docs/infinite-frontier.md`](docs/infinite-frontier.md) | Génération contrôlée des mondes après le contenu scénarisé |
| [`docs/release-checklist.md`](docs/release-checklist.md) | Qualité, Google Play, tests fermés et publication progressive |

Une étape n’est terminée que si le document spécialisé correspondant est à jour.

## 3. Définition de la version 1.0

La version 1.0 contient obligatoirement :

- Ferrum Delta complète selon le budget de contenu défini ;
- Cryos IX jouable après prestige, avec une boucle complète minimale et des règles réellement différentes ;
- la chaîne brute → raffinée → composant → technologie ;
- robots extracteurs, raffineurs, assembleurs et logistiques ;
- robots nommés, maîtrise, traits et transfert d’un vétéran ;
- quatre spécialisations principales ;
- modules, fabrication, démontage et au moins deux ensembles de synergie ;
- carte 2.5D fluide en paysage ;
- missions, contrats, Codex, archives et ressources exceptionnelles ;
- pluie de météorites interactive ;
- progression hors ligne robuste ;
- prestige et transfert vers Cryos IX ;
- début fonctionnel de la frontière interplanétaire ;
- publicités récompensées facultatives ;
- effets visuels, vibrations facultatives et audio essentiel minimal ;
- paramètres graphiques et accessibilité ;
- politique de confidentialité, consentement et fiche Google Play complète.

## 4. Règles globales de qualité

### 4.1 Gameplay

- Une session utile dure de 2 à 8 minutes.
- Le joueur dispose toujours d’au moins trois objectifs parallèles après le tutoriel.
- Aucune attente unique ne bloque toute la progression.
- Une publicité ne débloque jamais une fonction obligatoire.
- Une mauvaise décision économique reste récupérable.
- Une nouvelle planète modifie au minimum une ressource, une chaîne, une contrainte, une règle logistique et un événement.

### 4.2 UX

- Une action fréquente nécessite au maximum deux pressions.
- Toutes les cibles tactiles mesurent au moins 48 dp.
- Aucun overflow sur 640 × 320 et 844 × 390.
- Les boutons, panneaux et textes respectent les zones système et les encoches.
- Aucun élément important n’est expliqué uniquement par une couleur.
- Les écrans de production indiquent toujours entrée, stock, durée, robot, résultat, blocage et prochaine action utile.

### 4.3 Performance

- Objectif : 60 FPS sur appareil Android moyen.
- Minimum accepté : 30 FPS stable sur appareil faible en mode qualité faible.
- Aucun gel visible pendant sauvegarde, chargement JSON ou attribution publicitaire.
- La reprise après arrière-plan ne duplique aucune ressource ni récompense.
- Les particules, ombres, météorites et drones sont plafonnés par niveau de qualité.

### 4.4 Robustesse

- Toute transaction économique importante est atomique.
- Toute ressource exceptionnelle est sauvegardée immédiatement.
- Les migrations de sauvegarde sont versionnées et testées.
- Une sauvegarde corrompue restaure automatiquement la dernière copie valide.
- Les récompenses publicitaires et événements interactifs utilisent des identifiants idempotents.

## 5. Roadmap de développement

Chaque étape est développée sur une branche dédiée et fusionnée uniquement après validation des critères d’acceptation.

### Étape 0 — Fondation du projet

Branche : `setup/project-foundation`

Travaux :

- initialiser Gradle Kotlin et les modules ;
- intégrer LibGDX et KTX ;
- préparer les variantes debug et release ;
- configurer `sensorLandscape` pour l’activité de jeu ;
- mettre en place les conventions de code et de données ;
- créer les services abstraits : horloge, sauvegarde, audio, publicité, vibration, notifications ;
- ajouter une scène vide et un écran d’erreur minimal.

Critères d’acceptation :

- installation et démarrage sur appareil réel ;
- activité de jeu verrouillée dans les deux sens paysage ;
- aucune dépendance Android dans le domaine ;
- démarrage sans crash hors connexion ;
- aucune CI/CD ajoutée.

### Étape 1 — Prototype de carte 2.5D

Branche : `feature/planet-map-prototype`

Travaux :

- caméra orthographique ;
- déplacement tactile et pincement ;
- limites de carte ;
- sélection d’un objet ;
- base temporaire ;
- trois gisements ;
- HUD minimal responsive ;
- prise en charge des zones système.

Critères d’acceptation :

- aucune sortie de carte ;
- sélection fiable malgré zoom et déplacement ;
- aucun overflow en 640 × 320 et 844 × 390 ;
- 60 FPS sur appareil cible moyen avec la scène de test ;
- recentrage Base fonctionnel.

### Étape 2 — Économie minimale

Branche : `feature/core-economy`

Travaux :

- ressources et inventaire ;
- gisements et réserves ;
- extraction continue ;
- SpaceDollars ;
- vente ;
- définitions JSON ;
- calculs déterministes et tests JVM.

Critères d’acceptation :

- extraire, collecter et vendre fonctionne sans duplication ;
- stocks et monnaie utilisent des entiers ;
- arrondis conformes à `docs/economy-and-formulas.md` ;
- simulation de 24 heures sans valeur négative ni dépassement.

### Étape 3 — Raffinage

Branche : `feature/refining-system`

Travaux :

- robot RF ;
- recettes ;
- file persistante ;
- réservation des ingrédients ;
- annulation et remboursement ;
- collecte ;
- blocages de stockage ;
- premier effet visuel.

Critères d’acceptation :

- fermeture forcée pendant une tâche sans perte ni duplication ;
- annulation conforme à la recette ;
- résultat conservé si stockage plein ;
- relance d’une recette en deux actions maximum.

### Étape 4 — Assemblage et technologies

Branche : `feature/assembly-system`

Travaux :

- robot AS ;
- composants ;
- technologies installables ;
- effets de technologie ;
- arbre de déblocage ;
- comparaison avant/après.

Critères d’acceptation :

- chaîne complète brute → raffinée → composant → technologie ;
- aucune technologie indispensable vendue sans avertissement ;
- effets appliqués dans l’ordre officiel des multiplicateurs.

### Étape 5 — Sauvegarde et progression hors ligne

Branche : `feature/save-offline-progress`

Travaux :

- stockage local structuré ;
- snapshots alternés ;
- migrations ;
- calcul hors ligne ;
- écran de retour ;
- restauration après corruption ;
- cycle de vie Android complet.

Critères d’acceptation :

- reprise correcte après arrière-plan, fermeture forcée et redémarrage du téléphone ;
- test d’absence de 1 minute, 8 heures et 24 heures ;
- gisement épuisé et stockage plein simulés correctement ;
- changement d’heure ne crée pas de gain excessif.

### Étape 6 — Secteurs et exploration

Branche : `feature/sectors-exploration`

Travaux :

- secteurs ;
- brouillard ;
- scanner ;
- coûts ;
- gisements rares ;
- centrage sur mission ;
- ouverture visuelle d’une zone.

Critères d’acceptation :

- chaque secteur possède au moins une raison stratégique d’être ouvert ;
- coût et prérequis toujours visibles ;
- aucun secteur indispensable définitivement inaccessible ;
- ouverture fluide en mode qualité faible.

### Étape 7 — Prototype de plaisir interactif

Branche : `feature/meteor-shower-prototype`

Travaux :

- pluie de météorites de 45 à 90 secondes ;
- récupération tactile ;
- assistance ;
- fragments standards et rare de test ;
- interruption et reprise ;
- résumé et Codex temporaire.

Critères d’acceptation :

- jouable en 640 × 320 sans masquer le HUD essentiel ;
- aucun fragment obligatoire pour la campagne ;
- aucun besoin de réflexes extrêmes ;
- interruption système sans duplication ;
- stabilité au nombre maximal de fragments autorisé.

### Étape 8 — Robots et automatisation avancée

Branche : `feature/robot-identity-automation`

Travaux :

- niveaux ;
- robot logistique ;
- plusieurs files ;
- priorités ;
- nom, numéro de série et statistiques ;
- maîtrise et traits ;
- évolution visuelle.

Critères d’acceptation :

- chaque robot important est identifiable ;
- automatisation compréhensible sans tutoriel textuel long ;
- aucun trait ne rend un robot inutilisable ;
- 50 robots ou drones visibles au niveau de qualité adapté.

### Étape 9 — Spécialisations, modules et synergies

Branches : `feature/specializations` puis `feature/modules-synergies`

Travaux :

- quatre spécialisations ;
- essai et changement contrôlé ;
- emplacements de modules progressifs ;
- fabrication, amélioration et démontage ;
- deux ensembles complets ;
- simulateur comparatif.

Critères d’acceptation :

- deux parties peuvent suivre des stratégies nettement différentes ;
- aucune spécialisation ne domine extraction, valeur, vitesse et progression simultanément ;
- trois ou quatre statistiques visibles maximum par module ;
- aucun coffre aléatoire payant.

### Étape 10 — Missions, contrats, tutoriel et Codex

Branches : `feature/missions-tutorial` puis `feature/codex-collections`

Travaux :

- tutoriel progressif ;
- missions principales et secondaires ;
- contrats ;
- exploits ;
- Codex permanent ;
- collections ;
- objectifs visibles.

Critères d’acceptation :

- première semaine entièrement guidée sans bloquer l’interface ;
- au moins trois objectifs parallèles après introduction ;
- aucun marqueur permanent pour une entrée actuellement impossible ;
- tutoriel reprenable après fermeture.

### Étape 11 — Narration et ressources exceptionnelles

Branches : `feature/narrative-archives` puis `feature/rare-resources`

Travaux :

- NOVA ;
- transmissions courtes ;
- ruines et anomalies ;
- archives ;
- protection contre la malchance ;
- première ressource rare scénarisée ;
- premier robot vétéran.

Critères d’acceptation :

- narration ignorée sans bloquer la gestion ;
- résumé disponible dans les archives ;
- aucune ressource indispensable soumise à un hasard pur ;
- découverte rare sauvegardée immédiatement.

### Étape 12 — Direction artistique, feedback et audio essentiel

Branche : `feature/visual-satisfaction`

Travaux :

- modèles et textures Ferrum Delta ;
- éclairage, particules et shaders ;
- animations des robots ;
- effets de météorites ;
- retours haptiques facultatifs ;
- sons essentiels temporaires puis définitifs : interaction, production terminée, rareté, erreur, ouverture de secteur et décollage ;
- réglages qualité, effets, vibration et volume.

Critères d’acceptation :

- aucune action importante ne change uniquement un nombre ;
- réduction des animations disponible ;
- jeu utilisable sans son ;
- audio essentiel présent avant la 1.0 ;
- 30 FPS stable en qualité faible sur appareil faible cible.

### Étape 13 — Ferrum Delta complète

Branche : `content/ferrum-delta`

Travaux :

- produire le budget défini dans `docs/content-v1.md` ;
- finaliser recettes, secteurs, technologies, missions, événements, collections et chapitre narratif ;
- simuler plusieurs profils de joueurs ;
- corriger les blocages et stratégies dominantes.

Critères d’acceptation :

- joueur régulier simulé entre 25 et 40 jours ;
- joueur occasionnel non bloqué ;
- joueur très actif ne termine pas en quelques jours sans usage publicitaire intensif ;
- aucune ressource obligatoire perdue définitivement ;
- contenu complet validé sur une nouvelle sauvegarde.

### Étape 14 — Prestige et Cryos IX

Branches : `feature/planet-prestige` puis `content/cryos-ix`

Travaux :

- vaisseau et transfert ;
- Noyaux Stellaires ;
- robot vétéran ;
- conservation et réinitialisation ;
- Cryos IX ;
- froid, chaleur, réseau thermique ;
- modules et recettes propres.

Critères d’acceptation :

- transfert atomique et reprenable après crash ;
- Codex, archives et bonus permanents conservés ;
- Cryos IX possède une boucle complète minimale ;
- la planète ne se limite pas à de nouveaux multiplicateurs.

### Étape 15 — Frontière interplanétaire 1.0

Branche : `feature/infinite-frontier`

Travaux :

- carte stellaire ;
- mondes générés à partir de secteurs préconstruits ;
- trois familles visuelles ;
- modificateurs compatibles ;
- objectifs générés ;
- validation automatique ;
- sauvegarde multi-planètes.

Critères d’acceptation :

- aucune combinaison impossible ;
- chaîne de progression toujours réalisable ;
- pas de répétition immédiate de même famille et mêmes modificateurs ;
- graine persistée ;
- retour sur la partie après fermeture ;
- aucune mention « jeu terminé ».

### Étape 16 — Publicités récompensées

Branche : `feature/rewarded-ads`

Travaux :

- AdMob et UMP ;
- offres pilotées par données ;
- récompenses idempotentes ;
- plafonds quotidiens ;
- délais ;
- habillage orbital ;
- écrans de consentement et confidentialité.

Critères d’acceptation :

- jeu entièrement jouable sans publicité ;
- échec réseau non bloquant ;
- récompense attribuée une seule fois ;
- plafonds conformes à `docs/monetization.md` ;
- aucune publicité pendant tutoriel, narration ou grande animation.

### Étape 17 — Finition et publication

Branche : `release/1.0`

Travaux :

- accessibilité complète ;
- optimisation ;
- tests fermés ;
- correction des crashs et ANR ;
- signature et versionnement ;
- icône, captures et fiche Play Store ;
- sécurité des données ;
- politique de confidentialité ;
- publication progressive.

Critères d’acceptation :

- checklist `docs/release-checklist.md` entièrement validée ;
- aucune anomalie bloquante connue ;
- sauvegarde migrée depuis toutes les versions de test conservées ;
- tests hors connexion, mise à jour et appareil faible réussis ;
- retour arrière documenté.

## 6. Budget de portée

Pour éviter un projet impossible à terminer :

- le MVP valide uniquement la carte et la chaîne économique ;
- le prototype de plaisir valide la pluie de météorites avant tout contenu massif ;
- le vertical slice valide une heure de contenu avec une spécialisation, un module, une collection et une transmission ;
- Ferrum Delta n’est produite en totalité qu’après validation du vertical slice ;
- Cryos IX 1.0 possède moins de contenu que Ferrum Delta mais une boucle réellement complète ;
- la frontière 1.0 repose sur des secteurs préconstruits et des règles contrôlées, pas sur une génération libre ;
- les nouvelles planètes manuelles, nouvelles familles procédurales et musiques complètes sont postérieures à la 1.0.

## 7. Définition de terminé

Une fonctionnalité est terminée lorsque :

- son comportement principal et ses erreurs sont gérés ;
- son état est sauvegardé et migrable ;
- elle fonctionne après reprise si nécessaire ;
- son interface est utilisable en paysage sur les formats de référence ;
- ses calculs critiques sont testés ;
- ses paramètres sont pilotés par données ;
- son retour visuel est compréhensible ;
- elle reste utilisable sans publicité ;
- elle respecte l’accessibilité ;
- elle ne dégrade pas les budgets de performance ;
- les documents concernés sont mis à jour ;
- ses critères d’acceptation sont validés sur appareil réel.

## 8. Priorité immédiate

Commencer uniquement par l’étape 0, puis l’étape 1. Ne développer ni publicité, ni spécialisation, ni contenu de 30 jours avant validation de la carte 2.5D en paysage et de la chaîne économique minimale.
