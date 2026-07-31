# Miner Space — Brouillon Sécurité des données Google Play

Ce document prépare le formulaire Google Play. Il doit être revérifié avec les déclarations générées par les SDK réellement inclus dans le bundle signé.

## Fonctionnement sans compte

- aucun compte Miner Space ;
- aucune synchronisation serveur de la sauvegarde ;
- progression et réglages stockés localement ;
- sauvegarde Android et transfert d’appareil désactivés ;
- aucun identifiant publicitaire enregistré dans la sauvegarde de gameplay.

## SDK publicitaires

Google Mobile Ads et UMP peuvent traiter, selon la configuration, la région et le consentement : identifiants de l’appareil ou publicitaires, adresse IP, informations réseau, diagnostics, performances du SDK, interactions publicitaires et informations de consentement.

Finalités à vérifier : publicité ou marketing, mesure, sécurité et prévention de la fraude, conformité légale et fonctionnement du SDK.

## Données non envoyées par Miner Space

La version 1.0 n’envoie pas volontairement à un serveur Miner Space : inventaire, missions, progression, stratégie, Codex, archives, nom des robots ou rapports de panne locaux.

## Contrôles avant soumission

1. analyser le bundle signé dans Play Console ;
2. comparer les déclarations Google Mobile Ads et UMP ;
3. confirmer le public cible et la configuration familiale si applicable ;
4. publier une URL HTTPS de politique de confidentialité ;
5. renseigner une adresse de contact réelle ;
6. refaire le formulaire après toute modification de SDK.
