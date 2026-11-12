[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# User interface

MineColonies windows opened in a real game window on a prepared colony. Each test takes screenshots and checks what the window shows
against the colony's data on the server (names, levels, counts), and that no text is an untranslated key
(`com.minecolonies.…`, `structurize.…`).

- **Run:** `./gradlew runClientGameTest` (only some: `-Pe2eUiFilter=UI-01,UI-03`). Opens a game window; real keyboard and mouse input
  is ignored the whole time. Not part of `./gradlew build`.
- **Screenshots:** `build/e2e-screenshots/<name>.png` (names below). Report: `build/e2e-ui-report.xml`. Log:
  `build/run/clientGameTest/logs/latest.log` (the run folder is cleared at the start of each run; screenshots are not in it).
- **Code:** `client/UiClientGameTest` (the tests, client side), `ui/UiColonySetup` (the colony, server side), `client/UiTestSupport`
  (open a window, read its texts, screenshot).

All UI tests share one colony, prepared once by the server game test `minecolonies-gametest:ui_colony_setup_colony` on the integrated
server (the same `ColonyFixture` as the server tests). It is `manualOnly`, so the headless run never picks it:

- Arena at (64, 4, 64), town hall at the center, builder's hut level 1 at (12, 5, 12), warehouse level 1 at (52, 5, 12), university
  level 1 at (12, 5, 52), post box at (44, 5, 44) (relative positions).
- Citizen 1: builder in the builder's hut, skills 3, 7, 11, … (all different, in `Skill` order). Citizen 2: skills 10, spawned without a job (the colony hires it as researcher in the university by itself).
- Research `minecolonies:civilian/stamina` started (progress 0).
- "Move in" off. The window's player (`E2EViewer`, creative mode) is added as an officer and stands 14 blocks north of the town hall.
  The colony owner is still the fake player `E2EOwner`.

The setup test waits until the client test is done, then passes and deletes the colony as usual. Each UI test runs on its own: a failed
test is recorded and the next one still runs.

---

## UI-01 Town hall window

**Tests:** the town hall's main page opens, and its statistics page shows the colony's real citizen count.

**Setup**
- The shared UI colony (2 citizens).

**What happens**
1. Opens `WindowMainPage` for the town hall view, takes a screenshot.
2. Opens `WindowStatsPage` for the town hall view, takes a screenshot.

**Passes when**
- The main page shows the colony name or the title with level 1, and no untranslated keys.
- The statistics page's label `totalCitizens` reads `<n>/<max>` where n is the server's citizen count (2).
- The statistics page shows no untranslated keys.

**Screenshots:** `ui01-town-hall-main.png`, `ui01-town-hall-stats.png`.

**Time budget:** about 2 seconds (10 ticks per window).

---

## UI-02 Builder's hut window

**Tests:** a worker hut window shows the right building and its worker.

**Setup**
- The shared UI colony: builder's hut level 1 with the builder hired.

**What happens**
1. Opens `WindowHutBuilderModule` for the builder's hut view (without the first-time hut guide), takes a screenshot.

**Passes when**
- The title (`name`) is the translated building name followed by the server's building level (1).
- The worker list shows the builder's name as stored on the server.
- No untranslated keys.

**Screenshots:** `ui02-builder-hut.png`.

**Time budget:** about 1 second.

---

## UI-03 Citizen window

**Tests:** the citizen window shows the citizen's real name, skills and job.

**Setup**
- The shared UI colony: the builder with skills 3, 7, 11, … (all different).

**What happens**
1. Opens `MainWindowCitizen` for the builder's citizen view, takes a screenshot.
2. Opens `JobWindowCitizen` for the same citizen, takes a screenshot.

**Passes when**
- The `name` label equals the citizen's name on the server.
- For every skill, the label with the skill's name (`athletics`, `dexterity`, …) shows the server's skill level.
- The job page names the citizen's job (translated job key from the server).
- No untranslated keys on either page.

**Screenshots:** `ui03-citizen-main.png`, `ui03-citizen-job.png`.

**Time budget:** about 2 seconds.

---

## UI-04 Build tool window

**Tests:** the build tool window opens on the selected structure pack and lists its categories.

**Setup**
- The shared UI colony. The client's selected structure pack (default).

**What happens**
1. Opens Structurize's `WindowExtendedBuildTool` at a spot 20 blocks north of the town hall, waits 40 ticks for the categories to
   load, takes a screenshot.

**Passes when**
- A structure pack is selected on the client and its name is shown.
- The window shows at least 5 texts (pack, buttons, categories).
- No untranslated keys.

**Screenshots:** `ui04-build-tool.png`.

**Time budget:** about 3 seconds.

---

## UI-05 Research tree

**Tests:** the university window and the research tree of a branch show the branch's research.

**Setup**
- The shared UI colony: university level 1, research `civilian/stamina` started.

**What happens**
1. Opens `UniversityModuleWindow` (the list of research branches), takes a screenshot.
2. Opens `WindowResearchTree` for the branch `minecolonies:civilian`, takes a screenshot.

**Passes when**
- Every first-tier research of the branch (from the server's global research tree, names translated) appears in the tree.
- No untranslated keys in either window.

**Screenshots:** `ui05-university.png`, `ui05-research-tree.png`.

**Time budget:** about 2 seconds.

---

## UI-06 Post box request window

**Tests:** the post box window (requesting items from the colony) lists requestable items.

**Setup**
- The shared UI colony: a post box placed like a player places it.

**What happens**
1. Opens `WindowPostBoxMain` for the post box view, waits 20 ticks, takes a screenshot.

**Passes when**
- The item list `resources` has at least one row.
- No untranslated keys.

**Screenshots:** `ui06-postbox.png`.

**Time budget:** about 2 seconds.

---

## UI-07 Warehouse window

**Tests:** the warehouse window opens with the right title.

**Setup**
- The shared UI colony: warehouse level 1.

**What happens**
1. Opens the warehouse building view's own window (`IBuildingView.getWindow()`), takes a screenshot.

**Passes when**
- The title is the translated building name followed by the server's building level (1).
- No untranslated keys.

**Screenshots:** `ui07-warehouse.png`.

**Time budget:** about 1 second.

---

## UI-08 Human input is ignored

**Tests:** a person at the keyboard or mouse can't affect a test window.

**Setup**
- None (runs before the world is created).

**What happens**
1. Looks at the mixins applied to Minecraft's `SDLEventHandler` (where real keyboard, mouse and text events from the window enter the
   game).

**Passes when**
- The test mod's `SDLEventHandlerMixin` and Fabric's client game test input blocker are both applied. Both drop every real input event
  in client game test runs; simulated input from tests (`context.getInput()`) bypasses them.

**Screenshots:** none.

**Time budget:** instant.
