[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Fabric port integration

Checks that the port works with Fabric and with other Fabric mods. These tests exist in
`src/gametest/java/com/minecolonies/gametest/TransferBridgeGameTests.java` and run with `./gradlew runGameTest`.

---

## FAB-01 Other mods can use MineColonies racks

**Tests:** a MineColonies rack is visible to other Fabric mods as a normal item storage.

**Setup**
- A single rack, nothing else (no colony needed).

**What happens**
1. Through Fabric's item transfer API, the test inserts 10 diamonds in a transaction it then cancels, inserts 10 diamonds for real,
   and takes out 4.

**Passes when**
- The rack is found as a Fabric item storage.
- The cancelled insert leaves the rack empty.
- After the real insert and the removal, the rack holds 6 diamonds (checked through MineColonies' own item handler).

**Time budget:** 1 second.

---

## FAB-02 MineColonies can use vanilla chests

**Tests:** MineColonies' item handling works with a vanilla chest.

**Setup**
- A single chest.

**What happens**
1. Through MineColonies' item handler, the test puts in 5 iron ingots.

**Passes when**
- The chest holds 5 iron ingots.

**Time budget:** 1 second.

---

## FAB-03 MineColonies can use Fabric-only storages

**Tests:** MineColonies' item handling works with blocks that only Fabric's transfer API understands.

**Setup**
- A composter (its item access exists only through Fabric API).

**What happens**
1. Through MineColonies' item handler, the test puts in a pumpkin pie.

**Passes when**
- The composter takes the pie and its level rises to 1.

**Time budget:** 1 second.

---

## FAB-04 MineColonies food is compostable

**Tests:** MineColonies food and crops use the vanilla compost chance tiers.

**Setup**
- A composter.

**What happens**
1. The test reads the compostable tier of cabbage, golden bread and apple pie, then puts an apple pie into the empty composter.

**Passes when**
- Cabbage has `low_medium`, golden bread `medium_high`, apple pie `always_add_one`.
- The composter level rises to 1.

**Time budget:** 1 second.

---

## FAB-05 The pharao scepter shoots fire arrows

**Tests:** the fire arrow item creates its fire arrow entity when it is shot (`ItemFireArrow#createArrow`, called by vanilla's projectile
weapon code; the scepter turns every arrow it shoots into a fire arrow).

**Setup**
- A fake player (survival) with the pharao scepter in his hand (first hotbar slot) and 2 arrows in his inventory, outside any colony.

**What happens**
1. The player lets go of the scepter at full charge (`releaseUsing` with 20 ticks of drawing).

**Passes when**
- Exactly one fire arrow entity exists next to the player, owned by him and burning.
- One arrow is left in the inventory.

**Time budget:** 1 second.

**Found while writing it:** the scepter accepted any item as ammo (`getAllSupportedProjectiles` was `itemStack -> true`, also in
upstream 1.21), so vanilla took the first inventory slot as ammo: an empty first slot meant no shot, any other item was used up, and the
scepter itself in the first slot used itself up and the shot threw `IllegalArgumentException: Invalid weapon firing an arrow` on the
server. Fixed in `ItemPharaoScepter.getAllSupportedProjectiles` (any arrow item).

---

## FAB-06 A dropped fire arrow stays an item

**Tests:** a fire arrow item on the ground stays an item. On NeoForge 1.21.1 the item's `createEntity` hook replaced every dropped fire
arrow (for example the pharao's loot) with a new fire arrow entity at (0, 0, 0), so the item vanished; the port doesn't copy that hook
on purpose ([PROTOCOL.md, Open Fabric-port TODOs](PROTOCOL.md#open-fabric-port-todos-missing-features-not-bugs)).

**Setup**
- An item entity with 4 fire arrows.

**What happens**
1. 5 ticks pass.

**Passes when**
- The item entity still exists and still holds 4 fire arrows.

**Time budget:** 1 second.

---

## FAB-07 Creative tab additions of other mods reach MineColonies

**Tests:** MineColonies' list of all creative tab items (`CraftingUtils.forEachCreativeTabItems`, used for its item lists) contains the
items other Fabric mods add to a tab through Fabric API (`CreativeModeTabEvents`), also on a dedicated server where the tabs are never
built. On NeoForge these came from `BuildCreativeModeTabContentsEvent`.

**Setup**
- The test registers (once) a Fabric API listener that adds a stick named "e2e creative tab marker" to the vanilla ingredients tab.
  The tab is not built on the test server.

**What happens**
1. The test collects the items of all tabs through `CraftingUtils.forEachCreativeTabItems`.

**Passes when**
- The ingredients tab's items contain its own items (iron ingot) and the marker stick.

**Time budget:** 1 second.

---

## FAB-08 Tag conditions see the loaded tags

**Tests:** `neoforge:tag_empty` conditions in MineColonies json data (`neoforge:conditions` in files read by `BaseDataListener`, e.g.
study items) are tested against the tags being loaded, like NeoForge's staged tags. Before, they always saw empty tags.

**Setup**
- The condition context of a data reload (`AddReloadListenerEvent#getConditionContext`) built from the server's registries.
- A MineColonies data listener (a `BaseDataListener` in the test) with two files: one with "`minecraft:logs` is not empty", one with
  "`minecraft:logs` is empty".

**What happens**
1. The test checks two conditions directly, then lets the listener load the two files with that context.

**Passes when**
- `minecraft:logs` is not empty and a missing tag is empty.
- The listener keeps only the file whose condition is true.

**Time budget:** 1 second.

---

## Re-implemented NeoForge features (PRT)

Features of the NeoForge original that the port had to build again on Fabric (former `FABRIC-PORT TODO`s). The server tests are in
`src/gametest/java/com/minecolonies/gametest/suites/PortFeatureTests.java` (`./gradlew runGameTest -Pe2eFilter=port_feature_tests`);
the highlight tips are UI test [UI-16](ui.md#ui-16-structurize-highlight-tips).

---

## PRT-01 Solid placeholders follow the worldgen surface rules

**Tests:** Structurize's solid placeholder block (`structurize:blocksolidsubstitution`) becomes the block world generation would put at
that spot: the dimension's material rule (the 26.x name of the surface rule) is evaluated there (`BlockUtils#getWorldgenBlock`, through
`MaterialRuleContextInvoker`). Before, the port fell back to the dimension's default block or dirt.

**Setup**
- The test server's Nether uses vanilla's real noise generator (test mod mixin `GameTestServerNetherMixin`; the overworld, where every
  other test runs, and the End stay flat). No colony.
- For a warped forest and a crimson forest (found with `findClosestBiome3d` far from the origin): a column at y = 80 where vanilla's own
  single-position evaluation (`MaterialSystem#topMaterial`) gives a top block (nylium or wart block).

**What happens**
1. Air above and below the spot; a 1x1x1 blueprint holding a solid placeholder is pasted there the way the creative build tool does
   (`CreativeStructureHandler`, "pretty" placement, `PlaceStructureOperation`).
2. The spot is buried (netherrack 3 blocks above and below) and the placeholder is pasted again.

**Passes when**
- On the surface the placeholder became exactly the block vanilla's material rule gives (warped or crimson nylium or wart block, not
  netherrack).
- Buried, it became netherrack.

**Time budget:** 100 ticks (finishes in its first tick; generating the Nether chunks takes about a second).

---

## PRT-02 Domum Ornamentum blocks sound like their material

**Tests:** materially textured Domum Ornamentum blocks use the sound of their main material for place, step and fall sounds (NeoForge's
positional `getSoundType`; on Fabric the sound lookup mixins in `com.ldtteam.domumornamentum.fabric.mixin` call
`IMateriallyTexturedBlock#getSoundType(state, level, pos, entity)`, which the blocks override).

**Setup**
- A stone block per case in a plain test area and a fake player. The sounds the server plays are recorded (test mod mixin
  `SoundCaptureMixin`).
- Cases: light brick with moss (expects `SoundType.MOSS`), light brick with amethyst (`AMETHYST`), timber frame with a deepslate frame
  (main component) and an oak planks centre (`DEEPSLATE`). The block's own sound differs in each case.

**What happens**
1. The fake player places the block from its item carrying the material data (`ItemStack#useOn` → `BlockItem#place`).
2. The player stands on the block and its step sound is played (`Entity#playStepSound`, what walking calls).
3. The player's landing sound is played (`LivingEntity#playBlockFallSound`).

**Passes when**
- The block and its block entity (with the given materials) are there.
- The place, step and fall sounds are the material's sound events, with vanilla's volume and pitch for that sound type.

**Not covered:** the client-only hit sound while mining and the break sound (level event 2001). They use the same lookup
(`ClientLevelHitSoundMixin`, `LevelEventHandlerBreakSoundMixin`); the mixins are applied in every client run (`defaultRequire` 1).

**Time budget:** 100 ticks (finishes in its first tick).
