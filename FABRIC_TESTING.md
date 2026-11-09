# Fabric 26.3 port: manual test plan

Work through the tests in order: later tests depend on earlier ones. For each test, tick the box if the **Expected** result happens. If anything goes wrong, note the test number and send:

- what you did and what you saw (a screenshot helps for anything visual)
- the log lines around the error. The full log is in `run/logs/latest.log`, and crash reports are in `run/crash-reports/`.

Start the client with `./gradlew runClient --no-daemon`.

---

## A. Setup

**A1. New test world**
1. Singleplayer → Create New World. Game mode: **Creative**. Allow commands: **On**. World type: Superflat is fine.
2. Create the world.

- [ ] Expected: the world loads with no crash and no red errors in the console.

**A2. Creative tabs**
1. Open the creative inventory and look for the MineColonies tabs (huts, general items, decorations) and the Domum Ornamentum and Structurize tabs.

- [ ] Expected: the tabs exist and every item has a proper icon: no purple-and-black squares, no blank slots. Hover a few items: each has a readable name, not `item.minecolonies.xyz`.

**A3. Hut block rendering**
1. Place a few hut blocks by hand: Town Hall, Builder's Hut, Barracks, Blacksmith, Stone Smeltery and Combat Academy.

- [ ] Expected: every block has its 3D model with the correct textures. They will ask to be placed through the build tool; you can cancel that for now.

---

## B. Structurize (building tools)

**B1. Build tool preview**
1. Take the **Build Tool** from the Structurize tab and right-click the ground.
2. Choose **Minecolonies Original → Town Hall**. The search should finish and list buildings, and the progress block should animate.
3. Close the window.

- [ ] Expected: the window opens, the list fills in a few seconds, there's no missing-texture square, and the log has no `Unsupported key` errors.

**B2. Blueprint ghost**
1. Open the build tool again, pick the Town Hall, and choose to place it. A transparent preview of the building appears in the world.
2. Move it with the arrow buttons, rotate it, and mirror it.

- [ ] Expected: the semi-transparent building renders, follows your moves and rotations, and stays visible while you walk around it.

**B3. Scan tool**
1. Take the **Scan Tool**. Left-click one block and right-click another to select a box around something you built.
2. Use the tool's button to save the scan.

- [ ] Expected: the selection box renders and saving prints a success message. The file appears under `run/blueprints/`.

**B4. Shape tool**
1. Take the **Shape Tool**, open it, pick a sphere, and place it.

- [ ] Expected: the shape preview renders and placement puts blocks in the world.

---

## C. Founding a colony

**C1. Supply camp or ship**
1. Take a **Supply Camp** (or Supply Ship) item from the creative tab and right-click the ground (or water, for the ship).
2. Confirm the placement.

- [ ] Expected: the camp builds instantly, and its chest contains a Town Hall item and starter supplies.

**C2. Place the town hall**
1. Place the Town Hall with the build tool (the B2 flow), then confirm.
2. Accept the "create colony" prompt.

- [ ] Expected: a chat message confirms the colony was created, and within a few seconds about 4 citizens spawn near the town hall.

**C3. Citizens**
1. Look at the citizens.

- [ ] Expected: skins and models render, citizens walk around (pathfinding), and name tags appear above their heads.

**C4. Town hall GUI**
1. Right-click the town hall block.
2. Click through every tab: actions, information, permissions, citizens, settings, and so on.
3. Rename the colony.

- [ ] Expected: every page renders with text and icons (no missing textures), the buttons work, and the new name shows up in chat and in the GUI.

**C5. Citizen GUI**
1. Right-click a citizen.

- [ ] Expected: the citizen window opens with name, health, happiness and skills. Navigation between its pages works.

---

## D. Building and work

**D1. Builder's Hut**
1. Place a Builder's Hut with the build tool, next to the town hall.
2. Open it and hire one of the citizens as a builder.

- [ ] Expected: the hut is registered with the colony, and the hired citizen walks to it.

**D2. Build request**
1. Open the Builder's Hut GUI and request an upgrade of the hut itself to level 1.
2. Open the request list (the clipboard item or the builder's GUI) to see what materials are needed.
3. Put the requested materials into the Builder's Hut chest. You can take them from the creative inventory.

- [ ] Expected: the builder picks up the materials, walks to the site and places blocks. A progress percentage appears in the GUI and the building completes.

**D3. Requests and delivery**
1. Build a **Warehouse**, then a **Courier's Hut**, and hire a courier.
2. Put the materials for a second build into the warehouse.

- [ ] Expected: the courier carries the materials from the warehouse to the builder.

**D4. Farm field**
1. Build a **Farmer's Hut** and hire a farmer. Place a **Field** (scarecrow) and open its GUI.
2. Assign the field to the farmer and choose a seed.

- [ ] Expected: the field GUI shows the scarecrow icons for each direction. The farmer tills the soil and plants. MineColonies' farmland renders as tilled soil, not a missing model.

**D5. Lumberjack and miner** (optional)
1. Build a Lumberjack hut, hire a worker, and set the cutting area. Do the same for a Miner, with a mine shaft.

- [ ] Expected: the workers do their jobs without errors in the console.

---

## E. Decorations, crafting and integrations

**E1. Architect's Cutter (Domum Ornamentum)**
1. Craft or take an **Architect's Cutter** and open it.
2. Put two different blocks in the material slots and craft a timber frame or shingle.

- [ ] Expected: the resulting block shows the two chosen textures, both in the inventory and when placed.

**E2. Crafting worker**
1. Build a **Sawmill** or **Blacksmith**, hire a worker, and add a recipe in the hut GUI.

- [ ] Expected: the recipe can be added, the worker crafts it when requested, and the recipe list shows proper item icons.

**E3. JEI** (only if you install JEI for Fabric 26.3 into `run/mods`)

- [ ] Expected: MineColonies recipe categories appear in JEI with no errors.

---

## F. Combat and research

**F1. Guards**
1. Build a **Guard Tower** and hire a guard.

- [ ] Expected: the guard equips gear and patrols.

**F2. Raid**
1. Run `/minecolonies colony raid 1 now` (where 1 is your colony ID; `/minecolonies colony list` shows it).

- [ ] Expected: raiders spawn near the colony, guards fight them, and the raid ends without a crash.

**F3. Research**
1. Build a **University**, open it, and start a research.

- [ ] Expected: the research tree GUI renders and the research starts.

---

## G. Persistence and multiplayer

**G1. Save and reload**
1. Leave the world, then load it again.

- [ ] Expected: the colony, its citizens, the buildings and their levels, and open requests are all still there.

**G2. Dedicated server**
1. Run `./gradlew runServer --no-daemon` in one terminal (`run-server/eula.txt` is already set).
2. Run `./gradlew runClient --no-daemon` in a second terminal, then Multiplayer → Direct Connect → `localhost`.
3. Repeat C1, C2 and C4 on the server.

- [ ] Expected: you can connect and the colony works as in singleplayer. Opening GUIs syncs data with no "disconnected" errors.

**G3. Config**
1. Open `run/config/minecolonies-client.toml`, change a value, and save.

- [ ] Expected: the change takes effect, or is picked up after a restart, and the file isn't corrupted.
