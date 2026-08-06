# Miner Space — État d’implémentation

## Règles d’exécution

- branche active : `main` ;
- développement direct sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique ;
- validations locales et manuelles uniquement.

## Étapes 0 à 16

Statut : **implémentées dans le code, validations Android restantes**.

Présent : économie, sauvegarde, Ferrum Delta, robots, stratégie, missions, Codex, narration, présentation, prestige, Cryos IX, frontière et publicités récompensées.

## Refactorisation structurelle

Statut : **terminée avant intégration des assets définitifs**.

- écrans rangés par fonctionnalité ;
- anciens écrans non routés supprimés ;
- `FerrumCommandScreen` réduit à un orchestrateur ;
- caméra, entrées, actions, HUD, textes, état et scène Ferrum séparés ;
- état visuel global supprimé ;
- écrans Stratégie, Missions et Météorites allégés ;
- gros agrégats du domaine divisés en définitions, état et moteur ;
- garde-fou local contre les monolithes, les fichiers obsolètes et les workflows.

Le détail se trouve dans [`refactoring-status.md`](refactoring-status.md).

## Étape 17 — Finition et publication

Statut : **outillage et durcissement du dépôt implémentés ; publication encore NO-GO jusqu’aux validations externes**.

### Accessibilité

- texte 100 %, 115 % et 130 % ;
- contraste élevé ;
- cinq profils de vision des couleurs ;
- réduction des animations, flashes, shaders et étincelles ;
- vibration et son désactivables ;
- politique et crédits accessibles depuis les réglages ;
- migration automatique du profil `presentation` format 1 vers format 2.

### Build et sécurité

- versionCode 100 et versionName 1.0.0 ;
- signature release injectée hors dépôt ;
- validation bloquante des secrets, identifiants AdMob, URL HTTPS et contact ;
- R8 et `shrinkResources` actifs ;
- identifiants publicitaires de test réservés au debug ;
- trafic HTTP en clair interdit ;
- sauvegarde Android et transfert d’appareil désactivés ;
- icône adaptative et écran de lancement ;
- rapport de panne local sans envoi automatique.

### Publication

- politique de confidentialité locale ;
- licences et crédits ;
- brouillon Sécurité des données ;
- fiche Google Play et plan de captures ;
- plan de test fermé ;
- matrice de compatibilité des sauvegardes ;
- publication progressive et retour arrière documentés ;
- scripts de préflight Windows et Unix sans workflow.

### Contrôles effectués hors Android

- compilation Kotlin du modèle d’accessibilité et du codec ;
- migration d’un snapshot `presentation` format 1 ;
- aller-retour complet du format 2 ;
- réduction effective des budgets de flashes ;
- accès cyclique aux cinq modes de vision des couleurs ;
- validation syntaxique XML des nouvelles ressources.

### Blocages avant GO

1. exécuter la compilation et tous les tests locaux après la refactorisation ;
2. créer et sauvegarder la clé réelle hors dépôt ;
3. publier la politique de confidentialité en HTTPS ;
4. renseigner le contact et les identifiants AdMob de production ;
5. produire les captures et ressources Play Store finales ;
6. exécuter le bundle signé sur les appareils cibles ;
7. tester toutes les migrations des versions diffusées ;
8. terminer un test fermé et traiter les retours ;
9. raccorder et valider les offres publicitaires contextuelles réelles ;
10. compléter les formulaires Play Console ;
11. confirmer zéro panne bloquante, ANR et perte de sauvegarde.
