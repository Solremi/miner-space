# Miner Space — Publication progressive et retour arrière

## Préparation

- conserver le dernier Android App Bundle validé et son mapping R8 ;
- sauvegarder la clé de signature hors du dépôt avec deux copies chiffrées ;
- archiver versionCode, versionName, schémas de sauvegarde et versions de contenu ;
- publier la politique de confidentialité avant la soumission ;
- ne jamais placer les mots de passe ou la clé dans Git.

## Publication progressive

1. test interne ;
2. test fermé ;
3. petit pourcentage de production ;
4. observation des erreurs, performances et avis ;
5. augmentation par paliers uniquement si les indicateurs restent stables.

## Arrêt et correctif

Suspendre l’augmentation en cas de perte de sauvegarde, blocage de progression, erreur publicitaire répétée ou problème de stabilité. Préparer ensuite un versionCode supérieur, conserver la compatibilité des sauvegardes et tester le correctif par-dessus la version concernée.

## Limite du retour arrière Android

Une ancienne version ne doit pas être considérée comme un mécanisme automatique de restauration. Après migration d’une sauvegarde, le correctif doit savoir lire la version migrée. Toute migration destructive est interdite sans copie ou format de repli.
