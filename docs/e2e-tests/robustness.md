[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Robustness

Colonies surviving unloading, restarts, old worlds and size.

---

## ROB-01 Chunks unload during work

**Tests:** citizens and huts pick up where they left off after their chunks unload and load again.

**Setup**
- A colony with a builder halfway through a work order, a courier carrying items, and a farmer in a field.
- `forceLoadColony` off for this test.

**What happens**
1. The test unloads the colony's chunks and loads them again.

**Passes when**
- All citizens come back (entities exist again, matched to their citizen data, no duplicates).
- The builder continues the same work order from where it stopped.
- The courier's items are not lost.

**Time budget:** 3 minutes.

**Not implemented (not feasible in a game test):** the test area's chunks are kept loaded by the game test framework
and there is no supported way to unload and reload chunks inside a running test. Entity save/load and respawn are covered by COL-06
(entities saved to NBT and loaded back) and ROB-06 (lost entity respawned).

---

## ROB-02 Server restart

**Tests:** a colony in full swing survives a save and a fresh load of the whole world.

**Setup**
- A colony with many systems active: open requests, running research, an active raid, work orders, sick citizens, a child.

**What happens**
1. The test saves the world and reloads the colony data the way a server start does.

**Passes when**
- All of it is restored (see [COL-06](colony.md#col-06-save-and-reload-a-colony) for the list).
- The raid continues or ends cleanly, it doesn't leave orphaned raiders.
- No errors are logged during the load.

**Time budget:** 2 minutes.

**Not implemented (not feasible in a game test):** needs a world save and a fresh server start. The colony-data part is
covered by COL-06 (in-place reload of one colony from its saved NBT); raiders, sick citizens and children across a restart are not.

---

## ROB-03 Large colony performance

**Tests:** a big colony doesn't slow the server down too much.

**Setup**
- A colony with 100 citizens and 30 huts, all workers employed.

**What happens**
1. The colony runs for five minutes of game time.

**Passes when**
- The average server tick time spent in MineColonies stays below a set budget (to be measured on a reference machine and then
  fixed in the test).
- No tick takes longer than a set maximum.

**Time budget:** 5 minutes.

**Not implemented:** 100 citizens and 30 huts don't fit into the 64x64 arena, and the tick budget has to be measured on a
reference machine first; a timing assertion would make the run machine dependent.

---

## ROB-04 Loading a 1.21.1 world

**Tests:** a world saved with MineColonies on NeoForge 1.21.1 loads in the Fabric port.

**Setup**
- A saved test world from 1.21.1 shipped with the test mod, containing a colony with huts, citizens, requests and research.

**What happens**
1. The test server loads the world, which runs the data upgrade.

**Passes when**
- The colony, its buildings, citizens, research and requests are all there with the same values as before the upgrade.
- Racks and other block entities keep their contents.
- No errors are logged during the upgrade.

**Time budget:** 2 minutes.

**Not implemented:** there is no 1.21.1 test world in the repository, and the test server can't load a different world
in the middle of a run.

---

## ROB-05 Colonies don't affect each other

**Tests:** two colonies side by side keep their citizens, items and requests apart.

**Setup**
- Two colonies close together, each with a warehouse, a courier and a worker requesting the same item.
- The item is only in colony A's warehouse.

**What happens**
1. Both colonies run.

**Passes when**
- Colony A's request is fulfilled from A's warehouse.
- Colony B's request goes to B's player, never to A's warehouse.
- No citizen of one colony works for or lives in the other.

**Time budget:** 5 minutes.

**Implemented:** `ColonyConnectionsRobustnessTests.rob05_coloniesDontAffectEachOther`.

**As implemented:** two small colonies (town hall block only, set to level 1, `initialColonySize` 1). A: warehouse with 2 wooden axes, a
courier hut with a courier (the warehouse takes couriers hired at a courier hut), a lumberjack. B: a lumberjack. Both lumberjacks request an
axe. A's courier delivers one from A's warehouse; B's request is held by B's own retrying/player resolver, A's warehouse keeps its second
axe and B's lumberjack gets none; every citizen works for and belongs to its own colony.

---

## ROB-06 Lost citizen entities come back

**Tests:** if a citizen's entity disappears, the colony spawns it again.

**Setup**
- A colony with a worker.

**What happens**
1. The test removes the worker's entity without killing it (as some mods or chunk errors do).

**Passes when**
- The colony notices and spawns the citizen again, with the same data, job and home.
- There is never more than one entity for that citizen.

**Time budget:** 2 minutes.

**Implemented:** `ColonyConnectionsRobustnessTests.rob06_lostCitizenEntityComesBack`. The entity is removed with
`RemovalReason.DISCARDED`; the colony respawns it on its 5-minute check (`CitizenManager.onColonyTick`, about 5600-6000 ticks), with
the same citizen data, job and home, never two entities.

---

## ROB-07 Pathfinding runs on one known thread

**Tests:** all MineColonies pathfinding goes through `Pathfinding.getExecutor()`, the executor the test server waits for at the
end of every tick (see [Speed](framework.md#pathfinding-stays-deterministic)). If this breaks, other tests would quietly start to
depend on the machine's speed.

**Setup**
- A colony with a citizen of each kind that requests paths: a worker walking to its hut, a guard on patrol, a fisherman looking
  for water, and a raider during a small raid.

**What happens**
1. The colony runs while all of them request paths.

**Passes when**
- Every running thread named `Minecolonies Pathfinding*` belongs to `Pathfinding.getExecutor()`.
- The executor's submitted job count went up during the test, so the paths really went through it.
- At the end of every tick, the submitted and finished job counts are equal.

**Time budget:** 1 minute.

**Implemented:** `ColonyConnectionsRobustnessTests.rob07_pathfindingOnOneThread` with a lumberjack, a guard and a 2-barbarian raid
(no fisherman). For 600 ticks: every job counted by the executor before tick N is completed at tick N+1, and no thread named
`Minecolonies Pathfinding*` other than the executor's exists. Real path jobs are counted as submitted jobs minus the one marker job the
test server submits per tick.
