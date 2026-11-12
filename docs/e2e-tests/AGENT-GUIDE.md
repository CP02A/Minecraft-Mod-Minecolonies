[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Writing end-to-end tests: practical guide

For whoever writes the tests designed in this folder. Read [framework.md](framework.md) once for the ideas; this page is the how-to.

## Ground rules

- **Never change MineColonies code** (`src/main`, `src/fabric`, `src/datagen`, `vendor/`). Tests exercise the real code. If a test
  finds a bug, the test fails and you report the bug (log lines, `file:line`); you don't fix it here.
- Put your tests only in **your suite class** in `src/gametest/java/com/minecolonies/gametest/suites/`. All six suites are already
  registered in `src/gametest/resources/fabric.mod.json`; don't edit that file, the framework classes or another group's suite. If
  you need a helper the fixture doesn't have, write it as a `private static` method in your suite and mention it in your report
  (it can move into the framework later).
- Results depend on game ticks only. Wait for outcomes (`awaitUntil`), never for real time, and never for an exact tick.
- In `docs/E2E-tests.md`, change only the status cells of your own rows (📝 → ✅). Leave the "Progress" line alone; it is updated
  after the groups are merged.

| Suite class | Test docs | Filter prefix |
|---|---|---|
| `ColonyConnectionsRobustnessTests` | colony.md, connections.md, robustness.md | `colony_connections_robustness_tests` |
| `ConstructionResearchTests` | construction.md, research.md | `construction_research_tests` |
| `CitizensServicesTests` | citizens.md, services.md | `citizens_services_tests` |
| `LogisticsCraftingTests` | logistics.md, jobs-crafting.md | `logistics_crafting_tests` |
| `FarmingGatheringTests` | jobs-farming.md, jobs-gathering.md | `farming_gathering_tests` |
| `DefenseVisitorsTests` | defense.md, visitors-quests.md | `defense_visitors_tests` |

## Running tests

```
./gradlew runGameTest                                   # everything (also part of ./gradlew build)
./gradlew runGameTest -Pe2eFilter=citizens_services_tests        # one suite
./gradlew runGameTest "-Pe2eFilter=minecolonies-gametest:*_cit01_*"   # one test (any vanilla selector with ':')
```

- `-Pe2eFilter=<prefix>` without `:` runs the tests whose id starts with `minecolonies-gametest:<prefix>`. With `:` the value is
  passed on unchanged as a vanilla test selector (`*` and `?` wildcards). It sets the system property `fabric-api.gametest.filter`.
- If the filter matches no test, the server exits with "Test selection matcher (...) found no tests" and Gradle reports a failure.
- Test id = `minecolonies-gametest:` + snake_case of `<SuiteClass><methodName>`. A digit does not start a new word
  (`col01FoundColony` → `..._col01found_colony`), so name methods with an underscore after the test id:
  `cit01_eatingWhenHungry` → `minecolonies-gametest:citizens_services_tests_cit01_eating_when_hungry`.
- Results: `build/gametest-report.xml` (JUnit format). Full log: `build/run/gameTest/logs/latest.log` (also printed to the console).
  Failed tests are listed at the end of the log after `required tests failed`.
- **Time:** about 25-45 s per run on this machine, almost all of it Gradle and server start-up (about 20 s). The 9 framework tests
  run in about 5-7 s. A colony test costs roughly 0.5-1 s of real time per 500 game ticks (a 64x64 arena with a few huts and
  citizens), plus about 0.5 s to set up. Budgets of several thousand ticks are fine.
- Each worktree has its own `build/` folder, so several worktrees can run tests at the same time.

## Skeleton of a colony test

```java
package com.minecolonies.gametest.suites;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.gametest.framework.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

public class CitizensServicesTests
{
    /**
     * CIT-xx Short title: what it tests.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 3000, skyAccess = true)
    public void citXX_shortTitle(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);   // floor, morning, clear weather, cleanup at the end
        final IBuilding[] hut = new IBuilding[1];                      // lambdas need effectively final holders
        final ICitizenData[] worker = new ICitizenData[1];

        fixture.flow()
          .run("set up colony", () -> {
              fixture.createColony();                                  // level 1 town hall at the arena center (32, 5, 32)
              fixture.setMoveIn(false);                                // no random starting citizens
              hut[0] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              worker[0] = fixture.spawnCitizen(20, hut[0], null);      // all skills 20, hired, no home
          })
          .awaitUntil("worker is at his hut", 2500,
            () -> worker[0].getEntity().map(e -> e.blockPosition().closerThan(hut[0].getPosition(), 4)).orElse(false),
            () -> ColonyFixture.describe(worker[0]))
          .check("hut is still level 1", () -> hut[0].getBuildingLevel() == 1, () -> "level " + hut[0].getBuildingLevel())
          .succeed();
    }
}
```

Working examples: `ColonyConnectionsRobustnessTests` (COL-01, a worker walking to his hut) and
`framework/FrameworkGuardTests.fixtureHelpersWork` (uses almost every helper).

Tests that touch only a block or an entity and need no colony: plain `@GameTest` (default 8x8x8 structure, run in parallel), like
`TransferBridgeGameTests`.

## `@GameTest` settings for colony tests

| Setting | Use |
|---|---|
| `environment = ColonyFixture.COLONY_ENVIRONMENT` | **Required** for every test that creates a colony: these tests run one at a time. |
| `structure = ColonyFixture.ARENA` | The 64x32x64 arena. |
| `maxTicks` | The time budget in ticks (20 ticks = 1 s of game time). |
| `skyAccess = true` | Recommended. Without it the arena has a barrier ceiling at height 32 (rain and sky checks see a roof). |
| `maxAttempts` / `requiredSuccesses` | For behavior that depends on chance. Re-runs of colony tests also run one at a time. |
| `rotation` | Leave at the default. The fixture assumes an unrotated arena. |

## Fixture API (`com.minecolonies.gametest.framework.ColonyFixture`)

Positions are **relative to the arena** (like `GameTestHelper`), unless the parameter is called `absPos`. The floor's top (grass) is
at y = `FLOOR_Y` (4); huts and citizens stand at y = `GROUND_Y` (5). `ARENA_CENTER` = (32, 5, 32).

| Method | What it does |
|---|---|
| `static ColonyFixture create(GameTestHelper helper)` | First line of every colony test. Lays the floor, sets time 1000 and clear weather, deletes leftover colonies (logs a warning), registers cleanup. |
| `IColony createColony()` / `createColony(BlockPos townHallPos)` | Pastes a level 1 town hall (build tool creative placement) and runs the server side of `CreateColonyMessage` with the fake owner. Synchronous: the colony exists when it returns. |
| `IColony colony()` | The colony (throws if none yet). |
| `IBuilding placeBuilding(BuildingEntry type, int level, BlockPos pos)` (+ `RotationMirror` overload) | Pastes blueprint `<registry path><level>` (e.g. `builder2`) from the "Minecolonies Original" pack instantly and registers the building at that level. Throws if the building isn't registered or has another level. Types: `ModBuildings.builder.get()`, `.home.get()` (residence), `.wareHouse.get()`, `.lumberjack.get()`, ... |
| `PastedBlueprint pasteHut(String name, int level, BlockPos absPos, RotationMirror rm)` / `paste(String subPath, BlockPos absPos, RotationMirror rm)` | Lower level: paste by blueprint name, or any blueprint of the pack by path (decorations, fields). |
| `ColonyFixture usePack(String packName)` | Paste later huts from another structure pack. |
| `ICitizenData spawnCitizen(int skillLevel, IBuilding work, IBuilding home)` | New citizen, all skills = `skillLevel`, hired into `work`, living in `home` (both may be null). Spawns near work, else home, else town hall. The entity normally exists at once; wait for `getEntity().isPresent()` to be safe. |
| `ICitizenData spawnCitizen(Map<Skill,Integer> skills, IBuilding work, IBuilding home, BlockPos spawnAt)` | Same with chosen skills (missing ones stay random) and spawn position. |
| `hire(ICitizenData, IBuilding)` / `hire(ICitizenData, IBuilding, JobEntry)` | Hire like the hire button (first job module, or the module of that job). |
| `setSkills(ICitizenData, Map<Skill,Integer>)` | Set exact skill levels. |
| `setCitizenNeeds(ICitizenData, double saturation, float health)` | Hunger and health (health needs the entity). |
| `setMoveIn(boolean)` | Town hall setting "move in": with it on, the colony tops up to 4 random citizens by itself (about one every 1500 ticks). Turn it off when the test spawns its own citizens. |
| `static String describe(ICitizenData)` | One line for failure messages: position, citizen AI state, job, worker AI state, status, navigating. |
| `IItemHandler inventory(IBuilding)` | The building's combined inventory (hut + racks), as MineColonies sees it. |
| `fillBuilding(IBuilding, ItemStack...)` / `fillWarehouse(ItemStack...)` | Put items into a building / the first warehouse. Throws if they don't fit. |
| `int countItems(IBuilding, Predicate<ItemStack>)` | Count items in a building's combined inventory. |
| `IServerWorkOrder addWorkOrder(WorkOrderType, IBuilding)` | BUILD/UPGRADE (next level), REPAIR, REMOVE, as the hut window buttons. Returns null if the colony refused (e.g. an upgrade to level N needs a builder's hut of level ≥ N). |
| `startResearch(Identifier id, int progress)` / `completeResearch(Identifier id)` | E.g. `Identifier.parse("minecolonies:civilian/agile")`. Complete applies the effects like instant research. Ids: file paths under `src/datagen/generated/minecolonies/data/minecolonies/researches/`. |
| `RaidSpawnResult startRaid(String culture, Integer amount, BlockPos spawnAt)` | Forced raid. Culture: `barbarian_raid`, `amazon_raid`, `egyptian_raid`, `norsemen_raid`, `pirate_raid`, `drowned_pirate_raid` or null (random). Other strings return `NO_SPAWN_POINT`. Ships are off. |
| `setTime(long dayTime)` / `setWeather(boolean rain, boolean thunder)` | 1000 morning, 6000 noon, 13000 night, 18000 midnight. Day cycle and weather cycle are off, so the value stays. |
| `setGameRule(GameRule<T>, T)` / `setConfig(ConfigValue<T>, T)` | Change for this test only; restored at the end. E.g. `setGameRule(GameRules.ADVANCE_TIME, true)`, `setGameRule(GameRules.RANDOM_TICK_SPEED, 100)`, `setConfig(MineColonies.getConfig().getServer().creativeResolve, true)`. |
| `fillFloor(BlockState top, BlockState below)` | Replace the arena floor (e.g. sand, or stone for a quarry). |
| `owner()`, `level()`, `helper()`, `abs(rel)`, `rel(abs)` | Fake owner (`OWNER_ID`, `OWNER_NAME`), level, helper, position conversion. |
| `TestFlow flow()` | See below. |

### `TestFlow` (`fixture.flow()`)

| Method | What it does |
|---|---|
| `run(String what, Runnable action)` | Runs once. Any exception (also a failed `helper.assertX`) fails the test with the step name. |
| `awaitUntil(String what, BooleanSupplier cond)` | Waits until `cond` is true; limited by `maxTicks`. |
| `awaitUntil(String what, BooleanSupplier cond, Supplier<String> found)` | Same; on timeout the message is "... waiting for: what. Found: found". |
| `awaitUntil(String what, int maxTicks, BooleanSupplier cond, Supplier<String> found)` | Same with its own tick limit for this step. |
| `check(String what, BooleanSupplier cond, Supplier<String> found)` | Checks once; fails with "Check failed: what. Found: ...". |
| `idle(int ticks)` | Let ticks pass (only when there is nothing observable to wait for). |
| `succeed()` | Last call: the test passes when all steps are done. |

- Steps that finish at once run in the same tick; a wait that isn't met is retried every tick. A failure stops the flow.
- A `helper.assertX` that throws inside an `awaitUntil` condition counts as "not yet"; any other exception fails the test.
- Every finished wait logs `Test <id>: '<what>' after N ticks` — use it to pick budgets (2-3x what you see).
- Don't mix with `helper.succeedWhen`/`helper.succeed()` in the same test, and call `ColonyFixture.create` only once per test.

## What the framework does for you

- Test server: mob spawning, patrols, wandering traders, vanilla raids, weather and day cycles, fire spread and random ticks are off;
  MineColonies test config (`minColonyDistance` 1, `initialCitizenAmount` 4, colony raids off, `forceLoadColony` on, `creativeResolve`
  off, `researchCreativeCompletion` off, `diseaseModifier` 100). See `E2EBootstrap`.
- At the end of each server tick it waits for MineColonies' pathfinding thread, so paths are ready in the next tick on any machine.
- Colony tests run one at a time; each deletes its colony (citizens, buildings, claims), removes left-over entities in and around the
  arena, and restores config and game rules when it ends, passed or failed. A passed test also has its entities removed by vanilla.

## Gotchas found while building the framework

- **A colony only runs while a player is near it.** The fake owner stands at the town hall (`createColony` puts him there) and counts
  as that player. If you move `owner()` out of the colony's claim, the colony goes inactive and citizens stop working.
- **Idle workers stay idle.** A builder without a work order, a farmer without a field, etc. never walks to his hut; the citizen AI
  stays `IDLE` (`AbstractEntityAIBasic.canGoIdle()`). Give workers something to do.
- **Night and rain stop work.** The fixture sets morning and clear weather; don't turn on the day cycle unless the test is about it.
- **Residence beds = residence level** (`LivingBuildingModule.getModuleMax()`): a level 1 residence houses one citizen.
  `spawnCitizen` throws "beds used 1/1" when it is full.
- **Blueprint names** come from the building's registry path plus level (`ModBuildings.BUILDER_ID` + `1` → `builder1`), searched
  anywhere in the pack. Not every hut has all 5 levels in every pack (e.g. tavern 1-3, gatehouse 1-3).
- **Footprints:** level 1 huts are up to about 15 blocks wide, the town hall sits at the center. Spots that worked:
  (12, 5, 12), (52, 5, 12), (12, 5, 52), (52, 5, 52). Keep at least about 20 blocks between hut positions, and check the paste didn't
  overlap if a test behaves oddly.
- **Upgrades need a builder of that level:** `addWorkOrder(UPGRADE, hut)` returns null unless a builder's hut of at least the target
  level exists.
- **Messages to the owner are dropped.** The fake player has no client, so you can't read chat messages (e.g. why a work order was
  refused). Check the colony state instead, or look at `latest.log`.
- **Tick counts vary between runs** because MineColonies' AI uses random numbers and the test area is at a random place each run (the
  walking test took 386 to 478 ticks). Budget generously; for chance-based outcomes use `maxAttempts`/`requiredSuccesses`.
- **Test areas are at random world coordinates** (millions of blocks out). Never hard-code absolute positions; use `abs(...)`.
- **Outside the arena** there is only air down to the flat world's ground far below (y = -61), and those chunks are not force-loaded;
  the barrier walls keep citizens in. The colony's claim (4 chunks around the town hall) is larger than the arena.
- Logs from MineColonies' colony deletion ("Kill Citizen", "Delete Building at") at the end of each colony test are normal.
- Some MineColonies values are read once into static fields when a class loads (e.g. `initialCitizenAmount` in
  `CitizenDiseaseHandler`); changing such a config with `setConfig` mid-run has no effect on them.

## Known framework limitations

- **No save and reload helper** (COL-06): there is no fixture call that writes and re-reads a colony, and the server can't be
  restarted inside a game test run. A test can serialize and re-read the colony with its own NBT methods; document what you did.
- **Other players:** only the fake owner exists. For permission tests create more fake players with
  `net.fabricmc.fabric.api.entity.FakePlayer.get(level, new GameProfile(uuid, name))` and add them to the colony's permissions; they
  are not in the server's player list, so code that looks players up there won't find them.
- **`diseaseModifier` can't be 0** (config minimum is 1); 100 makes random disease very rare but not impossible.
- **Visitor/quest timers, sounds and particles** are not turned off (no MineColonies setting for it).
- **No multi-server split:** one `runGameTest` runs every test in one server. Groups run side by side only as separate runs
  (e.g. one per worktree with `-Pe2eFilter`).
- **Real-time timers** in MineColonies (`RecentTargetCache`, colony offline catch-up) still use the clock; tests must not depend on them.
- `./gradlew build` runs all game tests, so one failing test anywhere fails the build.
