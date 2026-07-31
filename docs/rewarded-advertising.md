# Publicités récompensées 1.0

## Objectif

La publicité constitue une option de confort. Elle ne débloque aucun contenu obligatoire et ne remplace aucune action normale.

## Transaction

1. L’écran vérifie consentement, disponibilité, contexte, plafond et délai.
2. Une demande unique est créée et sauvegardée en `PREPARED`.
3. AdMob est ouvert avec cet identifiant.
4. Le callback utilisateur passe la demande à `SDK_REWARDED` et cet état est sauvegardé.
5. Le moteur applique un droit persistant et passe à `COMMITTED`.
6. L’identifiant rejoint l’ensemble des demandes déjà engagées.

Une demande `SDK_REWARDED` retrouvée au chargement est engagée. Une demande déjà engagée est rejetée sans modifier les droits.

## Plafonds

Le plafond global est de dix engagements par jour. Chaque offre possède également sa limite et son délai. Les offres de retour et d’événement utilisent un identifiant de portée pour empêcher plusieurs utilisations sur la même occurrence.

## Consentement

UMP actualise l’information à chaque lancement. Le SDK publicitaire n’est initialisé qu’après `canRequestAds()`. L’écran orbital permet de relancer la collecte ou d’ouvrir les options de confidentialité lorsqu’elles sont requises.

## Limites techniques actuelles

Les droits sont persistés et consommables par le moteur. Les offres quotidiennes sont accessibles depuis la transmission orbitale. Les offres liées au retour hors ligne et aux météores nécessitent leur identifiant de contexte et restent masquées ou refusées depuis le hub générique.
