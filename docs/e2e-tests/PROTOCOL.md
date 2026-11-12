[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md) · [Upstream bug reports](../Upstream-bugs.md)

# Test protocol: MineColonies end-to-end tests (Fabric port)

This is the summary of all end-to-end test work. Details per group are in the [group protocols](#group-protocols). The bugs found in the
original MineColonies and Structurize code are written up for reporting in [Upstream-bugs.md](../Upstream-bugs.md).

## Summary

- **Date:** 2026-11-12 to 2026-11-13.
- **Branch:** `e2e-tests`. The final runs are on the commit that adds the round-2 version of this file.
- **Game:** Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.2+26.3, Windows 11.
- **Result:** every designed test that can run in the test environment is implemented and passes; every mod bug the tests found is
  fixed, and its test is required. No optional (`required = false`) tests are left.

### How the tests were made

1. **Framework first** (commit `bc90de050c`): colony fixture, test arena, one colony test at a time, a pathfinding wait, test config.
2. **Seven agents in parallel**, each in its own worktree: six wrote one suite group each (construction/research, citizens/services,
   logistics/crafting, colony/connections/robustness, defense/visitors, farming/gathering). The seventh built watch mode and the UI tests.
3. **Merge** of the seven branches (commits `aca36796ed` .. `aa8e3cb6de`), then three full runs: same result each time.
4. **Framework fixes** found by the agents (commit `396b6db53a`, see [Framework issues fixed](#framework-issues-fixed-during-the-work)).
   They made the miner tests possible.
5. **Miner, quarrier and nether tests** GAT-03..06 (commit `25ee2d3495`).
6. **First final step** (commit `cecb684dce`): the flaky herder test fixed, full runs until two in a row passed (12 tests made more
   robust, see [Flakiness](#flakiness-first-final-step)), the first version of this protocol. At that point 16 tests were optional bug tests.
7. **Round 2: bug fixes** (see [Round 2](#round-2-bug-fixes)): five agents fixed every bug in parallel, the branches were merged, four
   tests that failed now and then under full-run load were made robust (plus one that the compost fix slowed down), and the bugs in original code were collected in
   [Upstream-bugs.md](../Upstream-bugs.md).

### Test counts

| What | Count |
|---|---|
| Designed test IDs (rows in the [overview](../E2E-tests.md)) | 112 |
| ✅ Passing | 107 |
| ❌ Fails because of a mod bug | 0 |
| ⛔ Not implementable | 5 (ROB-01..04, DEF-10), plus parts of DEF-02 (cavalry) and DEF-06 (ship raids) |
| 📝 Planned | 0 |
| Server game test methods (headless run) | 154, all required |
| UI test methods (`runClientGameTest`) | 9 (UI-01..09), all required |

`./gradlew build` no longer runs the game tests (round 2); run them with `runGameTest` and `runClientGameTest`.

### Final runs

Three full headless runs, `./gradlew --no-daemon runGameTest`, on the final code, one after the other on the same machine, then the UI
tests once:

| | Run 1 | Run 2 | Run 3 |
|---|---|---|---|
| Game tests | 154 | 154 | 154 |
| Required passed | 154 of 154 | 154 of 154 | 154 of 154 |
| Optional tests | none | none | none |
| Wall time (Gradle included) | 5 min 3 s | 5 min 17 s | 4 min 45 s |
| Test server time | 4.55 min | 4.83 min | 4.31 min |

**UI tests** (`./gradlew --no-daemon runClientGameTest`): UI-01..09 all passed, 1 min 6 s wall time.

**Run time:** earlier full runs on this machine took 2.5-4 minutes wall time; in this round 2 min 55 s to 5 min 17 s (server time 2.4-4.8
minutes). The spread comes from the random AI (long tests such as CRF-06 take 20,000-34,000 ticks), the longer florist test, and the
machine's load. Ticks run as fast as the CPU allows.

## Round 2: bug fixes

Five agents, each in its own worktree on `cecb684dce`, merged with `--no-ff` in this order:

| Branch (merge commit) | What |
|---|---|
| `worktree-agent-a0f8cd80cd6f3aae7` | Fabric-port bugs: the farmer's soil check uses `BlockTags.SUBSTRATE_OVERWORLD` (also two places in vendored Structurize) (E2E-FRM-01-1), the fisherman's FISHING loot parameters and `ResearchUnlocked` (E2E-FRM-07-1), the brewing teaching window looks the potion up in the synchronized recipes (E2E-CRF01-2) with new UI test UI-09, new test `frm01_farmerTillsOtherSoils`. |
| `worktree-agent-a25a12751c56c1bea` | E2E-LOG06-1 was a test bug (test fixed); E2E-GAT-03-1 fixed in `MovementHandler` (upstream bug). |
| `worktree-agent-aed880abfef73f8e8` | 8 upstream fixes: happiness average, study chance, friends' hut access, connection sign unlink, ally teleport range, quest cancel, tavern removal, raid end uses the last raid. |
| `worktree-agent-adf382d070a92752f` | 7 upstream fixes: crusher, enchanter, miner liquids, nether worker, herder mate range 10 → 8 (beekeeper too), compost timer, Structurize entity material count. |
| `worktree-agent-ac07a28591d170d77` | Test infrastructure: leisure breaks shortened to 60 ticks instead of ended at once, new test CIT-15 (leisure break); SRV-06 (graveyard revival) replaces `srv04b`; watch mode camera follows the test's focus (`WatchFocus`); game tests no longer part of `check`/`build`; README. |

**Merge conflicts:** only `docs/e2e-tests/PROTOCOL.md` (the bug table, twice); resolved by keeping every branch's status. The expected
conflict in `CitizensServicesTests.java` (`srv04b` removed in one branch, nearby edits in another) merged cleanly.

**Every bug test is required now.** The 16 optional tests of the first final step either became required (bug fixed, or test fixed for
E2E-LOG06-1) or were replaced (`srv04b` → SRV-06).

**Two fixes change gameplay balance** and are flagged for the MineColonies maintainers in [Upstream-bugs.md](../Upstream-bugs.md): the
study chance (E2E-SRV02-1, the base chance drops from 87.5% to 12.5%) and the happiness average (E2E-CIT06-1, neutral factors count
again, typical values rise).

### Flaky required tests (round 2)

Four required tests were known to fail now and then in full runs only; a fifth (`frm03_florist`) failed once in the verification runs
of this round. All causes were in the tests, none in the mod:

| Test | What happened | Cause | Fix (test side) |
|---|---|---|---|
| `frm06_chickenHerderBreeds` | Setup failed at tick 0: "no pen inside the hut area". | The pen search used `Level.canSeeSky`, which reads the sky light. The light engine updates the pasted hut asynchronously; usually it hadn't yet, so the coop of the chicken farmer counted as open and the chickens went there (pen of 3 positions, confirmed in a traced run). Under full-run load the light was sometimes already updated, the coop counted as covered, and nothing was left at that height (the hut's real pen is sunk two blocks lower). | No sky check: the result is the one of the passing runs, every time. |
| `cit13_gettingUnstuck` | Timeout "worker got out of the hole". | 17 logged runs needed 1,920-4,260 ticks (the stuck handler escalates in steps, the last one teleports); the budget was 5,000. | Budget 12,000 (test 15,000). |
| `srv03_hospital` | Timeout "influenza cured, measles cure requested, ...". | A patient waiting in a hospital bed recovers by himself with a 1 in 7,200 roll every 20 ticks (`EntityAISickTask.waitForCure`), about 1.5% within the usual 2,100 ticks. When it hit the measles patient (no cure in stock), "measles still sick" never came true. Before the leisure fix, leisure breaks were the other cause. | Conditions are latched; the measles patient may end healthy only while no measles cure item is in the hospital, with the healer or with him (an extra check makes sure). Nothing the test checked is dropped. |
| `def05_injuredGuardRetreats` | Timeout "guard attacks the husk", worker AI `GUARD_SLEEP`. | A guard with no target falls asleep at random (`AbstractEntityAIGuard.shouldSleep`, about 1 in 35 every 10 s at skill 30, for 2,500-3,000 ticks) and wakes only when hurt or woken by another guard. The test's husk is weakened so it can't hurt him. Intended mod behavior. | A sleeping guard counts as ready and is woken the way a hit wakes him (`setLastHurtByMob(husk)`); retreat and recovery are checked unchanged. |
| `frm03_florist` | Timeout "the compost is used up" (2 compost left), once in the round-2 verification runs. | The E2E-FRM-03-1 fix: each compost now lasts its full 300 s (150 s at the test's double speed) instead of expiring early, so using up 3 per bed ends at about test tick 11,000-12,000 regardless of when the step starts. The step had 10,000 ticks from its start. | Budget 20,000 (test 34,000). |

**Results.** Before the four fixes, the merged code (run 0, 3 min 6 s) happened to pass all 154 required tests. With the four fixes, three full runs
before the florist budget: runs 1 and 3 passed, run 2 failed only `frm03_florist` (see the table). After the florist budget the three
final runs above passed. In these six runs: `frm06_chickenHerderBreeds` set up every time (chick after 379-549 ticks), `cit13_gettingUnstuck`
got out after 2,112-4,248 ticks (budget now 12,000), `srv03_hospital` finished after 2,100-2,195 ticks,
`def05_injuredGuardRetreats` finished after 2,060-2,520 ticks, and `frm03_florist`'s compost step took 7,045-9,945 ticks in the passing runs and more than 10,000 in the failing one (budget now 20,000).

## Open Fabric-port TODOs (missing features, not bugs)

Features of the NeoForge original that the port doesn't have yet. None of them is a bug the tests found; each is marked
`FABRIC-PORT TODO` (or documented as a limitation) in the code, checked with grep for this list.

| What | Where |
|---|---|
| The spear's custom item renderer is not registered (`SpearItemTileEntityRenderer`; 26.x needs a `SpecialModelRenderer` in the item model definition). | `MineColonies.java:491` |
| Child citizens use adult proportions (1.21.1 scaled head and body via `HumanoidModel#young`; 26.x needs baby meshes). | `RenderBipedCitizen.java:86` |
| Structurize's solid placeholder blocks ignore worldgen surface rules (26.x replaced `SurfaceRules`); they fall back to the dimension's default block or dirt. | `vendor/structurize/.../BlockUtils.java:166` |
| JEI: crafter recipes derived from vanilla recipes are missing on dedicated servers (needs a recipe sync). | `JEIPlugin.java:203` |
| JEI: blocks are drawn from their item model (26.x GUI has no immediate-mode 3D block rendering). | `compatibility/jei/RenderHelper.java:36` |
| Dynamic Trees compatibility removed (no Fabric 26.x release); the no-op proxy stays. | `CompatibilityManager.java:838` |
| NeoForge's `PreventRemoteMovement` flag is ignored. | `AbstractEntityAIInteract.java:375` |
| `ItemFireArrow`: the `createEntity` hook (custom item entity) is never called. | `ItemFireArrow.java:28` |
| Event shim limitations: use-block/use-item denial only partly supported; client `EntityJoinLevel` can't be cancelled; `ArrowLooseEvent` ignores the bow charge; forced pickup / `ItemEntityPickupEvent.Post`; registry aliases; staged tag conditions; creative tab event modifications; `ModMismatchEvent` never fires. | shim package `com.ldtteam.shim` |
| **DONE:** in-game config screen. Mod Menu (optional) opens the settings of MineColonies and Structurize, built from vanilla widgets: client, common and server settings, saved to the files the shim loads; server settings editable only in a singleplayer world, read-only otherwise (as on NeoForge). Tests UI-10, UI-11. | `ConfigurationScreen.java`, `ConfigurationSectionScreen.java`, `ShimModMenuIntegration.java` |
| **REMOVED (unused):** atlas metadata (`RegisterTextureAtlasesEvent#addAdditionalMetadata`). No mod code calls it; MineColonies and Structurize pass their metadata section (`GuiMetadataSection`) in the `AtlasConfig` they register. | `RegisterTextureAtlasesEvent.java` |
| **REMOVED (unused):** client particle hooks (`IClientBlockExtensions` `addHitEffects`/`addDestroyEffects`, `RegisterClientExtensionsEvent#registerBlock`). No block registers client block extensions (neither here nor in upstream MineColonies 1.21). | `RegisterClientExtensionsEvent.java` |
| About 20 Domum Ornamentum blocks ignore the positional `getSoundType` overload (vanilla sound type is used). | e.g. `vendor/domumornamentum/.../AllBrickBlock.java:99` |
| Structurize's scan tool and tag substitution items have no highlight tip (selected item name overlay). | `ItemScanTool.java:257`, `ItemTagSubstitution.java:116` |

## Flakiness (first final step)

**The flaky chicken test.** `frm06_chickenHerderBreeds` (required) timed out once in a full run (no chick in 6,000 ticks) and
passed alone twice. Cause: bug [E2E-FRM-06-1](#e2e-frm-06-1-herder-feeds-animals-that-are-too-far-apart-to-mate) plus the test's
slowed animals. The herder fed a pair up to 10 blocks apart; vanilla's mate search is 8 blocks. Slowed animals that drifted 8-10
blocks apart stayed there, so each try failed and the herder waited 60 s. In 200 traced repeats one took 5,148 ticks. Fix (test side):
while neither animal is in love and they are more than 6 blocks apart, the second is put back next to the first; animals that got out
of the hut area are put back in the pen. After the fix: chicken 200 of 200 (at most 699 ticks), swine 100 of 100 (at most 1,194).
`frm06_swineHerderBreeds` was optional for the same reason and is required again. The bug itself is fixed in round 2.

**More rare failures found while verifying.** Full runs were repeated until two in a row passed all required tests. Of the 12 full
runs before the final two, 8 had 1-3 required failures (almost every time a different test, never a mod bug) and 4 passed. Causes and
fixes:

| Test | What happened | Fix |
|---|---|---|
| `def03_patrolGuardFollow` | Guards nap 2500-3000 ticks at random, and a guarding guard walks to his tower to dump, idles and walks back lazily. 6,000 and later 12,000 ticks ran out (1 of 51 repeats). | Budgets up to 30,000 ticks. |
| `frm06_swineHerderBreeds` | The pigs slipped out of the hut area; the piglet was born outside it. | Animals outside the area are put back in the pen. |
| `srv03_hospital`, `log09_fullWarehouse`, `log01_warehouseDeliversRequest`, `srv02_library` | A citizen went on a random 3-minute leisure break (`CitizenData.update`). | The test mod ended leisure breaks at once; since round 2 it shortens them to 3 seconds ([framework.md](framework.md#test-configuration)); two budgets raised too. |
| `srv04_graveyard` | The undertaker resurrected the citizen (about 3% without research), so he was never buried. | The roll is forced to miss (SRV-06 tests resurrection). |
| `def11_raidTimingFollowsConfig` | No raid in ten nights: the raid's spawn search goes outwards while chunks are loaded and ended past the ground ring. Depends on which chunks other tests left loaded. | Sky raiders on (the test checks timing, not spawn places). |
| `crf01_alchemist` | Between brews the alchemist went for mistletoe without shears and waited on a tool request. | Shears in the warehouse. |
| `crf06_alchemistHarvests` | All 3 attempts missed the mistletoe (random job choice). | 5 attempts. |
| `gat05_quarrierDigsLayers`, `frm02_planterBamboo`, `crf04_concreteMixerWater`, `crf01_fletcher` | Normal but slow runs went past the budget (e.g. quarry 34,000, bamboo 20,000). | Budgets roughly doubled. |

Budgets are in game ticks and cost little real time, so they were raised generously. Each fix is noted in the group protocol's table.

## Bugs found

20 reports: 19 real bugs, all **FIXED**, and 1 that was not a mod bug (E2E-LOG06-1, a test setup problem). By origin: 3 specific to the
Fabric port, 15 in original MineColonies code and 1 in original Structurize code. The 16 bugs in original code are written up for
reporting, with steps to reproduce and patches, in [Upstream-bugs.md](../Upstream-bugs.md). Each fix has a required test.

| ID | Title | Severity | Origin | Where | Status / test |
|---|---|---|---|---|---|
| [E2E-FRM-01-1](#e2e-frm-01-1-farmer-never-tills-grass-blocks) | Farmer never tills grass blocks | High | Fabric port (26.3 tag change) | `EntityAIWorkFarmer.java:337` | **FIXED**; `frm01_farmerTillsGrass`, `frm01_farmerTillsOtherSoils` |
| [E2E-FRM-07-1](#e2e-frm-07-1-fisherman-throws-on-every-catch) | Fisherman throws on every catch, never gets loot | High | Fabric port (26.3 loot params) | `NewBobberEntity.java:519-525` | **FIXED**; `frm07_fisherman` |
| [E2E-CRF01-2](#e2e-crf01-2-brewing-recipes-cant-be-taught-in-the-gui) | Brewing recipes can't be taught in the GUI | High | Fabric port | `WindowBrewingstandCrafting.java:122-127` | **FIXED**; UI-09 (client) |
| [E2E-CON-02-1](#e2e-con-02-1-ally-teleport-only-works-within-15-blocks-of-the-target-gate) | Ally teleport only works within 15 blocks of the target gate | High (for the feature) | Upstream (#11888) | `TeleportToColonyMessage.java:89` | **FIXED**; `con02b_allyTeleportFromOwnGate` |
| [E2E-CIT06-1](#e2e-cit06-1-negative-happiness-factors-can-raise-total-happiness) | Negative happiness factors can raise total happiness | Medium | Upstream | `CitizenHappinessHandler.java:148` | **FIXED** (gameplay change); `cit06b_negativeFactorsLowerHappiness` |
| [E2E-SRV02-1](#e2e-srv02-1-study-items-make-studying-less-successful) | Study items make studying less successful | Medium | Upstream (#7349) | `CitizenSkillHandler.java:152` | **FIXED** (gameplay change); `srv02b_studyItemsHelp` |
| [E2E-VIS04-1](#e2e-vis04-1-cancelling-an-accepted-quest-at-a-delivery-step-does-nothing) | Cancelling an accepted quest at a delivery step does nothing | Medium | Upstream | `QuestDeliveryInteraction.java:55-73` | **FIXED**; `vis04_cancelAcceptedQuestAtDelivery` |
| [E2E-CRF01-1](#e2e-crf01-1-crusher-over-crafts-a-request-and-wastes-ingredients) | Crusher over-crafts a request and wastes ingredients | Medium | Upstream | `EntityAIWorkCrusher.java:115-120, 228` | **FIXED**; `crf01_crusher` |
| [E2E-CRF07-1](#e2e-crf07-1-enchanter-drains-himself) | Enchanter drains himself instead of the station's workers | Medium | Upstream | `EntityAIWorkEnchanter.java:324` | **FIXED**; `crf07_enchanterDrainsStationWorker` |
| [E2E-GAT-03-1](#e2e-gat-03-1-miner-falls-off-the-shaft-ladder) | Miner falls off the shaft ladder | Medium | Upstream | `MovementHandler.java:138-150` (JUMPING state) | **FIXED**; `gat03_minerDigsShaftSafely` |
| [E2E-GAT-04-1](#e2e-gat-04-1-miner-never-removes-liquids-around-a-node) | Miner never removes liquids around a node | Medium | Upstream | `EntityAIStructureMiner.java:794-820` | **FIXED**; `gat04_minerSealsWater` |
| [E2E-GAT-06-1](#e2e-gat-06-1-nether-worker-idles-after-returning) | Nether worker idles after returning until the next trip is allowed | Medium | Upstream | `EntityAIWorkNether.java:156`, `AbstractEntityAICrafting.java:162` | **FIXED**; `gat06_netherWorkerReturnsAfterTravel` |
| [E2E-COL-05-1](#e2e-col-05-1-friends-cant-open-hut-menus) | Friends can't open hut menus | Low | Upstream | `ColonyPermissionEventHandler.java:372, 412` | **FIXED**; `col05b_friendOpensHut` |
| [E2E-CON-01-1](#e2e-con-01-1-a-removed-connection-sign-leaves-a-stale-link) | A removed connection sign leaves a stale link in the other colony | Low | Upstream | `ColonyConnectionManager.java:169, 372` | **FIXED**; `con01b_reconnectAfterSignRemoved` |
| [E2E-VIS02-1](#e2e-vis02-1-removing-a-tavern-right-after-a-recruit-throws) | Removing a tavern right after a recruit throws and leaves visitors behind | Low | Upstream | `TavernBuildingModule.java:309-313`, `VisitorManager.java:301` | **FIXED**; `vis02_tavernRemovedAfterRecruiting` |
| [E2E-FRM-06-1](#e2e-frm-06-1-herder-feeds-animals-that-are-too-far-apart-to-mate) | Herder feeds animals that are too far apart to mate | Low | Upstream | `AbstractEntityAIHerder.java:78, 454` | **FIXED** (beekeeper too); `frm06_herderFeedsPairOutOfMateRange` |
| [E2E-FRM-03-1](#e2e-frm-03-1-composted-bed-expires-at-once-after-it-sat-idle) | Composted bed expires at once after it sat idle | Low | Upstream | `TileEntityCompostedDirt.java:75, 122` | **FIXED**; `frm03_compostLastsOnIdleBed` |
| [E2E-BLD-07-1](#e2e-bld-07-1-resource-list-misses-items-of-blueprint-entities) | Resource list misses items of blueprint entities | Low | Upstream (Structurize) | Structurize `StructurePlacer.handleEntitySpawn` | **FIXED**; `bld07_decorationWithEntitiesMaterialList` |
| [E2E-DEF-S1](#e2e-def-s1-raid-end-reads-the-first-raid-instead-of-the-last) | Raid end reads the first raid instead of the last | Low | Upstream | `RaidManager.java:1061, 1078` | **FIXED**; `defs1_raidEndUsesLastRaid` |
| [E2E-LOG06-1](#e2e-log06-1-orphaned-delivery-a-tool-request-is-never-delivered) | Orphaned delivery: a tool request is never delivered | – | Not a mod bug (test setup) | test `log06_toolsByHutLevel` | Test fixed; `log06_toolsByHutLevel` |

"Upstream" means the same code is in MineColonies `upstream/version/1.21` (the port's base) and, unless [Upstream-bugs.md](../Upstream-bugs.md)
says otherwise, in `version/main` (1.20.1). E2E-DEF-S1 had no ID in its group protocol; the ID is new here.

### Bug details

#### E2E-FRM-01-1 Farmer never tills grass blocks

**FIXED** (Fabric port): the checks use `BlockTags.SUBSTRATE_OVERWORLD` (the same blocks as 1.21.1's `#minecraft:dirt`) in the farmer
and in two places in vendored Structurize (`GrassPlacementHandler`, `BlockUtils` fancy dirt match).

`EntityAIWorkFarmer.findHoeableSurface` accepted only `BlockTags.DIRT` and farmland. In Minecraft 26.3 the `#minecraft:dirt` tag no
longer has `grass_block` (in 1.21.x it had). A field on grass was never tilled. The new test `frm01_farmerTillsOtherSoils` covers coarse
dirt, rooted dirt, podzol and mycelium. More: [farming protocol](protocol/farming-gathering.md#e2e-frm-01-1-farmer-never-tills-grass-blocks).

#### E2E-FRM-07-1 Fisherman throws on every catch

**FIXED** (Fabric port): the FISHING loot parameters are built like vanilla 26.3's `FishingHook` (in `NewBobberEntity` and the
fisherman's bonus loot); `ResearchUnlocked` resolves the bobber to its owner citizen.

`NewBobberEntity.retrieve` passed `ATTACKING_ENTITY` to the FISHING loot parameters. 26.3 doesn't allow it there, so every catch threw
`IllegalArgumentException` and the AI paused. More:
[farming protocol](protocol/farming-gathering.md#e2e-frm-07-1-fisherman-throws-on-every-catch-never-gets-loot).

#### E2E-CRF01-2 Brewing recipes can't be taught in the GUI

**FIXED** (Fabric port): the window looks the potion up in the client's synchronized brewing recipes. UI test UI-09 checks water bottle
plus nether wart gives an awkward potion on the client.

26.3 made brewing data-driven; the teaching window couldn't compute the output, so it never sent the recipe. More:
[logistics protocol](protocol/logistics-crafting.md#e2e-crf01-2-fabric-port-brewing-recipes-cant-be-taught-in-the-gui).

#### E2E-LOG06-1 Orphaned delivery: a tool request is never delivered

Not a mod bug. The test's request list collects every state a request had, so the one shovel request showed up twice (an old
`FOLLOWUP_IN_PROGRESS` snapshot with its delivery, then `COMPLETED`); traced, the delivery went through the courier normally. The miner
was really waiting for cobblestone nobody had, and under the arena's dirt was only the barrier floor, so he never needed a pickaxe. The
test now uses stone ground and puts cobblestone and ladders into the hut; it is required. More:
[logistics protocol](protocol/logistics-crafting.md#e2e-log06-1-orphaned-delivery-a-tool-request-is-never-delivered-fixed-not-a-mod-bug-test-setup).

#### E2E-CON-02-1 Ally teleport only works within 15 blocks of the target gate

**FIXED:** the range is measured to the origin colony's gate houses.
[Upstream report](../Upstream-bugs.md#e2e-con-02-1-ally-teleport-only-works-when-the-player-is-within-15-blocks-of-the-target-gate).

`TeleportToColonyMessage` checked that the player is within 15 blocks of the *target* colony's gate. The button is in the player's
*own* gate house window, so it only worked when the two gates are close. Introduced by "Packet range (#11888)". More:
[colony protocol](protocol/colony-connections-robustness.md#e2e-con-02-1-ally-teleport-only-works-when-the-gates-are-at-most-15-blocks-apart).

#### E2E-CIT06-1 Negative happiness factors can raise total happiness

**FIXED:** all factors are averaged again. Changes gameplay (typical happiness rises); flagged for the maintainers.
[Upstream report](../Upstream-bugs.md#e2e-cit06-1-unemployed-or-sick-citizens-can-be-happier-than-healthy-employed-ones).

`getHappiness` skipped factors that are exactly 1.0 and averaged the rest. A below-neutral factor above the current average pulled the
average up. In the test, an unemployed or sick citizen had 3.50, the healthy employed one 3.00. More:
[citizens protocol](protocol/citizens-services.md#e2e-cit06-1-negative-happiness-factors-can-raise-total-happiness).

#### E2E-SRV02-1 Study items make studying less successful

**FIXED:** the success chance is `1 / customChance`. Changes gameplay (base chance 87.5% → 12.5%, items raise it as configured); flagged
for the maintainers. [Upstream report](../Upstream-bugs.md#e2e-srv02-1-library-study-items-make-studying-less-successful).

Since #7349 the success check was inverted. 4000 rolls each: no item 89%, paper 50%, book 26%. The book, configured as the best item,
was the worst. More: [citizens protocol](protocol/citizens-services.md#e2e-srv02-1-study-items-make-studying-less-successful).

#### E2E-VIS04-1 Cancelling an accepted quest at a delivery step does nothing

**FIXED:** the cancel answer is applied to the quest.
[Upstream report](../Upstream-bugs.md#e2e-vis04-1-cancelling-an-accepted-quest-at-a-delivery-step-does-nothing).

At a delivery step, "cancel" only closed the dialogue; `applyToQuest` (which deletes the quest) was never called. More:
[defense protocol](protocol/defense-visitors.md#e2e-vis04-1-cancelling-an-accepted-quest-at-a-delivery-step-does-nothing).

#### E2E-CRF01-1 Crusher over-crafts a request and wastes ingredients

**FIXED:** after the last craft `craft()` returns `finalizeCraftingTask()` (dump, then `afterDump()` closes the request).
[Upstream report](../Upstream-bugs.md#e2e-crf01-1-crusher-crafts-a-request-again-before-it-is-closed-wastes-ingredients).

After the last craft for a request the crusher reset, but the request stayed open until the worker dumped. On the next tick it took
the crusher-mode recipe and, in state `CRAFT`, skipped the daily limit and crafted again. A request for 2 gravel used 8 cobblestone
(about 2 of 3 runs). More: [logistics protocol](protocol/logistics-crafting.md#e2e-crf01-1-crusher-over-crafts-a-request-and-wastes-ingredients).

#### E2E-CRF07-1 Enchanter drains himself

**FIXED:** the citizen is picked from the station building's job modules.
[Upstream report](../Upstream-bugs.md#e2e-crf07-1-enchanter-drains-himself-instead-of-the-workers-of-his-stations).

`gatherAndDrain` walked to the station building but picked the citizen from the enchanter's own work module. More:
[logistics protocol](protocol/logistics-crafting.md#e2e-crf07-1-enchanter-drains-himself-instead-of-the-stations-workers).

#### E2E-GAT-03-1 Miner falls off the shaft ladder

**FIXED:** JUMPING ends (forward input 0) when the navigation has no path.
[Upstream report](../Upstream-bugs.md#e2e-gat-03-1-miner-falls-off-the-shaft-ladder-when-he-stops-on-it).

Climbing the ladder, the citizen's `MovementHandler` is in state JUMPING, which sets the forward input every tick and ends only on the
ground. When the miner reached the spot to mine from while still on the ladder, the navigation was stopped, but JUMPING went on: he kept
walking in the direction he faced (turned towards the block he mines), climbed on and drifted sideways off the ladder, then fell 3.3-4
blocks. More: [farming protocol](protocol/farming-gathering.md#e2e-gat-03-1-miner-falls-off-the-shaft-ladder-fixed).

#### E2E-GAT-04-1 Miner never removes liquids around a node

**FIXED:** the liquid check runs before the node's blueprint is loaded.
[Upstream report](../Upstream-bugs.md#e2e-gat-04-1-miner-never-seals-water-or-lava-next-to-a-mine-node).

The liquid check around a node ran only when the node's blueprint was already loaded. After loading, the AI went on building and didn't
come back to the check. More: [farming protocol](protocol/farming-gathering.md#e2e-gat-04-1-miner-never-removes-liquids-around-a-node).

#### E2E-GAT-06-1 Nether worker idles after returning

**FIXED:** `hasWorkToDo()` is also true while the job is "in nether" (results to work off).
[Upstream report](../Upstream-bugs.md#e2e-gat-06-1-nether-worker-stands-at-the-portal-for-up-to-3-days-after-an-expedition).

The returned worker's AI started in IDLE and left it only when the next trip was allowed (one trip per 3-day period). More:
[farming protocol](protocol/farming-gathering.md#e2e-gat-06-1-nether-worker-idles-after-returning-until-the-next-trip-is-allowed).

#### E2E-COL-05-1 Friends can't open hut menus

**FIXED:** the container check skips huts (`ACCESS_HUTS` governs them).
[Upstream report](../Upstream-bugs.md#e2e-col-05-1-friends-cant-open-hut-menus).

The permission handler let a hut click through for `ACCESS_HUTS`, then treated the hut as a container (it has an item handler) and
refused it with `OPEN_CONTAINER`, which friends don't have. More:
[colony protocol](protocol/colony-connections-robustness.md#e2e-col-05-1-friends-cant-open-hut-menus).

#### E2E-CON-01-1 A removed connection sign leaves a stale link

**FIXED:** the partner sign in the other colony is unlinked too.
[Upstream report](../Upstream-bugs.md#e2e-con-01-1-a-removed-connection-sign-leaves-a-stale-link-in-the-other-colony).

Removing a connection sign only reset pointers in its own colony; the partner sign still pointed at the removed sign, so a new link
was refused. More:
[colony protocol](protocol/colony-connections-robustness.md#e2e-con-01-1-a-removed-connection-sign-leaves-a-stale-link-in-the-other-colony).

#### E2E-VIS02-1 Removing a tavern right after a recruit throws

**FIXED:** `onDestroyed` skips unknown visitor ids.
[Upstream report](../Upstream-bugs.md#e2e-vis02-1-removing-a-tavern-right-after-recruiting-a-visitor-throws-and-leaves-visitors-behind).

The tavern kept a recruited visitor's id until its next colony tick. Removing the tavern before that passed a null visitor to
`VisitorManager.removeCivilian`: `NullPointerException`, later visitors stayed registered. More:
[defense protocol](protocol/defense-visitors.md#e2e-vis02-1-removing-a-tavern-right-after-a-recruit-throws-and-leaves-visitors-behind).

#### E2E-FRM-06-1 Herder feeds animals that are too far apart to mate

**FIXED:** `DISTANCE_TO_BREED` = 8 in the herder and beekeeper AI.
[Upstream report](../Upstream-bugs.md#e2e-frm-06-1-herder-and-beekeeper-feeds-animals-that-are-too-far-apart-to-mate).

The herder paired animals up to 10 blocks apart; vanilla's `BreedGoal` looks for a partner only within 8 blocks. A pair 8-10 blocks
apart was put in love, never met, the food was wasted and the herder waited 60 s. This caused the first flaky chicken test. More:
[farming protocol](protocol/farming-gathering.md#e2e-frm-06-1-herder-feeds-animals-that-are-too-far-apart-to-mate).

#### E2E-FRM-03-1 Composted bed expires at once after it sat idle

**FIXED:** `compost()` resets the timer; new test `frm03_compostLastsOnIdleBed`.
[Upstream report](../Upstream-bugs.md#e2e-frm-03-1-florist-compost-on-a-bed-that-sat-idle-expires-at-once).

The compost timer counted on every tick, also while the bed wasn't composted. A bed idle for more than 300 s lost new compost at its
first update. More:
[farming protocol](protocol/farming-gathering.md#e2e-frm-03-1-composted-bed-expires-at-once-after-it-sat-idle-minor-observation).

#### E2E-BLD-07-1 Resource list misses items of blueprint entities

**FIXED** (Structurize): the resource count (`StructurePlacer.handleEntitySpawn`, simulate mode) counted only the first entity of a
block position; now all. New required test `bld07_decorationWithEntitiesMaterialList` (failed before: 1 torch, 1 item frame short).
[Upstream report](../Upstream-bugs.md#e2e-bld-07-1-material-list-and-builders-request-list-misses-blueprint-entities-that-share-a-block-structurize).
More: [construction protocol](protocol/construction-research.md#e2e-bld-07-1-resource-list-misses-items-of-blueprint-entities-confirmed-fixed).

#### E2E-DEF-S1 Raid end reads the first raid instead of the last

**FIXED:** uses `getLastRaid()`; covered by `defs1_raidEndUsesLastRaid`.
[Upstream report](../Upstream-bugs.md#e2e-def-s1-raid-end-uses-the-colonys-first-raid-instead-of-the-one-that-just-ended).

`RaidManager.onRaidEventFinished` read `raidHistories.get(0)` (the colony's first raid) for the mercy check and the victory sound,
instead of the last raid. More: [defense protocol](protocol/defense-visitors.md#suspect-code-reading-not-reproduced-by-a-test).

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
- **Flaky tests** (final step and round 2, test side): herder animals kept within mate range and inside the hut area, and the fixes in
  the [Flakiness](#flakiness-first-final-step) and [round 2](#flaky-required-tests-round-2) tables.

Still open: the town hall can't be rotated; `-Pe2eFilter` supports only `*` and `?`; no seeded random source for the AI; UI tests need
a desktop with a GPU; don't run `runGameTest` and `runClientGameTest` in the same checkout at once (Loom clears the run folder).

## Design corrections

The test designs were written before the code was read. Where MineColonies works differently, the test follows the mod and the design
doc got an "As implemented" or "As tested" note. The main ones:

- **Colony** ([colony.md](colony.md)): growth beyond the first citizens is only by children (COL-03). Friends couldn't open huts (bug E2E-COL-05-1, fixed).
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
- **Visitors** ([visitors-quests.md](visitors-quests.md)): cancel of an accepted quest is only offered at delivery steps (and did nothing there: bug E2E-VIS04-1, fixed).

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
