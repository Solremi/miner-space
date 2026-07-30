# Miner Space — Checklist de publication 1.0

## 1. Identité et build

- identifiant d’application définitif ;
- nom public définitif ;
- icône adaptative ;
- écran de lancement ;
- versionCode et versionName validés ;
- clé de signature sauvegardée hors du dépôt ;
- configuration debug séparée de release ;
- identifiants publicitaires de test absents du build release ;
- logs de debug et menus développeur désactivés ;
- minification et ressources validées sans casser la sérialisation.

## 2. Gameplay

- Ferrum Delta complète depuis une nouvelle sauvegarde ;
- Cryos IX accessible et jouable ;
- prestige sans perte de données ;
- frontière interplanétaire fonctionnelle ;
- aucune mission bloquante ;
- aucune ressource obligatoire sans source garantie ;
- aucune spécialisation dominante sur toutes les métriques ;
- aucune publicité nécessaire ;
- pluie de météorites facultative et stable ;
- progression hors ligne conforme.

## 3. Paysage et appareils

Tests réels :

- 640 × 320 ;
- 844 × 390 ;
- appareil 720p ;
- appareil 1080p ;
- tablette ;
- appareil avec encoche ;
- paysage gauche et droit ;
- retour après rotation ;
- boutons système en navigation gestuelle et trois boutons.

Conditions :

- aucun overflow ;
- aucun bouton inaccessible ;
- aucune zone importante sous un inset ;
- texte 130 % utilisable ;
- 30 FPS stable en qualité faible sur appareil faible cible.

## 4. Sauvegarde et mise à jour

- reprise après fermeture forcée ;
- reprise après redémarrage téléphone ;
- snapshot corrompu restauré ;
- migration depuis toutes les versions de test supportées ;
- mise à jour par-dessus une version précédente ;
- aucune duplication de publicité ;
- aucune duplication de météorite ;
- prestige interrompu et repris ;
- changement d’heure plafonné ;
- absence de 24 heures simulée.

## 5. Réseau et publicité

- jeu lancé hors connexion ;
- offre publicitaire indisponible non bloquante ;
- UMP testé avec consentement accordé, refusé et non requis ;
- récompense attribuée une fois ;
- plafonds quotidiens ;
- publicité interrompue ;
- politique de confidentialité accessible ;
- préférences publicitaires accessibles ;
- aucune publicité pendant tutoriel, narration ou décollage.

## 6. Accessibilité

- taille de texte ;
- contraste élevé ;
- daltonisme ;
- réduction des animations ;
- réduction des flashs ;
- vibrations désactivables ;
- jeu compréhensible sans son ;
- météorites en mode assistance ;
- confirmations des actions irréversibles ;
- icônes et formes en complément des couleurs.

## 7. Audio

Minimum version 1.0 :

- interaction principale ;
- validation ;
- erreur ;
- production terminée ;
- découverte rare ;
- ouverture de secteur ;
- pluie de météorites ;
- décollage ;
- ambiance minimale de base.

- volumes séparés ;
- mute complet ;
- aucune coupure brutale au passage arrière-plan ;
- aucune ressource audio manquante.

## 8. Performance et stabilité

- démarrage à froid testé ;
- mémoire sur session de 30 minutes ;
- 100 gisements ;
- 50 robots ou drones selon qualité ;
- pluie maximale ;
- changement rapide de panneaux ;
- plusieurs tâches terminées simultanément ;
- sauvegarde multi-planètes ;
- aucun crash bloquant ;
- aucun ANR connu ;
- aucun freeze de plusieurs secondes ;
- journal des erreurs exploitable.

## 9. Google Play

- fiche courte et longue ;
- captures paysage ;
- image de présentation ;
- icône haute résolution ;
- catégorie et tags ;
- classification du contenu ;
- public cible ;
- déclaration des publicités ;
- formulaire de sécurité des données ;
- URL de politique de confidentialité ;
- coordonnées de contact ;
- pays de distribution ;
- test interne ;
- test fermé ;
- retour des testeurs traité ;
- publication progressive configurée.

## 10. Données et juridique

- données envoyées par AdMob documentées ;
- aucune donnée personnelle dans les logs de gameplay ;
- consentement conforme à la zone ;
- licences des bibliothèques et assets listées ;
- crédits accessibles ;
- procédure de demande de suppression des données si un compte est ajouté ultérieurement ;
- sauvegarde locale expliquée dans la politique.

## 11. Observabilité

Avant publication :

- événements critiques testés ;
- crash reporting configuré uniquement après décision de confidentialité ;
- métriques de tutoriel ;
- première extraction ;
- premier robot ;
- premier secteur ;
- jour de progression ;
- blocage de mission ;
- prestige ;
- publicité proposée et récompensée ;
- désactivation possible des outils non indispensables.

## 12. Déploiement

1. test interne ;
2. test fermé limité ;
3. correction des blocages ;
4. publication à faible pourcentage ;
5. surveillance des crashs, ANR et avis ;
6. augmentation progressive ;
7. arrêt du déploiement si anomalie critique.

## 13. Retour arrière

- conserver le dernier bundle stable ;
- ne jamais publier une migration destructrice sans copie ;
- documenter les changements de schéma ;
- préparer une correction compatible avec les sauvegardes déjà migrées ;
- ne pas dépendre d’un retour automatique à une version Android antérieure.

## 14. Go / No-Go

La publication est autorisée uniquement si :

- aucune anomalie critique ouverte ;
- aucune perte de sauvegarde reproduite ;
- gameplay paysage validé sur les formats de référence ;
- Ferrum Delta et transfert vers Cryos IX terminables ;
- monétisation facultative et conforme ;
- politique de confidentialité publiée ;
- tests fermés validés ;
- plan de correctif et retour arrière documenté.
