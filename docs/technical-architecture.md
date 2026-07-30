# Miner Space — Architecture technique

## 1. Modules Gradle

```text
miner-space/
├── androidApp/        # cycle de vie Android, AdMob, UMP, notifications, launcher
├── game/              # LibGDX, rendu, caméra, entrées, HUD, effets
├── domain/            # règles de jeu pures et cas d’usage
├── data/              # sauvegarde, migrations, repositories, chargement JSON
├── simulation/        # simulateur économique et génération de mondes
├── shared/            # types, temps, résultats, logs, mathématiques fixes
├── assets/            # modèles, textures, shaders, localisation et données
├── docs/
├── build.gradle.kts
├── settings.gradle.kts
└── roadmap.md
```

Le module `domain` ne dépend ni d’Android, ni de LibGDX, ni d’AdMob.

## 2. Orientation et activité Android

- L’activité de gameplay utilise `sensorLandscape`.
- La rotation entre paysage gauche et paysage droit conserve la scène et les panneaux ouverts.
- Les écrans de consentement, confidentialité et restauration utilisent une UI Android adaptative ou un écran LibGDX responsive.
- Les dimensions ne sont jamais calculées depuis une résolution fixe : utiliser viewport, insets et unités logiques.

## 3. Services abstraits

Interfaces à définir dès l’étape 0 :

```text
ClockService
SaveService
AudioService
HapticService
RewardedAdsService
ConsentService
NotificationService
LifecycleService
AnalyticsService
ContentRepository
RemoteConfigService
```

Le domaine reçoit ces services par injection ou par ports dédiés. Les implémentations Android restent dans `androidApp`.

## 4. Modèle de données minimal

### Définitions statiques

```text
ResourceDefinition
RefinedMaterialDefinition
RecipeDefinition
TechnologyDefinition
DepositDefinition
SectorDefinition
PlanetDefinition
PlanetRuleDefinition
BuildingDefinition
BuildingUpgradeDefinition
RobotDefinition
RobotTraitDefinition
ModuleDefinition
ModuleSetDefinition
SpecializationDefinition
MissionDefinition
ContractDefinition
EventDefinition
CodexEntryDefinition
TutorialStepDefinition
AdOfferDefinition
EconomyBalanceConfig
AccessibilityConfig
```

### États dynamiques

```text
PlayerProfile
PlanetState
SectorState
DepositState
BuildingState
RobotState
RobotMasteryState
InventoryState
ProductionJob
ProductionSnapshot
MissionProgress
ContractState
SpecializationState
ModuleInstance
CodexState
RareDiscoveryState
NarrativeArchiveState
MeteorShowerEventState
PrestigeState
AdRewardLedger
NotificationPreferences
GameSettings
SaveMetadata
```

### Versionnement

Chaque fichier de contenu contient :

```json
{
  "schemaVersion": 1,
  "contentVersion": "1.0.0",
  "items": []
}
```

Chaque sauvegarde contient :

- `saveSchemaVersion` ;
- `contentVersion` ;
- `createdAt` ;
- `updatedAt` ;
- `checksum` ;
- `activePlanetId` ;
- `transactionSequence`.

## 5. Conventions des identifiants

- minuscules ASCII ;
- séparateur `_` ;
- identifiants stables après publication ;
- jamais de nom affiché utilisé comme clé ;
- préfixes : `raw_`, `refined_`, `component_`, `tech_`, `robot_`, `module_`, `mission_`, `planet_`, `sector_`.

## 6. Temps et simulation

- UTC epoch milliseconds pour les horodatages persistés.
- Temps monotone pour les animations et durées de session.
- Horloge injectée dans le domaine pour les tests.
- Pas de minuterie d’interface comme source de vérité.
- Les calculs hors ligne utilisent le même moteur que la simulation accélérée.

## 7. Rendu et performance

### Budgets de scène 1.0

- 100 gisements chargés au maximum, avec activation visuelle selon distance.
- 50 robots ou drones visibles simultanément en qualité élevée.
- 25 en qualité moyenne, 12 en qualité faible.
- particules regroupées et réutilisées par pool ;
- modèles répétés instanciés ;
- atlas de textures ;
- ombres dynamiques limitées aux objets principaux ;
- LOD ou simplification à distance ;
- aucun chargement de fichier sur le thread de rendu.

### Niveaux de qualité

- Faible : 30 FPS cible, ombres minimales, particules réduites, densité météorites simplifiée.
- Moyen : 60 FPS cible sur appareil moyen.
- Élevé : effets complets avec budgets plafonnés.

## 8. Écrans et overlays

Écrans principaux :

```text
BootScreen
LoadingScreen
MainMenuScreen
PlanetScreen
PlanetTransferScreen
SettingsScreen
RecoveryScreen
FatalErrorScreen
```

Overlays ou panneaux contextuels :

```text
DepositPanel
ProductionPanel
RobotPanel
InventoryPanel
TechnologyPanel
MissionPanel
ContractPanel
SectorMapPanel
SpecializationPanel
ModulePanel
CodexPanel
ArchivePanel
AccessibilityPanel
AdOfferPanel
OfflineSummaryPanel
```

Les panneaux secondaires restent au-dessus de `PlanetScreen` lorsque cela préserve le contexte.

## 9. Transactions du domaine

Les opérations suivantes sont atomiques :

- achat ou vente ;
- lancement ou annulation d’une tâche ;
- collecte ;
- amélioration ;
- équipement ou démontage de module ;
- récompense publicitaire ;
- résultat d’événement rare ;
- prestige ;
- transfert de robot vétéran.

Une transaction produit :

- identifiant unique ;
- état avant ;
- mutations ;
- état après ;
- horodatage ;
- raison ;
- statut d’écriture.

## 10. Tests techniques

- tests JVM du domaine ;
- tests de propriétés pour ressources et coûts ;
- snapshots de sauvegarde ;
- migrations depuis chaque version conservée ;
- tests d’intégration Android du cycle de vie ;
- tests visuels des formats paysage ;
- profils mémoire et allocations ;
- simulation accélérée de 60 jours ;
- tests de génération de 10 000 mondes de frontière sans combinaison impossible.

## 11. Localisation

Même si la première sortie est en français :

- aucun texte affiché dans la logique ;
- clés de traduction stables ;
- pluriels gérés ;
- formats de nombres et durées centralisés ;
- mise en page testée avec textes 30 % plus longs.
