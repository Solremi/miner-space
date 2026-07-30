# Miner Space — Sauvegarde et cycle de vie Android

## 1. Objectif

Garantir qu’aucune fermeture, rotation paysage, publicité, mise à jour ou interruption système ne duplique ou ne supprime une progression valide.

## 2. Stratégie de sauvegarde

- Base locale structurée pour les états principaux.
- DataStore pour préférences et réglages.
- Deux snapshots alternés : A et B.
- Checksum pour chaque snapshot.
- Écriture atomique dans un fichier temporaire puis remplacement.
- Journal court des transactions critiques.
- Sauvegarde avant et après prestige.

## 3. Déclencheurs de sauvegarde

Sauvegarde immédiate après :

- vente ou achat ;
- lancement, annulation ou collecte d’une tâche ;
- amélioration de bâtiment ou robot ;
- équipement ou démontage d’un module ;
- obtention d’une ressource exceptionnelle ;
- résultat de pluie de météorites ;
- attribution publicitaire ;
- fin de mission importante ;
- changement de spécialisation ;
- transfert planétaire.

Sauvegarde différée avec debounce de quelques secondes pour :

- déplacement de caméra ;
- ouverture de panneaux ;
- préférences non critiques.

## 4. Cycle de vie

### `onPause` ou passage en arrière-plan

- figer un snapshot de simulation ;
- enregistrer l’heure valide ;
- persister les tâches et événements actifs ;
- mettre en pause les effets et entrées ;
- ne pas considérer l’application comme fermée tant que l’état n’est pas écrit.

### `onResume`

1. vérifier la sauvegarde ;
2. comparer les horodatages ;
3. calculer la progression hors ligne ;
4. résoudre les transactions en attente ;
5. afficher l’écran de retour si nécessaire ;
6. reprendre la scène sans dupliquer les animations de récompense.

### Rotation entre les deux paysages

- aucune recréation de partie ;
- conservation de la caméra ;
- conservation de la sélection ;
- recalcul uniquement de la mise en page ;
- aucune tâche relancée.

### Fermeture forcée ou manque de mémoire

- restaurer le dernier snapshot valide ;
- rejouer seulement les transactions validées du journal ;
- ne jamais réattribuer une récompense déjà confirmée.

## 5. Cas particuliers

### Appel, écran verrouillé ou fenêtre système

- pluie de météorites mise en pause si l’application perd le focus ;
- si la pause dépasse la fenêtre maximale, convertir les fragments déjà collectés en résultat définitif ;
- aucune pénalité sur les fragments non récupérés.

### Publicité

États :

```text
OFFERED
STARTED
SDK_REWARDED
COMMITTED
CANCELLED
FAILED
```

- seul `SDK_REWARDED` peut déclencher la transaction ;
- `COMMITTED` est persisté après écriture de la récompense ;
- au retour, une récompense `SDK_REWARDED` non `COMMITTED` est reprise une seule fois ;
- une publicité interrompue avant validation ne donne rien et ne consomme pas la limite.

### Changement d’heure ou de fuseau

- stocker UTC et dernier temps serveur connu lorsque disponible ;
- détecter un retour en arrière important ;
- plafonner le temps hors ligne au maximum autorisé ;
- ne jamais punir agressivement ;
- afficher un message discret si le calcul a été plafonné.

### Mise à jour de l’application

1. conserver une copie pré-migration ;
2. exécuter les migrations dans l’ordre ;
3. valider les invariants ;
4. écrire un nouveau snapshot ;
5. conserver temporairement l’ancien snapshot pour retour arrière.

## 6. Invariants de sauvegarde

Après chaque chargement :

- quantités et monnaies non négatives ;
- identifiants uniques ;
- une tâche appartient à une file existante ;
- les entrées réservées correspondent à une tâche ;
- un robot n’est pas affecté simultanément à deux emplois incompatibles ;
- un module n’est équipé que sur un robot ;
- la planète active existe ;
- le Codex ne perd jamais une entrée découverte ;
- une transaction publicitaire n’est engagée qu’une fois.

## 7. Corruption

Ordre de restauration :

1. snapshot principal ;
2. snapshot alterné ;
3. copie pré-migration ;
4. nouvelle partie uniquement si aucune donnée n’est récupérable.

L’utilisateur est informé si une sauvegarde plus ancienne a été restaurée, sans jargon technique.

## 8. Sauvegarde cloud future

L’architecture prévoit sans l’imposer en 1.0 :

- identifiant de profil ;
- version de sauvegarde ;
- résolution de conflit par comparaison de progression ;
- aperçu avant remplacement ;
- fusion limitée aux collections permanentes lorsqu’elle est sûre ;
- aucune synchronisation silencieuse détruisant une sauvegarde locale plus avancée.

## 9. Tests obligatoires

- fermeture forcée pendant extraction ;
- fermeture pendant raffinage terminé ;
- fermeture pendant collecte ;
- fermeture pendant publicité récompensée ;
- interruption pendant météorites ;
- redémarrage téléphone ;
- absence de 1 minute, 8 heures, 24 heures et 7 jours ;
- gisement épuisé hors ligne ;
- batterie vide hors ligne ;
- stockage plein hors ligne ;
- rotation paysage gauche/droite ;
- migration depuis chaque schéma conservé ;
- snapshot principal volontairement corrompu ;
- prestige interrompu à chaque étape de transaction.
