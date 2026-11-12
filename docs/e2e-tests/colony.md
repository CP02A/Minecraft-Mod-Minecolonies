[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Colony lifecycle

Founding a colony, its population, its territory, who may do what in it, and whether it survives a save and reload.

---

## COL-01 Found a colony

**Tests:** placing a town hall founds a working colony owned by the right player.

**Setup**
- Empty flat arena, no colony nearby.
- The fake owner player holds a town hall block.

**What happens**
1. The fake player places the town hall at the arena center (the same code path as a real player placing it).
2. The test waits for the colony to register.

**Passes when**
- The colony manager has exactly one new colony at that position, in the overworld.
- The colony's owner is the fake player and the owner has the owner rank.
- The town hall is registered as a building of the colony, level 1 after it is built (the test pastes it instantly).
- The chunks around the town hall are claimed by this colony (`initialColonySize` radius).

**Time budget:** 5 seconds.

**Implemented:** `ColonyConnectionsRobustnessTests.col01_foundColony`.

---

## COL-02 First citizens arrive

**Tests:** a new colony gets its starting citizens and they become real entities.

**Setup**
- Colony from COL-01 with a town hall only.

**What happens**
1. The colony ticks normally.

**Passes when**
- The citizen manager holds `initialCitizenAmount` citizens (4 in the test config).
- Each citizen has a living entity in the world inside the colony border.
- Every citizen has a name, a gender, skills within the allowed range and no job.

**Time budget:** 7000 ticks (about 6 game minutes). The design said 30 seconds, but the colony lets starting citizens arrive one at a
time: the first after about 1000 ticks, then one about every 1500 ticks (`CitizenManager.onColonyTick`, checked every 500 ticks), so
the fourth arrives after about 5500 ticks. At test speed that is a few seconds of real time.

**Implemented:** `ColonyConnectionsRobustnessTests.col02_firstCitizensArrive` (also checks that the starting citizens are not all of one gender).

---

## COL-03 Housing limits the population

**Tests:** new citizens only move in while there are free beds, and the colony stops growing when housing is full.

**Setup**
- Colony with a town hall and one residence at level 1 (known number of beds).
- Population exactly at the current housing limit.

**What happens**
1. The colony runs for a while with full housing.
2. The test adds a second residence.
3. The colony runs again.

**Passes when**
- While housing is full, no new citizens spawn.
- After the second residence is added, citizens move in until the new beds are full, then growth stops again.
- The population never goes above `maxCitizenPerColony`.

**Time budget:** 2 minutes.

**Implemented:** `ColonyConnectionsRobustnessTests.col03_housingLimitsPopulation`.

**As implemented (design corrected):** beyond `initialCitizenAmount` a colony grows only by children (`ReproductionManager.trySpawnChild`),
which needs a free bed, a male and a female citizen and "move in" on. The test sets `initialCitizenAmount` to 2, houses a couple in a level 2
residence (2 beds) and zeroes the reproduction timer (a new colony waits 5-15 game minutes before its first try; after that it tries every
500 ticks while citizens < max(4, beds)). Full housing: no child within 2000 ticks. After a second level 2 residence is added, two children
are born (about 600 ticks) and growth stops at 4.

---

## COL-04 Territory grows with buildings

**Tests:** buildings extend the colony's claim, and the claim shrinks again when they are removed.

**Setup**
- Colony with a town hall only. The test records the claimed chunks.

**What happens**
1. A guard tower (which claims a large radius) is placed near the border.
2. The guard tower is removed again.

**Passes when**
- After placing it, chunks around the tower that were not claimed before belong to the colony.
- After removing it, those extra chunks are released, and chunks claimed by the town hall stay claimed.

**Time budget:** 10 seconds.

**Implemented:** `ColonyConnectionsRobustnessTests.col04_territoryGrowsWithBuildings`.

**As implemented:** with the default `initialColonySize` (4) the whole arena is already claimed, so the test founds the colony with
`initialColonySize` 1. A hut can only be placed inside the claim, so the level 1 guard tower (claim radius 2) stands inside it near its
border (46, 5, 46). The tower is removed by the owner breaking its hut block (BreakEvent -> `building.destroy()`).

---

## COL-05 Colony protection and permissions

**Tests:** outsiders can't grief a colony, and members can do what their rank allows.

**Setup**
- Colony owned by the fake owner, plus three more fake players: an officer, a friend (low rank) and an outsider.
- A hut and a chest inside the colony.

**What happens**
1. Each fake player tries to break a block, place a block, open the chest and open the hut's menu.

**Passes when**
- The owner and officer can do all of it.
- The friend can do only what the friend rank allows by default.
- The outsider is blocked from all of it, and the blocks are unchanged.
- With `enableColonyProtection` off (second run), the outsider is no longer blocked.

**Time budget:** 10 seconds.

**Implemented:** `ColonyConnectionsRobustnessTests.col05_protectionAndPermissions` and `ColonyConnectionsRobustnessTests.col05b_friendOpensHut`
(was bug E2E-COL-05-1, fixed; now required), plus `ColonyConnectionsRobustnessTests.col05c_outsiderItemActionsRefused` for item actions.

**As implemented:** the players are Fabric fake players acting through `ServerPlayerGameMode` (`destroyBlock`, `useItemOn`), so the
Fabric events and the shim's NeoForge events fire as for a real player. Break/place are judged by the block state, chest and hut by the
interaction result (`Fail` = refused). Findings that changed the expectations:
- The friend rank may not break, place or open containers. It has `ACCESS_HUTS`, but huts expose an item handler, so the container check
  refused it with `OPEN_CONTAINER` (bug E2E-COL-05-1, fixed: the container check now skips huts; tested in COL-05b).
- With `enableColonyProtection` off the outsider can break, place and open the chest, but hut menus stay rank protected (the
  `ACCESS_HUTS` checks in `EventHandler` and `ColonyPermissionEventHandler` don't look at the config).

**COL-05c Item actions** (`col05c_outsiderItemActionsRefused`): a friend and an outsider stand in the colony. Each drinks a water potion
(`ServerPlayerGameMode#useItem`, refused as `THROW_POTION`), shoots a bow and the pharao scepter at full charge with one arrow in the
inventory (`releaseUsing`, refused as `SHOOT_ARROW` through `ArrowLooseEvent`) and touches a diamond on the ground (`ItemEntity#playerTouch`,
refused as `PICKUP_ITEM` through `ItemEntityPickupEvent.Pre`). Passes when the friend can do all four (he starts drinking, an arrow and a
fire arrow fly, the diamond is in his inventory), the outsider none of them, and the town hall lists the outsider's `THROW_POTION`,
`SHOOT_ARROW` and `PICKUP_ITEM` denials. Time budget: 5 seconds (done in the first tick). The scepter part failed before the port fix (bug E2E-COL-05-2): its
`use`/`releaseUsing` overrides had lost the `ArrowNockEvent`/`ArrowLooseEvent` calls, so outsiders could shoot it in a colony.

---

## COL-06 Save and reload a colony

**Tests:** a busy colony comes back exactly as it was after saving and loading.

**Setup**
- Colony with several huts, hired citizens, open requests, an unfinished work order and research in progress.

**What happens**
1. The test saves the colony data, unloads the colony and loads it again from the saved data.

**Passes when**
- Buildings, levels, citizens (names, jobs, homes, skills, saturation, happiness), requests, work orders and research progress are
  the same as before.
- Citizens keep working afterwards (at least one worker returns to its job within the budget).

**Time budget:** 30 seconds.

**Implemented:** `ColonyConnectionsRobustnessTests.col06_saveAndReload`.

**As implemented:** a server restart is not possible inside a game test run, so the test reloads one colony in place: `getColonyTag()`
(what the world save writes), citizen entities saved to NBT and unloaded, the colony removed from the save data and its event handler
unregistered, a new `Colony.loadColony(tag)` added and `onWorldLoad` called (what a server start does), hut block entities pointed at the
new objects, citizen entities loaded back from their NBT. Compared before/after: buildings (type, level, assigned citizens), citizens
(name, gender, job, work, home, skills, saturation, happiness recomputed from the stored modifiers - the cached value is reset on both
sides), work orders, open request tokens, research state and progress, claimed chunks. Then all citizens have entities again and the
lumberjack's worker AI runs.

---

## COL-07 Delete a colony

**Tests:** deleting a colony removes it cleanly.

**Setup**
- Colony with huts and citizens.

**What happens**
1. The test deletes the colony through the same code as the delete command, keeping the buildings standing.

**Passes when**
- The colony manager no longer has it.
- All its citizen entities are gone.
- Its chunks are no longer claimed.
- A new colony can be founded in the same place afterwards.

**Time budget:** 10 seconds.

**Implemented:** `ColonyConnectionsRobustnessTests.col07_deleteColony` (uses `deleteColonyByDimension`, the call of the delete command).
