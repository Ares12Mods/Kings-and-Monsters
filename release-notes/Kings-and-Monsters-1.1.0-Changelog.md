# Kings & Monsters 1.1.0

## Compatibility

- Added compatibility with No Man's Land.
- No Man's Land Cut Vines are now removed correctly when they interfere with Ogre Forts and Outposts.
- Ogre Fort cleanup now also removes No Man's Land Shelf Mushroom Blocks and vanilla Mushroom Stems and Brown Mushroom Blocks that intersect the structure.
- Swamp Outposts now apply the same Cut Vine and mushroom cleanup while preserving authored structure blocks.
- No Man's Land remains completely optional.
- Added optional compatibility with Iron's Spells 'n Spellbooks.
- Ogre spell resistance now reflects each Ogre's role and lore, including strong Nature resistance for Ogre Mages and the Ogre King.
- Ogre Mages now have a slight vulnerability to Fire spells.
- Iron's Spells 'n Spellbooks remains completely optional.

## World Generation

- Increased the minimum spacing between additional Ogre Forts from 1,024 to 1,280 blocks.
- Added Bog Iron generation to Lush Caves as a rarer secondary source.
- Existing Bog Iron generation in Swamps and Mangrove Swamps remains unchanged.
- Fixed Ogre Merchants freezing or exhausting memory when spawned in worlds whose biome source cannot contain a valid swamp Fort, such as the default plains-only Superflat preset.
- In those worlds, the Merchant now safely omits the unavailable Fort Map while retaining all other trades.
- Fort Map targeting in normal worlds is unchanged.

## Advancements

- Added a dedicated Kings & Monsters advancement tab.
- Organized the existing Ogre Captain and Ogre King advancements beneath the new tab.
- Existing advancement requirements and rewards remain unchanged.

## Patchouli Compendium

- Added custom pixel art for the Kings & Monsters Patchouli Compendium item.
- Added a new Combat Effects section to the Compendium.
- Added in-game explanations for Crippled, Dazed, Bleeding, and Bogfume Rage, including their sources and gameplay effects.

## Notifications

- The locked Bog Iron mining message now appears as a centered, fading Ogre notification instead of a chat message.
- Ogre Merchant arrival messages now appear as centered, fading Ogre notifications instead of chat messages.
- Both messages retain their original wording.

## Combat and Audio

- Moved the Ogre King's double-club overhead-slam pre-impact roar two ticks earlier so it better matches the attack wind-up.
- The attack's impact timing, damage, animation length, and other Ogre King attack sounds remain unchanged.
- Corrected oversized Ogre Guard and Ogre Mage collision heights to match their physical rendered bodies.
- Guards and Mages can now use normal navigation through any opening their corrected collision boxes genuinely fit, including two-block-high passages.
- Larger Ogres continue to reject passages that cannot contain their collision boxes.

## Visual Fixes

- Adjusted the wearable Ogre King's Crown downward by half a model pixel for a better fit on the player's head.

## Tooltips and Configuration

- Added a client configuration option allowing players and modpack creators to disable Kings & Monsters custom tooltips.
- Disabling custom tooltips also restores the normal tooltip presentation for supported Curios items and hides the mod's custom effect descriptions and tooltip colors.
- Custom tooltips remain enabled by default.
- The setting is available as `showCustomTooltips` in `kingsandmonsters-client.toml`.

## Technical

- Verified that the Grunt Captain already uses the shared Ogre anti-one-shot protection for attacks against players.
- Improved optional-mod compatibility without adding No Man's Land as a required dependency.
