# Kings & Monsters 2.0.1 (Minecraft 1.21.1, NeoForge)

A quick hotfix for ogre patrols showing up far too often.

## Fixes
- Fixed ogre patrols spawning every couple of minutes. The new camps in 2.0 each ran their own patrol timer, so standing near several of them meant being hunted by all of them in turn.
- You now get at most one patrol at a time per minimum cooldown (40 minutes by default), no matter how many camps are nearby.
- Only the full outposts, the Ogre King's fort and moving camps send patrols now. Supply camps, the King's Quarry, abandoned plague camps, Trapper huts and boar traps no longer do.
- Retaliation for looting a tribute chest works the same as before.

If you want even fewer patrols, the Patrols section of the config lets you turn them off or raise the cooldowns.
