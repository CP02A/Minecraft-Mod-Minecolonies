[← Back to the test overview](../../E2E-tests.md) · [Farming tests](../jobs-farming.md) · [Gathering tests](../jobs-gathering.md)

# Protocol: farming and gathering tests

Suite: `src/gametest/java/com/minecolonies/gametest/suites/FarmingGatheringTests.java`, run with
`./gradlew --no-daemon runGameTest -Pe2eFilter=farming_gathering_tests`. Last two runs: all 21 required tests passed; optional: the two bug tests fail, the flaky swine breeding test passed in one of the two. GAT-03..06 added later (4 required, 3 optional bug tests): passed in 5 GAT runs and the full run, the 3 bug tests failed every time. In that full run `frm06_chickenHerderBreeds` timed out once (no baby in 6000 ticks). Cause found later: bug E2E-FRM-06-1 plus the test's slowed animals (see below); fixed on the test side, and `frm06_swineHerderBreeds` is required again. Repeated runs after the fix: chicken 200 of 200 (at most 699 ticks, before: up to 5,148), swine 100 of 100 (at most 1,194). Runtimes are the game tick of each test's last step in that run. They vary between runs because the AI uses
random numbers.

| ID | Test method(s) | Status | Runtime (ticks) | Notes |
|---|---|---|---|---|
| FRM-01 | `frm01_farmerFieldCycle` | ✅ | 12010 | Field on dirt. The farmer works a field once per colony day, so the test lets nights pass while he idles. |
| FRM-01 | `frm01_farmerTillsGrass` | ✅ | 5440 | Was optional (E2E-FRM-01-1, grass never tilled); bug FIXED, test required. |
| FRM-01 | `frm01_farmerTillsOtherSoils` | ✅ | 5765 | Field of coarse dirt, rooted dirt, podzol, mycelium (in turn); all tilled and planted, as on 1.21.1 (added with the E2E-FRM-01-1 fix). |
| FRM-02 | `frm02_planterSugarCane`, `frm02_planterCactus`, `frm02_planterBamboo` | ✅ | 6135 / 6145 / 12190 | One test per field type (level 1 owns one field). Bamboo is slow and leaves one position very long; the test accepts all but one position. Final step: wait budget 20,000 → 40,000 (bamboo took 11,200-19,700 ticks and once ran out). |
| FRM-03 | `frm03_florist` | ✅ | 10620 | Level 2 shop, "don't grow" list, compost at 3 per bed, double-speed beds. Observation E2E-FRM-03-1. |
| FRM-04 | `frm04_composter` | ✅ | 370 | Exact seed use checked; apples (not allowed) untouched. |
| FRM-05 | `frm05_beekeeper` | ✅ | 740 | Honeycomb, then honey (the "both" mode needs research). Dexterity 99. |
| FRM-06 | `frm06_*Breeds*` (chicken, cow, sheep, rabbit) | ✅ | 349 / 1014 / 660 / 479 | Baby born + egg picked up from the ground (the item entity is gone; in one run it was afterwards in neither the hut nor the herder, not investigated) / milk (large bottle) / wool. |
| FRM-06 | `frm06_swineHerderBreeds` | ✅ | 459 (median of 100) | Was optional and flaky: the pigs drifted apart and the herder fed them anyway (E2E-FRM-06-1). Required again since the test keeps the pair within 6 blocks. |
| FRM-06 | `frm06_herderFeedsPairOutOfMateRange` (optional) | ❌ mod bug | ~400 | E2E-FRM-06-1: two chickens that can't move, 9 blocks apart; the herder puts both in love ("both in love 9.3 blocks apart"). Failed 22 of 22 runs. |
| FRM-06 | `frm06_*ButchersAboveLimit` (chicken, cow, sheep, pig, rabbit) | ✅ | 955 / 1405 / 840 / 1005 / 1265 | 6 adults → at most 3 adults, drops in hut, outsider alive. Limit is soft by design. |
| FRM-07 | `frm07_fisherman` | ✅ | 1250 | Was optional (E2E-FRM-07-1, every catch threw); bug FIXED, test required. Loot after 895 ticks, rod breaks and is requested after 1250. |
| GAT-01 | `gat01_lumberjackFellsAndReplants` | ✅ | 8440 | One oak + one birch, defoliation on. |
| GAT-02 | `gat02_lumberjackFollowsSettings` | ✅ | ~5650 | 3245 until the oak is replanted, then a fixed 2400-tick wait (it checks that something does *not* happen). |
| GAT-03 | `gat03_minerDigsShaft` | ✅ | 18630 / 18795 | Deep arena, level 1 mine; the first level is built right under the hut (y 6), not at y -16. Owner fulfils the main shaft's framed blocks etc. (`OwnerFulfils`). |
| GAT-03 | `gat03_minerDigsShaftSafely` (optional) | ❌ mod bug | fails at about 7500-10200 | E2E-GAT-03-1: the miner falls off the shaft ladder (3.3-4 blocks, 1 damage). Intermittent: passed in some of the final-step full runs. |
| GAT-04 | `gat04_minerMinesNodes` | ✅ | 31310 / 31885 | First level as in GAT-03 (~18000), then max depth = that level; two tunnel nodes built (~13500), supports, cave hole closed, iron ores in the hut. |
| GAT-04 | `gat04_minerSealsWater` (optional) | ❌ mod bug | 24975 / 28625 (check fails) | E2E-GAT-04-1: the water source next to the first node is never removed. |
| GAT-05 | `gat05_quarrierDigsLayers` | ✅ | 28990 / 24910 (budget raised to 60,000 after a run that needed more than 34,000) | Simple quarry on stone; top layer is open from the hut paste, the quarrier digs the next two (~235 blocks each), top down, nothing outside. |
| GAT-06 | `gat06_netherExpedition` | ✅ | 7280 | Simulated trip (no real nether): leaves after ~1250, travel skipped, respawn after ~4900 (colony's 5-minute citizen check), four `onWakeUp` calls (E2E-GAT-06-1), 3 fights, 10 loot types, loot in the hut. |
| GAT-06 | `gat06_netherWorkerReturnsAfterTravel` (optional) | ❌ mod bug | 10083 (timeout) | E2E-GAT-06-1: the returned worker idles until the next trip is allowed. |

## Bugs found

### E2E-GAT-03-1 Miner falls off the shaft ladder

- **Severity:** medium: fall damage (1 heart at 4 blocks) every time the miner climbs out of the shaft at the wrong moment; deeper shafts
  could hurt more.
- **Port-specific: unclear.** The miner AI only asks to walk to the hut (`AbstractEntityAIBasic.walkToBuilding`, from
  `EntityAIStructureMiner.startWorkingAtOwnBuilding` / NEEDS_ITEM); the fall happens in the citizen's path following on the ladder
  (MineColonies pathfinding, which the port changed in places, e.g. `PathfindingUtils.blocksMotion`). Not checked against upstream.
- **Evidence:** `gat03_minerDigsShaftSafely` logs "[MINE] tick N: falling ..." every tick of the fall: at about tick 10,150 the miner
  (worker AI NEEDS_ITEM: his shovel broke at the shaft bottom, he climbs up to fetch a new one) is at y 7.43 next to the ladder (ladder
  x, ladder z + 1, over the open shaft), not on a climbable block, falls to y 4 (fall distance 4.06) and takes 1 damage. Same tick range in
  4 of the first 5 runs; in run 4 a 3.35-block fall at tick 7,492.
- **Reproduce:** `-Pe2eFilter=minecolonies-gametest:farming_gathering_tests_gat03_miner_digs_shaft_safely`.

### E2E-GAT-04-1 Miner never removes liquids around a node

- **Severity:** medium: water or lava next to a node is not sealed and flows into the tunnels (lava: the miner can burn).
- **Likely upstream too** (code unchanged by the port). `EntityAIStructureMiner.executeStructurePlacement`
  (`EntityAIStructureMiner.java:794-820`) checks a 9x9x7 box around the node for liquid sources and replaces them with the fill block,
  but only when the node's blueprint is already loaded. On the first call it starts loading the blueprint and returns LOAD_STRUCTURE
  (line 801); after loading, the AI goes on with BUILDING_STEP (`afterStructureLoading`) and does not come back before the node is done
  (`executeSpecificCompleteActions` clears the blueprint), so the check is not reached.
- **Evidence:** `gat04_minerSealsWater`: water source one block past the open end of the first node (inside the checked box); after the
  node is COMPLETED: "at the source Block{minecraft:water}[level=0], fluid within 4 blocks true" (3 runs).
- **Reproduce:** `-Pe2eFilter=minecolonies-gametest:farming_gathering_tests_gat04_miner_seals_water`.

### E2E-GAT-06-1 Nether worker idles after returning until the next trip is allowed

- **Severity:** medium: an expedition takes up to 3 colony days instead of the travel time; the worker stands at the portal, the log
  says IN_PROGRESS and the loot stays in the job meanwhile.
- **Likely upstream too** (code unchanged by the port). The respawned worker's AI starts in IDLE; `AbstractEntityAICrafting` leaves IDLE
  only when `hasWorkToDo()` (`AbstractEntityAICrafting.java:162`), and `EntityAIWorkNether.hasWorkToDo` (`EntityAIWorkNether.java:156`) is
  "crafting task or `building.isReadyForTrip()`". The trip was just recorded (`BuildingNetherWorker.recordTrip`, 1 trip per 3-day
  period, reset in `onWakeUp`), so `decide()` with its "`job.isInNether()` → NETHER_AWAY" branch is not reached until the period resets.
- **Evidence:** `gat06_netherWorkerReturnsAfterTravel`: 4000 ticks after the respawn: "log IN_PROGRESS, job in nether true, trip
  results to work off 40, worker AI IDLE, job status IDLE". With four `onWakeUp` calls (required `gat06_netherExpedition`) the worker
  carries on at once and the trip completes within about 1000 ticks.
- **Reproduce:** `-Pe2eFilter=minecolonies-gametest:farming_gathering_tests_gat06_nether_worker_returns_after_travel`.

### E2E-FRM-01-1 Farmer never tills grass blocks

- **Status: FIXED** (branch `worktree-agent-a0f8cd80cd6f3aae7`). The checks now use `BlockTags.SUBSTRATE_OVERWORLD` (26.3: `#dirt`, `#mud`,
  `#moss_blocks`, `#grass_blocks`, the same blocks as 1.21.1's `#minecraft:dirt`) in `EntityAIWorkFarmer.findHoeableSurface` and in the
  two other `BlockTags.DIRT` uses (vendored Structurize: `PlacementHandlers.GrassPlacementHandler`, `BlockUtils` fancy-placement dirt
  match). `frm01_farmerTillsGrass` is required and passes (5,440 ticks).
  Coarse and rooted dirt: the farmer never used the hoe's block transformation (upstream's `getToolModifiedState` check in
  `findHoeableSurface` is unreachable, the port's `TURNS_INTO_FARMLAND` check too); any soil in the tag is set straight to farmland by
  `createCorrectFarmlandForSeed`, so the tag fix restores 1.21.1 behaviour for them as well. Covered by `frm01_farmerTillsOtherSoils`.

- **Severity:** high: a field placed on normal grass is never worked; the farmer cycles the field EMPTY → HOED → PLANTED without doing
  anything and idles. Works on plain dirt.
- **Port-specific** (Minecraft 26.3 change). `EntityAIWorkFarmer.findHoeableSurface` (`EntityAIWorkFarmer.java:337`) only accepts
  `BlockTags.DIRT`, farmland or MineColonies farmland. In 26.3 the vanilla `#minecraft:dirt` tag is `dirt, coarse_dirt, rooted_dirt` only
  (grass is in `#minecraft:substrate_overworld` / `maintains_farmland`); in 1.21.x it contained `grass_block`, so upstream works.
  Other `BlockTags.DIRT` uses in the port may have the same problem.
- **Evidence:** `frm01_farmerTillsGrass` times out with "48 cells: farmland 0, ..., field stage HOED", farmer idle with hoe and 64 seeds;
  the same test on dirt (`frm01_farmerFieldCycle`) tills all 48 cells.
- **Reproduce:** `-Pe2eFilter=minecolonies-gametest:farming_gathering_tests_frm01_farmer_tills_grass`.

### E2E-FRM-07-1 Fisherman throws on every catch, never gets loot

- **Status: FIXED** (branch `worktree-agent-a0f8cd80cd6f3aae7`). FISHING loot params built like vanilla 26.3 `FishingHook` (origin, tool,
  this entity, luck) in `NewBobberEntity.retrieve` and in the same mistake in `EntityAIWorkFisherman.generateBonusLoot`. The
  `minecolonies:research_unlocked` loot condition (used by the fisherman loot table) read the citizen from `ATTACKING_ENTITY`; it now
  also resolves a projectile `THIS_ENTITY` (the bobber) to its owner. `frm07_fisherman` is required and passes (1,250 ticks).

- **Severity:** high: the fisherman job produces nothing.
- **Port-specific** (Minecraft 26.3 change). `NewBobberEntity.retrieve` (`NewBobberEntity.java:519-525`) builds the FISHING loot params with
  `LootContextParams.ATTACKING_ENTITY`; 26.3's `LootContextParamSets.FISHING` doesn't allow that parameter, so `LootParams.Builder.create`
  throws `IllegalArgumentException: Parameters not allowed in this parameter set: [<parameter minecraft:attacking_entity>]`.
- **Evidence:** `latest.log`: "Statemachine for state FISHERMAN_START_FISHING threw an exception" with that message and the stack
  `NewBobberEntity.retrieve:525 ← EntityAIWorkFisherman.retrieveRod:683 ← caughtFish:671 ← doFishing:484`, followed by
  "Pausing Citizen ... (com.minecolonies.job.fisherman) for 100 Seconds", 200, 400 ... 1638400 seconds, all logged within about one
  second (the pause seems not to stop the AI between errors; not investigated further). The fisherman finds the pond on his own; the rod
  wears (damage 60/64) but nothing is caught.
- **Reproduce:** `-Pe2eFilter=minecolonies-gametest:farming_gathering_tests_frm07_fisherman`.

### E2E-FRM-06-1 Herder feeds animals that are too far apart to mate

- **Severity:** low: the breeding food is wasted and the herder waits 60 s (`breedTimeOut`) before the next try. Animals in a normal pen
  wander, so a later try usually works.
- **Likely upstream too** (code unchanged by the port). `AbstractEntityAIHerder.breedAnimals` pairs animals up to `DISTANCE_TO_BREED` = 10
  blocks apart (`AbstractEntityAIHerder.java:78`, used at line 454). Vanilla's `BreedGoal` looks for a partner only within 8 blocks
  (`PARTNER_TARGETING` range 8, box inflated by 8). A pair 8-10 blocks apart is put in love, never meets, and the love runs out after
  600 ticks.
- **Evidence:** the full-run timeout of `frm06_chickenHerderBreeds`. A traced repeat (200 runs, state every 200 ticks) had one run of
  5,148 ticks: both chickens in love (love 540 / 420), 8.09 blocks apart, not moving, no chick; the herder fed them again 2,200 ticks
  later with the same result; the third try worked after one chicken had moved. The optional test
  `frm06_herderFeedsPairOutOfMateRange` (two chickens that can't move, 9 blocks apart) failed 22 of 22 runs with "both in love 9.3
  blocks apart on tick ~395".
- **Test side:** the breeding tests slow the animals, so a pair that drifted apart stayed apart for thousands of ticks.
  `keepWithinMateRange` puts the second animal back next to the first while neither is in love and they are more than 6 blocks apart.
- **Reproduce:** `-Pe2eFilter=minecolonies-gametest:farming_gathering_tests_frm06_herder_feeds_pair_out_of_mate_range`.

### E2E-FRM-03-1 Composted bed expires at once after it sat idle (minor, observation)

- **Severity:** low (each bed needs two composts after idling; the first is wasted).
- **Likely upstream too** (plain MineColonies logic, nothing port-specific). `TileEntityCompostedDirt.tick()` increments `ticker` on every
  tick, also while the bed is not composted; the reset (`ticker >= TICKER_LIMIT * TICKS_SECOND` → `composted = false`) only runs in
  `updateTick` while composted. A bed that sat idle for more than 300 s therefore loses its new compost at the first update. The florist
  always composts the first non-composted bed, so it composts bed 1 twice before moving on.
- **Evidence:** FRM-03 diagnostics via reflection: after 24 composts only the first bed ever had a flower set, `ticker=92049` there and
  `400049` on the never-composted beds; with the beds sped up 50x every compost expired at once and no flower ever grew.
- **Reproduce:** `frm03_florist` with the speed-up loop raised from 1 to 49 extra ticks.

## Design changes

All changes are written into the test sections of `jobs-farming.md` / `jobs-gathering.md` ("As implemented"). The main ones:

- **FRM-01/FRM-02:** field workers work each field once per colony day and idle for the rest of it (`getBuildingExtensionToWorkOn`).
  The tests let nights pass while the worker idles. FRM-01 runs on dirt, the grass variant is the bug test.
- **FRM-02:** one test per plantation field type (a level 1 plantation owns one field); bamboo needs the "jungle master" research.
- **FRM-03:** the hut's flower list is a "don't grow" list; level 2 for more than two flower types.
- **FRM-05:** "both" mode needs research; the test does honeycomb then honey. Angering bees is intended (chance falls with dexterity).
- **FRM-06:** the animal limit is soft: no butchering at 3 or fewer adults, chance (adults / limit)^4 above; breeding ignores the limit. The
  tests check "a baby is born" and "6 adults go down to at most 3", not "exactly the limit". The cowhand milks with large bottles by default.
- **Level 1 huts allow only wood/gold tools** (`IBuilding.getMaxEquipmentLevel`); stone tools are ignored and requested again. Lumberjacks
  with defoliation and farmers wear out several wooden tools per test.
- **GAT-01:** leaves are cut (defoliation) because random ticks are 0; the lumberjack needs hoes for leaves. Trees must be within 50 blocks
  of the hut (search range).
- **GAT-02:** the sapling list is a "don't cut" list.
- **GAT-03:** the first mining level is 5 blocks under the hut's shaft start (then one every 6 blocks down to the hut level's mining
  depth), not at the absolute heights 48/16/-16/-100 (those are the depth limits per hut level). The test waits for the first level.
  Materials the colony can't deliver come from the creative owner (`OwnerFulfils`, like the request window's "fulfil" button) instead
  of the retrying resolver's ~40,000 ticks. Fall/damage part split off as an optional test (bug).
- **GAT-04:** no pre-built shaft: the test runs the GAT-03 mine to its first level, then caps the hut's max depth to keep the miner on it.
  Ores are placed in the first three tunnel nodes; the cave hole is an air pocket in a node's wall corner. Water/lava part split off as an
  optional test (bug).
- **GAT-05:** the quarrier is hired in a mine (quarrier job) and assigned to the quarry; he dumps into the quarry. The quarry hut's
  blueprint already opens the top layer; "first layers" = the next two.
- **GAT-06:** the trip is simulated (entity removed, travelling manager); the test skips the travel time and, because of
  E2E-GAT-06-1, runs the hut's morning update four times. Death is allowed by the rules (log KILLED) but did not happen.

## Framework issues

- **`ColonyFixture.rel()` is not the inverse of `abs()` (fixed later, commit 396b6db):** `rel(abs(new BlockPos(44, 5, 44)))` returned `(-44, 5, -44)` in every run.
  `helper.spawn(type, fixture.rel(absPos))` therefore spawns in the wrong place. Worked around with absolute positions everywhere (own
  `spawnAnimal`).
- **Re-runs of colony tests don't run (fixed later, commit 396b6db):** with `maxAttempts = 3, requiredSuccesses = 1` the log shows "Starting re-run of tests: ..." and
  the run ends within the same second with the first failure. Chance-based colony tests can't use `maxAttempts`.
- **Herder huts at `(12, 5, 12)` reach over the arena wall:** several herder blueprints extend up to 19 blocks to one side of the hut
  block, so the pen hangs outside the arena and animals fall off. The suite uses `(44, 5, 48)` for herder huts and checks the hut area
  is inside the arena.
- **Not every pen is closed:** some herder blueprints have open pens, so animals leave the hut area (the herder only sees animals inside
  the building's corners). The suite closes the area's border with fence gates and slows the animals (`fenceArea`, `spawnAnimal`).
- **Shared scratchpad / git-ignored lang file:** the worktree needed `src/main/resources/assets/minecolonies/lang/en_us.json` copied from
  the main checkout (git-ignored, server NPE otherwise), and the session scratchpad is shared between agents, so logs went to `build/`.
- **Mine tests need room for the dug blocks:** a level 1 mine's own rack fills up during the shaft; the suite adds four racks next to the
  hut (`addRack`, registered like player-placed racks).
- Helpers that could move to the framework: `OwnerFulfils` (creative owner fulfils requests the colony can't), `addRack`, `spawnAnimal` (absolute), `DayPump` (pass nights while a worker idles), `describeWork`
  (inventory + open requests in failure messages), `growTree` (vanilla tree grower), `fenceArea`.
