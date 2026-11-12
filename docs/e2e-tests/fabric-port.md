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
