# Miner Space — Inventaire complet des assets à produire

Ce document est la liste de production de référence pour les sons, modèles 3D, ressources, effets, éléments d’interface et visuels marketing de Miner Space 1.0.

L’objectif n’est pas de créer un fichier différent pour chaque occurrence visible. Les secteurs, gisements, robots et bâtiments doivent être assemblés avec des kits modulaires réutilisables. Les noms proposés ci-dessous sont stables et peuvent être utilisés directement dans les dossiers du projet.

## 1. Formats de production recommandés

| Type | Fichier source à conserver | Format idéal dans le jeu | Contraintes principales |
|---|---|---|---|
| Modèle 3D | `.blend` | `.glb` avec animations intégrées | origine au sol, échelle en mètres, axe Y vertical, matériaux limités |
| Texture 3D | `.png` ou `.tga` non compressé | `.ktx2`/Basis si le chargeur est ajouté, sinon `.webp` ou `.png` | 512 à 2048 px, puissances de deux, atlas privilégiés |
| Icône et UI | `.svg` ou `.afdesign`/`.psd` | `.webp` lossless ou `.png` transparent | source vectorielle, lisible à 32 px |
| VFX 2D | `.png` haute qualité | atlas `.webp`/`.png` transparent | atlas de 4 à 16 images, peu de surdessin |
| Son court | `.wav`, PCM 48 kHz, 24 bits | `.ogg`, Vorbis, 48 kHz | mono sauf effet spatial large, pic maximal autour de -1 dBFS |
| Ambiance et musique | `.wav`, PCM 48 kHz, 24 bits | `.ogg`, Vorbis, stéréo | boucle sans clic, point de boucle documenté |
| Voix NOVA | `.wav`, mono, 48 kHz, 24 bits | `.ogg`, mono | voix sèche, sans musique, marge avant/après de 100 ms |
| Logo marketing | `.svg` | `.png`/`.webp` selon la plateforme | fond transparent et version monochrome |

### Budgets 3D indicatifs pour mobile

- petit accessoire : 200 à 1 500 triangles ;
- gisement ou prop moyen : 1 000 à 4 000 triangles ;
- robot : 3 000 à 8 000 triangles selon le niveau ;
- bâtiment : 5 000 à 15 000 triangles ;
- grand chantier ou décor principal : 15 000 à 30 000 triangles ;
- maximum conseillé : deux matériaux par petit objet, quatre pour un bâtiment majeur ;
- prévoir une version simplifiée ou un LOD pour les bâtiments, robots et gros décors.

## 2. Arborescence recommandée

```text
assets/
  audio/
    ambience/
    music/
    sfx/ui/
    sfx/industry/
    sfx/robots/
    sfx/exploration/
    voice/nova/
  models/
    shared/
    ferrum/
    cryos/
    frontier/
    robots/
    buildings/
  textures/
    shared/
    ferrum/
    cryos/
    frontier/
  ui/
    icons/
    panels/
    badges/
  vfx/
  marketing/
```

## 3. Priorités de production

- **P0 — prototype jouable** : HUD, neuf ressources brutes, trois robots EX/RF/AS, bâtiments de base, gisements, sons UI et production.
- **P1 — Ferrum Delta complète** : quinze bâtiments, quatre familles de robots, ressources raffinées, composants, technologies, modules, secteurs et événements.
- **P2 — Cryos IX** : environnement glacé, réseau thermique, ressources et boucle propre à Cryos.
- **P3 — Frontière et marketing** : trois familles procédurales, modificateurs, éléments Play Store et polish final.

# 4. Assets audio

Tous les sons courts doivent être livrés en `.wav` master et `.ogg` runtime. Prévoir deux ou trois variations pour les sons joués fréquemment afin d’éviter la répétition.

## 4.1 Interface et feedback général

| Priorité | Nom runtime | Durée | Description |
|---|---|---:|---|
| P0 | `sfx_ui_tap_01.ogg` à `03` | 0,05–0,12 s | clic holographique discret, propre et non agressif |
| P0 | `sfx_ui_confirm_01.ogg` à `02` | 0,15–0,30 s | validation positive courte, tonalité cyan/verte |
| P0 | `sfx_ui_error_01.ogg` à `02` | 0,20–0,40 s | erreur technique nette sans alarme stridente |
| P0 | `sfx_ui_panel_open_01.ogg` | 0,20–0,35 s | panneau holographique qui se matérialise |
| P0 | `sfx_ui_panel_close_01.ogg` | 0,15–0,30 s | fermeture légère, inverse du son d’ouverture |
| P1 | `sfx_ui_counter_gain_01.ogg` à `03` | 0,08–0,18 s | petit tintement de valeur ou ressource gagnée |
| P1 | `sfx_ui_purchase_01.ogg` | 0,35–0,60 s | paiement en SpaceDollars, métallique et satisfaisant |
| P1 | `sfx_ui_unlock_01.ogg` | 0,60–1,00 s | déverrouillage important, montée énergétique courte |
| P1 | `sfx_ui_mission_complete_01.ogg` | 1,00–1,80 s | signature de mission terminée, plus ample qu’une validation |
| P1 | `sfx_ui_achievement_01.ogg` | 1,50–2,50 s | exploit permanent, son premium mais sobre |
| P1 | `sfx_ui_codex_discovery_01.ogg` | 0,80–1,30 s | nouvelle entrée de Codex, scanner et scintillement |
| P1 | `sfx_ui_rare_discovery_01.ogg` | 1,20–2,00 s | découverte rare ou exceptionnelle, cristal et énergie |

## 4.2 Industrie, extraction et fabrication

| Priorité | Nom runtime | Durée | Description |
|---|---|---:|---|
| P0 | `sfx_mining_drill_start_01.ogg` | 0,40–0,80 s | démarrage d’un foret électrique lourd |
| P0 | `sfx_mining_drill_loop_01.ogg` | boucle 2–4 s | forage mécanique régulier, mono, boucle propre |
| P0 | `sfx_mining_drill_stop_01.ogg` | 0,35–0,70 s | ralentissement et arrêt du foret |
| P0 | `sfx_mining_impact_01.ogg` à `04` | 0,15–0,35 s | impacts minéraux variés, mélange roche/métal |
| P0 | `sfx_mining_collect_01.ogg` à `03` | 0,20–0,45 s | minerai aspiré ou transféré dans le stockage |
| P0 | `sfx_refinery_start_01.ogg` | 0,50–0,90 s | vannes, chambre thermique et alimentation |
| P0 | `sfx_refinery_loop_01.ogg` | boucle 3–6 s | ronronnement de transformation avec conduits actifs |
| P0 | `sfx_refinery_complete_01.ogg` | 0,80–1,30 s | matériau propre expulsé, vapeur et confirmation |
| P0 | `sfx_assembly_start_01.ogg` | 0,40–0,80 s | bras mécaniques qui se mettent en position |
| P0 | `sfx_assembly_loop_01.ogg` | boucle 2–4 s | servomoteurs, soudure légère et impulsions |
| P0 | `sfx_assembly_complete_01.ogg` | 0,80–1,40 s | verrouillage final d’un composant |
| P1 | `sfx_storage_transfer_01.ogg` à `02` | 0,25–0,50 s | déplacement de ressources par convoyeur ou drone |
| P1 | `sfx_technology_install_01.ogg` | 1,20–2,00 s | technologie installée, plusieurs verrous et montée d’énergie |
| P1 | `sfx_module_equip_01.ogg` | 0,50–0,90 s | module fixé sur un robot ou un système |
| P1 | `sfx_module_unequip_01.ogg` | 0,35–0,70 s | déconnexion mécanique courte |
| P1 | `sfx_building_upgrade_01.ogg` | 1,20–2,20 s | transformation de bâtiment et activation de nouvelles pièces |
| P1 | `sfx_production_queue_ready_01.ogg` | 0,50–0,90 s | tâche terminée et prête à collecter |
| P1 | `sfx_contract_complete_01.ogg` | 0,80–1,30 s | livraison commerciale réussie |

## 4.3 Robots

| Priorité | Nom runtime | Durée | Description |
|---|---|---:|---|
| P0 | `sfx_robot_ex_move_loop_01.ogg` | boucle 2–3 s | châssis lourd, chenilles ou pas mécaniques courts |
| P0 | `sfx_robot_ex_drill_01.ogg` | 0,50–1,00 s | foret frontal avec couple élevé |
| P0 | `sfx_robot_rf_process_01.ogg` | 0,60–1,20 s | chambre interne et circulation de matière |
| P0 | `sfx_robot_as_servo_01.ogg` à `03` | 0,15–0,40 s | mouvements de bras articulés |
| P1 | `sfx_robot_lg_hover_loop_01.ogg` | boucle 2–4 s | propulsion légère ou roues rapides de transport |
| P1 | `sfx_robot_lg_load_01.ogg` | 0,35–0,70 s | chargement d’un conteneur |
| P1 | `sfx_robot_lg_unload_01.ogg` | 0,35–0,70 s | déchargement dans le stockage |
| P1 | `sfx_robot_level_up_01.ogg` | 1,00–1,60 s | amélioration du châssis et activation d’extensions |
| P1 | `sfx_robot_mastery_up_01.ogg` | 1,20–2,00 s | passage de maîtrise, identité plus premium |
| P1 | `sfx_robot_veteran_01.ogg` | 1,80–3,00 s | promotion vétéran, insigne et noyau renforcé |
| P1 | `sfx_robot_priority_change_01.ogg` | 0,25–0,45 s | changement d’ordre ou de priorité |
| P1 | `sfx_robot_found_01.ogg` | 1,00–1,80 s | découverte d’un robot abandonné |

## 4.4 Exploration, secteurs et météorites

| Priorité | Nom runtime | Durée | Description |
|---|---|---:|---|
| P0 | `sfx_scanner_ping_01.ogg` à `03` | 0,40–0,90 s | impulsion sonar futuriste avec retour lointain |
| P0 | `sfx_sector_scan_complete_01.ogg` | 0,80–1,40 s | fin d’analyse d’un secteur |
| P0 | `sfx_sector_open_01.ogg` | 1,20–2,20 s | brouillard levé, énergie et terrain révélé |
| P0 | `sfx_meteor_spawn_01.ogg` à `03` | 0,25–0,60 s | météorite entrant rapidement dans la zone |
| P0 | `sfx_meteor_collect_common_01.ogg` à `03` | 0,15–0,35 s | fragment standard récupéré |
| P0 | `sfx_meteor_collect_rare_01.ogg` | 0,70–1,20 s | cœur météorique récupéré, plus brillant et grave |
| P0 | `sfx_meteor_miss_01.ogg` | 0,25–0,50 s | fragment perdu sans son punitif |
| P1 | `sfx_meteor_event_start_01.ogg` | 1,00–1,80 s | début de pluie, alerte contrôlée et montée d’énergie |
| P1 | `sfx_meteor_event_end_01.ogg` | 1,00–1,80 s | fin d’événement et résumé |
| P1 | `sfx_anomaly_crystal_01.ogg` | 1,20–2,20 s | résonance cristalline étrange |
| P1 | `sfx_anomaly_archive_01.ogg` | 1,20–2,20 s | archive ancienne, données fragmentées |
| P1 | `sfx_orbital_merchant_arrive_01.ogg` | 1,20–2,00 s | arrivée d’un marchand orbital |
| P1 | `sfx_temporary_deposit_appear_01.ogg` | 0,80–1,40 s | gisement exceptionnel qui émerge |

## 4.5 Prestige, Cryos IX et frontière

| Priorité | Nom runtime | Durée | Description |
|---|---|---:|---|
| P1 | `sfx_launch_countdown_01.ogg` | 3–5 s | compte à rebours technique sans voix obligatoire |
| P1 | `sfx_launch_ignition_01.ogg` | 2–4 s | allumage de moteurs et montée en puissance |
| P1 | `sfx_launch_liftoff_01.ogg` | 3–6 s | décollage principal, grave et spectaculaire |
| P1 | `sfx_prestige_core_gain_01.ogg` | 1,50–2,50 s | attribution de Noyaux Stellaires |
| P2 | `sfx_cryos_heat_node_build_01.ogg` | 0,80–1,40 s | nœud thermique installé et réseau connecté |
| P2 | `sfx_cryos_heat_pulse_01.ogg` | 0,50–1,00 s | impulsion orange circulant dans le réseau |
| P2 | `sfx_cryos_freeze_warning_01.ogg` | 0,70–1,20 s | froid critique, verre gelé et alerte douce |
| P2 | `sfx_cryos_ice_crack_01.ogg` à `03` | 0,30–0,80 s | craquements de glace variés |
| P2 | `sfx_cryos_aurora_resonance_01.ogg` | 1,00–2,00 s | cristal auroral et harmonique froide |
| P3 | `sfx_frontier_world_generate_01.ogg` | 1,20–2,20 s | route stellaire créée et nœuds assemblés |
| P3 | `sfx_frontier_world_select_01.ogg` | 0,50–0,90 s | monde sélectionné sur la carte stellaire |
| P3 | `sfx_frontier_world_complete_01.ogg` | 1,80–3,00 s | stabilisation d’un monde et récompense permanente |
| P3 | `sfx_frontier_temporal_echo_01.ogg` | 1,20–2,50 s | écho temporel inversé et spatial |

## 4.6 Ambiances et musiques

| Priorité | Nom runtime | Durée | Description |
|---|---|---:|---|
| P0 | `amb_ferrum_base_loop.ogg` | boucle 30–90 s | vent sec, machines lointaines, poussière et activité industrielle |
| P1 | `amb_ferrum_deep_loop.ogg` | boucle 30–90 s | profondeurs minérales, grondement sourd et anomalies |
| P2 | `amb_cryos_surface_loop.ogg` | boucle 30–90 s | vent glacé, résonances de glace et réseau thermique lointain |
| P2 | `amb_cryos_abyss_loop.ogg` | boucle 30–90 s | abîme froid, structures sous tension et craquements |
| P3 | `amb_frontier_volcanic_loop.ogg` | boucle 30–90 s | lave, cendres et machines thermiques |
| P3 | `amb_frontier_crystalline_loop.ogg` | boucle 30–90 s | cristaux, vent léger et harmoniques prismatiques |
| P3 | `amb_frontier_derelict_loop.ogg` | boucle 30–90 s | coque abandonnée, métal qui travaille et signaux faibles |
| P1 | `music_ferrum_management_loop.ogg` | boucle 90–180 s | thème industriel calme, électronique discrète |
| P1 | `music_meteor_event_loop.ogg` | boucle 45–90 s | rythme plus actif sans masquer les sons de collecte |
| P2 | `music_cryos_management_loop.ogg` | boucle 90–180 s | nappes froides, pulsation thermique et cristaux |
| P3 | `music_frontier_map_loop.ogg` | boucle 90–180 s | exploration stellaire, mystérieuse et ouverte |
| P1 | `music_launch_sequence.ogg` | 20–45 s | montée cinématique du départ planétaire |

## 4.7 Voix NOVA

Prévoir douze transmissions courtes. Chaque fichier doit durer environ 3 à 10 secondes, rester compréhensible sans musique et posséder une version texte identique dans le jeu.

- `voice_nova_transmission_01.ogg` — premier contact, voix calme et artificielle.
- `voice_nova_transmission_02.ogg` — confirmation que la base est observée.
- `voice_nova_transmission_03.ogg` — indication sur les premiers secteurs.
- `voice_nova_transmission_04.ogg` — signal provenant des ruines.
- `voice_nova_transmission_05.ogg` — avertissement sur une anomalie cristalline.
- `voice_nova_transmission_06.ogg` — fragment d’archive incomplet.
- `voice_nova_transmission_07.ogg` — mention de la navigation interplanétaire.
- `voice_nova_transmission_08.ogg` — identification du robot vétéran.
- `voice_nova_transmission_09.ogg` — activation de la Voûte NOVA.
- `voice_nova_transmission_10.ogg` — préparation du départ.
- `voice_nova_transmission_11.ogg` — souvenir de Cryos IX.
- `voice_nova_transmission_12.ogg` — ouverture de la frontière.

# 5. Kit 3D partagé

Ces objets sont réutilisés sur plusieurs planètes avec changement de matériaux et accessoires.

| Priorité | Nom | Format | Description |
|---|---|---|---|
| P0 | `prop_platform_hex_small.glb` | GLB | petite plateforme technique pour robot, caisse ou gisement |
| P0 | `prop_platform_hex_large.glb` | GLB | socle de bâtiment, connecteurs visibles |
| P0 | `prop_pipe_straight.glb` | GLB | segment de conduite modulaire |
| P0 | `prop_pipe_corner.glb` | GLB | coude de conduite à 90 degrés |
| P0 | `prop_pipe_junction.glb` | GLB | jonction à trois voies |
| P0 | `prop_cable_bundle.glb` | GLB | faisceau de câbles épais, silhouette lisible |
| P0 | `prop_container_small.glb` | GLB | caisse industrielle standard |
| P0 | `prop_container_large.glb` | GLB | conteneur de ressources transportable |
| P0 | `prop_beacon_small.glb` | GLB | balise de sélection ou de mission |
| P0 | `prop_work_light.glb` | GLB | projecteur industriel orientable |
| P1 | `prop_antenna_small.glb` | GLB | petite antenne de communication |
| P1 | `prop_antenna_large.glb` | GLB | antenne radar ou orbitale |
| P1 | `prop_hologram_projector.glb` | GLB | projecteur pour cartes et objectifs |
| P1 | `prop_drone_cargo.glb` | GLB | drone simple de transport, animation d’hélice ou de poussée |
| P1 | `prop_drone_scanner.glb` | GLB | drone de reconnaissance avec capteur cyan |
| P1 | `prop_fence_segment.glb` | GLB | barrière industrielle modulaire |
| P1 | `prop_stair_ramp.glb` | GLB | rampe ou escalier de plateforme |
| P1 | `prop_repair_arm.glb` | GLB | bras technique pour bâtiments et chantier |

# 6. Ferrum Delta — environnement et secteurs

## 6.1 Kit de terrain Ferrum

| Priorité | Nom | Description |
|---|---|---|
| P0 | `env_ferrum_ground_tile_a.glb` | dalle minérale rouge sombre, relativement plate |
| P0 | `env_ferrum_ground_tile_b.glb` | variante fissurée et plus rocheuse |
| P0 | `env_ferrum_cliff_straight.glb` | falaise anguleuse modulaire |
| P0 | `env_ferrum_cliff_corner.glb` | angle intérieur/extérieur de falaise |
| P0 | `env_ferrum_rock_a.glb` à `f.glb` | six roches de tailles et silhouettes différentes |
| P0 | `env_ferrum_plate_mineral_a.glb` à `c.glb` | plaques métalliques naturelles affleurantes |
| P1 | `env_ferrum_canyon_bridge.glb` | arche ou pont naturel pour le passage logistique |
| P1 | `env_ferrum_ruin_wall_a.glb` à `c.glb` | fragments de ruines anciennes réutilisables |
| P1 | `env_ferrum_ruin_console.glb` | console ancienne pour archives et anomalies |
| P1 | `env_ferrum_dust_vent.glb` | petite cheminée naturelle émettant de la poussière |
| P1 | `env_ferrum_crater_small.glb` et `large.glb` | cratères pour météorites et zones profondes |

Textures recommandées : `tex_ferrum_terrain_atlas_01`, `tex_ferrum_rocks_atlas_01`, `tex_ferrum_ruins_atlas_01`, avec albedo, normal légère, roughness et emission si nécessaire.

## 6.2 Repères des quatorze secteurs

Chaque secteur utilise le kit précédent et un repère principal distinct.

| ID de secteur | Nom du modèle | Description |
|---|---|---|
| `sector_core_delta` | `landmark_ferrum_core_delta.glb` | zone initiale, plateau stable et emplacement de la base |
| `sector_copper_ridge` | `landmark_ferrum_copper_ridge.glb` | crête conductrice avec grandes veines orangées |
| `sector_crystal_flats` | `landmark_ferrum_crystal_flats.glb` | plaine de cristaux bas et lisibles |
| `sector_logistics_pass` | `landmark_ferrum_logistics_pass.glb` | gorge servant de raccourci aux convois |
| `sector_nickel_basin` | `landmark_ferrum_nickel_basin.glb` | bassin circulaire sombre et robuste |
| `sector_silica_dunes` | `landmark_ferrum_silica_dunes.glb` | dunes minérales claires et verre naturel |
| `sector_cobalt_trench` | `landmark_ferrum_cobalt_trench.glb` | tranchée bleutée avec structures conductrices |
| `sector_titanium_shelf` | `landmark_ferrum_titanium_shelf.glb` | grande terrasse métallique claire |
| `sector_iridium_fault` | `landmark_ferrum_iridium_fault.glb` | faille profonde à reflets froids |
| `sector_xenon_depths` | `landmark_ferrum_xenon_depths.glb` | cavité sombre avec cristaux cyan-violet |
| `sector_archive_ruins` | `landmark_ferrum_archive_ruins.glb` | ruines et terminal d’archive central |
| `sector_quantum_abyss` | `landmark_ferrum_quantum_abyss.glb` | gouffre avec fragments en suspension |
| `sector_nova_vault` | `landmark_ferrum_nova_vault.glb` | porte monumentale NOVA et halo blanc-violet |
| `sector_launch_shipyard` | `landmark_ferrum_launch_shipyard.glb` | plateau final accueillant le chantier spatial |

# 7. Gisements et ressources Ferrum

Les 34 gisements ne nécessitent pas 34 modèles uniques. Produire trois socles de gisement et neuf inserts de minerai. Les combinaisons, tailles et rotations créent la variété.

## 7.1 Socles de gisement

- `deposit_base_surface_small.glb` — petit affleurement accessible.
- `deposit_base_surface_large.glb` — veine principale ou gisement riche.
- `deposit_base_deep.glb` — gisement profond avec plateforme de forage.
- `deposit_depleted_state.glb` — état épuisé avec cavité et débris.

## 7.2 Inserts bruts et icônes

| ID | Modèle | Icône | Description |
|---|---|---|---|
| `raw_iron` | `resource_raw_iron.glb` | `icon_resource_raw_iron.webp` | roche sombre, veines rouges et métalliques |
| `raw_copper` | `resource_raw_copper.glb` | `icon_resource_raw_copper.webp` | roche brun-orange conductrice |
| `raw_crystal` | `resource_raw_crystal.glb` | `icon_resource_raw_crystal.webp` | cristaux transparents bleu clair |
| `raw_nickel` | `resource_raw_nickel.glb` | `icon_resource_raw_nickel.webp` | nodules gris froids et robustes |
| `raw_titanium` | `resource_raw_titanium.glb` | `icon_resource_raw_titanium.webp` | plaques naturelles argentées et propres |
| `raw_silica` | `resource_raw_silica.glb` | `icon_resource_raw_silica.webp` | roche sableuse claire et légèrement vitreuse |
| `raw_cobalt` | `resource_raw_cobalt.glb` | `icon_resource_raw_cobalt.webp` | minerai bleu métallique intense |
| `raw_iridium` | `resource_raw_iridium.glb` | `icon_resource_raw_iridium.webp` | fragments sombres avec reflets blancs froids |
| `raw_xenon_ore` | `resource_raw_xenon_ore.glb` | `icon_resource_raw_xenon_ore.webp` | roche noire contenant une lueur cyan-violet |

## 7.3 Matériaux raffinés

Tous sont des objets isolés et propres, destinés aux cartes de production et au Codex.

| ID | Fichier conseillé | Description |
|---|---|---|
| `refined_iron_ingot` | `icon_resource_refined_iron_ingot.webp` | lingot compact en ferrite industrielle |
| `refined_copper_plate` | `icon_resource_refined_copper_plate.webp` | plaque conductrice brun-orange |
| `refined_crystal_lens` | `icon_resource_refined_crystal_lens.webp` | lentille facettée cyan transparente |
| `refined_nickel_alloy` | `icon_resource_refined_nickel_alloy.webp` | bloc d’alliage gris bleuté |
| `refined_titanium_beam` | `icon_resource_refined_titanium_beam.webp` | poutre structurelle claire et légère |
| `refined_silica_glass` | `icon_resource_refined_silica_glass.webp` | plaque de verre technique translucide |
| `refined_cobalt_coil` | `icon_resource_refined_cobalt_coil.webp` | bobine énergétique bleue |
| `refined_iridium_mesh` | `icon_resource_refined_iridium_mesh.webp` | grille métallique rare à reflets froids |
| `refined_xenon_matrix` | `icon_resource_refined_xenon_matrix.webp` | matrice cristalline cyan-violet lumineuse |

## 7.4 Composants standards

Format conseillé : icône `.webp` 512 × 512 avec source 3D ou vectorielle. Un petit modèle `.glb` n’est nécessaire que si le composant apparaît physiquement dans l’atelier ou le Codex 3D.

| ID | Nom de fichier | Description visuelle |
|---|---|---|
| `component_power_cell` | `icon_component_power_cell.webp` | cellule d’énergie cylindrique orange-cyan |
| `component_sensor_array` | `icon_component_sensor_array.webp` | ensemble de capteurs et lentilles |
| `component_servo_core` | `icon_component_servo_core.webp` | moteur compact avec anneau mobile |
| `component_heat_sink` | `icon_component_heat_sink.webp` | dissipateur à ailettes graphite |
| `component_logic_board` | `icon_component_logic_board.webp` | carte logique lumineuse protégée |
| `component_magnetic_coupler` | `icon_component_magnetic_coupler.webp` | deux anneaux magnétiques imbriqués |
| `component_pressure_valve` | `icon_component_pressure_valve.webp` | vanne technique renforcée |
| `component_optic_bus` | `icon_component_optic_bus.webp` | faisceau de fibres et connecteurs |
| `component_titanium_frame` | `icon_component_titanium_frame.webp` | cadre structurel triangulé |
| `component_storage_matrix` | `icon_component_storage_matrix.webp` | cube de stockage modulaire |
| `component_drone_chassis` | `icon_component_drone_chassis.webp` | châssis léger à quatre supports |
| `component_reactor_shell` | `icon_component_reactor_shell.webp` | coque circulaire haute résistance |
| `component_navigation_core` | `icon_component_navigation_core.webp` | noyau gyroscopique bleu-blanc |
| `component_quantum_relay` | `icon_component_quantum_relay.webp` | relais à anneaux flottants violets |
| `component_archive_decoder` | `icon_component_archive_decoder.webp` | module de données ancien et moderne combiné |
| `component_plasma_injector` | `icon_component_plasma_injector.webp` | injecteur fin avec chambre orange |
| `component_gravity_anchor` | `icon_component_gravity_anchor.webp` | pièce lourde en forme d’ancre technologique |
| `component_fusion_lattice` | `icon_component_fusion_lattice.webp` | réseau géométrique autour d’un cœur lumineux |
| `component_launch_clamp` | `icon_component_launch_clamp.webp` | verrou mécanique de grande taille |
| `component_orbital_beacon` | `icon_component_orbital_beacon.webp` | balise compacte à antenne verticale |
| `component_stellar_compressor` | `icon_component_stellar_compressor.webp` | compresseur annulaire très avancé |
| `component_nova_resonator` | `icon_component_nova_resonator.webp` | cristal NOVA enfermé dans trois anneaux |
| `component_transfer_core` | `icon_component_transfer_core.webp` | noyau de transfert blanc-violet |
| `component_interplanetary_bus` | `icon_component_interplanetary_bus.webp` | bus de données et puissance multi-connecteurs |

## 7.5 Technologies

Chaque technologie nécessite une icône 512 × 512 et, si elle transforme visuellement la base, un petit kit d’accessoires 3D.

- `icon_tech_extraction_protocol.webp` — foret amélioré et flux de minerai.
- `icon_tech_quantum_sorting.webp` — tri de particules dans plusieurs anneaux.
- `icon_tech_logistics_mesh.webp` — réseau de routes et nœuds reliés.
- `icon_tech_deep_scanner.webp` — faisceau pénétrant plusieurs couches de terrain.
- `icon_tech_thermal_recovery.webp` — chaleur recyclée en boucle.
- `icon_tech_robot_parallelism.webp` — plusieurs robots synchronisés.
- `icon_tech_storage_lattice.webp` — cubes de stockage interconnectés.
- `icon_tech_crystal_analysis.webp` — cristal observé par trois capteurs.
- `icon_tech_iridium_forging.webp` — métal froid dans une forge avancée.
- `icon_tech_xenon_stabilization.webp` — cristal xénon maintenu par un champ.
- `icon_tech_archive_decoding.webp` — archive ancienne reconstruite numériquement.
- `icon_tech_orbital_assembly.webp` — pièces assemblées autour d’une orbite.
- `icon_tech_nova_navigation.webp` — trajectoire stellaire autour du symbole NOVA.
- `icon_tech_interplanetary_launch.webp` — vaisseau franchissant une porte lumineuse.

## 7.6 Modules

Tous les modules utilisent une coque commune par rareté et un insert central distinct. Format conseillé : icône 512 × 512 et petit modèle 3D optionnel.

| Nom | Fichier | Description |
|---|---|---|
| Forge Drill | `icon_module_forge_drill.webp` | tête de forage renforcée |
| Forge Thermal | `icon_module_forge_thermal.webp` | régulation de chaleur orange |
| Forge Chassis | `icon_module_forge_chassis.webp` | blindage lourd graphite |
| Survey Optics | `icon_module_survey_optics.webp` | lentilles de prospection cyan |
| Survey Quantum | `icon_module_survey_quantum.webp` | capteur à anneaux violets |
| Survey Archive | `icon_module_survey_archive.webp` | lecteur de données anciennes |
| Storage Capsule | `icon_module_storage_capsule.webp` | capsule de stockage compacte |
| Compact Battery | `icon_module_compact_battery.webp` | batterie courte et dense |
| Servo Accelerator | `icon_module_servo_accelerator.webp` | servo rapide à rotor visible |
| Magnetic Sifter | `icon_module_magnetic_sifter.webp` | tamis magnétique annulaire |
| Thermal Regulator | `icon_module_thermal_regulator.webp` | radiateur et capteur thermique |
| Optic Predictor | `icon_module_optic_predictor.webp` | prisme et trajectoires anticipées |
| Cargo Weave | `icon_module_cargo_weave.webp` | treillis de transport vert-cyan |
| Crystal Resonator | `icon_module_crystal_resonator.webp` | cristal vibrant dans un support |
| Titanium Bracing | `icon_module_titanium_bracing.webp` | renfort triangulé clair |
| Cobalt Driver | `icon_module_cobalt_driver.webp` | moteur bleu à bobine |
| Iridium Filter | `icon_module_iridium_filter.webp` | filtre métallique fin et rare |
| Xenon Compass | `icon_module_xenon_compass.webp` | boussole énergétique cyan-violet |
| Quantum Harvester | `icon_module_quantum_harvester.webp` | collecteur à bras concentriques |
| Archive Coprocessor | `icon_module_archive_coprocessor.webp` | processeur avec glyphes NOVA |
| Gravity Stabilizer | `icon_module_gravity_stabilizer.webp` | masse centrale et anneaux stables |
| Orbital Matrix | `icon_module_orbital_matrix.webp` | réseau orbital miniature |
| NOVA Heart | `icon_module_nova_heart.webp` | cœur blanc-violet exceptionnel |
| Stellar Crown | `icon_module_stellar_crown.webp` | couronne d’énergie légendaire |

## 7.7 Ressources rares et objet final

| ID | Fichier | Description |
|---|---|---|
| `rare_prismatic_ferrite` | `resource_rare_prismatic_ferrite.glb` et `icon_resource_rare_prismatic_ferrite.webp` | ferrite sombre présentant des reflets prismatiques |
| `rare_xenon_crystal` | `resource_rare_xenon_crystal.glb` et `icon_resource_rare_xenon_crystal.webp` | grand cristal cyan-violet stable |
| `rare_archive_fragment` | `resource_rare_archive_fragment.glb` et `icon_resource_rare_archive_fragment.webp` | fragment de mémoire ancienne avec glyphes |
| `rare_meteor_core` | `resource_rare_meteor_core.glb` et `icon_resource_rare_meteor_core.webp` | cœur incandescent orange et blanc |
| `rare_nova_core` | `resource_rare_nova_core.glb` et `icon_resource_rare_nova_core.webp` | noyau blanc-violet entouré d’anneaux |
| `final_interplanetary_departure_array` | `object_interplanetary_departure_array.glb` et `icon_final_interplanetary_departure_array.webp` | assemblage final réunissant navigation, transfert et lancement |

# 8. Bâtiments Ferrum Delta

Chaque bâtiment majeur doit posséder au minimum trois états visuels : `mk1`, `mk2` et `mk3`. Les niveaux supplémentaires peuvent être réalisés par accessoires optionnels plutôt que par un modèle entièrement différent.

| ID | Fichiers | Description et animation attendue |
|---|---|---|
| `building_base_delta` | `building_base_delta_mk1.glb` à `mk3` | centre de commandement, hologramme, antennes et noyau central |
| `building_refinery` | `building_refinery_mk1.glb` à `mk3` | cuves, conduits, matière visible et vapeur |
| `building_assembler` | `building_assembler_mk1.glb` à `mk3` | bras mécaniques, plateforme et soudure |
| `building_robot_bay` | `building_robot_bay_mk1.glb` à `mk3` | baies de maintenance et supports de robots |
| `building_storage_hub` | `building_storage_hub_mk1.glb` à `mk3` | silos modulaires et indicateur de remplissage |
| `building_scanner_array` | `building_scanner_array_mk1.glb` à `mk3` | antennes et impulsion radar verticale |
| `building_logistics_port` | `building_logistics_port_mk1.glb` à `mk3` | quais, convoyeurs et drones de transport |
| `building_research_lab` | `building_research_lab_mk1.glb` à `mk3` | laboratoire vitré, hologrammes et capteurs |
| `building_meteor_lab` | `building_meteor_lab_mk1.glb` à `mk3` | stockage de fragments, radar et chambre rare |
| `building_deep_foundry` | `building_deep_foundry_mk1.glb` à `mk3` | forge lourde pour titane et iridium |
| `building_quantum_foundry` | `building_quantum_foundry_mk1.glb` à `mk3` | anneaux flottants et énergie violette |
| `building_archive_vault` | `building_archive_vault_mk1.glb` à `mk3` | coffre de données et consoles anciennes |
| `building_orbital_beacon` | `building_orbital_beacon_mk1.glb` à `mk3` | haute antenne et faisceau vers le ciel |
| `building_launch_shipyard` | `building_launch_shipyard_mk1.glb` à `mk3` | chantier monumental avec bras et passerelles |
| `building_interplanetary_array` | `building_interplanetary_array_mk1.glb` à `mk3` | anneaux de transfert et cœur final NOVA |

### Vaisseau de départ

- `ship_interplanetary_freighter_frame.glb` — structure nue visible pendant la construction.
- `ship_interplanetary_freighter_half.glb` — coque partiellement assemblée.
- `ship_interplanetary_freighter_complete.glb` — vaisseau final de marchandises et exploration.
- `ship_interplanetary_freighter_landing_gear.glb` — train d’atterrissage séparé si animation.
- `ship_interplanetary_freighter_engine.glb` — moteur animé et réutilisable.

# 9. Robots

Chaque famille utilise trois châssis visuels correspondant aux niveaux 1–2, 3–4 et 5. Les spécialisations sont des kits d’accessoires. Les animations sont intégrées dans les GLB : `idle`, `move`, `work`, `success`, `shutdown`.

## 9.1 Extracteur EX — Aster

- `robot_ex_tier1.glb` — châssis compact, centre de gravité bas, foret simple.
- `robot_ex_tier2.glb` — blindage renforcé, double stabilisateur, foret amélioré.
- `robot_ex_tier3.glb` — trois extensions visibles, capteur rare et noyau plus intense.
- `robot_ex_spec_foreur.glb` — kit Foreur, tête large et refroidissement.
- `robot_ex_spec_prospecteur.glb` — kit Prospecteur, scanner et prélèvement précis.
- `robot_ex_veteran_aster.glb` — plaques usées élégantes, insigne et noyau distinctif.

## 9.2 Raffineur RF — Rhea

- `robot_rf_tier1.glb` — silhouette verticale et petite chambre de transformation.
- `robot_rf_tier2.glb` — conduits supplémentaires et chambre transparente.
- `robot_rf_tier3.glb` — double chambre, contrôle thermique avancé.
- `robot_rf_spec_metallurgiste.glb` — creuset renforcé et accent orange.
- `robot_rf_spec_thermicien.glb` — échangeurs, ailettes et accent cyan.
- `robot_rf_veteran_rhea.glb` — insigne, matériaux premium et conduits lumineux.

## 9.3 Assembleur AS — Kestrel

- `robot_as_tier1.glb` — deux bras et petite plateforme holographique.
- `robot_as_tier2.glb` — quatre bras, outils interchangeables.
- `robot_as_tier3.glb` — bras fins supplémentaires et projection complète.
- `robot_as_spec_microfabricant.glb` — optique précise et pinces fines.
- `robot_as_spec_architecte.glb` — bras lourds et plan holographique étendu.
- `robot_as_veteran_kestrel.glb` — insigne et mouvements plus assurés.

## 9.4 Logistique LG — Nox

- `robot_lg_tier1.glb` — petit transporteur rapide à deux modules.
- `robot_lg_tier2.glb` — plateforme élargie et propulsion améliorée.
- `robot_lg_tier3.glb` — trois conteneurs et capteur de réseau.
- `robot_lg_spec_convoyeur.glb` — capacité de transport accrue.
- `robot_lg_spec_regulateur.glb` — capteurs, tri et contrôle des flux.
- `robot_lg_veteran_nox.glb` — marquages de route et noyau vert-cyan.

## 9.5 Éléments communs aux robots

- `robot_attachment_module_standard.glb` — coque de module commune.
- `robot_attachment_module_improved.glb` — coque améliorée.
- `robot_attachment_module_advanced.glb` — coque avancée.
- `robot_attachment_module_exceptional.glb` — coque exceptionnelle avec halo.
- `robot_decal_trait_precise.webp`, `enduring`, `fast`, `stable`, `prospector` — symboles de traits.
- `robot_badge_mastery_novice.webp`, `experienced`, `expert`, `veteran` — badges de maîtrise.

# 10. Cryos IX

## 10.1 Kit de terrain et réseau thermique

- `env_cryos_ground_snow_a.glb` et `b.glb` — sol glacé avec variations.
- `env_cryos_cliff_ice_straight.glb` et `corner.glb` — falaises translucides.
- `env_cryos_crystal_a.glb` à `f.glb` — cristaux de tailles variées.
- `env_cryos_frozen_fissure.glb` — fissure froide.
- `env_cryos_thermal_fissure.glb` — fissure chauffée orange.
- `env_cryos_steam_vent.glb` — bouche thermique avec VFX de vapeur.
- `building_cryos_landing_base.glb` — base initiale isolée.
- `building_cryos_heat_generator.glb` — générateur d’énergie et de chaleur.
- `building_cryos_thermal_node.glb` — nœud du réseau, cinq instances réutilisées.
- `prop_cryos_thermal_conduit_straight.glb`, `corner.glb`, `junction.glb` — conduites du réseau.
- `building_cryos_frontier_gate.glb` — porte finale vers la carte stellaire.

## 10.2 Repères des six secteurs

- `landmark_cryos_landing.glb` — zone d’atterrissage et balises chaudes.
- `landmark_cryos_glass_fields.glb` — champs de verre glacé.
- `landmark_cryos_brine_rift.glb` — faille saline et vapeur.
- `landmark_cryos_aurora_shelf.glb` — plateau auroral et cristaux lumineux.
- `landmark_cryos_thermal_abyss.glb` — abîme profond avec réseau suspendu.
- `landmark_cryos_frontier_gate.glb` — structure de stabilisation interplanétaire.

## 10.3 Ressources Cryos

| ID | Fichier | Description |
|---|---|---|
| `raw_cryonite` | `resource_raw_cryonite.glb` et icône associée | minerai bleu froid à cœur dense |
| `raw_ice_silicate` | `resource_raw_ice_silicate.glb` | silicate translucide couvert de givre |
| `raw_thermal_brine` | `resource_raw_thermal_brine.glb` | capsule ou poche de saumure orange-bleu |
| `raw_aurora_crystal` | `resource_raw_aurora_crystal.glb` | cristal turquoise-violet irisé |
| `refined_cryonite_plate` | `icon_resource_refined_cryonite_plate.webp` | plaque cryogénique renforcée |
| `refined_thermal_glass` | `icon_resource_refined_thermal_glass.webp` | verre isolant bleu et orange |
| `refined_thermal_salt` | `icon_resource_refined_thermal_salt.webp` | cristaux salins énergétiques |
| `refined_aurora_lens` | `icon_resource_refined_aurora_lens.webp` | lentille irisée pour capteurs |
| `component_heat_cell` | `icon_component_heat_cell.webp` | cellule de chaleur orange protégée |
| `component_thermal_conduit` | `icon_component_thermal_conduit.webp` | conduite isolée miniature |
| `component_aurora_sensor` | `icon_component_aurora_sensor.webp` | capteur à lentille aurorale |
| `component_frontier_coupler` | `icon_component_frontier_coupler.webp` | coupleur de porte stellaire |

## 10.4 Technologies et modules Cryos

- `icon_tech_cryos_efficient_grid.webp` — réseau thermique optimisé.
- `icon_tech_cryos_thermal_recovery.webp` — chaleur recyclée.
- `icon_tech_cryos_cryo_drill.webp` — foret isolé contre le froid.
- `icon_tech_cryos_insulated_logistics.webp` — transport protégé.
- `icon_tech_cryos_frontier_stabilizer.webp` — stabilisation de la porte.

Modules :

- `icon_module_cryos_01.webp` — isolateur thermique de base.
- `icon_module_cryos_02.webp` — batterie chauffante.
- `icon_module_cryos_03.webp` — foret cryogénique.
- `icon_module_cryos_04.webp` — conduite à récupération de chaleur.
- `icon_module_cryos_05.webp` — capteur de gel.
- `icon_module_cryos_06.webp` — résonateur auroral.
- `icon_module_cryos_07.webp` — blindage de l’abîme.
- `icon_module_cryos_08.webp` — cœur de frontière thermique.

# 11. Frontière interplanétaire

Chaque famille utilise un kit de terrain, huit repères de secteur et une palette dédiée. Les repères peuvent réutiliser les bâtiments et props partagés.

## 11.1 Famille volcanique

- `frontier_volcanic_ground_kit.glb` — sol noir, cendres et fissures de lave.
- `frontier_volcanic_lava_kit.glb` — rivières, bassins et cascades modulaires.
- `landmark_volcanic_01_ash_plain.glb` — plaine de cendres.
- `landmark_volcanic_02_basalt_forge.glb` — forge basaltique.
- `landmark_volcanic_03_magma_bridge.glb` — pont magmatique.
- `landmark_volcanic_04_active_caldera.glb` — caldeira active.
- `landmark_volcanic_05_obsidian_crypt.glb` — crypte d’obsidienne.
- `landmark_volcanic_06_thermal_bastion.glb` — bastion thermique.
- `landmark_volcanic_07_mineral_fault.glb` — faille minérale.
- `landmark_volcanic_08_lava_relay.glb` — relais de lave.

## 11.2 Famille cristalline

- `frontier_crystalline_ground_kit.glb` — sol clair, facettes et prismes.
- `frontier_crystalline_crystal_kit.glb` — cristaux modulaires multicolores.
- `landmark_crystalline_01_prismatic_garden.glb` — jardin prismatique.
- `landmark_crystalline_02_glass_workshop.glb` — atelier de verre.
- `landmark_crystalline_03_luminous_nexus.glb` — nexus lumineux.
- `landmark_crystalline_04_aurora_storm.glb` — tempête aurorale.
- `landmark_crystalline_05_quartz_sanctuary.glb` — sanctuaire de quartz.
- `landmark_crystalline_06_ice_citadel.glb` — citadelle de glace.
- `landmark_crystalline_07_resonant_vein.glb` — veine résonante.
- `landmark_crystalline_08_spectral_relay.glb` — relais spectral.

## 11.3 Famille épave

- `frontier_derelict_hull_kit.glb` — plaques de coque, poutres et couloirs cassés.
- `frontier_derelict_cable_kit.glb` — câbles, conduites et lumières défaillantes.
- `landmark_derelict_01_broken_hold.glb` — soute éventrée.
- `landmark_derelict_02_ghost_refinery.glb` — raffinerie fantôme.
- `landmark_derelict_03_relay_ring.glb` — anneau de relais.
- `landmark_derelict_04_command_bridge.glb` — pont de commandement.
- `landmark_derelict_05_sealed_archive.glb` — archive scellée.
- `landmark_derelict_06_orbital_shipyard.glb` — chantier orbital abandonné.
- `landmark_derelict_07_mineral_cargo.glb` — cargo minéral.
- `landmark_derelict_08_automated_node.glb` — nœud automatisé.

## 11.4 Modificateurs de monde

Chaque modificateur nécessite au minimum une icône et, lorsque pertinent, un petit prop ou VFX de lecture immédiate.

- `icon_modifier_low_gravity.webp` + `vfx_low_gravity_dust` — poussière et objets flottant lentement.
- `icon_modifier_dense_atmosphere.webp` + `vfx_dense_fog` — brume plus dense et silhouettes atténuées.
- `icon_modifier_resource_scarcity.webp` — filon mince et symbole de rareté.
- `icon_modifier_rich_veins.webp` — filon large et éclat plus intense.
- `icon_modifier_unstable_orbit.webp` + `vfx_orbit_warning` — trajectoires irrégulières dans le ciel.
- `icon_modifier_electromagnetic_storm.webp` + `vfx_em_storm` — arcs électriques contrôlés.
- `icon_modifier_ancient_beacons.webp` + `prop_ancient_beacon.glb` — balise ancienne verticale.
- `icon_modifier_automated_ruins.webp` + `prop_automated_ruin.glb` — mécanisme ancien encore actif.
- `icon_modifier_thermal_fissures.webp` + `vfx_thermal_fissure` — fissures orange animées.
- `icon_modifier_crystal_resonance.webp` + `vfx_crystal_resonance` — onde traversant les cristaux.
- `icon_modifier_derelict_drones.webp` + `prop_derelict_drone.glb` — drone endommagé autonome.
- `icon_modifier_temporal_echo.webp` + `vfx_temporal_echo` — double image et particules inversées.

# 12. Effets visuels

Les VFX doivent avoir une version complète et une version réduite. Les effets ne doivent jamais être la seule manière de comprendre un résultat.

| Priorité | Nom | Format | Description |
|---|---|---|---|
| P0 | `vfx_mining_impact_atlas.webp` | atlas 8 images | poussière, éclats rocheux et étincelles |
| P0 | `vfx_mining_beam_atlas.webp` | atlas 6 images | faisceau ou cône de forage |
| P0 | `vfx_refinery_heat_atlas.webp` | atlas 8 images | chaleur interne et vapeur |
| P0 | `vfx_assembly_sparks_atlas.webp` | atlas 8 images | soudure et assemblage |
| P0 | `vfx_selection_ring.webp` | texture | anneau de sélection cyan, compatible daltonisme |
| P0 | `vfx_scanner_pulse.webp` | texture/mesh | onde circulaire de scanner |
| P0 | `vfx_sector_reveal_atlas.webp` | atlas 8 images | brouillard qui se dissipe et onde au sol |
| P0 | `vfx_meteor_trail_common.webp` | texture | traînée standard courte |
| P0 | `vfx_meteor_trail_rare.webp` | texture | traînée large avec motif distinct |
| P0 | `vfx_meteor_collect_atlas.webp` | atlas 6 images | implosion légère à la collecte |
| P1 | `vfx_resource_transfer_atlas.webp` | atlas 6 images | particules circulant dans un conduit |
| P1 | `vfx_technology_install_atlas.webp` | atlas 10 images | circuits qui s’allument progressivement |
| P1 | `vfx_rare_discovery_atlas.webp` | atlas 12 images | halo, facettes et particules |
| P1 | `vfx_nova_signal_atlas.webp` | atlas 12 images | glyphes blancs-violets et interférences |
| P1 | `vfx_launch_exhaust_atlas.webp` | atlas 12 images | flammes et fumée de moteur |
| P1 | `vfx_launch_dust_atlas.webp` | atlas 8 images | poussière au sol lors du décollage |
| P2 | `vfx_cryos_steam_atlas.webp` | atlas 8 images | vapeur thermique orange-blanche |
| P2 | `vfx_cryos_frost_atlas.webp` | atlas 8 images | givre progressif sur les bords |
| P2 | `vfx_aurora_ribbon.webp` | texture/mesh | ruban auroral lent et transparent |
| P3 | `vfx_star_route.webp` | texture/mesh | ligne de route interplanétaire animée |
| P3 | `vfx_world_stabilized_atlas.webp` | atlas 12 images | monde verrouillé dans une orbite stable |

# 13. Interface et icônes

## 13.1 Composants réutilisables

- `ui_panel_primary_9slice.webp` — panneau principal sombre semi-transparent.
- `ui_panel_secondary_9slice.webp` — panneau compact.
- `ui_button_primary_9slice.webp` — bouton d’action lumineux.
- `ui_button_secondary_9slice.webp` — bouton standard.
- `ui_button_disabled_9slice.webp` — état désactivé avec forme claire.
- `ui_card_robot_9slice.webp` — carte de robot.
- `ui_card_resource_9slice.webp` — carte de ressource.
- `ui_card_codex_9slice.webp` — carte de découverte.
- `ui_badge_common.webp`, `uncommon`, `rare`, `epic`, `legendary`, `exceptional` — raretés avec forme différente.
- `ui_progress_horizontal_9slice.webp` — progression générale.
- `ui_progress_circular.webp` — extraction, scan ou tâche.
- `ui_tooltip_9slice.webp` — aide courte.
- `ui_modal_confirmation_9slice.webp` — confirmation d’action irréversible.
- `ui_hologram_noise.webp` — texture d’interférence très légère.

## 13.2 Icônes fonctionnelles

Toutes les icônes doivent exister en source SVG et rester lisibles à 32 px.

- `icon_currency_space_dollar.svg`
- `icon_energy.svg`
- `icon_heat.svg`
- `icon_cold.svg`
- `icon_storage.svg`
- `icon_inventory.svg`
- `icon_production.svg`
- `icon_refining.svg`
- `icon_assembly.svg`
- `icon_robot.svg`
- `icon_technology.svg`
- `icon_module.svg`
- `icon_mission.svg`
- `icon_contract.svg`
- `icon_achievement.svg`
- `icon_codex.svg`
- `icon_collection.svg`
- `icon_sector.svg`
- `icon_scanner.svg`
- `icon_meteor.svg`
- `icon_archive.svg`
- `icon_nova.svg`
- `icon_settings.svg`
- `icon_audio.svg`
- `icon_vibration.svg`
- `icon_accessibility.svg`
- `icon_privacy.svg`
- `icon_rewarded_ad.svg`
- `icon_prestige.svg`
- `icon_stellar_core.svg`
- `icon_frontier.svg`
- `icon_world_volcanic.svg`
- `icon_world_crystalline.svg`
- `icon_world_derelict.svg`
- `icon_warning.svg`
- `icon_confirm.svg`
- `icon_back.svg`
- `icon_center_base.svg`
- `icon_center_mission.svg`
- `icon_locked.svg`
- `icon_unlocked.svg`

# 14. Éléments narratifs et événements

- `prop_nova_terminal.glb` — terminal ou projecteur utilisé pour les transmissions.
- `prop_archive_fragment_a.glb` à `c.glb` — variations de fragments d’archive.
- `prop_crystal_anomaly_a.glb` et `b.glb` — deux anomalies cristallines.
- `prop_abandoned_robot_ex.glb` — robot extracteur abandonné.
- `prop_abandoned_robot_lg.glb` — robot logistique abandonné.
- `prop_orbital_merchant_ship.glb` — petit vaisseau marchand, distinct du vaisseau du joueur.
- `prop_temporary_exceptional_deposit.glb` — gisement temporaire plus spectaculaire.
- `ui_nova_portrait.webp` — représentation abstraite de NOVA, sans visage humain obligatoire.
- `ui_nova_waveform.webp` — forme d’onde ou signal accompagnant les transmissions.
- `ui_archive_locked.webp` et `ui_archive_resolved.webp` — états visuels des archives.

# 15. Visuels marketing et Google Play

Les dimensions exactes doivent être vérifiées dans la Play Console au moment de l’export, mais les sources doivent être produites en haute résolution.

- `marketing_app_icon_master.svg` — icône principale, silhouette de planète et noyau industriel.
- `marketing_app_icon_512.png` — export haute résolution de l’icône.
- `marketing_feature_graphic_master.psd` — composition Ferrum, robots et base.
- `marketing_logo_horizontal.svg` — logo Miner Space horizontal.
- `marketing_logo_compact.svg` — logo compact pour l’icône et les panneaux.
- `marketing_keyart_ferrum.png` — scène principale Ferrum Delta.
- `marketing_keyart_cryos.png` — scène Cryos IX.
- `marketing_keyart_frontier.png` — carte stellaire et trois familles.
- `marketing_screenshot_01_base.png` — base Ferrum en activité.
- `marketing_screenshot_02_production.png` — chaîne brute vers raffinée et composant.
- `marketing_screenshot_03_robots.png` — quatre familles de robots.
- `marketing_screenshot_04_meteor.png` — pluie de météorites.
- `marketing_screenshot_05_exploration.png` — ouverture d’un secteur.
- `marketing_screenshot_06_cryos.png` — réseau thermique.
- `marketing_screenshot_07_frontier.png` — sélection d’un monde.
- `marketing_screenshot_08_accessibility.png` — réglages et lisibilité.

# 16. Livrables demandés pour chaque modèle 3D

Pour éviter les problèmes d’intégration, chaque objet 3D doit être livré avec :

1. le fichier source `.blend` ;
2. le fichier final `.glb` ;
3. une image de contrôle sur fond neutre ;
4. le nombre de triangles ;
5. la liste des matériaux et textures ;
6. le point d’origine et l’échelle utilisés ;
7. les animations intégrées avec leur nom ;
8. la licence ou l’origine de chaque texture externe ;
9. une variante sans émission si nécessaire pour le mode faible ;
10. une indication `P0`, `P1`, `P2` ou `P3` dans le suivi de production.

# 17. Ordre conseillé de création

1. Kit UI, icônes principales et sons UI.
2. Kit de terrain Ferrum, roches et neuf minerais.
3. Centre de commandement, raffinerie, assembleur et stockage.
4. Robots EX, RF et AS de niveau initial.
5. Sons d’extraction, raffinage et assemblage.
6. Scanner, ouverture de secteur et pluie de météorites.
7. Robot LG, bâtiments intermédiaires et ressources raffinées.
8. Composants, technologies, modules et bâtiments avancés.
9. Ruines, NOVA, ressources rares et chantier du vaisseau.
10. Cryos IX et réseau thermique.
11. Trois familles de la frontière.
12. Musiques, voix NOVA et visuels marketing finaux.

Le développement peut intégrer les fichiers progressivement en conservant exactement les noms définis dans cette liste.