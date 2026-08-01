# Miner Space — Checklist de publication 1.0

Légende : `[x]` contrôlé dans le dépôt ; `[ ]` validation externe ou sur appareil obligatoire. La publication reste **NO-GO** tant qu’une case obligatoire demeure ouverte.

## Identité et build

- [x] identifiant `fr.solremi.minerspace`, nom Miner Space, versionCode 100 et versionName 1.0.0 ;
- [x] icône adaptative Android et écran de lancement ;
- [x] debug et release séparés ; R8 et suppression des ressources activés en release ;
- [x] build release bloqué si signature, identifiants AdMob, URL HTTPS ou contact manquent ;
- [x] scripts de préflight et vérification statique du dépôt ;
- [x] secrets et preuves de publication attendus hors Git ;
- [ ] clé de signature réelle créée, sauvegardée et testée ;
- [ ] bundle signé installé et SHA-256 enregistré ;
- [ ] mapping R8 archivé ;
- [ ] icône Play Store 512 × 512, captures et image de présentation exportées.

## Gameplay et sauvegarde

- [x] Ferrum Delta, prestige, Cryos IX et frontière présents ;
- [x] sources garanties et génération de frontière testées ;
- [x] jeu normal indépendant des publicités ;
- [x] snapshots alternés, checksum, restauration de repli et migration `presentation` 1 → 2 ;
- [x] journal multi-slots avec reprise idempotente au démarrage ;
- [x] transfert planétaire, ouverture de secteur, récompense météorique et consommation publicitaire atomiques ;
- [x] actions de production publiées uniquement après sauvegarde réussie ;
- [x] offres retour hors ligne et météorites raccordées dans leur écran dédié ;
- [ ] campagne et transfert terminés sur une nouvelle sauvegarde réelle ;
- [ ] migration par-dessus chaque version de test conservée ;
- [ ] offres contextuelles validées avec publicité réelle, refus, fermeture et absence de réseau ;
- [ ] aucune mission bloquante ou stratégie dominante confirmée par test fermé.

## Paysage et accessibilité

- [x] nouveaux écrans avec insets sûrs et cibles principales de 48 unités ;
- [x] politique de layout testée pour 640 × 320 et 844 × 390 ;
- [x] test instrumenté de l’orientation paysage ;
- [x] texte 100 %, 115 % et 130 % ;
- [x] contraste élevé ;
- [x] défaut, deutéranopie, protanopie, tritanopie et monochrome ;
- [x] réduction des animations et des flashes ;
- [x] vibration et son désactivables ;
- [ ] 640 × 320, 844 × 390, 720p, 1080p, tablette et encoche sur appareils réels ;
- [ ] paysage gauche et droit, navigation gestuelle et trois boutons ;
- [ ] texte 130 % sans overflow sur tous les écrans ;
- [ ] 30 FPS stable en qualité faible.

## Réseau, publicité et confidentialité

- [x] trafic HTTP en clair interdit ; sauvegardes Android et transfert d’appareil désactivés ;
- [x] UMP demandé avant initialisation AdMob ;
- [x] récompenses idempotentes, limitées par contexte et non bloquantes ;
- [x] politique et crédits consultables hors connexion ;
- [x] préférences publicitaires accessibles ;
- [x] diagnostics et crashs locaux sans envoi automatique ni contenu de sauvegarde ;
- [ ] URL HTTPS de politique publiée ;
- [ ] adresse de contact réelle renseignée ;
- [ ] UMP testé accordé, refusé et non requis ;
- [ ] formulaire Sécurité des données validé dans Play Console.

## Performance et stabilité

- [x] budgets LOW/MEDIUM/HIGH et profil appareil faible ;
- [x] registre d’assets à références comptées avant intégration des fichiers définitifs ;
- [x] moniteur de frame à tampon fixe ;
- [x] journal de panne local enrichi de diagnostics privés bornés ;
- [ ] démarrage à froid et session de 30 minutes mesurés ;
- [ ] 100 gisements, 50 robots, pluie maximale et tâches simultanées testés ;
- [ ] aucun crash bloquant, ANR ou gel confirmé par test fermé.

## Google Play et déploiement

- [x] brouillons de fiche, Sécurité des données, plan de captures, test fermé et retour arrière ;
- [x] modèle privé de preuves et vérificateur Go/No-Go ;
- [ ] classification, public cible, publicité, pays et coordonnées complétés ;
- [ ] test interne et test fermé terminés ;
- [ ] retours testeurs traités ;
- [ ] publication progressive configurée ;
- [ ] responsable du retour arrière confirmé.

## Go / No-Go

Exécuter d’abord :

```sh
sh scripts/release-readiness.sh --repository-only
```

Puis, lorsque `release-evidence.properties` est intégralement renseigné :

```sh
sh scripts/release-readiness.sh
```

La version reste **NO-GO** jusqu’au bundle signé, à la politique HTTPS, aux tests appareil, aux migrations réelles, à la validation des publicités contextuelles, au test fermé, aux captures et aux formulaires Play Console.
