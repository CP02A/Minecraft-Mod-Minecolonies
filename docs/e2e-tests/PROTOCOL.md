[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Test protocol: MineColonies end-to-end tests (Fabric port)

This is the summary of all end-to-end test work. Details per group are in the [group protocols](#group-protocols).

## Summary

- **Date:** 2026-11-12 to 2026-11-13.
- **Branch:** `e2e-tests`. The final runs are on the commit that adds this file (parent `25ee2d3495`).
- **Game:** Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.2+26.3, Windows 11.

### How the tests were made

1. **Framework first** (commit `bc90de050c`): colony fixture, test arena, one colony test at a time, a pathfinding wait, test config.
2. **Seven agents in parallel**, each in its own worktree: six wrote one suite group each (construction/research, citizens/services,
   logistics/crafting, colony/connections/robustness, defense/visitors, farming/gathering). The seventh built watch mode and the UI tests.
3. **Merge** of the seven branches (commits `aca36796ed` .. `aa8e3cb6de`), then three full runs: same result each time.
4. **Framework fixes** found by the agents (commit `396b6db53a`, see [Framework issues fixed](#framework-issues-fixed-during-the-work)).
   They made the miner tests possible.
5. **Miner, quarrier and nether tests** GAT-03..06 (commit `25ee2d3495`).
6. **Final step:** the flaky herder test fixed, then full runs until two in a row passed (12 tests made more robust on the way, see
   [Flakiness](#flakiness)), this protocol.

### Test counts

| What | Count |
|---|---|
| Designed test IDs (rows in the [overview](../E2E-tests.md)) | 109 |
| ✅ Passing | 102 (14 of them with an optional part that fails because of a mod bug) |
| ❌ Fails because of a mod bug | 2 (LOG-06, FRM-07) |
| ⛔ Not implementable | 5 (ROB-01..04, DEF-10), plus parts of DEF-02 (cavalry) and DEF-06 (ship raids) |
| 📝 Planned | 0 |
| Server game test methods (headless run) | 149: 133 required, 16 optional |
| UI test methods (`runClientGameTest`) | 8 (all pass) |

Optional tests (`required = false`) reproduce a mod bug. They fail on purpose and don't fail the run.

### Final runs

Two full headless runs, `./gradlew --no-daemon runGameTest`, on the same machine, one after the other:

| | Run 1 | Run 2 |
|---|---|---|
| Game tests | 149 | 149 |
| Required passed | 133 of 133 | 133 of 133 |
| Optional failed | 16 | 15 |
| Wall time (Gradle included) | 3 min 46 s | 2 min 36 s |
| Test server time | 3.18 min | 2.16 min |

Optional tests that failed in both runs (14, each a known bug): `cit06b_negativeFactorsLowerHappiness`, `srv02b_studyItemsHelp`,
`col05b_friendOpensHut`, `con01b_reconnectAfterSignRemoved`, `con02b_allyTeleportFromOwnGate`, `frm01_farmerTillsGrass`,
`frm06_herderFeedsPairOutOfMateRange`, `frm07_fisherman`, `gat04_minerSealsWater`, `gat06_netherWorkerReturnsAfterTravel`,
`crf07_enchanterDrainsStationWorker`, `log06_toolsByHutLevel`, `vis02_tavernRemovedAfterRecruiting`,
`vis04_cancelAcceptedQuestAtDelivery`. Intermittent bug tests: `gat03_minerDigsShaftSafely` failed in both runs, `crf01_crusher` in
run 1 only.

**Run time:** a full run takes 2.5-4 minutes wall time, of which 2-3 minutes are the test server; the rest is Gradle, compiling
and server start. Ticks run as fast as the CPU allows. Earlier runs on the same machine took 1.9-2.3 minutes of server time; the
spread comes from the random AI (long tests such as CRF-06 take 20,000-34,000 ticks) and re-runs.

### Flakiness

**The flaky chicken test.** `frm06_chickenHerderBreeds` (required) timed out once in a full run (no chick in 6,000 ticks) and
passed alone twice. Cause: bug [E2E-FRM-06-1](#e2e-frm-06-1-herder-feeds-animals-that-are-too-far-apart-to-mate) plus the test's
slowed animals. The herder feeds a pair up to 10 blocks apart; vanilla's mate search is 8 blocks. Slowed animals that drifted 8-10
blocks apart stayed there, so each try failed and the herder waited 60 s. In 200 traced repeats one took 5,148 ticks. Fix (test side):
while neither animal is in love and they are more than 6 blocks apart, the second is put back next to the first; animals that got out
of the hut area are put back in the pen. After the fix: chicken 200 of 200 (at most 699 ticks), swine 100 of 100 (at most 1,194).
`frm06_swineHerderBreeds` was optional for the same reason and is required again. A new optional test shows the bug.

**More rare failures found while verifying.** Full runs were repeated until two in a row passed all required tests. Of the 12 full
runs before the final two, 8 had 1-3 required failures (almost every time a different test, never a mod bug) and 4 passed. The last
pair before the final one passed too, but its leisure fix made runs twice as slow; it was replaced and the pair repeated. Causes and
fixes:

| Test | What happened | Fix |
|---|---|---|
| `def03_patrolGuardFollow` | Guards nap 2500-3000 ticks at random, and a guarding guard walks to his tower to dump, idles and walks back lazily. 6,000 and later 12,000 ticks ran out (1 of 51 repeats). | Budgets up to 30,000 ticks. |
| `frm06_swineHerderBreeds` | The pigs slipped out of the hut area; the piglet was born outside it. | Animals outside the area are put back in the pen. |
| `srv03_hospital`, `log09_fullWarehouse`, `log01_warehouseDeliversRequest`, `srv02_library` | A citizen went on a random 3-minute leisure break (`CitizenData.update`). | The test mod ended leisure breaks at once; now it shortens them to 3 seconds ([framework.md](framework.md#test-configuration)); two budgets raised too. |
| `srv04_graveyard` | The undertaker resurrected the citizen (about 3% without research), so he was never buried. | The roll is forced to miss (SRV-06, formerly SRV-04b, tests resurrection). |
| `def11_raidTimingFollowsConfig` | No raid in ten nights: the raid's spawn search goes outwards while chunks are loaded and ended past the ground ring. Depends on which chunks other tests left loaded. | Sky raiders on (the test checks timing, not spawn places). |
| `crf01_alchemist` | Between brews the alchemist went for mistletoe without shears and waited on a tool request. | Shears in the warehouse. |
| `crf06_alchemistHarvests` | All 3 attempts missed the mistletoe (random job choice). | 5 attempts. |
| `gat05_quarrierDigsLayers`, `frm02_planterBamboo`, `crf04_concreteMixerWater`, `crf01_fletcher` | Normal but slow runs went past the budget (e.g. quarry 34,000, bamboo 20,000). | Budgets roughly doubled. |

Budgets are in game ticks and cost little real time, so they were raised generously. Each fix is noted in the group protocol's table.

**Intermittent bug tests** (optional): `crf01_crusher` ([E2E-CRF01-1](#e2e-crf01-1-crusher-over-crafts-a-request-and-wastes-ingredients))
fails in about half of the runs, `gat03_minerDigsShaftSafely` ([E2E-GAT-03-1](#e2e-gat-03-1-miner-falls-off-the-shaft-ladder)) in most.
In the full run before the final step the crusher was the 15th optional failure (14 bug tests plus the crusher; GAT-03b also
failed, the swine test passed that time).

## Bugs found

20 bugs: 17 confirmed by a test run, 3 suspected (code reading or not reproduced). By origin: 3 specific to the Fabric port (2
confirmed, 1 suspected), 15 upstream or likely upstream (13 confirmed, 2 suspected), 2 unclear (both confirmed).

"Confirmed" means a test run shows it: an optional test that fails, or (E2E-FRM-03-1) a traced variant of a test. "Suspected" means
code reading, or seen once without a test that reproduces it.

| ID | Title | Severity | Origin | Where | Reproduced by |
|---|---|---|---|---|---|
| [E2E-FRM-01-1](#e2e-frm-01-1-farmer-never-tills-grass-blocks) | Farmer never tills grass blocks | High | Fabric port (26.3 tag change) | `EntityAIWorkFarmer.java:337` | ✅ `frm01_farmerTillsGrass` |
| [E2E-FRM-07-1](#e2e-frm-07-1-fisherman-throws-on-every-catch) | Fisherman throws on every catch, never gets loot | High | Fabric port (26.3 loot params) | `NewBobberEntity.java:519-525` | ✅ `frm07_fisherman` |
| [E2E-CRF01-2](#e2e-crf01-2-brewing-recipes-cant-be-taught-in-the-gui) | Brewing recipes can't be taught in the GUI | High | Fabric port | `WindowBrewingstandCrafting.java:122-127` | ❔ code reading (known port TODO) |
| [E2E-LOG06-1](#e2e-log06-1-orphaned-delivery-a-tool-request-is-never-delivered) | Orphaned delivery: a tool request is never delivered | High | Unclear (probably upstream) | request system / courier, no single line found | ✅ `log06_toolsByHutLevel` |
| [E2E-CON-02-1](#e2e-con-02-1-ally-teleport-only-works-within-15-blocks-of-the-target-gate) | Ally teleport only works within 15 blocks of the target gate | High (for the feature) | Upstream (#11888) | `TeleportToColonyMessage.java:89` | ✅ `con02b_allyTeleportFromOwnGate` |
| [E2E-CIT06-1](#e2e-cit06-1-negative-happiness-factors-can-raise-total-happiness) | Negative happiness factors can raise total happiness | Medium | Upstream | `CitizenHappinessHandler.java:148` | ✅ `cit06b_negativeFactorsLowerHappiness` |
| [E2E-SRV02-1](#e2e-srv02-1-study-items-make-studying-less-successful) | Study items make studying less successful | Medium | Upstream (#7349) | `CitizenSkillHandler.java:152` | ✅ `srv02b_studyItemsHelp` |
| [E2E-VIS04-1](#e2e-vis04-1-cancelling-an-accepted-quest-at-a-delivery-step-does-nothing) | Cancelling an accepted quest at a delivery step does nothing | Medium | Upstream | `QuestDeliveryInteraction.java:55-73` | ✅ `vis04_cancelAcceptedQuestAtDelivery` |
| [E2E-CRF01-1](#e2e-crf01-1-crusher-over-crafts-a-request-and-wastes-ingredients) | Crusher over-crafts a request and wastes ingredients | Medium | Likely upstream | `EntityAIWorkCrusher.java:115-120, 228` | ✅ `crf01_crusher` (about 2 of 3 runs) |
| [E2E-CRF07-1](#e2e-crf07-1-enchanter-drains-himself) | Enchanter drains himself instead of the station's workers | Medium | Likely upstream | `EntityAIWorkEnchanter.java:324` | ✅ `crf07_enchanterDrainsStationWorker` |
| [E2E-GAT-03-1](#e2e-gat-03-1-miner-falls-off-the-shaft-ladder) | Miner falls off the shaft ladder | Medium | Unclear | citizen path following on ladders, started by `AbstractEntityAIBasic.java:756` | ✅ `gat03_minerDigsShaftSafely` |
| [E2E-GAT-04-1](#e2e-gat-04-1-miner-never-removes-liquids-around-a-node) | Miner never removes liquids around a node | Medium | Likely upstream | `EntityAIStructureMiner.java:794-820` | ✅ `gat04_minerSealsWater` |
| [E2E-GAT-06-1](#e2e-gat-06-1-nether-worker-idles-after-returning) | Nether worker idles after returning until the next trip is allowed | Medium | Likely upstream | `EntityAIWorkNether.java:156`, `AbstractEntityAICrafting.java:162` | ✅ `gat06_netherWorkerReturnsAfterTravel` |
| [E2E-COL-05-1](#e2e-col-05-1-friends-cant-open-hut-menus) | Friends can't open hut menus | Low | Upstream | `ColonyPermissionEventHandler.java:372, 412` | ✅ `col05b_friendOpensHut` |
| [E2E-CON-01-1](#e2e-con-01-1-a-removed-connection-sign-leaves-a-stale-link) | A removed connection sign leaves a stale link in the other colony | Low | Upstream | `ColonyConnectionManager.java:169, 372` | ✅ `con01b_reconnectAfterSignRemoved` |
| [E2E-VIS02-1](#e2e-vis02-1-removing-a-tavern-right-after-a-recruit-throws) | Removing a tavern right after a recruit throws and leaves visitors behind | Low | Upstream | `TavernBuildingModule.java:309-313`, `VisitorManager.java:301` | ✅ `vis02_tavernRemovedAfterRecruiting` |
| [E2E-FRM-06-1](#e2e-frm-06-1-herder-feeds-animals-that-are-too-far-apart-to-mate) | Herder feeds animals that are too far apart to mate | Low | Likely upstream | `AbstractEntityAIHerder.java:78, 454` | ✅ `frm06_herderFeedsPairOutOfMateRange` |
| [E2E-FRM-03-1](#e2e-frm-03-1-composted-bed-expires-at-once-after-it-sat-idle) | Composted bed expires at once after it sat idle | Low | Likely upstream | `TileEntityCompostedDirt.java:75, 122` | ✅ traced variant of `frm03_florist` (no own test) |
| [E2E-BLD-07-1](#e2e-bld-07-1-resource-list-misses-items-of-blueprint-entities) | Resource list misses items of blueprint entities | Low | Likely upstream | `WindowBuildBuilding.java:295` (`updateResources`), Structurize resource count | ❔ seen in early BLD-07 runs, no test |
| [E2E-DEF-S1](#e2e-def-s1-raid-end-reads-the-first-raid-instead-of-the-last) | Raid end reads the first raid instead of the last | Low | Upstream | `RaidManager.java:1061, 1078` | ❔ code reading |

Legend: ✅ confirmed by a test run, ❔ suspected. E2E-DEF-S1 had no ID in its group protocol; the ID is new here.

### Bug details

#### E2E-FRM-01-1 Farmer never tills grass blocks

`EntityAIWorkFarmer.findHoeableSurface` accepts only `BlockTags.DIRT` and farmland. In Minecraft 26.3 the `#minecraft:dirt` tag no
longer has `grass_block` (in 1.21.x it had). A field on grass is never tilled; on plain dirt it works. Other `BlockTags.DIRT` uses in the
port may have the same problem. More: [farming protocol](protocol/farming-gathering.md#e2e-frm-01-1-farmer-never-tills-grass-blocks).

#### E2E-FRM-07-1 Fisherman throws on every catch

`NewBobberEntity.retrieve` passes `ATTACKING_ENTITY` to the FISHING loot parameters. 26.3 doesn't allow it there, so every catch throws
`IllegalArgumentException` and the AI pauses. The fisherman finds the pond and wears out his rod but never gets loot. More:
[farming protocol](protocol/farming-gathering.md#e2e-frm-07-1-fisherman-throws-on-every-catch-never-gets-loot).

#### E2E-CRF01-2 Brewing recipes can't be taught in the GUI

The teaching window has a "FABRIC-PORT TODO": 26.3 made brewing data-driven, the window can't compute the output, so it never sends the
recipe. The server side works (`crf01_alchemist` teaches through it). Client GUI only, not testable headless. More:
[logistics protocol](protocol/logistics-crafting.md#e2e-crf01-2-fabric-port-brewing-recipes-cant-be-taught-in-the-gui).

#### E2E-LOG06-1 Orphaned delivery: a tool request is never delivered

A miner in a level 1 hut asks for a second shovel. The request waits on a delivery that is `IN_PROGRESS`, but no courier has it: the
courier's queue and the warehouse queue are empty, the courier idles. The miner stays in `NEEDS_ITEM` for good. 3 of 3 runs. Not
checked on NeoForge. More: [logistics protocol](protocol/logistics-crafting.md#e2e-log06-1-orphaned-delivery-a-tool-request-is-never-delivered).

#### E2E-CON-02-1 Ally teleport only works within 15 blocks of the target gate

`TeleportToColonyMessage` checks that the player is within 15 blocks of the *target* colony's gate. The button is in the player's *own*
gate house window, so it only works when the two gates are close. Introduced by "Packet range (#11888)". More:
[colony protocol](protocol/colony-connections-robustness.md#e2e-con-02-1-ally-teleport-only-works-when-the-gates-are-at-most-15-blocks-apart).

#### E2E-CIT06-1 Negative happiness factors can raise total happiness

`getHappiness` skips factors that are exactly 1.0 and averages the rest. A below-neutral factor above the current average pulls the
average up. In the test, an unemployed or sick citizen had 3.50, the healthy employed one 3.00. More:
[citizens protocol](protocol/citizens-services.md#e2e-cit06-1-negative-happiness-factors-can-raise-total-happiness).

#### E2E-SRV02-1 Study items make studying less successful

Since #7349 the success check is inverted. 4000 rolls each: no item 89%, paper 50%, book 26%. The book, configured as the best item, is
the worst. More: [citizens protocol](protocol/citizens-services.md#e2e-srv02-1-study-items-make-studying-less-successful).

#### E2E-VIS04-1 Cancelling an accepted quest at a delivery step does nothing

At a delivery step, "cancel" only closes the dialogue; `applyToQuest` (which deletes the quest) is never called. The quest stays in
progress. Cancel in a normal dialogue works. More:
[defense protocol](protocol/defense-visitors.md#e2e-vis04-1-cancelling-an-accepted-quest-at-a-delivery-step-does-nothing).

#### E2E-CRF01-1 Crusher over-crafts a request and wastes ingredients

After the last craft for a request the crusher resets, but the request stays open until the worker dumps. On the next tick it takes
the crusher-mode recipe and, in state `CRAFT`, skips the daily limit and crafts again. A request for 2 gravel used 8 cobblestone.
Intermittent (about 2 of 3 runs). More: [logistics protocol](protocol/logistics-crafting.md#e2e-crf01-1-crusher-over-crafts-a-request-and-wastes-ingredients).

#### E2E-CRF07-1 Enchanter drains himself

`gatherAndDrain` walks to the station building but picks the citizen from the enchanter's own work module. The enchanter drains
himself; the station's worker is never visited. 2 of 2 runs and every full run since. More:
[logistics protocol](protocol/logistics-crafting.md#e2e-crf07-1-enchanter-drains-himself-instead-of-the-stations-workers).

#### E2E-GAT-03-1 Miner falls off the shaft ladder

Climbing out of the shaft for a new tool, the miner leaves the ladder sideways at about y 8 and falls 3.3-4 blocks (1 damage at 4).
Seen in most runs. The AI only asks to walk to the hut; the fall happens in path following on the ladder. The port changed
pathfinding in places, so the origin is unclear. More:
[farming protocol](protocol/farming-gathering.md#e2e-gat-03-1-miner-falls-off-the-shaft-ladder).

#### E2E-GAT-04-1 Miner never removes liquids around a node

The liquid check around a node runs only when the node's blueprint is already loaded. After loading, the AI goes on building and
doesn't come back to the check, so water or lava next to a node is never sealed. More:
[farming protocol](protocol/farming-gathering.md#e2e-gat-04-1-miner-never-removes-liquids-around-a-node).

#### E2E-GAT-06-1 Nether worker idles after returning

The returned worker's AI starts in IDLE and leaves it only when the next trip is allowed (one trip per 3-day period). Until then he
stands at the portal, the log says IN_PROGRESS and the loot stays in the job. More:
[farming protocol](protocol/farming-gathering.md#e2e-gat-06-1-nether-worker-idles-after-returning-until-the-next-trip-is-allowed).

#### E2E-COL-05-1 Friends can't open hut menus

The permission handler lets a hut click through for `ACCESS_HUTS`, then treats the hut as a container (it has an item handler) and
refuses it with `OPEN_CONTAINER`, which friends don't have. More:
[colony protocol](protocol/colony-connections-robustness.md#e2e-col-05-1-friends-cant-open-hut-menus).

#### E2E-CON-01-1 A removed connection sign leaves a stale link

Removing a connection sign only resets pointers in its own colony. The partner sign in the other colony still points at the removed
sign, so a new link is refused. More:
[colony protocol](protocol/colony-connections-robustness.md#e2e-con-01-1-a-removed-connection-sign-leaves-a-stale-link-in-the-other-colony).

#### E2E-VIS02-1 Removing a tavern right after a recruit throws

The tavern keeps a recruited visitor's id until its next colony tick. Removing the tavern before that passes a null visitor to
`VisitorManager.removeCivilian`: `NullPointerException`, later visitors stay registered. Also seen in the log when a test colony was
deleted right after a recruit. More:
[defense protocol](protocol/defense-visitors.md#e2e-vis02-1-removing-a-tavern-right-after-a-recruit-throws-and-leaves-visitors-behind).

#### E2E-FRM-06-1 Herder feeds animals that are too far apart to mate

The herder pairs animals up to 10 blocks apart; vanilla's `BreedGoal` looks for a partner only within 8 blocks. A pair 8-10 blocks apart
is put in love, never meets, the food is wasted and the herder waits 60 s. The optional test (two chickens that can't move, 9 blocks
apart) failed 22 of 22 runs. This was the cause of the flaky chicken test. More:
[farming protocol](protocol/farming-gathering.md#e2e-frm-06-1-herder-feeds-animals-that-are-too-far-apart-to-mate).

#### E2E-FRM-03-1 Composted bed expires at once after it sat idle

The compost timer counts on every tick, also while the bed isn't composted. A bed idle for more than 300 s loses new compost at its
first update, so the first compost is wasted. Seen with reflection diagnostics in a sped-up `frm03_florist`. More:
[farming protocol](protocol/farming-gathering.md#e2e-frm-03-1-composted-bed-expires-at-once-after-it-sat-idle-minor-observation).

#### E2E-BLD-07-1 Resource list misses items of blueprint entities

The material list of a decoration with 4 entities was one item frame and one torch short; a builder given exactly that list stopped
and asked for an item frame. Seen in early BLD-07 runs before the test switched to an entity-free decoration. Not checked in the real
GUI. More: [construction protocol](protocol/construction-research.md#e2e-bld-07-1-suspected-resource-list-misses-items-of-blueprint-entities).

#### E2E-DEF-S1 Raid end reads the first raid instead of the last

`RaidManager.onRaidEventFinished` reads `raidHistories.get(0)` (the colony's first raid) for the mercy check and the victory sound,
instead of the last raid. After more than one raid, these depend on the first raid's losses. Code reading only. More:
[defense protocol](protocol/defense-visitors.md#suspect-code-reading-not-reproduced-by-a-test).

## Not implementable

| ID | Why |
|---|---|
| [ROB-01](robustness.md#rob-01-chunks-unload-during-work) | The chunks of a running game test can't be unloaded and reloaded. |
| [ROB-02](robustness.md#rob-02-server-restart) | Needs a server restart. The colony data part is covered by COL-06 (save and reload in place). |
| [ROB-03](robustness.md#rob-03-large-colony-performance) | 100 citizens and 30 huts don't fit the arena; a tick budget needs a reference machine. |
| [ROB-04](robustness.md#rob-04-loading-a-1211-world) | No 1.21.1 world in the repo; another world can't be loaded during a run. |
| [DEF-10](defense.md#def-10-stable-and-cavalry) | The stablemaster trains after a long cooldown with 25% chance and led the horses out of the counted area. No deterministic setup found. |
| DEF-02 cavalry | Needs DEF-10. |
| DEF-06 ship raids, drowned pirates | Ships need deep water next to the colony; `drowned_pirate_raid` without a ship finds no spawn point. |

## Framework issues fixed during the work

All in commit `396b6db53a` unless noted. Details: [framework.md](framework.md), [agent guide](AGENT-GUIDE.md).

- **Re-runs didn't run.** `maxAttempts` re-runs of colony tests were announced but never ran. `RerunTracking` now puts each re-run in
  the server's tracker.
- **`rel()` was not the inverse of `abs()`.** Vanilla mirrors x/z; tests spawned in the wrong place.
- **Lost timeout message.** A wait without its own limit failed with "Test already finished" instead of what it was waiting for.
- **Fake players kept state** (inventory, game mode, effects) between tests. Now reset at setup and cleanup; the owner can be added to
  the player lists.
- **One colony per fixture.** Second colonies are now part of the fixture (`createSecondColony`, cleanup included).
- **No room for mines.** `makeDeepGround()` puts stone under the arena down to y -32. This made GAT-03/04 possible.
- **Fresh worktrees crashed** because the git-ignored `en_us.json` was missing. `mergeTranslations` now generates it.
- **Hiring into a warehouse** silently gave no courier; `hire()` now throws with a pointer to the courier's hut.
- **Random leisure breaks** (final step): citizens left work for 3 minutes at random; the test mod ended each break at once. Later
  changed: breaks last 3 seconds, so the leisure behavior still runs (CIT-15 tests it).
- **Flaky tests** (final step, test side): herder animals kept within mate range and inside the hut area, and the fixes in the
  [Flakiness](#flakiness) table.

Still open: the town hall can't be rotated; `-Pe2eFilter` supports only `*` and `?`; no seeded random source for the AI; UI tests need
a desktop with a GPU; don't run `runGameTest` and `runClientGameTest` in the same checkout at once (Loom clears the run folder).

## Design corrections

The test designs were written before the code was read. Where MineColonies works differently, the test follows the mod and the design
doc got an "As implemented" or "As tested" note. The main ones:

- **Colony** ([colony.md](colony.md)): growth beyond the first citizens is only by children (COL-03). Friends can't open huts (bug).
  Save and reload is in place, not a server restart (COL-06).
- **Connections** ([connections.md](connections.md)): there is no "disconnect"; CON-04 checks neutral and gate house removal. Removing a
  sign doesn't disconnect colonies.
- **Construction** ([construction.md](construction.md)): a level 1 residence takes 15,000-18,000 ticks, more than the 10-15 minutes
  planned. A removed hut stays registered as deconstructed until picked up (BLD-05).
- **Research** ([research.md](research.md)): requirements and costs only apply to survival players. Undoing a parent with a finished
  child is refused.
- **Citizens** ([citizens.md](citizens.md)): full saturation is 60, not 20. No "take shelter" in the rain. Mourning starts at the next
  wake-up.
- **Services** ([services.md](services.md)): an adult in a pupil place is removed, not refused. Mystical sites of level 1 and 2 give no bonus.
- **Logistics** ([logistics.md](logistics.md)): minimum stock is in stacks of 64. An unfulfillable request goes through the retrying
  resolver (~40,000 ticks) before the player.
- **Crafting** ([jobs-crafting.md](jobs-crafting.md)): recipe limit is 2^level x 5. Smeltery and sifter are not request crafters. The
  enchanter's drain takes no XP.
- **Farming** ([jobs-farming.md](jobs-farming.md)): field workers work each field once per colony day. The animal limit is soft
  (no butchering at 3 or fewer adults).
- **Gathering** ([jobs-gathering.md](jobs-gathering.md)): the first mining level is 5 blocks under the shaft start, not at an absolute
  height. Leaves need defoliation because random ticks are off.
- **Defense** ([defense.md](defense.md)): the druid slows mobs, doesn't kill them. Citizens flee when hurt, not on sight.
- **Visitors** ([visitors-quests.md](visitors-quests.md)): cancel of an accepted quest is only offered at delivery steps (and is buggy).

## How to rerun

| What | Command |
|---|---|
| All server game tests, headless | `./gradlew --no-daemon runGameTest` (report: `build/gametest-report.xml`) |
| One group or test | `./gradlew --no-daemon runGameTest -Pe2eFilter=farming_gathering_tests` or `-Pe2eFilter=minecolonies-gametest:farming_gathering_tests_frm06_herder_feeds_pair_out_of_mate_range` |
| Watch in a game window | `./gradlew --no-daemon runGameTest -Pe2eWatch -Pe2eFilter=<prefix>` (`-Pe2eWatchSpeed=2` for double speed) |
| UI tests with screenshots | `./gradlew --no-daemon runClientGameTest` (report: `build/e2e-ui-report.xml`, screenshots: `build/e2e-screenshots/`); some only: `-Pe2eUiFilter=UI-01,UI-03` |
| Build | `./gradlew --no-daemon build` (compiles and runs the isolation checks; no game tests) |

Test names in filters are the method names in snake case with the suite prefix, e.g. `frm06_chickenHerderBreeds` →
`farming_gathering_tests_frm06_chicken_herder_breeds`.

## Group protocols

- [Construction and research](protocol/construction-research.md)
- [Citizens and services](protocol/citizens-services.md)
- [Logistics and crafting](protocol/logistics-crafting.md)
- [Colony, connections, robustness](protocol/colony-connections-robustness.md)
- [Defense and visitors](protocol/defense-visitors.md)
- [Farming and gathering](protocol/farming-gathering.md)
- [UI tests and watch mode](protocol/ui-watch.md)
