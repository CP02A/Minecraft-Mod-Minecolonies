# Upstream issues: worker jobs

Bugs found by the Fabric port's end-to-end tests (see [../e2e-tests/PROTOCOL.md](../e2e-tests/PROTOCOL.md)) in code the port shares
with MineColonies. Each section is written so it can be filed as an upstream issue as it stands. Line numbers refer to MineColonies
`version/1.21` at `d66bdaae3a` ("Packet range (#11888)"), the branch the port is based on; the Forge 1.20.1 branch `version/main`
(`b2fb29ec11`) has the same code, its line numbers are given as extra info. The Structurize one refers to Structurize `version/1.21` at
`9ad76cfd3a` (the copy vendored by the port). The same changes are applied in the port; the e2e test that covers each one is named at the
end of each section.

---

## 1. Crusher crafts a request again before it is closed (wastes ingredients)

**Version / branch:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`). Intermittent.

**Steps to reproduce**
1. Build a crusher (level 1) with a worker; set its daily limit to 0 so it only works on requests.
2. Put 8 cobblestone into the crusher's hut and request 2 gravel from another hut (e.g. a builder needing 2 gravel; recipe: 2 cobblestone
   → 1 gravel).
3. Watch the crusher's inventory and the hut.

**Expected:** 4 cobblestone are used, 2 gravel are delivered, the request is done.

**Actual:** often (about 2 of 3 tries) all 8 cobblestone are used: the requester gets 2 gravel and 2 more stay with the crusher. Sometimes the
requester gets no gravel at all while the crusher holds it.

**Root cause:** `EntityAIWorkCrusher.craft()` (`core/entity/ai/workers/crafting/EntityAIWorkCrusher.java:223-236`; `version/main`
226-239). When the crafting
count is reached it resets the counters (`resetValues()`) and only finishes the request if `inventoryNeedsDump()` happens to be true;
otherwise it stays in `CRAFT` with the request still open. The next `decide()` → `GET_RECIPE` sees the request again (the gravel already in
the inventory counts as done, but `crush()` crafts whenever the progress is full, regardless of the counter) and crafts more; in `CRAFT`
the daily limit check (line 120) is skipped. `AbstractEntityAICrafting` does it differently: after the last craft it returns
`finalizeCraftingTask()` → `INVENTORY_FULL`, and `afterDump()` closes the request.

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/crafting/EntityAIWorkCrusher.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/crafting/EntityAIWorkCrusher.java
@@ craft()
             if (job.getCraftCounter() >= job.getMaxCraftingCount())
             {
                 incrementActionsDone(getActionRewardForCraftingSuccess());
-                currentRecipeStorage = null;
                 worker.decreaseSaturationForAction();
-                resetValues();
-                if (inventoryNeedsDump())
-                {
-                    if (job.getMaxCraftingCount() == 0 && job.getProgress() == 0 && job.getCraftCounter() == 0 && currentRequest != null)
-                    {
-                        job.finishRequest(true);
-                    }
-                }
+                // Dump now: afterDump() finishes the request. Staying in CRAFT with the request still open would craft it again.
+                return finalizeCraftingTask();
             }
```

**E2E test:** `LogisticsCraftingTests.crf01_crusher`.

---

## 2. Enchanter drains himself instead of the workers of his stations

**Version / branch:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`).

**Steps to reproduce**
1. Build an enchanter's tower (level 1) with an enchanter, and a lumberjack's hut with a worker.
2. In the enchanter's tower, add the lumberjack's hut as a station to drain.
3. Give the enchanter a book; watch whom he drains (particles go from the drained citizen to the enchanter).

**Expected:** the enchanter walks to the lumberjack's hut and drains the lumberjack (who gets the "random enchant" item reward).

**Actual:** the enchanter walks to the station and drains himself: the particle stream starts and ends at him, the enchant lands in his
own inventory, and the station's worker is never touched.

**Root cause:** `EntityAIWorkEnchanter.gatherAndDrain()` (`core/entity/ai/workers/service/EntityAIWorkEnchanter.java:324`; `version/main` 329) picks the
citizen from `getModuleForJob().getAssignedEntities()`. `getModuleForJob()` is the enchanter's own work module (`job.getWorkModule()`), so
the only candidate is the enchanter. It should pick from the workers of `buildingWorker`, the station it just walked to.

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/service/EntityAIWorkEnchanter.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/service/EntityAIWorkEnchanter.java
@@
 import com.minecolonies.api.colony.buildings.IBuilding;
+import com.minecolonies.api.colony.buildings.modules.IAssignsJob;
@@ gatherAndDrain()
             final List<AbstractEntityCitizen> workers = new ArrayList<>();
-            for (final Optional<AbstractEntityCitizen> citizen : getModuleForJob().getAssignedEntities())
+            for (final IAssignsJob module : buildingWorker.getModulesByType(IAssignsJob.class))
             {
-                citizen.ifPresent(workers::add);
+                for (final Optional<AbstractEntityCitizen> citizen : module.getAssignedEntities())
+                {
+                    citizen.ifPresent(workers::add);
+                }
             }
```

**E2E test:** `LogisticsCraftingTests.crf07_enchanterDrainsStationWorker`.

---

## 3. Miner never seals water or lava next to a mine node

**Version / branch:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`).

**Steps to reproduce**
1. Have a miner with a finished first mining level and cobblestone in the hut.
2. Next to the open end of the next node the miner will build (within 4 blocks of the node), place a water (or lava) source, as a flooded
   cave would be.
3. Let the miner build that node.

**Expected:** before building the node the miner replaces liquid sources around it with his fill block (the code for this exists:
"Check for liquids").

**Actual:** the source stays; water flows into the tunnels (lava can burn the miner).

**Root cause:** `EntityAIStructureMiner.executeStructurePlacement` (`core/entity/ai/workers/production/EntityAIStructureMiner.java:790-818`;
`version/main` 788-816).
The liquid check comes after the "preload structures" block. On the first call the blueprint is not loaded, so the method starts loading it
and returns `LOAD_STRUCTURE` (line 798). After loading, `afterStructureLoading()` goes to `BUILDING_STEP` and the AI does not come back
to this method before the node is done (`executeSpecificCompleteActions` clears the blueprint), so the check never runs.

**Suggested patch** (`version/1.21`; on `version/main` `initStructure` has an extra `rotation` argument): run the check before loading
the blueprint.
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/production/EntityAIStructureMiner.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/production/EntityAIStructureMiner.java
@@ executeStructurePlacement(...)
         mineNode.setStatus(MineNode.NodeStatus.IN_PROGRESS);
         building.markDirty();
-        //Preload structures
-        if (building.getWorkOrder() == null || building.getWorkOrder().getBlueprint() == null)
-        {
-            initStructure(mineNode,
-              new BlockPos(mineNode.getX(), building.getFirstModuleOccurance(MinerLevelManagementModule.class).getCurrentLevel().getDepth(), mineNode.getZ()),
-              building,
-              world,
-              job);
-            return LOAD_STRUCTURE;
-        }
-
         //Check for liquids
         for (int x = -NODE_DISTANCE / 2 - 1; x <= NODE_DISTANCE / 2 + 1; x++)
         {
             ...
         }
 
+        //Preload structures (after the liquid check: once loaded, the AI builds the node and doesn't come back here)
+        if (building.getWorkOrder() == null || building.getWorkOrder().getBlueprint() == null)
+        {
+            initStructure(mineNode,
+              new BlockPos(mineNode.getX(), building.getFirstModuleOccurance(MinerLevelManagementModule.class).getCurrentLevel().getDepth(), mineNode.getZ()),
+              building,
+              world,
+              job);
+            return LOAD_STRUCTURE;
+        }
+
         workingNode = null;
```

**E2E test:** `FarmingGatheringTests.gat04_minerSealsWater`.

---

## 4. Nether worker stands at the portal for up to 3 days after an expedition

**Version / branch:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`).

**Steps to reproduce**
1. Build a nether mine (level 1) with a worker, research "nether expedition log", give the hut the trip's inputs, tools, armor and food.
2. Let the worker go through the portal and wait the travel time.
3. Watch the worker after he reappears at the portal, and the hut's expedition log.

**Expected:** the worker works off the trip (fights, finds), walks home and puts the loot into the hut; the log says "completed".

**Actual:** he stands idle at the portal. The log stays "in progress" and the loot stays in the job until the hut allows the next trip (one
trip per 3-day period, reset in the morning), i.e. up to 3 colony days.

**Root cause:** the respawned worker's AI starts in `IDLE`, which `AbstractEntityAICrafting` only leaves when `hasWorkToDo()`
(`core/entity/ai/workers/crafting/AbstractEntityAICrafting.java:162`; `version/main` 164). `EntityAIWorkNether.hasWorkToDo()`
(`core/entity/ai/workers/production/EntityAIWorkNether.java:156`; `version/main` 149) is "crafting task or `building.isReadyForTrip()`"; the trip was just
recorded, so it is false and `decide()` never reaches its `job.isInNether()` → `NETHER_AWAY` branch.

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/production/EntityAIWorkNether.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/production/EntityAIWorkNether.java
@@ hasWorkToDo()
-        return super.hasWorkToDo() || building.isReadyForTrip();
+        // A worker back from a trip still has its results to work off (decide() -> NETHER_AWAY), even if the next trip isn't allowed yet.
+        return super.hasWorkToDo() || building.isReadyForTrip() || job.isInNether();
```

**E2E test:** `FarmingGatheringTests.gat06_netherWorkerReturnsAfterTravel`.

---

## 5. Herder (and beekeeper) feeds animals that are too far apart to mate

**Version / branch:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`).

**Steps to reproduce**
1. Build a chicken herder's hut with a worker and wheat seeds.
2. In its area, put two chickens 9 blocks apart that can't reach each other quickly (e.g. separated by a fence, or slowed).

**Expected:** the herder only feeds a pair that can find each other; vanilla's `BreedGoal` looks for a partner within 8 blocks.

**Actual:** the herder feeds both; they are in love but never look for each other (more than 8 blocks), the love runs out after 30 s, the
food is wasted and the herder waits 60 s (`breedTimeOut`) before the next try. In a normal pen this shows as breeding that sometimes takes
much longer than it should.

**Root cause:** `AbstractEntityAIHerder.DISTANCE_TO_BREED = 10`
(`core/entity/ai/workers/production/herders/AbstractEntityAIHerder.java:78`, used at line 454; `version/main` 76/452) is larger than vanilla's mate search range
(`BreedGoal.PARTNER_TARGETING` range 8, box inflated by 8). `EntityAIWorkBeekeeper` has the same constant (line 79, used at 287; bees use
`BreedGoal` too).

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/production/herders/AbstractEntityAIHerder.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/production/herders/AbstractEntityAIHerder.java
     /**
-     * Distance two animals need to be inside to breed.
+     * Distance two animals need to be inside to breed. Vanilla BreedGoal only looks for a partner within 8 blocks.
      */
-    private static final int DISTANCE_TO_BREED = 10;
+    private static final int DISTANCE_TO_BREED = 8;
--- a/src/main/java/com/minecolonies/core/entity/ai/workers/production/agriculture/EntityAIWorkBeekeeper.java
+++ b/src/main/java/com/minecolonies/core/entity/ai/workers/production/agriculture/EntityAIWorkBeekeeper.java
     /**
-     * Distance two animals need to be inside to breed.
+     * Distance two animals need to be inside to breed. Vanilla BreedGoal only looks for a partner within 8 blocks.
      */
-    private static final int DISTANCE_TO_BREED = 10;
+    private static final int DISTANCE_TO_BREED = 8;
```

**E2E test:** `FarmingGatheringTests.frm06_herderFeedsPairOutOfMateRange` (herder only; the beekeeper change is untested).

---

## 6. Florist: compost on a bed that sat idle expires at once

**Version / branch:** `version/1.21` (`d66bdaae3a`); also `version/main` (`b2fb29ec11`), same line numbers.

**Steps to reproduce**
1. Build a flower shop with a florist and several plant beds; give the florist one compost per bed, then let the beds sit without compost
   for more than 5 minutes (e.g. the florist ran out of compost).
2. Give the florist compost again and watch the first bed he composts.

**Expected:** the compost lasts 300 s (`TICKER_LIMIT`) and flowers grow on the bed during that time.

**Actual:** the bed loses its compost at its first update (within 1 s); the compost is wasted. The florist always composts the first
non-composted bed, so he composts the same bed twice before moving on.

**Root cause:** `TileEntityCompostedDirt.tick()` (`core/tileentities/TileEntityCompostedDirt.java:75`) increments `ticker` on every tick,
also while the bed is not composted. The reset (`ticker >= TICKER_LIMIT * TICKS_SECOND` → `composted = false`, line 122) runs in
`updateTick` only while composted, so after a long idle time the first update ends the compost. `compost()` (line 158) doesn't reset the
timer.

**Suggested patch**
```diff
--- a/src/main/java/com/minecolonies/core/tileentities/TileEntityCompostedDirt.java
+++ b/src/main/java/com/minecolonies/core/tileentities/TileEntityCompostedDirt.java
@@ compost(...)
         }
+        // The ticker also counts while not composted; restart it so the new compost lasts the full TICKER_LIMIT.
+        this.ticker = 0;
         this.composted = true;
     }
```

**E2E test:** `FarmingGatheringTests.frm03_compostLastsOnIdleBed`.

---

## 7. Material list (and builder's request list) misses blueprint entities that share a block (Structurize)

**Where to file:** Structurize (the counting code is there); affects MineColonies' build/decoration windows and builders.
**Version / branch:** Structurize `version/1.21` at `9ad76cfd3a` (vendored by the port; MineColonies `version/1.21` builds against
Structurize `1.0.837-1.21.1-snapshot`). In MineColonies `version/1.21`, `WindowBuildBuilding#updateResources` (line 335),
`WindowBuildDecoration#updateResources` (line 232) and the builder's `ENTITIES` request stage
(`AbstractEntityAIStructureWithWorkOrder.java:313`) all use this `StructurePlacer` `GET_RES_REQUIREMENTS` count. The 1.20.1 Structurize
used by `version/main` (`1.20.1-1.0.825`) was not checked.

**Steps to reproduce**
1. Open the build tool and choose `decorations/statues/pedastal_oak` of the default style (it has 4 entities; item frames holding items).
2. Look at the material list in the decoration window: 3 item frames, 6 torches.
3. Give a builder exactly that list and order the decoration.

**Expected:** the list contains every item the build needs; the builder finishes.

**Actual:** the list is one item frame and one torch short; the builder stops at the end (`NEEDS_ITEM`, "1x torch, 1x item_frame") and
requests them one by one.

**Root cause:** `StructurePlacer.handleEntitySpawn(..., simulate = true)` (`placement/StructurePlacer.java:346-437`, the `simulate` branch at
line 407) returns `MISSING_ITEMS` with the first entity's items. Entities are grouped by block position, so a second entity on the same
block (e.g. two item frames on different faces of one block) is never counted; for `pedastal_oak` that is one item frame with a torch. `getResourceRequirements` (line 522) uses only that result.

**Suggested patch** (Structurize)
```diff
--- a/src/main/java/com/ldtteam/structurize/placement/StructurePlacer.java
+++ b/src/main/java/com/ldtteam/structurize/placement/StructurePlacer.java
@@ public BlockPlacementResult handleEntitySpawn(...)
     {
+        // Several entities can share a block position (e.g. two item frames on different faces): collect the items of all of them.
+        final List<ItemStack> simulatedItems = new ArrayList<>();
         for (final CompoundTag compound : this.iterator.getBluePrintPositionInfo(localPos).getEntities())
@@
-                                if (simulate || !this.handler.hasRequiredItems(requiredItems))
+                                if (simulate)
+                                {
+                                    simulatedItems.addAll(requiredItems);
+                                    continue;
+                                }
+
+                                if (!this.handler.hasRequiredItems(requiredItems))
                                 {
                                     return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.MISSING_ITEMS, requiredItems);
                                 }
@@
         }
 
+        if (!simulatedItems.isEmpty())
+        {
+            return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.MISSING_ITEMS, simulatedItems);
+        }
         return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.SUCCESS);
```

**E2E test:** `ConstructionResearchTests.bld07_decorationWithEntitiesMaterialList`.
