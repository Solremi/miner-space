# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 13

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation, économie, sauvegarde hors ligne, Ferrum Delta, événements, robots, stratégie, missions, Codex, narration, feedback visuel, audio et simulations de campagne.

## Étape 14 — Prestige et Cryos IX

Statut : **transfert planétaire, acquis permanents et boucle Cryos IX implémentés ; validation Gradle complète et Android restante**.

### Prestige et transfert

- bouton `DÉPART` depuis Ferrum Delta ;
- chantier `sector_launch_shipyard` obligatoire ;
- robot à au moins 6 000 points de maîtrise obligatoire ;
- récompense de 3 Noyaux Stellaires ;
- slot séparé `prestige` ;
- état préparé écrit avant toute réinitialisation ;
- totaux attendus de Noyaux, Codex, archives, bonus et vétéran enregistrés ;
- rapprochement idempotent utilisant `max` et unions d’ensembles ;
- réinitialisation des slots planétaires `primary`, `sectors`, `strategy`, `robots` et `meteor_event` ;
- conservation physique et permanente des slots `progression` et `narrative` ;
- création idempotente du slot `cryos_ix` ;
- démarrage automatique sur l’écran de reprise si un transfert reste préparé ;
- démarrage automatique sur Cryos IX après clôture ;
- retour vers Ferrum désactivé après prestige ;
- identité, numéro, niveau, trait, maîtrise et statistiques du vétéran conservés.

### Cryos IX

- manifeste versionné `1.0.0` dans `assets/data/cryos-ix.json` ;
- factory déterministe `CryosIxContentFactory` ;
- 6 secteurs ;
- budget de 16 gisements ;
- 4 ressources locales, dont cryonite et saumure thermique ;
- 4 matériaux raffinés locaux ;
- 8 recettes spécifiques ;
- 5 technologies propres ;
- 8 modules de l’ensemble thermique ;
- 12 missions principales et 10 secondaires ;
- 3 événements adaptés au froid et non punitifs ;
- 2 découvertes narratives ;
- 30 entrées de Codex ;
- état persistant : énergie, chaleur, exposition au froid, réseau, stocks, secteurs, technologies, modules, missions et frontière ;
- installation initiale de la base ;
- génération d’énergie et chauffage séparés ;
- extraction refusée si la chaleur du secteur est insuffisante ;
- consommation réelle de chaleur à chaque extraction ou ouverture ;
- cinq nœuds thermiques consommant cryonite raffinée et verre thermique ;
- ouverture progressive des six secteurs ;
- fabrication d’un module cryogénique ;
- objectif final exigeant six secteurs, cinq nœuds, trois technologies et un module ;
- déblocage de la frontière interplanétaire.

### Contrôles effectués hors Android

- compilation Kotlin du moteur de prestige ;
- compilation Kotlin du moteur thermique Cryos IX ;
- compilation des deux codecs ;
- validation JSON du manifeste Cryos IX ;
- aller-retour du transfert préparé dans le codec ;
- rapprochement répété sans dépasser 3 Noyaux Stellaires ;
- simulation d’une interruption avant la clôture ;
- simulation complète de la boucle Cryos IX ;
- six secteurs ouverts et cinq nœuds construits ;
- douze missions principales validées ;
- objectif final impossible sans réseau, technologies et module ;
- aller-retour complet du slot `cryos_ix`.

### Validation Android restante

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. réaliser le transfert avec une sauvegarde Ferrum complète ;
5. fermer de force à chaque phase du protocole ;
6. vérifier les acquis permanents et le vétéran après reprise ;
7. parcourir la boucle Cryos IX sur une nouvelle sauvegarde transférée ;
8. tester 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. mesurer les FPS en qualité faible ;
10. confirmer l’absence de perte, duplication et blocage permanent.
