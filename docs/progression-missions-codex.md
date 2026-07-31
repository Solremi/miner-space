# Étape 10 — Missions, contrats, tutoriel et Codex

## Tutoriel progressif

Le tutoriel est composé de sept phases persistantes correspondant à la première semaine guidée : extraction du fer, cuivre, raffinage, composant, technologie, secteur et amélioration robotique. Il observe les systèmes réels et ne bloque jamais les autres écrans.

La phase courante, les phases terminées et l’objectif suivi sont sauvegardés dans le slot `progression`. Une fermeture ou une mise en arrière-plan reprend donc exactement la phase en cours.

## Objectifs parallèles

Les missions sont séparées en trois familles :

- principales, pour structurer Ferrum Delta ;
- secondaires, pour proposer des détours rentables ;
- exploits, permanents et non urgents.

Dès l’introduction, plusieurs missions sont actives en parallèle. Les prérequis forment un graphe acyclique et une mission réclamée ne peut plus attribuer sa récompense.

## Contrats

Trois contrats sont présentés simultanément : simple, rentable et ambitieux. Chaque catégorie possède plusieurs variantes et avance de manière déterministe après une livraison.

Une livraison consomme réellement la ressource demandée et crédite les SpaceDollars dans la même transaction logique. Une occurrence livrée n’est plus active et ne peut pas être livrée deux fois.

## Codex permanent

Le Codex conserve les découvertes de ressources, procédés, secteurs, robots et stratégies. Une entrée impossible avec l’état actuel n’est pas affichée : elle apparaît uniquement quand ses prérequis sont remplis et que le joueur a commencé à interagir avec son système.

Les collections regroupent plusieurs entrées. Une collection complète attribue une récompense unique et reste marquée comme réclamée.

## Interface

L’accès `MISSIONS` ouvre trois onglets : objectifs, contrats et Codex. Le bandeau de tutoriel reste visible, quatre éléments maximum sont présentés simultanément et les commandes utilisent des cibles tactiles de 48 unités.

## Persistance

Le slot `progression` bénéficie des snapshots alternés, checksums et remplacements atomiques existants. Les récompenses économiques sont écrites avant l’état de progression ; si la seconde sauvegarde échoue, l’ancien état économique est restauré.
