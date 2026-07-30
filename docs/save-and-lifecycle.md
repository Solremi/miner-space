# Miner Space — Sauvegarde et cycle de vie Android

## 1. Objectif

Aucune fermeture, interruption système, rotation paysage, mise à jour ou modification d’horloge ne doit supprimer ou dupliquer une progression valide.

## 2. Stockage local

L’état principal utilise un conteneur local structuré comprenant :

- version du conteneur ;
- numéro de séquence monotone ;
- heure UTC de sauvegarde ;
- version du schéma métier ;
- version du contenu ;
- taille du payload ;
- checksum CRC32 ;
- snapshot sérialisé.

Deux fichiers sont alternés : `primary.a.msv` et `primary.b.msv`. Le fichier le plus ancien est remplacé, ce qui conserve toujours la dernière copie valide précédente.

Une écriture suit cet ordre :

1. sérialisation immuable de l’état ;
2. écriture dans un fichier temporaire ;
3. vidage et synchronisation du fichier ;
4. remplacement atomique lorsque le système le permet ;
5. conservation intacte de l’autre snapshot.

L’ancien fichier unique `primary.msv` reste lisible pour permettre la migration.

## 3. Chargement et corruption

Au chargement :

1. lire les snapshots A et B ;
2. vérifier en-tête, taille et checksum ;
3. sélectionner le snapshot valide ayant la séquence la plus élevée ;
4. si l’un est corrompu, restaurer automatiquement l’autre ;
5. à défaut, essayer l’ancien fichier unique ;
6. démarrer une nouvelle partie seulement si aucune donnée n’est récupérable.

L’écran de retour informe le joueur lorsqu’une copie antérieure a été restaurée, sans exposer de jargon technique.

## 4. Schémas et migrations

Le schéma courant est le schéma 3.

- schéma 1 : économie et raffinage ;
- schéma 2 : ajout de l’assemblage et des technologies ;
- schéma 3 : métadonnées temporelles et reprise hors ligne robuste.

La migration :

1. décode le schéma source ;
2. ajoute les systèmes absents avec un état vide valide ;
3. aligne l’inventaire et les gisements sur le contenu courant ;
4. retire les recettes ou identifiants inconnus ;
5. conserve uniquement les technologies dont les prérequis restent cohérents ;
6. valide les invariants ;
7. écrit un nouveau snapshot sans supprimer immédiatement la copie précédente.

## 5. Déclencheurs de sauvegarde

Sauvegarde immédiate après :

- vente ou achat ;
- lancement, annulation ou collecte d’une tâche ;
- fabrication ou installation d’une technologie ;
- récompense importante ;
- changement structurel de progression.

Sauvegarde périodique pour la simulation continue et sauvegarde forcée lors du passage en arrière-plan.

## 6. Cycle de vie Android

### Reprise

1. charger et valider le snapshot ;
2. migrer si nécessaire ;
3. comparer l’heure UTC enregistrée et l’heure actuelle ;
4. calculer la progression hors ligne ;
5. réécrire l’état résolu ;
6. afficher le résumé de retour lorsque nécessaire ;
7. créer ensuite l’écran principal.

### Arrière-plan

`onPause`, `onStop`, `TRIM_MEMORY_UI_HIDDEN` et la mémoire faible signalent l’arrière-plan. Le jeu sauvegarde l’économie, les files et leurs horodatages avant de suspendre l’audio et les effets.

### Rotation paysage

L’activité n’est pas recréée. Seule la mise en page est recalculée ; les tâches ne sont jamais relancées.

## 7. Progression hors ligne

Le temps simulé est :

```text
tempsSimulé = min(max(heureActuelle - heureSauvegardée, 0), capacitéHorsLigne)
```

La capacité initiale est de 8 heures et pourra être améliorée jusqu’à 24 heures.

La simulation applique les limites réelles :

- réserve restante ;
- capacité de transport ;
- capacité de stockage ;
- fin des tâches RF et AS ;
- prérequis et technologies déjà installées.

Aucune production ne continue après saturation. Les tâches terminées restent collectables et ne sont jamais attribuées automatiquement deux fois.

## 8. Changement d’heure

- un léger décalage vers l’arrière est ignoré ;
- un retour significatif de l’horloge produit zéro progression ;
- une avance importante reste plafonnée par la capacité hors ligne ;
- le joueur reçoit un message discret en cas de plafonnement ou d’horloge incohérente ;
- aucune pénalité agressive n’est appliquée.

## 9. Écran de retour

Le résumé affiche selon les événements :

- durée d’absence et durée réellement simulée ;
- quantité extraite ;
- tâches RF et AS terminées ;
- gisements épuisés ;
- blocages par stockage ou transport ;
- plafonnement à 8 heures ;
- détection d’une modification d’horloge ;
- migration ou restauration d’un snapshot alterné.

L’écran respecte les zones sûres, les deux orientations paysage et une cible tactile minimale de 48 unités.

## 10. Invariants

Après chaque chargement :

- stocks, réserves et monnaies non négatifs ;
- inventaire limité aux capacités actuelles ;
- identifiants de tâches uniques ;
- tâche liée à une recette et une ressource existantes ;
- entrées réservées cohérentes ;
- technologies installées existantes et prérequis satisfaits ;
- aucune récompense ou collecte attribuée deux fois.

## 11. Tests obligatoires

- arrière-plan et reprise ;
- fermeture forcée pendant extraction, raffinage et assemblage ;
- redémarrage du téléphone ;
- absence de 1 minute, 8 heures, 24 heures et 7 jours ;
- gisement épuisé hors ligne ;
- stockage plein hors ligne ;
- retour et avance de l’horloge ;
- migration depuis chaque schéma conservé ;
- corruption volontaire du snapshot le plus récent ;
- rotation paysage gauche et droite ;
- absence de gel visible pendant lecture et écriture.

## 12. Éléments futurs

Le journal de transactions publicitaires, les copies dédiées avant prestige et la sauvegarde cloud seront ajoutés avec les systèmes concernés. Les snapshots actuels préservent déjà une copie antérieure adaptée aux migrations ordinaires.
