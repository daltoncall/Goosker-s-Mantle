# Manual test checklist

Use a fresh Fabric 1.20.1 test instance with Fabric API and remove Enhanced
Movement before testing.

## Mantle detection

- A bottom slab and a 1-block ledge do not trigger the mantle during either the
  upward or downward half of a normal jump (vanilla jumping still works).
- A 1.5-block ledge made from a full block plus a bottom slab triggers.
- A 2-block ledge triggers.
- A ledge higher than 2 blocks does not trigger with the default config.
- Holding jump while approaching the ledge triggers without another keybind.
- A running jump toward a distant ledge can catch it once the player reaches
  the ledge, rather than consuming the mantle at takeoff.
- Pressing jump after already becoming airborne can catch a nearby ledge.
- While falling, pressing or holding jump catches a reachable ledge even when
  the ledge is outside the height range relative to the original takeoff spot.
- After falling below the original takeoff surface, a ledge closer than the
  configured minimum above the player's feet still does not trigger.
- A falling catch does not wait for the two-tick normal-jump delay.
- Slabs, stairs, fences, and modded blocks use their collision-surface height.
- A centered iron-bar railing still permits a mantle onto a collision-free
  approach-side lip.
- A full solid block or ceiling above that same ledge still prevents mantling.
- A ledge with no standing-height clearance above it does not trigger.
- Flying, swimming, climbing, riding, and spectator mode do not trigger.
- Double-tapping jump to leave creative flight does not trigger a mantle, even
  if the second press is held near a valid ledge. Mantling only rearms after
  Space is fully released; the next press works normally.

## Cooldown and motion

- A successful mantle retains horizontal velocity and applies a 0.4 upward
  velocity.
- The vanilla jump begins first, followed by a visibly separate mantle lift at
  least two server ticks later.
- A second mantle cannot trigger for 30 ticks after the first.
- A successful mantle resets accumulated fall distance but does not change
  hunger.

## Presentation

- Both hands/items lift and smoothly lower in vanilla first person.
- Both arms lift and smoothly lower in vanilla third person.
- First-person Model displays the same full-body mantle pose.
- Not Enough Animations does not overwrite the mantle pose.
- Another multiplayer client sees the third-person animation.
- The custom mantle cue plays once per accepted mantle and is spatialized at
  the mantling player.

## Configuration

- `config/gooskers-mantle.json` is generated on first server or single-player
  launch.
- Changing the minimum and maximum values changes accepted collision heights
  after restart.
- Invalid or reversed values are clamped and rewritten safely.
