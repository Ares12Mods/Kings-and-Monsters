// Converts the Ogre Merchant Blockbench project animation tracks to GeckoLib's Bedrock JSON.
// Usage: node tools/export_merchant_animations.mjs <source.bbmodel> <destination.json>
import fs from 'node:fs';

const [, , source, destination] = process.argv;
if (!source || !destination) throw new Error('Expected source and destination paths');
const model = JSON.parse(fs.readFileSync(source, 'utf8'));
const wanted = new Map(model.animations
  .filter(animation => ['idle', 'walk', 'run', 'Trade'].includes(animation.name))
  .map(animation => [animation.name, animation]));

function vector(point) {
  return ['x', 'y', 'z'].map(axis => {
    const value = Number.parseFloat(String(point[axis] ?? 0).trim());
    return Number.isFinite(value) ? value : 0;
  });
}

function exportAnimation(animation) {
  const bones = {};
  for (const animator of Object.values(animation.animators ?? {})) {
    if (!animator.keyframes?.length) continue;
    const bone = {};
    for (const channel of ['rotation', 'position', 'scale']) {
      const frames = animator.keyframes.filter(frame => frame.channel === channel)
        .sort((a, b) => a.time - b.time);
      if (!frames.length) continue;
      const track = {};
      for (const frame of frames) track[String(frame.time)] = vector(frame.data_points[0]);
      bone[channel] = track;
    }
    if (Object.keys(bone).length) bones[animator.name] = bone;
  }
  return { loop: animation.loop === 'loop', animation_length: animation.length, bones };
}

const animations = {};
for (const [name, animation] of wanted) animations[name] = exportAnimation(animation);
// The supplied project has no separately-authored idle2 track. Keep a distinct full-cycle clip so
// code can alternate cleanly; artists can add eyelid/eyebrow keyframes to idle2 without code changes.
animations.idle2 = structuredClone(animations.idle);
animations.idle2.loop = false;
fs.writeFileSync(destination, `${JSON.stringify({ format_version: '1.8.0', animations }, null, 2)}\n`);
