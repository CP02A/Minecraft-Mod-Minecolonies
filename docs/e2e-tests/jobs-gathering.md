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

**As implemented:** not yet. Mining levels are absolute heights (y 48, 16, -16, -100; `BuildingMiner.MINING_LEVELS`), below the normal arena's barrier floor (ground at about y 9); use the deep arena (`ColonyFixture.makeDeepGround()`, stone down to y -32). Supply the shaft's framed planks yourself: the request otherwise waits for the retrying resolver (about 40,000 ticks).

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

**As implemented:** not yet; needs the deep arena like GAT-03.

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

**As implemented** (`FarmingGatheringTests`): not done (time box).

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

**As implemented** (`FarmingGatheringTests`): not done (time box).

