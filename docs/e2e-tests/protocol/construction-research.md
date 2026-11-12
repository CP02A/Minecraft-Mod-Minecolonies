[← Construction tests](../construction.md) · [Research tests](../research.md)

# Protocol: construction and research tests

Suite class: `src/gametest/java/com/minecolonies/gametest/suites/ConstructionResearchTests.java`, helpers in
`src/gametest/java/com/minecolonies/gametest/suites/construction/ConstructionSupport.java` (no game tests there, no
`fabric.mod.json` change). Run: `./gradlew --no-daemon runGameTest -Pe2eFilter=construction_research_tests`.

Final run: **all 14 required tests passed**, about 1 minute for the whole server run (the long builds take about 10-15 s real time).

| ID | Status | Runtime (ticks) | Notes |
|---|---|---|---|
| BLD-01 | ✅ | ~14,800-18,400 | Residence 1, survival build tool placement, materials from the hut window's resource list. Over the doc's 10 min budget; test allows 42,000. |
| BLD-02 | ✅ | ~14,900-16,500 | Warehouse + courier deliver; every requested stack is on the material list; no player resolver. |
| BLD-03 | ✅ | ~3,400-3,500 | Lumberjack 1 → 2; more inventory slots at level 2; lumberjack keeps his job throughout. |
| BLD-04 | ✅ | ~2,000-2,200 | 6 blocks removed, 3 replaced with diamond blocks; undamaged positions compared with their pre-damage state. |
| BLD-05 | ✅ | ~5,200 | Design changed: after removal the hut stays registered as *deconstructed* (level kept); pick-up then unregisters it and the worker loses his job. |
| BLD-06 | ✅ | ~215 | Hut window refuses the order; order added directly; not claimed in 200 ticks; claimed 15 ticks after `setBuildingLevel(2)`. |
| BLD-07 | ✅ | ~1,850 | `DecorationBuildRequestMessage` server side; entity-free decoration (`pillar`). No building registered. |
| BLD-07 (entities) | ✅ | <7,000 | `bld07_decorationWithEntitiesMaterialList`: builder given exactly the computed material list of `pedastal_oak` (4 entities) finishes. E2E-BLD-07-1 **FIXED**; failed before the fix (`NEEDS_ITEM`, 1 torch + 1 item frame). |
| BLD-08 | ✅ | ~3,250 | Three small decorations; two builders take one each; the third (assigned to builder 2) only ever claimed by builder 2. |
| BLD-09 | ✅ | ~18,000 | Dirt bumps, short grass, 3-log tree with leaves, 2×2 water pool inside the footprint; all cleared, world matches blueprint. |
| RES-01 | ✅ | ~2,550 | First Aid started by a survival player (hay taken from the university), skipped to 3 steps before the end; no progress in 1,100 ticks without researcher. |
| RES-02 | ✅ | <5 | Keen: refused (books kept) with residence levels 1; started (books taken) after adding a level 3 residence. |
| RES-03 | ✅ | <5 | Design changed: one colony, before/after. Max health attribute rises by the effect strength, also for a citizen spawned later. |
| RES-04 | ✅ | <5 | Hospital build refused until Stamina is finished. Guard types not covered. |
| RES-05 | ✅ | <5 | Design changed: undo of a parent with a finished child is refused (nothing taken); child then parent undone, effect back to the parent's strength, then 0; one ancient tome taken per undo. |

Counts: ✅ 14 · ❌ 0 · ⛔ 0 · 📝 0.

## Bugs found

No test fails because of a mod bug. One low-severity problem, confirmed and fixed:

### E2E-BLD-07-1 Resource list misses items of blueprint entities (confirmed, FIXED)

- **FIXED** (in the vendored Structurize): `StructurePlacer.handleEntitySpawn` with `simulate = true` returned after the first entity of a
  block position; entities sharing a block (here a second item frame on the same block, holding a torch) were never counted. It now
  collects the items of all entities of the position. The list is now `4 item_frame, 7 torch`. Confirmed by the new required test
  `bld07_decorationWithEntitiesMaterialList` (failed before the fix with `still needed: 1xminecraft:torch, 1xminecraft:item_frame`).
  The same count feeds `WindowBuildBuilding`/`WindowBuildDecoration#updateResources` and the builder's `ENTITIES` request stage. The
  analysis below is the original suspicion. Upstream: Structurize; [upstream issue](../../Upstream-bugs.md#e2e-bld-07-1-material-list-and-builders-request-list-misses-blueprint-entities-that-share-a-block-structurize).

- **Severity:** low (only the hut/decoration window's material list; the builder itself computes his needs correctly and requests the
  missing items).
- **Port-specific or upstream:** likely upstream. The list is computed by `WindowBuildBuilding#updateResources` /
  `WindowBuildDecoration` with Structurize's `StructurePlacer` (`GET_RES_REQUIREMENTS`); none of this is Fabric-specific code.
- **Evidence:** `ConstructionSupport.materials` repeats that computation server-side. For `decorations/statues/pedastal_oak`
  (4 entities) it lists `3 item_frame, 6 torch` (log line `BLD-07: material list of pedastal_oak (4 entities): ...`). A builder given
  exactly that list stopped in `NEEDS_ITEM`, still needing `1x torch, 1x item_frame` and requesting an item frame (first runs of
  BLD-07, before the test switched to an entity-free decoration). So one entity (an item frame holding a torch) seems not to be
  counted.
- **Reproduction:** BLD-07 with `ConstructionSupport.entityFreeDecoration` replaced by `"decorations/statues/pedastal_oak.blueprint"`.
  Not verified in the real client GUI.

## Design changes

- **BLD-01/02/09 time budget:** a level 1 residence takes 15,000-18,000 ticks, more than the documented 10-15 minutes. Budgets in the
  tests raised; measured values added to construction.md.
- **BLD-05:** removing a hut does not unregister it. MineColonies keeps it as *deconstructed* with its level (the hut window then
  offers "pick up", and the picked-up hut item remembers the level). The test now also does the pick-up, after which the building is
  gone and the worker unemployed.
- **BLD-06:** the situation "level 2 order with only a level 1 builder" can't be created through the hut window (it refuses);
  the order is added to the work manager directly.
- **BLD-07/08:** decorations come from the server side of `DecorationBuildRequestMessage`; an entity-free decoration is used (see the
  suspected bug). BLD-08 uses decorations as its "build work orders".
- **RES-02/05:** must be done by a survival player: for creative players the server skips requirements, research cost and undo cost.
- **RES-03:** one colony measured before/after (the fixture handles one colony per test); the effect is maximum health.
- **RES-05:** dependent research is not reset with its parent; undoing a parent is refused while a child exists.
- **RES-04:** a locked hut can be placed; the build request is what's refused.

## Framework issues

- **Fresh worktree can't start the test server:** `src/main/resources/assets/minecolonies/lang/en_us.json` is git-ignored, so a new
  worktree lacks it and the server dies at start-up (`NullPointerException` in `LanguageHandler$LanguageCache.load`, from
  `MineColonies.<init>`). Worked around by copying the file from the main checkout (not committed).
- **The fake owner is shared between tests:** `FakePlayer.get` caches by profile, so inventory changes stay for later tests (the
  fixture only resets the game mode). RES-05 clears the owner's inventory before use; RES-01/02/05 switch to survival and back.
- **Helpers that could move into the framework** (in `ConstructionSupport`): survival hut placement (`SurvivalHandler`), blueprint
  material list, blueprint-vs-world comparison, decoration work order request, builder diagnostics (open requests, still-needed
  resources, inventory).
- `-Pe2eFilter` with `:` doesn't support character classes (`bld0[789]*` found no tests); only `*` and `?`.
- The scratchpad folder is shared between parallel agents: generic log names (`run1.log`) got overwritten by another group's run.
