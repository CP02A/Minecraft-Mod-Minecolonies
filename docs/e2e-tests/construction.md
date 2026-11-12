[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Construction

Builders turning work orders into finished buildings: new huts, upgrades, repairs, removals and decorations.

---

## BLD-01 Builder builds a new hut

**Tests:** the builder walks a work order from start to finish and the result matches the blueprint.

**Setup**
- Colony with a builder's hut (level 1) and a hired builder.
- Every material for a level 1 hut (for example a residence) already in the builder's hut.
- A build work order for that hut on an empty, flat spot.

**What happens**
1. The builder claims the work order, clears the spot and places the structure layer by layer.

**Passes when**
- The work order is claimed by this builder, then removed when done.
- Every block of the placed structure matches the blueprint (block and state, ignoring blocks the blueprint marks as optional).
- The new building is registered at level 1.
- The builder gained XP.

**Time budget:** 10 minutes. *Measured: about 15,000 ticks (12-13 minutes of game time) for a level 1 residence with a builder of
skill 40; the test allows 40,000.*

*As implemented:* the residence is placed the way a survival player's build tool does it (MineColonies' `SurvivalHandler`: only the
hut block, building registered at level 0), and the build is requested like the hut window's button. The materials are the hut
window's resource list (`WindowBuildBuilding#updateResources` logic), put in the builder's hut together with stone tools and bread.
"Matches the blueprint" uses Structurize's own "block already done" rule (`IPlacementHandler.doesWorldStateMatchBlueprintState`),
which treats placeholder blocks the way the builder does.

---

## BLD-02 Builder asks for missing materials

**Tests:** a builder without materials requests exactly what's missing, and continues once it arrives.

**Setup**
- Same as BLD-01, but the builder's hut is empty.
- A warehouse with a courier, holding all materials.

**What happens**
1. The builder works out what the structure needs and opens requests.
2. The courier delivers from the warehouse.
3. The builder builds.

**Passes when**
- The builder's open requests together cover the blueprint's material list (no item asked for that isn't needed).
- The requests are fulfilled by the warehouse, not by the player.
- The hut is finished as in BLD-01.

**Time budget:** 15 minutes. *Measured: about 15,000 ticks; the test allows 42,000.*

*As implemented:* every "deliver item stack" request of the builder's hut seen during the build must be on the blueprint's material
list, no request may be assigned to a player resolver, and the warehouse must have lost materials. The builder gets his tools in his
hut (tool requests are not part of this test).

---

## BLD-03 Upgrade a hut

**Tests:** upgrading replaces the level 1 structure with level 2 and raises the building level.

**Setup**
- A level 1 hut (for example a lumberjack's hut), a builder's hut at level 2 and a builder.
- Materials for level 2 in the builder's hut.

**What happens**
1. The test creates an upgrade work order. The builder carries it out.

**Passes when**
- The world matches the level 2 blueprint.
- The building reports level 2, and things tied to the level change accordingly (for example more worker slots or storage).
- The hut's worker kept their job during and after the upgrade.

*As implemented:* "things tied to the level" = the hut's combined inventory has more slots at level 2 (more racks).

**Time budget:** 15 minutes.

---

## BLD-04 Repair a damaged hut

**Tests:** a repair order puts back exactly the blocks that are missing or wrong.

**Setup**
- A finished level 1 hut. The test removes several of its blocks and replaces a few with wrong ones.
- A builder with the repair materials.

**What happens**
1. The test creates a repair work order. The builder carries it out.

**Passes when**
- The hut matches its blueprint again.
- Blocks that were not damaged were not touched.
- The building level is unchanged.

*As implemented:* "blocks that were not damaged" are compared with their state before the damage: removing a block changes its
neighbours (a fence loses a connection, a torch on that wall drops), and the repair must bring those back as well.

**Time budget:** 5 minutes.

---

## BLD-05 Remove a hut

**Tests:** deconstruction takes the hut down and gives the materials back.

**Setup**
- A finished hut and a builder.

**What happens**
1. The test creates a removal work order. The builder carries it out.

**Passes when**
- The hut's blocks above ground are gone (only the hut block stays).
- The building stays registered, marked *deconstructed*, and keeps its level (intended MineColonies behavior: the hut window then
  offers "pick up", and a picked-up hut remembers its level).
- After the second "remove" (pick up), the building is no longer registered, the hut block is gone, the owner holds the hut item and
  the former worker is unemployed.
- The builder's inventory or hut holds recovered materials (wood, cobblestone, dirt).

**Time budget:** 10 minutes.

---

## BLD-06 Builder level limits

**Tests:** a builder can't take work orders above its hut's level.

**Setup**
- A builder's hut at level 1 with a builder.
- An upgrade order that would take another hut to level 2.

**What happens**
1. The colony runs.

**Passes when**
- The builder does not claim the level 2 order.
- After the builder's hut is raised to level 2 (fixture), the builder claims it.

*As implemented:* the hut window can't even create this order (it refuses an upgrade to level N unless a builder's hut of level N
exists), which the test checks first. The order is then added directly to the work manager (as if the level 2 builder had been
removed). "Raised to level 2" is `setBuildingLevel(2)`. The absence check waits 200 ticks (10 work manager rounds).

**Time budget:** 1 minute.

---

## BLD-07 Decorations from the build tool

**Tests:** a decoration placed with the build tool is built like any other work order.

**Setup**
- A builder with materials.
- A decoration work order created through the same code as the build tool (for example a small well from the default style pack).

**What happens**
1. The builder builds the decoration.

**Passes when**
- The world matches the decoration blueprint.
- No building is registered for it (decorations aren't huts).

*As implemented:* the order is created by the server side of `DecorationBuildRequestMessage` (what the build tool's decoration window
sends). The decoration is the first small one of the pack without entities (`decorations/statues/pillar`): for decorations with item
frames the hut window's material list misses items (see the protocol), so the builder would wait for a delivery.

**Time budget:** 5 minutes.

---

## BLD-08 Work orders are shared between builders

**Tests:** with two builders and two work orders, each builder takes one.

**Setup**
- Two builder's huts with a builder each, all materials provided.
- Two build work orders.

**What happens**
1. The colony runs.

**Passes when**
- Each work order is claimed by a different builder.
- Both structures are finished.
- A work order manually assigned to a specific builder is built by that builder.

*As implemented:* the work orders are small decorations (cheap and quick); the manually assigned one is a third decoration requested
for the second builder while both are busy, and it must only ever be claimed by that builder.

**Time budget:** 15 minutes.

---

## BLD-09 Clearing the site

**Tests:** the builder clears terrain, plants and water that are in the way.

**Setup**
- The build spot from BLD-01 covered with dirt bumps, tall grass, a small tree and a water pool.

**What happens**
1. The builder builds the hut.

**Passes when**
- The finished hut matches the blueprint with no leftover terrain, plants or water inside its footprint.
- No water flowed into the finished hut.

**Time budget:** 10 minutes. *Measured: about 18,000 ticks; the test allows 40,000.*

*As implemented:* the leftover check counts water, grass plants, oak leaves and logs at blueprint positions where the blueprint has a
different block (the residence itself is built from oak logs). The builder needs a hoe and shears for clearing; without them he
requests them and waits.
