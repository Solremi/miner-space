# Validation du vertical slice Ferrum Delta

## Commande locale

```sh
sh scripts/balance-check.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/balance-check.ps1
```

Cette commande ne publie rien. Elle exécute les tests du module de simulation et du module de jeu.

## Matrice visuelle déterministe

Les snapshots de layout couvrent :

- 640 × 320 ;
- 844 × 390 ;
- 844 × 390 avec encoche et barre de navigation ;
- cibles tactiles minimales de 48 unités ;
- absence de chevauchement des contrôles ;
- stabilité des coordonnées principales.

Ces snapshots contrôlent la géométrie, pas le rendu GPU final. Les captures sur appareils physiques restent obligatoires après intégration des modèles, textures et polices définitifs.

## Simulation d’équilibrage

`LongHorizonBalanceSimulator` couvre quatre habitudes : occasionnelle, régulière, active et régulière sans publicité, avec trois stratégies : extraction, logistique et recherche.

Les garde-fous actuels imposent :

- une complétion régulière entre 25 et 40 jours ;
- aucune stratégie dominante de plus de 15 % ;
- une progression terminable sans publicité en 45 jours maximum ;
- une simulation strictement déterministe.

Le modèle est un banc d’essai de référence. Après chaque modification importante des recettes, coûts, multiplicateurs ou limites hors ligne, ses paramètres doivent être rapprochés des valeurs réelles du contenu et les seuils doivent être réévalués avec les données du test fermé.
