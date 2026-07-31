# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 15

Statut : **implémentées dans le code, validations Android restantes**.

Présent : économie, sauvegarde hors ligne, Ferrum Delta, robots, stratégie, missions, Codex, narration, présentation, prestige, Cryos IX et frontière interplanétaire.

## Étape 16 — Publicités récompensées

Statut : **protocole idempotent, plafonds, consentement UMP, AdMob récompensé et écran orbital implémentés ; validation Gradle complète, compte AdMob et Android réels restants**.

### Offres et équilibrage

- huit offres pilotées par données ;
- plafond global de dix engagements par jour ;
- limites individuelles conformes à `docs/monetization.md` ;
- délais de 10, 20, 30 ou 60 minutes selon l’offre ;
- portées quotidiennes, par retour ou par événement ;
- boost orbital limité à 25 % pendant 15 minutes et non cumulable ;
- aucune récompense directe en Noyau Stellaire, ressource rare majeure ou objet narratif ;
- droits publicitaires séparés de la sauvegarde économique principale ;
- jeu normal toujours disponible sans publicité.

### Protocole d’attribution

- identifiant de demande créé avant ouverture du SDK ;
- état `PREPARED` sauvegardé avant l’appel AdMob ;
- callback utilisateur enregistré en `SDK_REWARDED` ;
- droit persistant appliqué en `COMMITTED` ;
- demande engagée ajoutée à l’ensemble des identifiants déjà traités ;
- second callback rejeté ;
- reprise automatique des demandes `SDK_REWARDED` après redémarrage ;
- annulation, indisponibilité et échec réseau sans consommation de plafond ;
- limite consommée uniquement au commit.

### Consentement et Android

- Google Mobile Ads SDK et UMP ajoutés au catalogue Gradle ;
- identifiants de test Google utilisés par défaut ;
- identifiants de production injectables par propriétés Gradle ;
- mise à jour UMP demandée à chaque lancement ;
- aucune initialisation publicitaire avant `canRequestAds()` ;
- état de consentement conservé localement ;
- accès aux préférences de confidentialité depuis l’écran orbital ;
- permissions Internet et état réseau ajoutées ;
- aucune bannière ni publicité interstitielle.

### Placement

- bouton `ORBITAL` ajouté au hub Ferrum ;
- une seule offre mise en avant à la fois ;
- récompense exacte, limite et délai affichés avant lecture ;
- bouton de retour aussi accessible que le bouton de lecture ;
- offres retour/événement refusées hors de leur contexte ;
- moteur bloquant explicitement les placements pendant tutoriel, narration et grande animation.

### Contrôles effectués hors Android

- compilation Kotlin du modèle et du moteur publicitaire ;
- simulation de dix engagements et rejet du onzième ;
- callback dupliqué rejeté ;
- annulation sans consommation de quota ;
- récupération d’une transaction `SDK_REWARDED` ;
- boost non cumulable et plafonné ;
- codec couvrant demandes en attente, portées, plafonds et droits ;
- validation des huit offres et des récompenses interdites ;
- correction des erreurs de compilation trouvées avant publication.

### Validation Android restante

1. synchroniser les dépendances Google ;
2. exécuter les tests Gradle ;
3. assembler et installer l’APK debug ;
4. tester les annonces de test sur appareil ;
5. configurer les messages UMP dans AdMob ;
6. tester EEE, refus, consentement et réouverture des préférences ;
7. tester hors connexion et interruption à chaque phase ;
8. valider les portées retour et événement dans leurs écrans dédiés ;
9. vérifier les formats paysage cibles ;
10. remplacer les identifiants de test avant publication.
