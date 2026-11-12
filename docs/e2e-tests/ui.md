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

---

## UI-09 Brewing teaching computes the potion

**Tests:** the alchemist's brewing teaching window (`WindowBrewingstandCrafting`) can work out the brewed potion on the client, so a
brewing recipe can be taught (bug E2E-CRF01-2).

**Setup**
- The test world (no colony needed).

**What happens**
1. On the client thread, calls `WindowBrewingstandCrafting.getBrewingOutput` (what the window's Done button uses) with a water bottle
   and nether wart, then with a water bottle and dirt. The lookup uses the brewing recipes synchronized to the client.

**Passes when**
- Water bottle + nether wart gives an awkward potion; water bottle + dirt gives nothing.

**Not covered:** opening the window from an alchemist hut and pressing Done (the server side of teaching, `AddRemoveRecipeMessage`, is
covered by `crf01_alchemist`).

**Screenshots:** none.

**Time budget:** instant.

---

## UI-10 Spear item renders in 3D

**Tests:** the spear item is drawn with its 3D model (special item model renderer `SpearItemTileEntityRenderer`) in hand, on armor
stands and in item frames, and with its flat sprite in the GUI, like the NeoForge version.

**Setup**
- The test world. A small scene 20 blocks west of the UI colony's arena (in the air, y 19-25): a stone floor, a concrete wall, an item
  frame with a spear on the wall, an armor stand holding a spear. The player holds a spear and stands at (26.5, 20, 24.5) looking at the
  wall. Runs after the other UI tests (it moves the player).

**What happens**
1. Resolves the spear's item model on the client for third person, first person, item frame and GUI and measures its bounding box.
2. Takes a screenshot in first person, then one in third person (front view), then empties the player's hand.

**Passes when**
- In hand (first and third person) and in the item frame the model's longest side is over 1.3 blocks (the 3D spear is about two blocks
  long; the flat sprite and the missing model are one block); in the GUI it is the flat one-block sprite.

**Look at the screenshots:** the spear in the hand, held by the armor stand and in the item frame is the 3D model; the hotbar shows the
flat sprite. Like in 1.21.1 the item frame transform (`models/item/spear_in_hand.json`, `fixed`) pushes most of the spear into the wall,
only the tip shows below the frame. Like in 1.21.1 the 3D spear is the trident mesh with the spear texture (the spear's own mesh
`SpearModel#createLayer` was never registered upstream).

**Screenshots:** `ui10-spear-first-person.png`, `ui10-spear-third-person.png`.

**Time budget:** about 4 seconds.

---

## UI-11 Child citizen proportions

**Tests:** child citizens are drawn with child proportions (1.21.1 `HumanoidModel#young`: head scaled by 0.75, body, arms and legs by
0.5), adults at full size.

**Setup**
- The shared UI colony. Spawns a male child and a male adult citizen 10 blocks north of the town hall (4 blocks in front of the player),
  frozen (`NoAI`), facing the player, each holding an iron sword. Both are removed again at the end. Runs after UI-07 (the citizen count
  check of UI-01 must not see them).

**What happens**
1. Waits until the client has both citizen entities and their data, takes a screenshot.
2. On the client, creates each citizen's render state with its renderer, poses the citizen model with it, and poses a citizen armor model
   (`CitizenArmorModel`) with it, like rendering does.

**Passes when**
- Child: head scale 0.75, body, arm and leg scale 0.5 in the citizen model, head 0.75 and body 0.5 in the armor model.
- Adult: all scales 1.

**Look at the screenshot:** the child is about half as tall as the adult with a large head; its sword is small and in its hand.

**Screenshots:** `ui11-child-citizen.png`.

**Time budget:** about 3 seconds.

---

## UI-12 JEI draws blocks in 3D

**Tests:** `compatibility.jei.RenderHelper#renderBlock`, which MineColonies' JEI recipe categories use for the composting barrel and the
crafters' intermediate blocks (e.g. furnace), draws the exact block state as a turned 3D block model (through the picture-in-picture
renderer `GuiBlockRenderer`), like 1.21.1, instead of the block's item icon. JEI itself is not in the test runtime; `RenderHelper` doesn't
need it.

**Setup**
- The test world (no colony needed).

**What happens**
1. Opens a test screen (`client/BlockRenderTestScreen`) that calls `RenderHelper.renderBlock` with the arguments of the composting
   category (barrel at 0 %, 60 % and done) and the generic crafter category (furnace, grown wheat crop), each with its item icon below.
2. Takes a screenshot and reads it back.

**Passes when**
- Every block covers its anchor point in the screenshot.
- The empty and the finished barrel differ at the top (each block is drawn with its own state; one shared picture would make them equal).

**Look at the screenshot:** turned 3D blocks, the barrels with different fill levels and fill gauges, a grown wheat crop (its item icon
below is wheat seeds).

**Screenshots:** `ui12-jei-blocks.png`.

**Time budget:** about 1 second.

---

## UI-13 JEI crafter recipes from synced recipes

**Tests:** on a dedicated server, the vanilla recipes that MineColonies' JEI crafter categories show (crafting, smelting, brewing,
architect's cutter) come from the recipes the server synchronizes to the client (Fabric `RecipeSynchronization`); the client builds a
recipe manager from them (`RecipeAnalyzer#createRecipeManager`). In singleplayer JEI uses the integrated server's recipe manager.

**Setup**
- The test world (no colony needed).

**What happens**
1. On the server, builds the crafter recipe map from the server's recipe manager (`RecipeAnalyzer#buildVanillaRecipesMap`, the
   singleplayer path).
2. On the client, builds the same map from the client's synchronized recipes, exactly like `JEIPlugin` does without an integrated server.

**Passes when**
- The server has small crafting, large crafting and smelting recipes, and both maps have the same number of recipes for every crafting
  type (so every serializer the crafters need is synchronized).

**Not covered:** JEI itself (not in the test runtime) and a real dedicated server (the integrated server synchronizes the same way).

**Screenshots:** none.

**Time budget:** instant.
