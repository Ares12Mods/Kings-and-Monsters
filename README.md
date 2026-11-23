Kings and Monsters
==================

Kings and Monsters is a NeoForge 1.21.1 mod that adds bog iron equipment,
animated ogre enemies, tribute/patrol systems, and ogre outpost worldgen.

Current State
-------------

- Kings & Monsters version: 2.0.0
- Minecraft: 1.21.1
- NeoForge: 21.1.230
- Java: 21
- Animation library: GeckoLib 4.8.4
- Required dependencies: GeckoLib, Curios
- Optional integrations: Better Combat, JEI, Jade, Patchouli, Boss Checklist

Version 2.0 Update
------------------

Version 2.0 is a major update: it adds the swamp, the Mudsnout Boars, the tribe's favor and
quest system, the Ogre King's court and much more.

### New creatures

- **Bog Toad** — a large animated swamp creature with custom movement, attacks,
  landing impacts, natural spawning, loot, sounds, and progression rewards.
- **Bogpole** — the Bog Toad's smaller aquatic life stage, with its own model,
  animation, behavior, renderer, and spawn egg.
- **Plague Ogre** — a Bogrot-infested Ogre variant that can spawn naturally and
  around Ogre outposts, with unique visuals, combat behavior, loot, and Bogrot
  immunity.

### Bogrot and swamp equipment

- Adds the complete **Bogrot** status system, including custom particles,
  periodic damage, Ogre vulnerability, immunity rules, brewing, and tooltips.
- Bog Toad Venom can be brewed into Potions of Bogrot.
- The **Bogfang Blade** applies Bogrot and can rupture the remaining effect
  damage with critical hits.
- The **Bogburst** enchantment spreads Bogrot to nearby targets when it is
  ruptured.
- Adds Bog Toad Skin, Boghide, Bogfangs, raw and cooked Toad Legs, Toad Stew,
  Boghide Boots, recipes, loot tables, and swamp-related advancements.
- Boghide Boots provide Bogstride swimming benefits and a sprint-jump landing
  attack.

### New equipment

- **Candle Helmet** — an iron helmet that can be placed as a block, lit with
  flint and steel, worn while lit, and used as a moving light source. The
  Brightwick enchantment increases its light level. Its placed model rests
  naturally on its front rim.
- **Padded Earmuffs** — a Curios head-slot item that reduces the penalties from
  the Ogre King's roar.

### Ogre and general bug fixes

- Ogre Merchant Backpacks now have expanded storage and reliably save items
  after stacking, shift-clicking, and closing the menu.
- Ogre Merchants correctly become invisible when loaded at night or when time
  is changed directly to night.
- Merchant guards now despawn with their assigned merchant instead of wandering
  indefinitely.
- Includes completed Ogre combat, movement, rendering, equipment, loot, and
  nametag corrections from the later development line.
- Includes completed Bog Iron armor/tool behavior fixes, Hunter's Spear combat
  fixes, effect-immunity handling, and related resource/tag corrections.

Development Setup
-----------------

Install or select a JDK 21 before running Gradle. On this workstation, the
project builds with:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build
```

Useful Gradle tasks:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```

### Editing structures with Forgematica

The development client includes Forgematica (the NeoForge Litematica port) and
MaFgLib, plus JourneyMap for worldgen testing. Running `runClient` automatically
copies the checked-in Ogre structure templates to
`run/schematics/kingsandmonsters`.

In Forgematica's Schematic Manager, import the desired vanilla `.nbt` template,
edit it in a creative test world, then export/save the finished template as
vanilla structure NBT under the same relative filename. Copy finished NBT files
back into the mod with:

```powershell
.\gradlew.bat importStructureSchematics
```

This workflow includes the King's Fort plus every grunt, brute, mage, and
variant Ogre outpost. Keep `.litematic` working files in `run/schematics`; only
the final `.nbt` templates are packaged by the mod.

Continuation Notes
------------------

- Main mod bootstrap: `src/main/java/com/kingsandmonsters/KingsAndMonsters.java`
- Registries: `ModBlocks`, `ModItems`, `ModEntities`, `world/ModFeatures`, and `world/ModStructures`
- Ogre entity logic: `src/main/java/com/kingsandmonsters/entity`
- GeckoLib render/model classes: `src/main/java/com/kingsandmonsters/entity/client`
- Tribute and patrol systems: `src/main/java/com/kingsandmonsters/tribute`
- Public addon API: `src/main/java/com/kingsandmonsters/api`
- Worldgen JSON and NBT structures: `src/main/resources/data/kingsandmonsters`

Before handing off changes, run `.\gradlew.bat build`. Use
`.\gradlew.bat build --warning-mode all` when touching Gradle files or
dependency versions.
