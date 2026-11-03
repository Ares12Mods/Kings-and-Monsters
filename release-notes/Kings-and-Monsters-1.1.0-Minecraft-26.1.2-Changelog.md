# Kings & Monsters 1.1.0 — Minecraft 26.1.2

## Platform Port

- Ported Kings & Monsters to Minecraft 26.1.2 and NeoForge 26.1.2.
- Ported the complete Kings & Monsters 1.1.0 gameplay and content set.
- Updated item, recipe, rendering, networking, world-generation, and data APIs for Minecraft 26.1.2.
- Updated supported required and optional integrations to their Minecraft 26.1.2 versions.

## Patchouli Compendium

- Added custom pixel art for the Kings & Monsters Patchouli Compendium item.
- Added a new Combat Effects section to the Compendium.
- Added in-game explanations for Crippled, Dazed, Bleeding, and Bogfume Rage, including their sources and gameplay effects.

## Tooltips and Configuration

- Added a client configuration option allowing players and modpack creators to disable Kings & Monsters custom tooltips.
- Disabling custom tooltips restores the normal tooltip presentation for supported Curios items and hides the mod's custom effect descriptions and tooltip colors.
- Custom tooltips remain enabled by default.
- The setting is available as `showCustomTooltips` in `kingsandmonsters-client.toml`.

## Compatibility Notes

- No Man's Land compatibility is not included in this build because a compatible Minecraft 26.1.2 version is not currently available.
- Iron's Spells 'n Spellbooks compatibility is not included in this build because a compatible Minecraft 26.1.2 version is not currently available.
- These absent optional integrations do not prevent Kings & Monsters from loading or functioning normally.

## Fixes

- Fixed Ogre Merchants freezing or exhausting memory when spawned in worlds whose biome source cannot contain a valid swamp Fort, such as the default plains-only Superflat preset.
- In those worlds, the Merchant now safely omits the unavailable Fort Map while retaining all other trades.
- Fort Map targeting in normal worlds is unchanged.
