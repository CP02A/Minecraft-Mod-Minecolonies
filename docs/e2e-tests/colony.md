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
