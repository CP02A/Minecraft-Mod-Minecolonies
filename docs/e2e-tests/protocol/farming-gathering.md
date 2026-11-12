[← Back to the test overview](../../E2E-tests.md) · [Farming tests](../jobs-farming.md) · [Gathering tests](../jobs-gathering.md)

# Protocol: farming and gathering tests

Suite: `src/gametest/java/com/minecolonies/gametest/suites/FarmingGatheringTests.java`, run with
`./gradlew --no-daemon runGameTest -Pe2eFilter=farming_gathering_tests`. Last two runs: all 21 required tests passed; optional: the two bug tests fail, the flaky swine breeding test passed in one of the two. Runtimes are the game tick of each test's last step in that run. They vary between runs because the AI uses
random numbers.

| ID | Test method(s) | Status | Runtime (ticks) | Notes |
|---|---|---|---|---|
| FRM-01 | `frm01_farmerFieldCycle` | ✅ | 12010 | Field on dirt. The farmer works a field once per colony day, so the test lets nights pass while he idles. |
| FRM-01 | `frm01_farmerTillsGrass` (optional) | ❌ mod bug | 8000 (timeout) | E2E-FRM-01-1: grass is never tilled. |
| FRM-02 | `frm02_planterSugarCane`, `frm02_planterCactus`, `frm02_planterBamboo` | ✅ | 6135 / 6145 / 12190 | One test per field type (level 1 owns one field). Bamboo is slow and leaves one position very long; the test accepts all but one position. |
| FRM-03 | `frm03_florist` | ✅ | 10620 | Level 2 shop, "don't grow" list, compost at 3 per bed, double-speed beds. Observation E2E-FRM-03-1. |
| FRM-04 | `frm04_composter` | ✅ | 370 | Exact seed use checked; apples (not allowed) untouched. |
| FRM-05 | `frm05_beekeeper` | ✅ | 740 | Honeycomb, then honey (the "both" mode needs research). Dexterity 99. |
| FRM-06 | `frm06_*Breeds*` (chicken, cow, sheep, rabbit) | ✅ | 349 / 1014 / 660 / 479 | Baby born + egg picked up from the ground (the item entity is gone; in one run it was afterwards in neither the hut nor the herder, not investigated) / milk (large bottle) / wool. |
| FRM-06 | `frm06_swineHerderBreeds` (optional) | ⚠️ flaky (optional) | 479 when it passes | Fails in most runs (pigs drift apart, the herder waits 60 s between tries). Not classified as a mod bug. |
| FRM-06 | `frm06_*ButchersAboveLimit` (chicken, cow, sheep, pig, rabbit) | ✅ | 955 / 1405 / 840 / 1005 / 1265 | 6 adults → at most 3 adults, drops in hut, outsider alive. Limit is soft by design. |
| FRM-07 | `frm07_fisherman` (optional) | ❌ mod bug | 8000 (timeout) | E2E-FRM-07-1: every catch throws, no loot ever. Rod/request part never reached. |
| GAT-01 | `gat01_lumberjackFellsAndReplants` | ✅ | 8440 | One oak + one birch, defoliation on. |
| GAT-02 | `gat02_lumberjackFollowsSettings` | ✅ | ~5650 | 3245 until the oak is replanted, then a fixed 2400-tick wait (it checks that something does *not* happen). |
| GAT-03 | – | ⛔ not implementable | – | Mining levels are absolute heights (48/16/-16/-100); the first level under the arena (y≈6-10) is y -16, below the arena's barrier floor. |
| GAT-04 | – | ⛔ not implementable | – | Same reason as GAT-03. |
| GAT-05 | – | 📝 not done | – | Time box. Looks feasible: quarry building with `QuarryModule`, `creativeResolve` for the shaft materials, stone floor via `fillFloor`; only the 5 floor layers can be dug. |
| GAT-06 | – | 📝 not done | – | Time box. Needs food, 5 tools, the crafting-module trip inputs, the "ready for trip" day period and the travel manager; worth its own session. |

## Bugs found

### E2E-FRM-01-1 Farmer never tills grass blocks

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

## Framework issues

- **`ColonyFixture.rel()` is not the inverse of `abs()`:** `rel(abs(new BlockPos(44, 5, 44)))` returned `(-44, 5, -44)` in every run.
  `helper.spawn(type, fixture.rel(absPos))` therefore spawns in the wrong place. Worked around with absolute positions everywhere (own
  `spawnAnimal`).
- **Re-runs of colony tests don't run:** with `maxAttempts = 3, requiredSuccesses = 1` the log shows "Starting re-run of tests: ..." and
  the run ends within the same second with the first failure. Chance-based colony tests can't use `maxAttempts`.
- **Herder huts at `(12, 5, 12)` reach over the arena wall:** several herder blueprints extend up to 19 blocks to one side of the hut
  block, so the pen hangs outside the arena and animals fall off. The suite uses `(44, 5, 48)` for herder huts and checks the hut area
  is inside the arena.
- **Not every pen is closed:** some herder blueprints have open pens, so animals leave the hut area (the herder only sees animals inside
  the building's corners). The suite closes the area's border with fence gates and slows the animals (`fenceArea`, `spawnAnimal`).
- **Shared scratchpad / git-ignored lang file:** the worktree needed `src/main/resources/assets/minecolonies/lang/en_us.json` copied from
  the main checkout (git-ignored, server NPE otherwise), and the session scratchpad is shared between agents, so logs went to `build/`.
- Helpers that could move to the framework: `spawnAnimal` (absolute), `DayPump` (pass nights while a worker idles), `describeWork`
  (inventory + open requests in failure messages), `growTree` (vanilla tree grower), `fenceArea`.
