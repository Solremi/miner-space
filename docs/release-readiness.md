# Miner Space — Vérification Go / No-Go

## 1. Contrôle du dépôt

Cette commande vérifie uniquement les éléments versionnés :

```sh
sh scripts/release-readiness.sh --repository-only
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/release-readiness.ps1 --repository-only
```

Elle contrôle notamment :

- version sémantique et versionCode ;
- présence des documents obligatoires ;
- sécurité du manifeste Android ;
- garde-fous du build release ;
- présence des coordinateurs de transactions, production et publicité ;
- diagnostics privés ;
- absence de placeholder dans les sources critiques ;
- absence de dossier de workflows.

Un succès signifie uniquement **REPOSITORY READY**. Il ne constitue pas une autorisation de publication.

## 2. Construire et analyser le bundle

Configurer les variables de signature, publicité, politique et support, puis exécuter :

```sh
sh scripts/release-preflight.sh
```

Le préflight lance le contrôle statique du dépôt, les tests JVM, Android Lint release et la construction du bundle signé. Il ne publie rien.

## 3. Collecter les preuves externes

Copier le modèle :

```sh
cp docs/release-evidence.example.properties release-evidence.properties
```

Le fichier réel est ignoré par Git. Il doit contenir :

- le SHA complet du commit testé ;
- le SHA-256 du bundle signé ;
- l’URL HTTPS réellement publiée ;
- l’emplacement du rapport de test ;
- les validations appareils, migration, accessibilité, performance, consentement et stabilité ;
- le résultat du test fermé ;
- la confirmation Play Console et du retour arrière.

Ne jamais mettre dans ce fichier les mots de passe, le keystore, les identifiants personnels des testeurs ou des extraits de sauvegarde.

## 4. Décision finale

```sh
sh scripts/release-readiness.sh
```

Codes de sortie :

- `0` : **GO**, dépôt et preuves externes complets ;
- `1` : **NO-GO**, problème dans le dépôt ;
- `2` : **NO-GO**, dépôt prêt mais preuves externes incomplètes.

La décision doit porter sur le même commit et le même bundle que ceux indiqués dans le fichier de preuves.
