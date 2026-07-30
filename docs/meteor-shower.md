# Miner Space — Pluie de météorites

## Objectif

La pluie de météorites est l’activité interactive courte principale du vertical slice. Elle est facultative, ne bloque aucune mission et doit rester jouable sans réflexes rapides.

## Prototype 0.7.0

- durée fixe : 60 secondes ;
- plage autorisée par la roadmap : 45 à 90 secondes ;
- 18 fragments actifs maximum ;
- apparition standard toutes les 1,4 seconde ;
- durée de présence d’un fragment : 6,5 secondes ;
- cœur météorique rare de test à 30 secondes ;
- capture par toucher ou glissement ;
- zone de capture standard généreuse ;
- assistance activée par défaut avec zone élargie et récupération automatique des fragments anciens ;
- aucun fragment nécessaire à la campagne principale.

## Interruption et reprise

Le temps de l’événement est du temps actif. Le passage en arrière-plan, un appel, le verrouillage de l’écran ou une fermeture forcée mettent donc la pluie en pause au lieu de laisser les fragments expirer.

L’état persistant contient :

- identifiant et graine de l’événement ;
- temps actif écoulé ;
- prochain index d’apparition ;
- fragments encore présents ;
- fragments récupérés ;
- état de l’assistance ;
- découvertes du Codex temporaire ;
- phase d’attribution de la récompense.

## Attribution idempotente

La récompense utilise deux écritures contrôlées :

1. sauvegarder les totaux d’inventaire attendus dans l’événement ;
2. écrire l’inventaire principal ;
3. marquer l’événement comme terminé.

Après une interruption, le jeu compare les stocks aux totaux attendus et n’ajoute que ce qui manque. Un arrêt entre deux écritures ne peut donc ni perdre ni dupliquer les fragments.

## Récompenses du prototype

- `meteor_fragment_standard` : fragment commun non vendu automatiquement ;
- `rare_meteor_core` : ressource exceptionnelle non vendable ;
- le cœur rare est garanti comme apparition de test, pas comme ressource obligatoire de progression.

## Codex temporaire

Trois entrées sont utilisées pour valider le flux :

- pluie météorique ;
- fragment standard ;
- cœur météorique.

Le Codex temporaire est visible pendant l’événement et dans le résumé. Le Codex permanent sera développé dans une étape ultérieure.

## Performance et accessibilité

- rendu par formes simples et traînées courtes ;
- aucune particule non plafonnée ;
- aucun effet indispensable uniquement indiqué par la couleur ;
- cœur rare plus grand et entouré d’un halo distinct ;
- HUD conservé hors de la zone de capture ;
- boutons de 48 unités ;
- zones sûres Android respectées ;
- cible principale : 640 × 320 et 844 × 390 en `sensorLandscape`.
