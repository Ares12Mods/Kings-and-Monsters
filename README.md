Kings and Monsters
==================

Kings and Monsters is a Forge 1.20.1 adventure and progression mod built around
organized monster factions. The Ogre Tribe is the first complete one: animated ogres,
guarded outposts and a royal fort, a shared anger system, personal favor and quests,
and an Ogre King to challenge. Version 2.0 adds a swamp, tameable boars and much more.

Current State
-------------

- Kings & Monsters version: 2.0.2
- Minecraft: 1.20.1
- Forge: 47.4.0
- Java: 17
- Animation library: GeckoLib 4.8.4
- Required dependencies: GeckoLib, Curios
- Optional integrations: Better Combat, JEI, Jade and Patchouli

Version 2.0 Update
------------------

Version 2.0 is the biggest update so far. Every ogre was animated again from scratch
with new attacks and sounds, mobs chase in straight lines instead of zigzagging along
path nodes, and there is a lot of new content around them. The full list is in
`release-notes/Kings-and-Monsters-2.0.0-Changelog.md`.

### New creatures

- **Bog Toad**: a large swamp predator that hops and bites with Bogrot II. Its heavy
  landings daze whatever is underneath. Toads can't be bred.
- **Bog Pole**: the toad's young. Poles hatch from Bog Toad Eggs on muddy banks and grow
  into toads over time.
- **Plague Ogre**: an Ogre Grunt that died infected with Bogrot II. They wander swamps and
  the edges of plains outposts and are immune to the rot.
- **Mudsnout Boar**: wild boars keep their distance and warn before they charge. They can
  be tamed, saddled, ridden and bred for speed and health traits. Some ogres ride them.
- **Ogre Trapper**: a hunter ogre found at Trapper huts and boar traps. He throws Bogjaw
  Traps and fights with the Trapper's Knife.

### The Ogre Tribe

- **Favor** is a personal reputation with the Tribe, separate from the shared anger level.
  At 50 favor you are appeased and ogres leave you alone.
- **Gold Coins** and **Golden Ogre Teeth** are tribute and currency. Hold a coin out to an ogre
  to pass for a while, or give coins to a Captain, the King or the Merchant for favor.
- **Quests**: 18 errands from Captains and the King, three offered at a time, with an
  on-screen tracker.
- **The King's court**: trade for royal relics, ask for a fair duel, or take the crown and
  call a warband of ogres to fight for you.
- **The Ogre King** has a new boss track, ground ripples you can jump, and a roar that also
  affects your tamed boars and summoned army.

### Swamp and boar equipment

- **Bogrot** is a full status effect: periodic armor-ignoring damage, extra damage to
  ogres, immunity rules, brewing, particles and Jade support.
- **Bogfang Blade** applies Bogrot and ruptures it on repeated critical hits. The **Bogburst**
  enchantment spreads the rupture.
- **Boghide armor** (swimming, a toad leap and Bogrot immunity with the full set) and
  **Blackhide armor** (a running Charge that rams enemies).
- Toad Stew, Toad Legs, Bone Broth, potions of Bogrot, Bog Sight and Amphibiousness.

### Structures and world

- New structures: the King's quarry, supply camps, a moving camp, an abandoned camp, Trapper
  huts, boar traps and mud wallows.
- Bog Toads, Bog Poles, Plague Ogres and Mudsnout boars spawn naturally.
- Bog Iron generates in swamps, mangroves and lush caves and stays locked until the King
  is beaten or yields a fair duel.

### Enchantments and items

- **Tyrant** works on swords, axes, maces and the ogre relics. **Heavy Throw** works on
  tridents and the Hunter's Spear. New: **Timber**, **Brightwick** and **Bogburst**.
- **Candle Helmet**, **Padded Earmuffs**, Bogsteel and Brute gear, the Ogre Stomach and more.

### Sound

- New idle, hurt and death sounds for every ogre, new attack sounds, boar and toad sounds, and
  a multi-part Ogre King boss track by Soul.

Building
--------

You need a JDK 17 or newer. Build the mod with:

```powershell
.\gradlew.bat build
```

On Linux or macOS use `./gradlew build`. The jar ends up in `build/libs`. Other useful tasks are
`runClient`, `runServer` and `runData`.

Project layout
--------------

- `src/main/java/com/kingsandmonsters`: the mod's code. Entities are in `entity`, the tribute and quest
  systems in `tribute`, worldgen in `world` and the public addon API in `api`.
- `src/main/resources`: assets, data, structures and the Patchouli compendium.
- `release-notes`: changelogs for each release.
- `description`: the text used on the mod pages.
- `tools`: helper scripts for converting structures and exporting animations.

License
-------

All Rights Reserved. The source is published so it can be read. See `LICENSE`.
