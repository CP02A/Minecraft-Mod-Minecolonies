[← Back to the test overview](../../E2E-tests.md) · [Logistics](../logistics.md) · [Crafting](../jobs-crafting.md)

# Protocol: logistics and crafting tests

Suite class: `src/gametest/java/com/minecolonies/gametest/suites/LogisticsCraftingTests.java`
(filter `-Pe2eFilter=logistics_crafting_tests`). Final run: 31 required tests passed; optional (bug) tests: LOG-06 and CRF-07 drain target failed, the crusher
(intermittent) passed; whole suite about 1-2 minutes real time (LOG-04 runs about 40,500 game ticks, CRF-06 21,000-57,000).

Runtime = game ticks of the main wait in the final run (ticks vary from run to run by roughly ±30 %).

| ID | Status | Runtime (ticks) | Notes |
|---|---|---|---|
| LOG-01 Warehouse delivers a requested item | ✅ | 500 | Resolved by `WarehouseConcreteRequestResolver` (stack of one item). Building request from a hut without a worker. |
| LOG-02 Couriers bring products to the warehouse | ✅ | 510 | Pickup started like the "force pickup" button; axe and saplings stay in the hut. |
| LOG-03 Crafting chain | ✅ | 900 | Sticks request → sawmill `PublicWorkerCraftingRequestResolver`, child crafting request for planks; exactly 1 log used. |
| LOG-04 Unfulfillable request goes to the player | ✅ | 40,455 | Retrying resolver for ~40,000 ticks (3 retries x 1200 RS ticks x 11), then player resolver; diamond put into a warehouse rack (not the postbox) → warehouse → courier. |
| LOG-05 Minimum stock | ✅ | 1,345 | Minimum stock is in stacks: 1 stack = 64 torches; refilled after 20 removed; never above 64. |
| LOG-06 Tools by hut level | ❌ | (timeout 3,500) | BUG E2E-LOG06-1: orphaned delivery, miner never gets a pickaxe. `required = false`. |
| LOG-07 Several couriers share the work | ✅ | 900 | 5 postboxes x 2 requests, 3 couriers (each in its own courier's hut), level 2 warehouse. |
| LOG-08 Requests cancelled when a hut is removed | ✅ | 515 | Hut broken while the courier carries items; requests gone, items back in the warehouse, no references left. |
| LOG-09 Full warehouse | ✅ | 745 | Courier keeps the items, `lastNotification` set; after a rack is added the items are stored. Final step: budget 3,000 → 8,000 (the courier once went on a random 3-minute leisure break). |
| LOG-10 Stash | ✅ | 450 | |
| CRF-01 Sawmill (oak log → planks) | ✅ | 1,395 | |
| CRF-01 Stonemason (stone → stone bricks) | ✅ | 1,480 | |
| CRF-01 Blacksmith (iron pickaxe) | ✅ | 5,090 | Slowest crafter (1,630 in an earlier run). |
| CRF-01 Fletcher (arrows) | ✅ | 1,365 | Once still not delivered after 5,500 ticks (courier still on his way); all crafter budgets raised to 11,000. |
| CRF-01 Baker (wheat → dough → bread) | ✅ | 1,955 | Built-in recipe chain, coal fuel. |
| CRF-01 Kitchen (chef) | ✅ | 1,200-1,620 | Stew trencher (built-in, needs a level 4 kitchen); kitchen from the "Dark Oak Treehouse" pack (none in the default pack); MineColonies crop items put straight into the warehouse. |
| CRF-01 Glassblower (glass → panes) | ✅ | 1,445 | |
| CRF-01 Dyer (white wool + red dye → red wool) | ✅ | 1,500 | |
| CRF-01 Mechanic (piston) | ✅ | 1,845 | |
| CRF-01 Crusher (cobblestone → gravel) | ❌ | 1,015 | BUG E2E-CRF01-1, intermittent (failed 6 of 9 runs; passed in the final run). `required = false`. Daily limit set to 0. |
| CRF-01 Sifter | ✅ | – | Covered by CRF-05 (the sifter is not a request crafter). |
| CRF-01 Smeltery (raw iron → ingot) | ✅ | – | Not a request crafter (refuses taught recipes, requests ores itself); covered by CRF-03. |
| CRF-01 Stone smeltery (cobblestone → stone) | ✅ | 2,235 | Furnace recipe, coal fuel. |
| CRF-01 Concrete mixer (powder → concrete) | ✅ | 2,850 | Needs a pickaxe/shovel (it requests one; first version timed out without it). |
| CRF-01 Alchemist (water bottle + nether wart → awkward) | ✅ | 3,190 | Taught through the server side (the teaching window's part is covered by UI-09 since E2E-CRF01-2 was fixed). Final step: shears in the warehouse (the alchemist once went for mistletoe without shears and waited on a tool request), budget 14,000. |
| CRF-02 Learning recipes and their limits | ✅ | 0 | Limit 2^level x 5 = 10 at level 1; 11th refused; accepted after the upgrade to level 2. |
| CRF-03 Furnaces and fuel | ✅ | 9,330 | 80 raw iron + 128 coal; second furnace placed in the level 1 hut; fuel requested only from the fuel list. |
| CRF-04 Concrete mixer and water | ✅ | 5,230 | Water compared as source blocks. Budget raised from 7,000 to 14,000 after one slow run. |
| CRF-05 Sifter meshes | ✅ | 455 (+1,200 idle) | Mesh with 3 uses left; drops only iron nugget/flint/coal; nothing sifted without mesh. |
| CRF-06 Alchemist (brew + harvest) | ✅ | 21,120 | Brewing covered by CRF-01 alchemist. Grown wart harvested and replanted, mistletoe gathered with shears from the warehouse. Random job choice (seen 21,120 / 29,110 / 57,145 ticks, one miss at 13,000): budget 43,000 per attempt, `maxAttempts = 5, requiredSuccesses = 1` (was 3; in one full run all three attempts missed the mistletoe). |
| CRF-07 Enchanter | ✅ | 1,195 + 6,615 | `crf07_enchanter`: book used, mana up, ancient tome → enchanted book in the hut, station worker keeps XP. Drain takes no XP (design change). |
| CRF-07 Enchanter, drain target | ❌ | 1,115 | BUG E2E-CRF07-1: the enchanter drains himself. `crf07_enchanterDrainsStationWorker`, `required = false`. |
| CRF-08 Cook keeps the restaurant stocked | ✅ | 5,335 | Menu foods made, no cooked chicken; citizens eating not checked (CIT-02). |

Counts by row: ✅ 30, ❌ 3, ⛔ 0, 📝 0 (34 test methods: 31 required, all passing; 3 optional bug tests).

## Bugs found

### E2E-CRF01-1 Crusher over-crafts a request and wastes ingredients
- **Severity:** medium (ingredients wasted, extra output stays in the crusher; intermittent).
- **Upstream or port:** most likely upstream. The code is shared MineColonies code with no port changes
  (`src/main/java/com/minecolonies/core/entity/ai/workers/crafting/EntityAIWorkCrusher.java`).
- **Evidence:** a request for 2 gravel (recipe 2 cobblestone → 1 gravel) with the daily limit at 0 used 8 cobblestone; the requester got
  2 gravel, the crusher worker held 2 more ("cobblestone used 8 (expected 4); product: lumberjack 2; citizen … 2"). In one earlier run
  the request completed but the requester got no gravel at all. Code: `craft()` (line ~220) calls `crush()`; once
  `craftCounter >= maxCraftingCount` it only calls `resetValues()` (line 228) and leaves the request open until the worker dumps. On the
  next tick `crush()` finds `currentRecipeStorage == null`, takes the crusher-mode recipe (line 117) and, because the state is `CRAFT`,
  skips the daily-limit check (line 120) and crafts again.
- **Reproduction:** `crf01_crusher` (fails about 2 of 3 runs; twice the requester got no gravel at all, "requester has 0 of 2; where: crusher 2").

### E2E-LOG06-1 Orphaned delivery: a tool request is never delivered
- **Severity:** high (the worker is stuck for good; here a miner never gets to work).
- **Upstream or port:** unknown, probably upstream (request system and courier code are shared code); not verified on NeoForge.
- **Evidence:** miner in a level 1 hut, wooden/stone/diamond pickaxes and wooden/stone shovels in the warehouse. The miner gets one shovel,
  then opens a second shovel request (levels 0..1). That request is `FOLLOWUP_IN_PROGRESS` with the warehouse resolver; its `Delivery`
  child is `IN_PROGRESS` with the `DeliveryRequestResolver`, but the courier's task queue is empty, the warehouse request queue is empty
  and the courier idles in `START_WORKING` for 3,000+ ticks. The miner stays in `NEEDS_ITEM` and never requests a pickaxe. Seen in 3 of 3
  runs after the diagnostics were added.
- **Reproduction:** `log06_toolsByHutLevel`.

### E2E-CRF01-2 Fabric port: brewing recipes can't be taught in the GUI
- **Status: FIXED** (branch `worktree-agent-a0f8cd80cd6f3aae7`). The brewing recipe serializer is already synced to the client (Fabric
  `RecipeSynchronization` in `MineColonies`); the window now looks the output up with
  `level.recipeAccess().getSynchronizedRecipes().getAllMatches(RecipeType.BREWING, new BrewingInput(potion, ingredient), level)`
  (`WindowBrewingstandCrafting.getBrewingOutput`, replaces 1.21.1's `potionBrewing().mix`). UI test UI-09 checks water bottle + nether
  wart → awkward potion on the client (passes); pressing Done in an opened window isn't automated, the server side is `crf01_alchemist`.
- **Severity:** high for alchemist players (the alchemist can only use built-in recipes).
- **Port-specific:** yes. `core/client/gui/containers/WindowBrewingstandCrafting.java` lines ~122-127: "FABRIC-PORT TODO: 26.3 made brewing
  data-driven … the output cannot be computed here"; `primaryOutput` is always empty, so the window never sends `AddRemoveRecipeMessage`.
- **Evidence:** code reading; the server side works (`crf01_alchemist` teaches through it and passes). Not testable headless (client GUI).

### E2E-CRF07-1 Enchanter drains himself instead of the station's workers
- **Severity:** medium (the enchanter still gains mana, but the station mechanic is meaningless; the "random enchant" lands in the
  enchanter's own inventory instead of the visited worker's).
- **Upstream or port:** most likely upstream (shared code, no port changes in
  `core/entity/ai/workers/service/EntityAIWorkEnchanter.java`).
- **Evidence:** `gatherAndDrain()` walks to the station building (`job.getPosToDrainFrom()`) but picks the citizen from
  `getModuleForJob().getAssignedEntities()`, which is the enchanter's own work module (`AbstractEntityAIBasic.getModuleForJob()` returns
  `job.getWorkModule()`), not `buildingWorker`'s workers. Test output: "drained [Kai T. Allard] (enchanter Kai T. Allard, station worker
  Emilia A. Crisp)" (AI field `citizenToGatherFrom`, read by reflection). Seen in 2 of 2 runs.
- **Reproduction:** `crf07_enchanterDrainsStationWorker`.

## Design changes

All recorded as "*As implemented*" notes in [logistics.md](../logistics.md) and [jobs-crafting.md](../jobs-crafting.md):
- LOG-01: concrete resolver; building-level request from a hut without a worker.
- LOG-02: pickup is triggered like "force pickup" (huts only ask for pickups when a worker dumps).
- LOG-04: retrying resolver comes first (~40,000 ticks); the postbox is a requester, so the diamond goes into a warehouse rack.
- LOG-05: minimum stock is in stacks (64), not 32 items.
- LOG-07: postboxes as requesters; one courier's hut per courier.
- LOG-09: "warehouse full" is checked through `TileEntityWareHouse.lastNotification` (players-only chat message).
- CRF-01: smeltery and sifter are not request crafters (covered by CRF-03/CRF-05); crusher daily limit set to 0; concrete mixer needs tools.
- CRF-02: limit is 2^level x 5.
- CRF-03: level 1 smeltery has one furnace (second one placed); one stack goes into one furnace, so 80 ore / 128 coal are used.
- CRF-04: water compared as source blocks; production is request-driven.
- CRF-05: the sifter shows a "no mesh" interaction instead of requesting a mesh.
- CRF-01 kitchen: chef meals need kitchen level 4; no kitchen in the default pack (Dark Oak Treehouse used).
- CRF-06: mistletoe isn't replanted, it is gathered from leaves with shears; jobs are chosen at random (large budget).
- CRF-07: the drain takes no XP; it uses a book and gives the enchanter mana. Sources are station huts.

## Framework issues

- **Timeout message lost when `awaitUntil` has no own tick limit** (`TestFlow`): when the test's `maxTicks` runs out, vanilla ticks the
  sequence once more and `stopIfFinished()` throws "Test already finished", which becomes the reported failure ("Test already finished on
  tick 6002") instead of the "Still waiting … Found: …" text. Workaround in this suite: every wait has its own budget below `maxTicks`.
- **Worktree setup:** `src/main/resources/assets/minecolonies/lang/en_us.json` is git-ignored, so a fresh worktree crashes at server start
  (NPE in `LanguageHandler$LanguageCache.load`); copied from the main checkout (not committed).
- **Chance-heavy AI:** the alchemist chooses harvesting at random (CRF-06 took 13,000+ ticks without success once, 29,110 when it
  passed); no seeded random source is available, so only a large budget helps.
- **Missing helpers** (written as private helpers in the suite, candidates for the fixture): placing a hut by footprint
  (`placeInBox`, reads blueprint size/offset), single-block buildings (stash/postbox via `setPlacedBy`), courier setup (courier's hut +
  warehouse assignment), request-system inspection (resolver per request, all requests), recipe teaching (grid/furnace/brewing),
  placing a block into a hut (`addBlockInHut`).
- The fixture's `hire(citizen, warehouse)` would put a citizen into the warehouse's courier module without a job (the warehouse module has
  no job of its own); couriers must be hired into a courier's hut.
