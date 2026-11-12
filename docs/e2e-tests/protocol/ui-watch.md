[← Back to the UI tests](../ui.md) · [How the tests work](../framework.md)

# Protocol: UI tests and watch mode

Run on Windows 11 (NVIDIA GPU, real window 854×480), Minecraft 26.3, Fabric API 0.165.2+26.3, branch `e2e-tests` + this work.

- UI tests: `./gradlew --no-daemon runClientGameTest` → 8 of 8 passed, about 1 min 15 s in total (world creation and Gradle included;
  the 7 window tests themselves take about 10 s).
- Watch mode: `./gradlew --no-daemon runGameTest -Pe2eWatch "-Pe2eFilter=minecolonies-gametest:*_co*"` → 3 of 3 passed (FAB-04,
  COL-01, the framework's walking test), 23 s of test time at 20 ticks per second; `build/gametest-report.xml` written, camera moved to
  each test (`build/e2e-screenshots/watch-colony_connections_robustness_tests_worker_walks_to_hut.png`).
- Headless: `./gradlew --no-daemon build` unchanged (the UI setup test is `manualOnly`, so it doesn't run headless).

| ID | Status | Notes |
|---|---|---|
| UI-01 | ✅ | Main page shows "E2E colony …", "Town Hall 1"; statistics "Number of Citizens: 2/2", "Builder: 1/1", "Researcher: 1/1". |
| UI-02 | ✅ | "Builder's Hut 1", "Builder: <name>" in the worker list. |
| UI-03 | ✅ | Name and all 11 skill levels (3, 7, … 43) match the server; job page "Job: Builder" with the builder's skills. |
| UI-04 | ✅ | Pack "Minecolonies Original", 9 category icon buttons. First version of the check counted texts; categories are icons without text, the check now counts the buttons of the `categories` view. |
| UI-05 | ✅ | University "1/1 Research in Progress", branches Civilian/Combat/Technology; civilian tree shows Keen (tier 1), Diligent, Haste, Rails (tier 2, locked). |
| UI-06 | ✅ | Post box lists items (Ice, Bow, Mud, Egg, …) with Request buttons. |
| UI-07 | ✅ | "Warehouse 1". |
| UI-08 | ✅ | Both the test mod's and Fabric's `SDLEventHandlerMixin` are applied. |

## Bugs found

None. All windows opened without errors, showed the server's data, and had no untranslated keys. Screenshots checked by eye:

| Screenshot | Looks right? |
|---|---|
| `ui01-town-hall-main.png`, `ui01-town-hall-stats.png` | Yes. Textures, ribbons and buttons complete. |
| `ui02-builder-hut.png`, `ui07-warehouse.png` | Yes. The warehouse main page has only "Build Options" and the inventory button; that is its layout (`WindowHutMinPlaceholder`). |
| `ui03-citizen-main.png`, `ui03-citizen-job.png` | Yes. Hearts, food and happiness rows, skill icons. |
| `ui04-build-tool.png` | Yes. Category icons, confirm/cancel, settings gear. |
| `ui05-university.png`, `ui05-research-tree.png` | Yes. At 854×480 the research tree shows only the first two tiers (the window is dragged/zoomed by the player). |
| `ui06-postbox.png` | Yes. Search field and quantity fields are black boxes (BlockUI text field style, also upstream). |

Observation, not a bug: the second citizen is spawned without a job and the colony hires it as researcher in the university by itself
(auto-hiring), so the statistics page shows "Unemployed: 0".

## Framework issues

- **Lang file missing in fresh worktrees:** `src/main/resources/assets/minecolonies/lang/en_us.json` is git-ignored (downloaded
  translations). Without it the client crashes at start (`NullPointerException` in `LanguageHandler$LanguageCache.load`,
  `LanguageHandler.java:74`, from `MineColonies.<init>`). Copy it from the main checkout; it must not be committed.
- **Loom clears `build/run/clientGameTest`** before `runClientGameTest` *and* before `runGameTest` (with `eula = true` and client tests
  enabled, `configureLaunch` depends on `acceptGameTestEula`, which depends on `deleteGameTestRunDir`). Don't run both in the same
  worktree at once. Screenshots are kept outside it (`build/e2e-screenshots`).
- **Watch mode positions:** test areas start at (64, 4, 64) instead of a random spot up to 15 million blocks out (the client renders
  with jitter that far out). Tests must not depend on their position anyway.
- **Watch mode has a real player** (spectator `E2EViewer`) near the running test, the headless run doesn't. Spectators are ignored by
  mobs and can't touch the world; a test that counts players would see one more.
- **No free camera:** all real input is dropped, including mouse look in watch mode. The camera is moved by the test.
- **Short tests flash by:** the camera checks every 2 ticks and jumps to each new test, but a test that ends within a few ticks
  (FAB-04, COL-01) is gone before you see much, and gets no `watch-*.png` (taken 40 ticks after the camera arrives). Its result still
  appears in chat and in the summary.
- Client game tests need a desktop session with a GPU; they can't run on a headless CI agent without a virtual display.
