[← Back to the test overview](../E2E-tests.md)

# How the end-to-end tests work

The tests check that colonies behave as intended without anyone playing through the mod. Each test builds the situation it needs
in a few ticks ("skipping ahead"), lets the colony run on its own, and then checks the result, including game state a player can't
see (colony data, citizen data, the request system, AI states, work orders).

## Where they run

- They are **server game tests** in the test mod in `src/gametest` (mod id `minecolonies-gametest`, not part of the release jar).
- `./gradlew runGameTest` starts a headless test server, runs every test and writes `build/gametest-report.xml`.
  `-Pe2eFilter=<prefix>` runs only some of them (see the [agent guide](AGENT-GUIDE.md)).
- `./gradlew build` does **not** run them. They are for whoever works on a behavior (run the tests about it while programming) and
  for a full run now and then. (Fabric Loom's `configureTests` makes `check` depend on `runGameTest`; `build.gradle` removes that
  dependency again. `check` keeps the isolation checks `checkJarIsolation` and `checkInjectedMembers`.)
- No real player and no game window are needed. Colonies are owned by a fixed fake player profile (a fixed UUID and name), so
  ownership and permissions work as they would for a real owner.
- The fake owner is a Fabric `FakePlayer` standing at the town hall. A MineColonies colony only runs (citizens work, requests are
  handled) while a player is near it; the fake owner counts as that player, so the colony is active for the whole test.
- How to write and run a test, the fixture API with signatures, and known limitations: [AGENT-GUIDE.md](AGENT-GUIDE.md).
- The same tests can be **watched in a game window** (`-Pe2eWatch`, see [Watch mode](#watch-mode)), and **UI tests** open MineColonies
  windows in a game window and take screenshots (see [UI tests](#ui-tests)).

| Command | What runs | Where | Report |
|---|---|---|---|
| `./gradlew runGameTest` | server game tests | headless test server, as fast as possible | `build/gametest-report.xml` |
| `./gradlew runGameTest -Pe2eWatch` | the same server game tests | integrated server of a game window, 20 ticks per second | `build/gametest-report.xml` |
| `./gradlew runClientGameTest` | UI tests ([ui.md](ui.md)) | game window | `build/e2e-ui-report.xml`, screenshots in `build/e2e-screenshots/` |

All three accept the filters described below; `./gradlew build` runs none of them.

## Test areas

- Colonies need far more room than the default 8×8×8 test structure. Colony tests use the **arena** `minecolonies-gametest:arena64`:
  an empty 64×32×64 structure (a few bytes of SNBT). `ColonyFixture.create` lays the floor in code (dirt, grass on top, 5 layers;
  huts stand at relative y = 5). Tests that need a forest, water or another surface build it on top of that floor in code
  (`fillFloor`, `helper.setBlock`) instead of shipping more structures.
- Like every game test, the arena is enclosed in barrier blocks (walls and floor; also a ceiling unless the test sets
  `skyAccess = true`), so citizens can't leave it.
- **Deep arena** for mines: mining levels are absolute heights (y 48, 16, -16, -100) and the arena ground is at about y 9, so a level 1
  mine digs to y -16, far below the barrier floor. `fixture.makeDeepGround()` replaces the barrier floor with stone and fills stone
  under the whole arena down to y -32 (removed when the test ends). Simpler than a taller structure: test positions are fixed at
  y 4 by the test server, and only mine tests pay for the extra blocks.
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

Besides the config, `ColonyFixture.create` sets the time to morning and clears the weather, and the test mod **shortens citizens'
leisure breaks to 3 seconds** (60 ticks; `E2EBootstrap` calls `ColonyFixture.shortenLeisureBreaks` at the end of every server tick):
each citizen takes a random 3-minute break now and then (`CitizenData.update`, about once per 144,000 ticks in a level 1 home), and a
full break hit one or two tests per full run at random (a courier, healer or student off work while the test waited). The leisure
behavior still runs: the citizen leaves work, idles and comes back after 60 ticks. The test mod remembers the tick each break started
and sets the remaining leisure time to 0 60 ticks later (the mod counts it down only every 60 ticks). CIT-15 checks a break;
`fixture.startLeisureBreak(citizen)` starts one.

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
  `maxAttempts`/`requiredSuccesses`). Re-runs run after all other tests; a small listener (`RerunTracking`) puts each re-run into
  the server's tracker in place of the failed attempt, without it the vanilla test server stopped before running them.

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

The world is a flat world, and only the test area is loaded. Only the colony being tested is loaded at a time. The headless test
server's Nether is the exception: it uses vanilla's real noise generator (`GameTestServerNetherMixin`), so PRT-01 can check worldgen
surface rules; it is only generated where a test loads chunks.

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

## Watch mode

`./gradlew runGameTest -Pe2eWatch` runs the server game tests in a real game window so you can watch citizens work. It combines with the
usual filter, e.g. `./gradlew runGameTest -Pe2eWatch -Pe2eFilter=colony_connections_robustness_tests`. Without `-Pe2eWatch` nothing
changes: `runGameTest` is headless as before.

- **How:** with `-Pe2eWatch`, `runGameTest` doesn't start the headless server; it runs Fabric's client game test task
  (`runClientGameTest`) in watch mode instead. The client test `client/WatchClientGameTest` creates a flat singleplayer world and starts
  the selected tests on its integrated server with `framework/IntegratedTestRun`, which does what the headless `GameTestServer` does:
  the same test instances from the registry, the same batching (the batch mixin still runs colony tests one at a time), the vanilla
  `GameTestRunner` and test ticker, retries (`maxAttempts`), and the same summary.
- **Same conditions as headless:** `E2EBootstrap` applies the test game rules and the MineColonies test config to the integrated server
  of a client test run (never to a normal `runClient` game), and `IntegratedServerMixin` waits for the pathfinding thread at the end of
  every integrated server tick, like `GameTestServerMixin` does headless. Differences: the test areas start at (64, 4, 64) instead of a
  random spot millions of blocks out (the client renders with jitter that far out), and a spectator player (`E2EViewer`) is in the
  world near the running test. Spectators are ignored by mobs and can't touch blocks.
- **Camera:** automatic; nobody steers it and there is no free camera. The player is a spectator that the client test
  (`client/WatchClientGameTest`) moves itself. Every 10 ticks the server works out the *focus* of the running test
  (`framework/WatchFocus`): in a colony test the town hall, every citizen, the huts they work or live in, and every other living entity
  in the area (animals, raiders, visitors); before the colony exists, and in a test without a colony, the whole test area (a small
  8×8×8 test gives a close-up). The camera looks down at 50° from the south-east (yaw 135) at the center of the focus box and stands
  just far enough back that all 8 corners of the box are in view (92% of the field of view). When a new test starts it jumps there; while the test runs it glides after the focus (6% of the way per
  tick), so walking citizens stay in view. Screenshots: `build/e2e-screenshots/watch-<test path>.png` 100 ticks after a test
  started, and `watch-<test path>-2.png` after 1200 ticks if it still runs (taken without the HUD; the log line with the focus
  box and camera position is next to it in `latest.log`). A chat line says watch mode is on; vanilla prints test
  results in chat.
- **Speed:** normal game speed, 20 ticks per second (Fabric's client test framework runs client and server in lockstep at the normal
  tick rate). `-Pe2eWatchSpeed=2` doubles it, `0.5` halves it (`/tick rate`). A colony test with a budget of 3000 ticks can take
  2.5 minutes to watch; filter to the tests you want to see.
- **End:** when all tests are done the log shows the same summary as headless ("All N required tests passed" or the failed tests),
  `build/gametest-report.xml` is written, the window closes, and the Gradle task fails if a required test failed.
- **Input:** keyboard and mouse can't change anything, not even the camera, and switching windows, minimizing or resizing doesn't pause
  or change the run (see [No human input](#no-human-input)); you can only watch. Closing the window aborts the run (the task fails).

## UI tests

Some behavior is only visible in menus. UI tests open MineColonies windows (BlockUI windows from `com.minecolonies.core.client.gui` and
Structurize's build tool) in a real game window, take screenshots and check what the windows show against the colony data on the server.
The tests are in [ui.md](ui.md).

- **Run:** `./gradlew runClientGameTest` (opens a window for about a minute). Only some: `-Pe2eUiFilter=UI-01,UI-03`. Not part of
  `./gradlew build` (neither are the server game tests); run it when you change UI code. Needs a desktop session (a real window and a GPU); it works on Windows.
- **How:** Fabric's client game test API (`fabric-client-gametest-api-v1`, enabled with `enableClientGameTests = true` in
  `build.gradle`, entrypoint `fabric-client-gametest` in the test mod's `fabric.mod.json`). The client test `client/UiClientGameTest`
  creates a flat world and starts the `manualOnly` server game test `ui/UiColonySetup` on the integrated server; it builds the colony with
  the normal `ColonyFixture`, hands positions and ids to the client through `ui/UiBridge`, and keeps the colony alive until the client
  is done. The client test then opens each window on the client thread, waits 10 ticks, takes a screenshot and reads the window's texts
  (`client/UiTestSupport`).
- **Screenshots:** `build/e2e-screenshots/<name>.png`, overwritten on each run (names in [ui.md](ui.md)). Look at them after a change:
  the checks catch wrong data and untranslated keys, not every layout problem.
- **Results:** a summary in the log (`build/run/clientGameTest/logs/latest.log`, also on the console), `build/e2e-ui-report.xml`, and
  the Gradle task fails if a required UI test failed. A UI test that fails because of a known MineColonies bug is marked not required
  in `UiClientGameTest` (with a `// BUG E2E-UI-n` comment) and listed in the summary without failing the run.

## No human input

A person at the computer must not be able to change a test by accident (for example by moving the player).

In every client test run (watch mode and UI tests):

| What the person does | What happens | Done by |
|---|---|---|
| Keyboard, mouse buttons, mouse movement, wheel, typing, IME text editing, dropping files on the window | Dropped where it enters the game: all input handlers of Minecraft's `SDLEventHandler` (every `handle…Event` except the key map change, which only relabels the key binds screen). | Fabric and `mixin/SDLEventHandlerMixin` |
| Gamepad, joystick, touch screen, pen | Nothing: Minecraft starts SDL with the video subsystem only, so there are no gamepad or joystick events, and touch and pen arrive only as the mouse events SDL makes from them (dropped above). | Minecraft |
| Clicking another window (focus loss) | The game stays "focused": no pause menu, the integrated server doesn't pause, even with "Pause on Lost Focus" on. | Fabric, `mixin/WindowMixin` (ignores focus events), `mixin/MinecraftMixin` (never pauses on focus loss) |
| Minimizing, restoring | Ignored: the game keeps ticking and rendering at full speed and doesn't pause. | Fabric and `mixin/WindowMixin` |
| Resizing, maximizing | The real window changes, the game doesn't: the framebuffer keeps its fixed test size (Fabric scales the picture into the window), so screenshots and rendering checks always have the same size. | Fabric |
| Changing the monitor resolution, fullscreen through the window system (macOS green button) | Ignored: would set the framebuffer to the real window size or change and save the fullscreen option. (F11 is a key and dropped anyway.) | `mixin/WindowMixin` |
| Moving the mouse over the window, clicking into it | The cursor is never captured: no relative mouse mode (hidden cursor locked in the window), no moving it to the window's center, so the person keeps using the mouse. The game's own cursor position never comes from the real mouse, not even when the GUI is resized (Minecraft reads the real cursor position then; it would hover buttons and show tooltips in screenshots). | Fabric and `mixin/InputConstantsMixin` (capture), `mixin/MouseHandlerMixin` (position) |
| Starting a run | The test window doesn't take the keyboard focus when it opens (SDL hints `SDL_WINDOW_ACTIVATE_WHEN_SHOWN`/`_RAISED` = 0), so typing elsewhere isn't interrupted. | `mixin/RenderSystemMixin` |
| Sound | None: the sound engine never opens the audio device (as on a computer without one), from the title screen to the end of the run. Fabric only mutes music. UI-08 checks it. | `mixin/SoundEngineMixin` |
| Exclusive fullscreen mouse confinement | Never: the window mouse grab is not applied. UI-08 checks it. | `mixin/WindowMixin` |
| Closing the window (close button, Alt+F4) | Allowed on purpose: it's the way to stop a run. The game shuts down normally (world saved, logs written) and the log says "The test window was closed: the client game test run is aborted without a test result (exit code 3)."; the game then exits with code 3, so the Gradle task fails (`finished with non-zero exit value 3`) instead of reporting an aborted run as passed. | `mixin/WindowMixin` (logs, marks), `mixin/MainMixin` (exit code) |

- The test mod repeats Fabric's blockers so the guarantee doesn't depend on Fabric; window size is left to Fabric alone (it keeps the
  real size to draw the fixed-size picture into the window). UI-08 checks that every blocker (ours and Fabric's) is applied and covers
  every input handler, and then tries it all for real: it pushes keyboard, mouse, text, file drop, focus, minimize, fullscreen and display
  events into the window's SDL event queue and resizes, maximizes and minimizes the real window ([ui.md, UI-08](ui.md#ui-08-human-input-is-ignored)).
- Watch mode was also tried from outside the game (Windows messages to the test window during a quarrier test: minimize, restore,
  resize, Escape/E/W and mouse clicks, then close): the game ticked on at 20 per second the whole time, no screen opened, and closing
  failed the task with exit value 3.
- Simulated input from tests (`context.getInput()`) goes straight to Minecraft's keyboard and mouse handlers and still works (UI-08
  checks that too).
- In watch mode the player is a spectator, so even without the input blocker it couldn't break or place blocks or hit entities. The
  camera is moved by the test only.
- These mixins are only active in client game test runs (`fabric.client.gametest` is set); a normal `runClient` game is not affected.
  MineColonies code is never changed.

## Status legend (used in the overview)

| Status | Meaning |
|---|---|
| ✅ Implemented | The test exists and passes. |
| 🚧 In progress | Being written. |
| 📝 Planned | Designed in these docs, not written yet. |
