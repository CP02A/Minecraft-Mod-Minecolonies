[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Jobs: gathering resources

Workers who collect raw materials from the world: wood, stone, ores and nether resources.

---

## GAT-01 Lumberjack fells and replants

**Tests:** the lumberjack cuts whole trees and replants them.

**Setup**
- A forester's hut with a lumberjack and an axe, in a forest arena with grown oak and birch trees.

**What happens**
1. The lumberjack works.

**Passes when**
- Each felled tree is gone completely (logs and leaves, no floating parts).
- A sapling of the same type is planted where each tree stood.
- Logs and saplings end up in the hut.

**Time budget:** 10 minutes.

**As implemented** (`FarmingGatheringTests`): one oak and one birch grown from saplings with the vanilla tree grower, within 50 blocks of the hut (the search range). Defoliation is on (random ticks are 0, leaves would never decay); leaves are cut with a hoe, so the hut holds wooden hoes (and wooden axes: a level 1 hut allows no better tools) and saplings.

---

## GAT-02 Lumberjack follows its settings

**Tests:** the lumberjack only cuts what it's allowed to.

**Setup**
- Like GAT-01, plus a spruce tree, a tree bigger than `maxTreeSize`, and a tree outside the assigned work area.
- Only oak is allowed in the hut's settings.

**What happens**
1. The lumberjack works.

**Passes when**
- Only oak trees inside the work area and within the size limit are cut.
- The other trees are untouched.

**Time budget:** 10 minutes.

**As implemented** (`FarmingGatheringTests`): every sapling except oak is on the "don't cut" list (it is a blacklist), maxTreeSize is set to 10, the work area is restricted to a box with an oak, a spruce and a hand-built oak with a 12 log trunk; another oak stands outside the box. After the small oak is cut and replanted, the test waits 2400 more ticks and checks the other trees still have all logs.

---

## GAT-03 Miner digs the shaft

**Tests:** the miner digs the main shaft down safely.

**Setup**
- A mine (level 1) with a miner, a pickaxe, a shovel, ladders, cobblestone and torches. Solid stone below the hut.

**What happens**
1. The miner digs the shaft to the first mining level.

**Passes when**
- The shaft has ladders all the way down and walls where the blueprint needs them.
- The miner never falls more than three blocks and takes no damage.
- The mined blocks end up in the hut.

**Time budget:** 15 minutes.

**As implemented** (`FarmingGatheringTests`, `gat03_minerDigsShaft` + `gat03_minerDigsShaftSafely`): deep arena
(`makeDeepGround`), town hall at (12, 5, 12), level 1 mine at (32, 5, 44) with four extra racks next to it (room for what the miner digs
out), 64 ladders, 64 cobblestone, 64 torches, 4 wooden pickaxes and 2 wooden shovels in the hut; miner with all skills 50.
- **The first mining level is right under the hut**, not at an absolute height: the miner builds a level 5 blocks below where the shaft
  starts (`MinerLevelManagementModule.startingLevelShaft` = first ladder - 4; here y 6 absolute, the hut's top ladder is at y 11), then one
  every 6 blocks further down until the hut level's mining depth (`BuildingMiner.getDepthLimit`: the first of y 48/16/-16/-100 below the
  hut per hut level, y -16 for a level 1 hut on the arena), where it switches to nodes. The test waits for the first level.
- Materials the hut doesn't have (framed blocks, slabs, fences, stairs and signs of the level's main shaft structure, replacement tools)
  are handed over by the creative owner like the request window's "fulfil" button (`OwnerFulfils`: only requests waiting at the retrying
  or player resolver; stack into the worker's inventory, request overruled). Without it the framed blocks come from the retrying resolver
  only after about 40,000 ticks.
- Checks: ladders from the hut's top ladder down to the level and a solid column behind them; at least 64 dug blocks (cobblestone, stone,
  dirt, minus what the test put in) in the hut.
- "Never falls more than three blocks, no damage" is `gat03_minerDigsShaftSafely` (was optional because of bug E2E-GAT-03-1, the
  miner falling off the shaft ladder; fixed).

---

## GAT-04 Miner mines a level

**Tests:** on a mining level, the miner builds out nodes and collects ores.

**Setup**
- A mine with a finished shaft and a mining level started, ore blocks placed inside the next nodes, and supplies.

**What happens**
1. The miner mines several nodes.

**Passes when**
- The nodes are dug out and supported as the mine's node structure requires.
- The placed ores end up in the hut.
- Holes to caves or lava are sealed off.

**Time budget:** 15 minutes.

**As implemented** (`FarmingGatheringTests`, `gat04_minerMinesNodes` + optional `gat04_minerSealsWater`): the GAT-03 mine runs until
its first level exists (no pre-built shaft). Then the test sets the hut's "max depth" setting to the level's depth + 3 (otherwise the
miner goes on with the shaft to the next level), puts two iron ore blocks into the middle of each of the level's three first tunnel nodes
and an air pocket (cave hole) into a wall corner of the first node (corners are solid in every rotation of the tunnel blueprint).
- Waits until two of these nodes are completed. Each built node's 7x7x5 box must have the tunnel blueprint's supports (at least 16 framed
  blocks, 4 fences, a torch) and be open (at least 20 air); the hole is solid again; the hut holds 2 iron ores per built node (the miner
  mines ores with silk touch, so they arrive as ore blocks).
- Lava/water: the optional `gat04_minerSealsWater` puts a water source one block past the open end of the first node (inside the range
  the miner checks for liquids before a node) and expects the fill block there after the node is built. Fails on bug E2E-GAT-04-1 (the
  check is never reached).
- Supplies as in GAT-03 (`OwnerFulfils` for what the colony can't deliver).

---

## GAT-05 Quarrier

**Tests:** the quarrier digs out its quarry layer by layer.

**Setup**
- A quarry with a quarrier and its tools, placed over solid stone.

**What happens**
1. The quarrier works through the first layers.

**Passes when**
- The blocks inside the quarry area are removed top down.
- The removed blocks end up in the hut.
- Nothing outside the quarry area is dug.

**Time budget:** 15 minutes.

**As implemented** (`FarmingGatheringTests`, `gat05_quarrierDigsLayers`): stone floor (`fillFloor`) and deep arena; level 1 mine at
(12, 5, 52) as the quarrier's workplace, simple quarry at (36, 5, 40); a citizen with all skills 50 is hired in the mine with the quarrier
job and assigned to the quarry (its quarry module, like the hut window's assign list). The mine holds wooden pickaxes and shovel,
cobblestone and torches; framed blocks and further tools (golden pickaxes for a level 1 mine) come from the creative owner
(`OwnerFulfils`).
- The quarrier digs the quarry's shaft blueprint (`simplequarryshaft1`, 18x26x23, from 2 below the quarry hut block downward) layer by
  layer: solid frame, decoration of the layer above, then clear. The test reads the blueprint from the mine's work order once loaded and
  takes the positions it has as air in the top three layers. The top layer (ground level) is already open: the quarry hut's own blueprint
  has the hole. So the quarrier digs the next two layers (about 235 blocks each).
- Passes when every air position of the three layers is air; per tick no position of a lower layer may be dug while the layer above still
  has undug positions (top down); the blocks one step outside the shaft's footprint (all columns around it, from ground level 5 layers
  down) are unchanged; at least 64 stone/cobblestone are in the quarry (the quarrier dumps into the quarry, not the mine).

---

## GAT-06 Nether expedition

**Tests:** the nether worker goes on an expedition and comes back with loot and a log entry.

**Setup**
- A nether mine with a nether worker, the required research done (fixture), a working nether portal, food and the gear it needs.

**What happens**
1. The nether worker leaves through the portal. The test skips the trip time.
2. The nether worker comes back.

**Passes when**
- The worker leaves and comes back alive (or its death is handled as the expedition rules say).
- Loot from the trip ends up in the hut.
- The expedition log module has a new entry for the trip.

**Time budget:** 15 minutes.

**As implemented** (`FarmingGatheringTests`, `gat06_netherExpedition` + optional `gat06_netherWorkerReturnsAfterTravel`): the trip is
simulated, there is no real nether travel: the worker walks to the hut's portal (obsidian frame from the blueprint, lit with the flint and
steel), the level's trip recipe is "crafted" into a list of finds and adventure tokens (fights), the citizen entity is removed and the
colony's travelling manager counts down the travel time (400 ticks per result). When it is over the entity respawns at the portal (at the
colony's next citizen check, up to 6000 ticks later), works off the list (fights mobs, mines finds, all in a few ticks), logs it and
walks home to dump the loot.
- Setup: level 1 nether mine at (20, 5, 14), research `technology/netherlog` completed (the log's view is gated by it), the level 1 trip
  inputs (64 cobblestone, 32 torches, 16 ladders), wooden axe, pickaxe, shovel and sword, flint and steel, leather armor, bread on the
  hut's menu; worker with all skills 50.
- The test skips the travel time (re-registers the trip with 0 ticks left). The returned worker then idles until the hut allows a new trip
  (1 trip per 3 days, bug E2E-GAT-06-1); the required test runs the hut's morning update (`onWakeUp`) four times, as four mornings would,
  and the worker carries on. The optional `gat06_netherWorkerReturnsAfterTravel` expects the trip to be worked off without that.
- Passes when the log says COMPLETED with loot, the worker is back near the hut (visible) and items of the logged loot are in the hut; or
  the worker died and the log says KILLED (allowed by the rules; not seen so far: the worker fought 3 mobs and came back).
