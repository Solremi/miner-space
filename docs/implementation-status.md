# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 12

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation, économie, sauvegarde hors ligne, exploration, événements, robots, stratégie, missions, contrats, tutoriel, Codex, narration, ressources rares, feedback visuel, réglages et audio essentiel.

## Étape 13 — Ferrum Delta complète

Statut : **catalogue v1 complet, carte active étendue et simulations déterministes implémentés ; validation Gradle complète et Android restante**.

Éléments présents :

- manifeste versionné `1.0.0` dans `assets/data/ferrum-delta-v1.json` ;
- factory déterministe produisant le catalogue complet et ses identifiants stables ;
- 14 secteurs : 1 initial, 10 standards, 2 profonds et 1 final ;
- carte d’exploration active étendue de 6 à 14 secteurs sans changer les identifiants existants ;
- 34 gisements, avec au moins deux sources par ressource brute ;
- 9 ressources brutes, 9 matériaux raffinés et 24 composants ;
- 14 technologies majeures ;
- 24 modules : 10 standards, 8 améliorés, 4 avancés et 2 exceptionnels ;
- 15 bâtiments et quatre familles de robots à cinq niveaux ;
- deux spécialisations par famille, cinq traits et quatre paliers de maîtrise ;
- 42 missions principales, 36 secondaires et 20 de maîtrise ou collection ;
- 12 modèles de contrats et 8 exploits ;
- 12 événements tous facultatifs ;
- 120 entrées de Codex, dont 15 à plusieurs niveaux d’analyse ;
- 10 collections ;
- 5 jalons narratifs et 12 transmissions NOVA ;
- archives actives étendues de 4 à 12 transmissions ;
- toute ressource obligatoire possède une source garantie hors événement aléatoire ;
- validation automatique des volumes, références et graphes acycliques ;
- simulateur de campagne déterministe dans le module `simulation`.

Résultats de simulation locale :

- joueur très actif sans publicité : 22 jours ;
- joueur très actif avec bonus maximal configuré : 18 jours ;
- joueur régulier : 32 jours ;
- joueur occasionnel : 50 jours ;
- 14 secteurs atteints pour chaque profil ;
- aucun secteur bloqué ;
- aucune source obligatoire perdue.

Contrôles déjà effectués hors Android :

- compilation Kotlin du modèle, de la factory et du simulateur ;
- chargement et validation du catalogue `1.0.0` ;
- validation des budgets exacts ;
- validation des dépendances acycliques des secteurs, technologies et missions ;
- simulation des trois profils ;
- contrôle des sources garanties ;
- adaptation des tests qui figeaient les anciens volumes de secteurs et de transmissions.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. démarrer une nouvelle sauvegarde et parcourir la campagne ;
5. tester également une sauvegarde issue du vertical slice ;
6. vérifier les 14 secteurs et leurs prérequis sur écran tactile ;
7. vérifier les 12 archives NOVA ;
8. tester 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. mesurer les FPS en qualité faible ;
10. confirmer les durées réelles et l’absence de blocage permanent.
