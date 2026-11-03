# Kings & Monsters 1.2.2 — Minecraft 1.21.1

Changes since 1.2.0.

## Ranged AI tracking (Mage, Archer, Guard)

- Replaced the constant-speed body-turn used while aiming/casting (a fixed number of degrees per tick, hard-stopping the instant it caught up) with the same proportional, decelerating turn arc already used for pathing and head-tracking. Ranged attacks now smoothly follow a side-dodging player instead of visibly jerking toward them.
- The Mage now turns its body — not just its head — while deciding what to cast, so it doesn't end up badly misaligned by the time a bolt commits.
- Preserved Magic Push's tighter, faster tracking relative to normal casts.
- Ogre Guard's ranged-pursuit goal no longer re-runs a full pathfind every recalculation tick when the target hasn't actually moved — it only repaths when the target has moved meaningfully or the current path has run out, matching the same optimization already used elsewhere (Grunt, Mage). Reduces unnecessary pathfinding work and further smooths pursuit.
- Guard's facing no longer fights navigation steering while still closing distance — it only takes over body yaw once actually in range and about to aim/fire.

## Ogre King

- Reduced downward head-tracking pitch so he no longer tucks his chin as aggressively when looking at a nearby target below eye level.
- Clamped and reduced excessive head tilt while chasing.

## Ogre Brute

- Reworked Temper Tantrum's impact timing to line up with the actual hand-to-ground contact frame, and retuned its damage.
- Reworked Punch: capped tracking during the swing, wider hit coverage, and a forward surge tied to the attack's own tick progression instead of a separate timer — tuned multiple times for start timing, duration, and distance.
- Added an anti-repeat safeguard so regular attack selection doesn't fall into an obvious pattern.
- General combat tuning pass: recovery timing, attack-selection weights, and Upswing Launch behavior.

## Ogre Grunt / Grunt Captain

- Fixed a dead zone in the Grunt's pursuit path that could leave him stuck neither approaching nor attacking.
- Cleaned up and stabilized the melee pursuit-to-attack transition so he stops cleanly and consistently at the moment he commits to a swing, and resumes pursuit correctly during recovery.
- Increased pursuit speed and tuned bite/attack approach distance and lunge strength.
- Synchronized and improved the forward "catch" movement during a swing that helps land hits against a moving target.
- Polished Grunt and Captain attack follow-through, and tuned Captain's Punch/Slash/Side Swipe catch behavior, Belly Bump sound, and head tracking during shared melee pursuit.

## Ogre Merchant

- Doubled backpack storage capacity.

## Outposts, tribute, and patrols

- Shortened tribute-retaliation delay.
- Tuned patrol cooldown and the outpost-reinforcement notification, and aligned the ogre anger tier thresholds with their documentation.

## Bog Iron

- Fixed Bog Iron Ore and Deepslate Bog Iron Ore always dropping the block itself regardless of whether the tool had Silk Touch — the Silk Touch condition used an item-predicate format that no longer matched this Minecraft version's loot table schema and was silently failing to gate the drop. Mining without Silk Touch now correctly drops Raw Bog Iron.
