# Miner Space — Robots et automatisation avancée

## 1. Objectif

Rendre les robots identifiables, utiles à l’automatisation et lisibles sans tutoriel textuel long. Cette étape fournit le vertical slice des quatre familles avant l’ajout des spécialisations et modules.

## 2. Familles présentes

| Famille | Robot initial | Fonction |
|---|---|---|
| Extracteur EX | Aster · FD-EX-0001 | suit l’activité des gisements et prépare les files d’extraction |
| Raffineur RF | Rhea · FD-RF-0002 | planifie les tâches de raffinage sur plusieurs files |
| Assembleur AS | Kestrel · FD-AS-0003 | planifie les composants et technologies sur plusieurs files |
| Logistique LG | Nox · FD-LG-0004 | transfère réellement les productions en attente vers le stockage |

Chaque robot possède un identifiant stable, un nom, un numéro de série, une famille, un niveau, un trait, une maîtrise, une priorité et des statistiques persistantes.

## 3. Niveaux et files

Chaque famille possède cinq niveaux fonctionnels :

- niveaux 1 et 2 : une file ;
- niveaux 3 et 4 : deux files ;
- niveau 5 : trois files.

Lors de l’ouverture du gestionnaire, les tâches RF et AS encore en attente sont redistribuées sur les files disponibles. Les tâches déjà actives ou prêtes à collecter ne sont pas redémarrées. Les durées restantes restent déterministes.

Les coûts d’amélioration sont définis dans `assets/data/robots.json` et payés en SpaceDollars. Une écriture économique réussie est requise avant de valider le niveau robotique.

## 4. Priorités

Les priorités disponibles sont :

- équilibrée ;
- mission ;
- désengorgement du stockage ;
- ressource rare ;
- valeur de vente.

Le robot LG utilise réellement la priorité sélectionnée pour ordonner les transferts. Il ne crée aucune ressource : chaque unité déplacée est soustraite d’un gisement en attente et ajoutée au stockage, dans la limite de sa capacité.

## 5. Maîtrise et traits

Quatre paliers de maîtrise sont disponibles : novice, expérimenté, expert et vétéran. La maîtrise progresse par travail réellement comptabilisé et temps actif.

Cinq traits sont définis : précis, endurant, rapide, stable et prospecteur. Tous apportent un bonus positif. Aucun trait ne réduit une capacité sous sa valeur de base et aucun robot ne peut devenir inutilisable.

## 6. Identité et évolution visuelle

Le niveau détermine trois paliers visuels :

- châssis initial ;
- châssis renforcé et éléments supplémentaires ;
- châssis avancé avec trois extensions visibles.

La couleur de famille est accompagnée du nom, du code EX/RF/AS/LG, du numéro de série et des libellés : aucune information importante ne dépend uniquement de la couleur.

## 7. Densité et qualité

Le rendu utilise des formes simples et des positions pré-calculées :

- qualité faible : 18 unités visibles ;
- qualité moyenne : 32 unités visibles ;
- qualité élevée : 50 unités visibles.

Le changement de qualité ne modifie jamais la simulation, uniquement la densité visuelle.

## 8. Sauvegarde

La flotte utilise le slot séparé `robots`, protégé par les snapshots alternés et checksums existants. Sont persistés : identités, niveaux, traits, maîtrise, priorités, statistiques, qualité, curseur d’équilibrage et dernier temps logistique.

Le transfert logistique écrit d’abord l’économie. Si l’écriture robotique suivante échoue, une reprise ne peut pas dupliquer les ressources puisque les quantités en attente ont déjà été diminuées dans l’état économique.

## 9. Tests

Les tests couvrent :

- quatre identités uniques ;
- cinq niveaux et trois files ;
- planification parallèle ;
- conservation exacte des ressources ;
- respect des capacités de stockage ;
- traits toujours bénéfiques ;
- progression de maîtrise ;
- cinquante unités en qualité élevée ;
- aller-retour complet du codec.
