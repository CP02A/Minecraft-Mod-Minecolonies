[← Back to the test overview](../E2E-tests.md)

# How the end-to-end tests work

The tests check that colonies behave as intended without anyone playing through the mod. Each test builds the situation it needs
in a few ticks ("skipping ahead"), lets the colony run on its own, and then checks the result, including game state a player can't
see (colony data, citizen data, the request system, AI states, work orders).

## Where they run

- They are **server game tests** in the test mod in `src/gametest` (mod id `minecolonies-gametest`, not part of the release jar).
- `./gradlew runGameTest` starts a headless test server, runs every test and writes `build/gametest-report.xml`. `./gradlew build`
  runs them too. `-Pe2eFilter=<prefix>` runs only some of them (see the [agent guide](AGENT-GUIDE.md)).
- No real player and no game window are needed. Colonies are owned by a fixed fake player profile (a fixed UUID and name), so
  ownership and permissions work as they would for a real owner.
- The fake owner is a Fabric `FakePlayer` standing at the town hall. A MineColonies colony only runs (citizens work, requests are
  handled) while a player is near it; the fake owner counts as that player, so the colony is active for the whole test.
- How to write and run a test, the fixture API with signatures, and known limitations: [AGENT-GUIDE.md](AGENT-GUIDE.md).

## Test areas

- Colonies need far more room than the default 8×8×8 test structure. Colony tests use the **arena** `minecolonies-gametest:arena64`:
  an empty 64×32×64 structure (a few bytes of SNBT). `ColonyFixture.create` lays the floor in code (dirt, grass on top, 5 layers;
  huts stand at relative y = 5). Tests that need a forest, water or another surface build it on top of that floor in code
  (`fillFloor`, `helper.setBlock`) instead of shipping more structures.
- Like every game test, the arena is enclosed in barrier blocks (walls and floor; also a ceiling unless the test sets
  `skyAccess = true`), so citizens can't leave it.
- A colony claims chunks around its town hall. To keep test colonies from overlapping, colony tests use their **own test
  environment** (`minecolonies-gametest:colony`); a small mixin on the vanilla batch factory splits that environment into batches of one
  test, so colony tests run **one at a time**, after the parallel tests. Every colony test deletes its colony when it ends (passed or
  failed).
- Tests that only touch one block or one entity (like the existing transfer bridge tests) keep using the small default structure and
  run in parallel.

## Test configuration

The test server starts with a test config so results don't depend on chance or on a player's settings:

| Setting | Test value | Why |
|---|---|---|
| `minColonyDistance` | 1 | Colonies of different tests may be close together. |
| `initialCitizenAmount` | fixed (4) | Predictable starting population. |
| `enableColonyRaids` | off | No random raids. Raid tests start raids themselves. |
| `forceLoadColony` | on | Colony chunks stay loaded while the test runs. |
| `creativeResolve` | off | Requests must be fulfilled the real way. A few tests turn it on to skip logistics they don't test. |
| `researchCreativeCompletion` | off | Research must really be done, except where a test only needs its effect. |
| `diseaseModifier` | 100 | Almost no random diseases. The config's minimum is 1, so 0 is impossible; higher means rarer, and 100 (the maximum) means about one disease per 1,000,000 checks. Disease tests infect a citizen on purpose. |
| `minDistanceFromWorldSpawn` | 0 | Test areas are near the world spawn. |

The values are set in memory when the test server has started (`E2EBootstrap`), before the first test. A test can change a value for
its own run with `fixture.setConfig(...)`; it is restored when the test ends.

## Skipping ahead: the fixture API

Tests don't wait for the colony to build itself up. A small helper layer in the test mod (`ColonyFixture`, package
`com.minecolonies.gametest.framework`) creates the situation directly. Exact signatures are in the [agent guide](AGENT-GUIDE.md).

| Helper | What it does |
|---|---|
| `createColony()` | Pastes a level 1 town hall at the arena center with the build tool's creative placement, then runs the server side of the "create colony" message, so the colony is owned by the fake player. |
| `placeBuilding(type, level, pos)` | Pastes the hut's blueprint instantly through Structurize and registers the building at that level, as if a builder had built it. |
| `spawnCitizen(skills, work, home)` | Creates a citizen with set skill levels, hires them into a building and moves them into a home (a residence bed). |
| `fillBuilding(building, items)` / `fillWarehouse(items)` | Puts items straight into a hut's racks or the warehouse. |
| `setCitizenNeeds(citizen, saturation, health, …)` | Sets hunger, health and other needs. |
| `setTime(dayTime)` / `setWeather(rain, thunder)` | Jumps to the needed time of day or weather. |
| `addWorkOrder(type, building)` | Creates a build, upgrade, repair or removal order. |
| `startResearch(id, progress)` / `completeResearch(id)` | Starts research at a given progress, or finishes it. |
| `startRaid(culture)` | Starts a raid of a given culture through the raid manager. |
| `flow()` | Steps that run one after another: actions, waits for an outcome (with a description of what was found on timeout), checks. |

## What a test checks

Every test has clear pass criteria, checked in code. Typical checks look straight into internal state:

- **Colony**: `IColony` (buildings, claimed chunks, permissions, happiness, raid manager, research tree, quest manager).
- **Citizens**: `ICitizenData` (job, home, skills and XP, saturation, health, happiness modifiers, disease, current AI state).
- **Request system**: open, assigned, fulfilled and cancelled requests, and who resolved them.
- **Work orders**: claimed by which builder, progress, removed when done.
- **World**: blocks placed or removed, items in racks and furnaces, entities spawned or killed.

Each test fails with a message that says what it expected and what it found, and where possible which citizen or building was
involved.

## Time budgets and randomness

- Each test has a **time budget** in game ticks (20 ticks = 1 second). The test fails if the pass criteria aren't met in time.
  Budgets are in game ticks, not seconds: the test server runs ticks as fast as it can (see [Speed](#speed)), so a long budget
  costs little real time.
- Where the game rolls dice (crop growth, compost chances, loot), tests either set the outcome up front (fully grown crops, chance
  tiers with a guaranteed result) or run the behavior several times and pass when enough runs succeed (`@GameTest`
  `maxAttempts`/`requiredSuccesses`).

## Speed

The tests should run as fast as possible without changing how MineColonies behaves. Three rules:

1. **The mod's code is never changed for tests.** Tests run the same MineColonies code as a real server. Everything below changes
   the test server, the world or the config, never the mod's logic. If the mod's code changes, the tests automatically test the
   new code.
2. **Ticks run as fast as the CPU allows.** The vanilla game test server already does this: `GameTestServer.waitUntilNextTick()`
   doesn't wait the usual 50 ms between ticks. So there is no tick rate to raise, and speed depends only on how much work each tick
   does and how many ticks a test needs.
3. **Results must not depend on real time**, only on game ticks. Otherwise a test could pass on a slow machine and fail on a fast
   one.

### Less work per tick

Everything a test isn't about is turned off, on top of the [test configuration](#test-configuration):

| Setting | Test value | Why |
|---|---|---|
| Mob spawning, patrols, wandering traders | off | Tests spawn the mobs they need. |
| Weather cycle | off | Tests set the weather themselves. |
| Day/night cycle | off | Tests set the time themselves. Tests about night or day turn the cycle on for their run. |
| Fire spread | off | Prevents side effects between tests. |
| Random tick speed | 0 | Turned up (for example 100) only for crop, tree and plant tests, so plants grow in seconds. |
| Visitor and quest timers, citizen sounds and particles | not changed | MineColonies has no setting to turn them off without changing its code. |

Also off: vanilla raids, death messages, command feedback. They are set in `E2EBootstrap` when the test server has started; a test
changes a rule for its own run with `fixture.setGameRule(...)` (restored at the end). The fixture starts every colony test at time 1000
(morning) with clear weather.

The world is a flat world, and only the test area is loaded. Only the colony being tested is loaded at a time.

### Fewer ticks per test

- Tests create the finished starting situation directly with the fixture API instead of waiting for it.
- Where the test isn't about it, the setup uses what the mod already offers to shorten work: citizens with high skills (shorter
  pauses between actions), research finished at once, `creativeResolve` for requests the test doesn't cover.
- Tests wait for an **outcome** ("the hut is level 2"), never for a fixed number of ticks. The time budget is only an upper limit.
- The mod's built-in pauses between actions are **not** shortened. That would change the behavior being tested and could hide bugs
  such as a worker getting stuck.

### Pathfinding stays deterministic

MineColonies calculates paths on its own background thread: every path job goes through `Pathfinding.getExecutor()`, a single
"Minecolonies Pathfinding Worker" thread. In normal play a path is usually ready a tick or two after it was requested. A test server
running flat out could tick many times before the thread is done, so citizens would wait a different number of ticks for paths
depending on the machine.

The fix only changes the **test server**, not the pathfinding:

- At the end of every tick, the test server waits until the pathfinding thread has finished every job it was given. It submits an
  empty marker job to the public `Pathfinding.getExecutor()` and waits for it: the executor has one thread and a first-in-first-out
  queue, so the marker finishes only after every job submitted before it. (The design compared `getTaskCount()` with
  `getCompletedTaskCount()`, but `ThreadPoolExecutor` briefly counts neither while its worker has taken a job from the queue and not
  started it yet, so the counts can be equal while a job is still pending. The marker has no such gap.) A mixin on
  `GameTestServer.tickServer` in the test mod does this; it touches only the test server. At the end of a run the log has a line
  `pathfinding wait: N ticks, M ended with path jobs pending`.
- So a path requested in one tick is always ready by the next tick, on every machine. The path is still calculated by the real code
  on the real thread, and it is the same path as in normal play (a path job stops after a set number of nodes, not after a set time).
- If MineColonies' pathfinding changes, the tests use the new code without any change.
- If `Pathfinding.getExecutor()` is renamed or removed, the test mod stops compiling, so the problem shows up at once.
- A guard test checks that every running thread named `Minecolonies Pathfinding*` belongs to the executor the test server waits for.
  If MineColonies ever adds a second pathfinding thread, this test fails instead of the other tests quietly becoming timing-dependent.

### Real-time timers that remain

Two places in MineColonies measure real time instead of game ticks. They stay as they are:

- `RecentTargetCache` (pathfinding) forgets recently used targets after a set number of real milliseconds. At full speed an entry
  lives for many more ticks than in normal play. Tests don't depend on when an entry expires.
- `Colony` catches up on time spent offline only after more than one real hour has passed. That never happens during a test run.

### Running tests side by side

Colony tests run one at a time inside one server, because their claims would overlap. To use all CPU cores, the colony suites can be
split into groups, each running in its own test server at the same time (`./gradlew runGameTest -Pe2eFilter=<group>` in separate
checkouts). There is no single command that starts several servers yet: one `./gradlew runGameTest` runs everything in one server.

### Results still vary a little between runs

The pathfinding wait removes the machine's speed from the results, but MineColonies' AI also uses random numbers (random pauses,
`Math.random`, citizen random sources), and each run places the test area at a random spot in the world. So the same test can take a
different number of ticks in different runs (the framework's walking test took between 386 and 478 ticks). Tests therefore wait for
outcomes with a generous budget, never for an exact tick.

## Status legend (used in the overview)

| Status | Meaning |
|---|---|
| ✅ Implemented | The test exists and passes. |
| 🚧 In progress | Being written. |
| 📝 Planned | Designed in these docs, not written yet. |
