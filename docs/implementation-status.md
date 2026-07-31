# Miner Space — État d’implémentation

## Règles d’exécution

- branche active demandée : `main` ;
- développement réalisé directement sur `main` ;
- aucun workflow, aucune CI/CD et aucun déploiement automatique sans demande explicite ;
- vérifications locales ou manuelles uniquement.

## Étapes 0 à 10

Statut : **implémentées dans le code, validations Android restantes**.

Présent : fondation, production, sauvegarde hors ligne, exploration, événements, robots, stratégies, missions, contrats, tutoriel et Codex.

## Étape 11 — Narration et ressources exceptionnelles

Statut : **implémentée dans le code ; moteur, chargeur et codec vérifiés localement ; validation Gradle et Android restante**.

Éléments présents :

- accès `ARCHIVES` depuis la scène principale ;
- NOVA comme couche facultative ne bloquant jamais la gestion ;
- quatre transmissions courtes ;
- quatre chapitres : contact, ruines, anomalie et héritage ;
- résumés persistants dans les archives ;
- prérequis en secteurs, technologies et chapitres précédents ;
- chapitres impossibles totalement masqués ;
- analyse répétable des anomalies ;
- chance déterministe et seuil de garantie configurable ;
- succès garanti en un à quatre essais selon le chapitre ;
- ferrite prismatique, fragment d’archive et cristal xénon scénarisés ;
- ressources obligatoires jamais soumises à un hasard pur ;
- attribution en deux phases avec total attendu ;
- reprise idempotente après interruption ;
- découverte rare enregistrée avant clôture de l’archive ;
- premier robot vétéran : Aster, sans changement de nom, série ou statistiques ;
- maîtrise portée au seuil vétéran de 6 000 points ;
- sauvegarde séparée `narrative` avec snapshots alternés et checksum ;
- contenu versionné `0.11.0` dans `assets/data/narrative.json` ;
- écran limité à quatre lignes, boutons de 48 unités et zones sûres.

Contrôles déjà effectués hors Android :

- compilation Kotlin du moteur narratif ;
- compilation Kotlin du chargeur JSON et du codec ;
- validation JSON du contenu `0.11.0` ;
- vérification du graphe de chapitres acyclique ;
- simulation des seuils de garantie 1, 2 et 3 essais ;
- vérification du calcul de quantité rare manquante ;
- vérification du complément de maîtrise vétéran ;
- aller-retour de l’attribution en attente dans le codec.

Validation encore nécessaire sur une machine Android équipée du SDK :

1. générer le wrapper Gradle et synchroniser le projet ;
2. exécuter `:domain:test`, `:data:test` et `:simulation:test` ;
3. assembler et installer l’APK debug ;
4. parcourir les quatre chapitres depuis une nouvelle sauvegarde ;
5. fermer de force pendant chaque phase d’attribution ;
6. vérifier les résumés après reprise ;
7. confirmer le palier vétéran dans l’écran Robots ;
8. contrôler 640 × 320 et 844 × 390 dans les deux sens paysage ;
9. vérifier les cibles tactiles et zones sûres ;
10. confirmer l’absence d’overflow, de duplication et de blocage permanent.
