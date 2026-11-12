# End-to-end tests

Automated tests that check MineColonies behaves as intended, without anyone playing through the mod. Each test sets up the situation
it needs (a colony, huts, citizens, items, time of day), lets the colony run on its own, then checks the result, including internal
state a player can't see.

- **Progress:** 13 of 109 tests implemented (the Fabric port tests, COL-01 and the UI tests). The rest are designed and planned.
- **How the tests work** (setup helpers, test areas, config, time budgets): [e2e-tests/framework.md](e2e-tests/framework.md)
- **Run them:** `./gradlew runGameTest` (report in `build/gametest-report.xml`); only some: `./gradlew runGameTest -Pe2eFilter=<prefix>`
- **Watch them in a game window:** `./gradlew runGameTest -Pe2eWatch` (combine with `-Pe2eFilter`); **UI tests** with screenshots: `./gradlew runClientGameTest`
- **Writing tests:** [e2e-tests/AGENT-GUIDE.md](e2e-tests/AGENT-GUIDE.md) (fixture API, skeleton, gotchas)
- Click a test ID for its setup, what happens and when it passes.

| Status | Meaning |
|---|---|
| ✅ | Implemented and passing |
| 🚧 | In progress |
| 📝 | Planned (designed, not written yet) |

## Colony lifecycle

Details: [e2e-tests/colony.md](e2e-tests/colony.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [COL-01](e2e-tests/colony.md#col-01-found-a-colony) | Found a colony | Placing a town hall founds a colony owned by the right player, with claimed land. | ✅ Implemented |
| [COL-02](e2e-tests/colony.md#col-02-first-citizens-arrive) | First citizens arrive | A new colony gets its starting citizens as living entities. | 📝 Planned |
| [COL-03](e2e-tests/colony.md#col-03-housing-limits-the-population) | Housing limits the population | Citizens only move in while there are free beds. | 📝 Planned |
| [COL-04](e2e-tests/colony.md#col-04-territory-grows-with-buildings) | Territory grows with buildings | Buildings extend the colony's claim, removing them shrinks it. | 📝 Planned |
| [COL-05](e2e-tests/colony.md#col-05-colony-protection-and-permissions) | Colony protection and permissions | Outsiders can't grief the colony, members can do what their rank allows. | 📝 Planned |
| [COL-06](e2e-tests/colony.md#col-06-save-and-reload-a-colony) | Save and reload a colony | A busy colony comes back unchanged after saving and loading. | 📝 Planned |
| [COL-07](e2e-tests/colony.md#col-07-delete-a-colony) | Delete a colony | Deleting a colony removes its citizens and claims cleanly. | 📝 Planned |

## Construction

Details: [e2e-tests/construction.md](e2e-tests/construction.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [BLD-01](e2e-tests/construction.md#bld-01-builder-builds-a-new-hut) | Builder builds a new hut | The builder builds a hut that matches its blueprint. | 📝 Planned |
| [BLD-02](e2e-tests/construction.md#bld-02-builder-asks-for-missing-materials) | Builder asks for missing materials | The builder requests exactly the missing materials and continues when they arrive. | 📝 Planned |
| [BLD-03](e2e-tests/construction.md#bld-03-upgrade-a-hut) | Upgrade a hut | Upgrading replaces the structure and raises the building level. | 📝 Planned |
| [BLD-04](e2e-tests/construction.md#bld-04-repair-a-damaged-hut) | Repair a damaged hut | A repair restores only the missing or wrong blocks. | 📝 Planned |
| [BLD-05](e2e-tests/construction.md#bld-05-remove-a-hut) | Remove a hut | Removing a hut takes it down and returns the materials. | 📝 Planned |
| [BLD-06](e2e-tests/construction.md#bld-06-builder-level-limits) | Builder level limits | A builder doesn't take orders above its hut level. | 📝 Planned |
| [BLD-07](e2e-tests/construction.md#bld-07-decorations-from-the-build-tool) | Decorations from the build tool | Decorations placed with the build tool get built. | 📝 Planned |
| [BLD-08](e2e-tests/construction.md#bld-08-work-orders-are-shared-between-builders) | Work orders are shared between builders | Two builders split two work orders. | 📝 Planned |
| [BLD-09](e2e-tests/construction.md#bld-09-clearing-the-site) | Clearing the site | The builder clears terrain, plants and water from the site. | 📝 Planned |

## Citizen life

Details: [e2e-tests/citizens.md](e2e-tests/citizens.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [CIT-01](e2e-tests/citizens.md#cit-01-eating-when-hungry) | Eating when hungry | A hungry citizen eats food from its inventory. | 📝 Planned |
| [CIT-02](e2e-tests/citizens.md#cit-02-eating-at-the-restaurant) | Eating at the restaurant | Hungry citizens are served at the restaurant. | 📝 Planned |
| [CIT-03](e2e-tests/citizens.md#cit-03-no-food-anywhere) | No food anywhere | A citizen with no food tells the player and is unhappy. | 📝 Planned |
| [CIT-04](e2e-tests/citizens.md#cit-04-sleeping-at-night) | Sleeping at night | Citizens sleep in their own beds at night and get up in the morning. | 📝 Planned |
| [CIT-05](e2e-tests/citizens.md#cit-05-rain) | Rain | Workers shelter from rain unless the config says otherwise. | 📝 Planned |
| [CIT-06](e2e-tests/citizens.md#cit-06-happiness-reacts-to-living-conditions) | Happiness reacts to living conditions | Each happiness factor moves happiness the right way. | 📝 Planned |
| [CIT-07](e2e-tests/citizens.md#cit-07-healing) | Healing | Injured citizens heal, faster at the hospital. | 📝 Planned |
| [CIT-08](e2e-tests/citizens.md#cit-08-disease-and-cure) | Disease and cure | A sick citizen is cured at the hospital with the right items. | 📝 Planned |
| [CIT-09](e2e-tests/citizens.md#cit-09-death-grave-and-mourning) | Death, grave and mourning | A death leaves a grave and the partner mourns. | 📝 Planned |
| [CIT-10](e2e-tests/citizens.md#cit-10-children-are-born-and-grow-up) | Children are born and grow up | Couples have children who grow up into workers. | 📝 Planned |
| [CIT-11](e2e-tests/citizens.md#cit-11-workers-gain-experience) | Workers gain experience | Working raises the job's skills. | 📝 Planned |
| [CIT-12](e2e-tests/citizens.md#cit-12-finding-the-way) | Finding the way | Citizens get through doors, stairs, ladders and water. | 📝 Planned |
| [CIT-13](e2e-tests/citizens.md#cit-13-getting-unstuck) | Getting unstuck | A stuck citizen gets free and resumes work. | 📝 Planned |
| [CIT-14](e2e-tests/citizens.md#cit-14-answering-a-citizen) | Answering a citizen | Answering a citizen's question changes what it does. | 📝 Planned |

## Requests and logistics

Details: [e2e-tests/logistics.md](e2e-tests/logistics.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [LOG-01](e2e-tests/logistics.md#log-01-warehouse-delivers-a-requested-item) | Warehouse delivers a requested item | A courier delivers a requested item from the warehouse. | 📝 Planned |
| [LOG-02](e2e-tests/logistics.md#log-02-couriers-bring-products-to-the-warehouse) | Couriers bring products to the warehouse | Couriers move surplus products into the warehouse. | 📝 Planned |
| [LOG-03](e2e-tests/logistics.md#log-03-crafting-chain) | Crafting chain | Missing items are crafted through a chain of crafters. | 📝 Planned |
| [LOG-04](e2e-tests/logistics.md#log-04-requests-the-colony-cant-fulfill-go-to-the-player) | Requests the colony can't fulfill go to the player | Items nobody can provide are requested from the player; the postbox completes it. | 📝 Planned |
| [LOG-05](e2e-tests/logistics.md#log-05-minimum-stock) | Minimum stock | Huts are kept at their minimum stock. | 📝 Planned |
| [LOG-06](e2e-tests/logistics.md#log-06-tools-by-hut-level) | Tools by hut level | Workers get tools within their hut level's tier limit. | 📝 Planned |
| [LOG-07](e2e-tests/logistics.md#log-07-several-couriers-share-the-work) | Several couriers share the work | Several couriers split the deliveries. | 📝 Planned |
| [LOG-08](e2e-tests/logistics.md#log-08-requests-are-cancelled-when-a-hut-is-removed) | Requests are cancelled when a hut is removed | Removing a hut cancels its requests without losing items. | 📝 Planned |
| [LOG-09](e2e-tests/logistics.md#log-09-full-warehouse) | Full warehouse | A full warehouse loses nothing and reports it. | 📝 Planned |
| [LOG-10](e2e-tests/logistics.md#log-10-stash) | Stash | Items put into a stash end up in the warehouse. | 📝 Planned |

## Jobs: farming, plants and animals

Details: [e2e-tests/jobs-farming.md](e2e-tests/jobs-farming.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [FRM-01](e2e-tests/jobs-farming.md#frm-01-farmer-field-cycle) | Farmer: field cycle | The farmer tills, plants, harvests and replants a field. | 📝 Planned |
| [FRM-02](e2e-tests/jobs-farming.md#frm-02-planter-plantation) | Planter: plantation | The planter harvests plantation crops and leaves the base plants. | 📝 Planned |
| [FRM-03](e2e-tests/jobs-farming.md#frm-03-florist) | Florist | The florist grows the chosen flowers with compost. | 📝 Planned |
| [FRM-04](e2e-tests/jobs-farming.md#frm-04-composter) | Composter | The composter turns allowed items into compost. | 📝 Planned |
| [FRM-05](e2e-tests/jobs-farming.md#frm-05-beekeeper) | Beekeeper | The beekeeper harvests full hives without harming the bees. | 📝 Planned |
| [FRM-06](e2e-tests/jobs-farming.md#frm-06-animal-herders) | Animal herders | Each herder keeps its animal count at the limit and uses its special action. | 📝 Planned |
| [FRM-07](e2e-tests/jobs-farming.md#frm-07-fisherman) | Fisherman | The fisherman finds water and fishes. | 📝 Planned |

## Jobs: gathering resources

Details: [e2e-tests/jobs-gathering.md](e2e-tests/jobs-gathering.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [GAT-01](e2e-tests/jobs-gathering.md#gat-01-lumberjack-fells-and-replants) | Lumberjack fells and replants | The lumberjack cuts whole trees and replants them. | 📝 Planned |
| [GAT-02](e2e-tests/jobs-gathering.md#gat-02-lumberjack-follows-its-settings) | Lumberjack follows its settings | The lumberjack only cuts allowed trees in its area. | 📝 Planned |
| [GAT-03](e2e-tests/jobs-gathering.md#gat-03-miner-digs-the-shaft) | Miner digs the shaft | The miner digs the shaft safely with ladders. | 📝 Planned |
| [GAT-04](e2e-tests/jobs-gathering.md#gat-04-miner-mines-a-level) | Miner mines a level | The miner digs out nodes and collects ores. | 📝 Planned |
| [GAT-05](e2e-tests/jobs-gathering.md#gat-05-quarrier) | Quarrier | The quarrier digs its quarry layer by layer. | 📝 Planned |
| [GAT-06](e2e-tests/jobs-gathering.md#gat-06-nether-expedition) | Nether expedition | The nether worker returns from an expedition with loot and a log entry. | 📝 Planned |

## Jobs: crafting and processing

Details: [e2e-tests/jobs-crafting.md](e2e-tests/jobs-crafting.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [CRF-01](e2e-tests/jobs-crafting.md#crf-01-every-crafter-crafts-its-recipes) | Every crafter crafts its recipes | Each crafting hut makes a typical recipe on request (15 huts). | 📝 Planned |
| [CRF-02](e2e-tests/jobs-crafting.md#crf-02-learning-recipes-and-their-limits) | Learning recipes and their limits | Huts accept only fitting recipes, up to their level's limit. | 📝 Planned |
| [CRF-03](e2e-tests/jobs-crafting.md#crf-03-furnaces-and-fuel) | Furnaces and fuel | Furnace workers keep furnaces fueled and filled. | 📝 Planned |
| [CRF-04](e2e-tests/jobs-crafting.md#crf-04-concrete-mixer-and-water) | Concrete mixer and water | The concrete mixer hardens powder with water. | 📝 Planned |
| [CRF-05](e2e-tests/jobs-crafting.md#crf-05-sifter-meshes) | Sifter meshes | The sifter needs a mesh and drops only that mesh's loot. | 📝 Planned |
| [CRF-06](e2e-tests/jobs-crafting.md#crf-06-alchemist) | Alchemist | The alchemist brews potions and harvests its ingredients. | 📝 Planned |
| [CRF-07](e2e-tests/jobs-crafting.md#crf-07-enchanter) | Enchanter | The enchanter draws XP from assigned citizens and enchants. | 📝 Planned |
| [CRF-08](e2e-tests/jobs-crafting.md#crf-08-cook-keeps-the-restaurant-stocked) | Cook keeps the restaurant stocked | The cook makes only the dishes on the menu. | 📝 Planned |

## Jobs: education, care and the dead

Details: [e2e-tests/services.md](e2e-tests/services.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [SRV-01](e2e-tests/services.md#srv-01-school) | School | The teacher teaches children and their skills rise. | 📝 Planned |
| [SRV-02](e2e-tests/services.md#srv-02-library) | Library | Students study with study items and get smarter. | 📝 Planned |
| [SRV-03](e2e-tests/services.md#srv-03-hospital) | Hospital | The healer treats several patients and asks for missing cures. | 📝 Planned |
| [SRV-04](e2e-tests/services.md#srv-04-graveyard) | Graveyard | The undertaker buries dead citizens in the graveyard. | 📝 Planned |
| [SRV-05](e2e-tests/services.md#srv-05-mystical-site) | Mystical site | A mystical site makes citizens happier, more at higher levels. | 📝 Planned |

## Defense and raids

Details: [e2e-tests/defense.md](e2e-tests/defense.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [DEF-01](e2e-tests/defense.md#def-01-guard-kills-a-hostile-mob) | Guard kills a hostile mob | A guard kills a hostile mob in its area. | 📝 Planned |
| [DEF-02](e2e-tests/defense.md#def-02-each-guard-type-fights-its-own-way) | Each guard type fights its own way | Each of the six guard types fights its own way. | 📝 Planned |
| [DEF-03](e2e-tests/defense.md#def-03-patrol-guard-and-follow-modes) | Patrol, guard and follow modes | Guards follow their patrol, guard and follow settings. | 📝 Planned |
| [DEF-04](e2e-tests/defense.md#def-04-guards-get-gear-by-level) | Guards get gear by level | Guards get and wear gear allowed by their building level. | 📝 Planned |
| [DEF-05](e2e-tests/defense.md#def-05-injured-guards-retreat-and-recover) | Injured guards retreat and recover | Badly hurt guards retreat, heal and return. | 📝 Planned |
| [DEF-06](e2e-tests/defense.md#def-06-raids-by-every-culture) | Raids by every culture | Raids of every culture arrive, attack and end correctly. | 📝 Planned |
| [DEF-07](e2e-tests/defense.md#def-07-surviving-a-raid-without-losses) | Surviving a raid without losses | A raid without deaths makes citizens happier. | 📝 Planned |
| [DEF-08](e2e-tests/defense.md#def-08-citizens-flee-from-danger) | Citizens flee from danger | Unarmed citizens run from monsters. | 📝 Planned |
| [DEF-09](e2e-tests/defense.md#def-09-training-raises-guard-skills) | Training raises guard skills | Training buildings raise guard skills. | 📝 Planned |
| [DEF-10](e2e-tests/defense.md#def-10-stable-and-cavalry) | Stable and cavalry | Cavalry guards ride horses from the stable. | 📝 Planned |
| [DEF-11](e2e-tests/defense.md#def-11-raid-timing-follows-the-config) | Raid timing follows the config | Raids come no more often than the config allows. | 📝 Planned |

## Research

Details: [e2e-tests/research.md](e2e-tests/research.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [RES-01](e2e-tests/research.md#res-01-researcher-finishes-a-research) | Researcher finishes a research | A researcher finishes a research and its effect becomes active. | 📝 Planned |
| [RES-02](e2e-tests/research.md#res-02-requirements-block-a-research) | Requirements block a research | Research can't start while requirements are missing. | 📝 Planned |
| [RES-03](e2e-tests/research.md#res-03-a-research-changes-behavior) | A research changes behavior | A research measurably changes citizen behavior. | 📝 Planned |
| [RES-04](e2e-tests/research.md#res-04-unlock-research) | Unlock research | Research unlocks buildings and guard types. | 📝 Planned |
| [RES-05](e2e-tests/research.md#res-05-undoing-a-research) | Undoing a research | Undoing a research removes its effect. | 📝 Planned |

## Visitors and quests

Details: [e2e-tests/visitors-quests.md](e2e-tests/visitors-quests.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [VIS-01](e2e-tests/visitors-quests.md#vis-01-visitors-come-to-the-tavern) | Visitors come to the tavern | A tavern attracts visitors. | 📝 Planned |
| [VIS-02](e2e-tests/visitors-quests.md#vis-02-recruiting-a-visitor) | Recruiting a visitor | Paying a visitor's cost makes it a citizen. | 📝 Planned |
| [VIS-03](e2e-tests/visitors-quests.md#vis-03-a-quest-from-start-to-reward) | A quest from start to reward | A quest goes from trigger to reward. | 📝 Planned |
| [VIS-04](e2e-tests/visitors-quests.md#vis-04-declining-and-cancelling-quests) | Declining and cancelling quests | Declined and cancelled quests end cleanly. | 📝 Planned |

## Colony connections

Details: [e2e-tests/connections.md](e2e-tests/connections.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [CON-01](e2e-tests/connections.md#con-01-connecting-two-colonies) | Connecting two colonies | Two colonies with gate houses and signs become connected. | 📝 Planned |
| [CON-02](e2e-tests/connections.md#con-02-becoming-allies) | Becoming allies | An ally request and confirmation make colonies allies. | 📝 Planned |
| [CON-03](e2e-tests/connections.md#con-03-feud) | Feud | A feud changes the relation for both colonies. | 📝 Planned |
| [CON-04](e2e-tests/connections.md#con-04-neutral-and-disconnect) | Neutral and disconnect | Relations can be reset to neutral and disconnected. | 📝 Planned |

## Robustness

Details: [e2e-tests/robustness.md](e2e-tests/robustness.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [ROB-01](e2e-tests/robustness.md#rob-01-chunks-unload-during-work) | Chunks unload during work | Citizens and huts resume after their chunks reload. | 📝 Planned |
| [ROB-02](e2e-tests/robustness.md#rob-02-server-restart) | Server restart | A colony in full swing survives a world restart. | 📝 Planned |
| [ROB-03](e2e-tests/robustness.md#rob-03-large-colony-performance) | Large colony performance | A 100-citizen colony stays within a tick time budget. | 📝 Planned |
| [ROB-04](e2e-tests/robustness.md#rob-04-loading-a-1211-world) | Loading a 1.21.1 world | A world from NeoForge 1.21.1 loads in the Fabric port. | 📝 Planned |
| [ROB-05](e2e-tests/robustness.md#rob-05-colonies-dont-affect-each-other) | Colonies don't affect each other | Two neighboring colonies keep their items and citizens apart. | 📝 Planned |
| [ROB-06](e2e-tests/robustness.md#rob-06-lost-citizen-entities-come-back) | Lost citizen entities come back | A citizen whose entity vanished is spawned again. | 📝 Planned |
| [ROB-07](e2e-tests/robustness.md#rob-07-pathfinding-runs-on-one-known-thread) | Pathfinding runs on one known thread | All MineColonies pathfinding goes through the executor the test server waits for. | 📝 Planned |

## Fabric port integration

Details: [e2e-tests/fabric-port.md](e2e-tests/fabric-port.md)

| ID | Test | What it checks | Status |
|---|---|---|---|
| [FAB-01](e2e-tests/fabric-port.md#fab-01-other-mods-can-use-minecolonies-racks) | Other mods can use MineColonies racks | Other Fabric mods can put items into and take them out of racks. | ✅ Implemented |
| [FAB-02](e2e-tests/fabric-port.md#fab-02-minecolonies-can-use-vanilla-chests) | MineColonies can use vanilla chests | MineColonies can put items into vanilla chests. | ✅ Implemented |
| [FAB-03](e2e-tests/fabric-port.md#fab-03-minecolonies-can-use-fabric-only-storages) | MineColonies can use Fabric-only storages | MineColonies can put items into Fabric-only storages. | ✅ Implemented |
| [FAB-04](e2e-tests/fabric-port.md#fab-04-minecolonies-food-is-compostable) | MineColonies food is compostable | MineColonies food uses the vanilla compost tiers. | ✅ Implemented |

## User interface

Details: [e2e-tests/ui.md](e2e-tests/ui.md). Run with `./gradlew runClientGameTest` (game window, screenshots in `build/e2e-screenshots/`).

| ID | Test | What it checks | Status |
|---|---|---|---|
| [UI-01](e2e-tests/ui.md#ui-01-town-hall-window) | Town hall window | The town hall pages open and show the colony's real citizen count. | ✅ Implemented |
| [UI-02](e2e-tests/ui.md#ui-02-builders-hut-window) | Builder's hut window | A hut window shows the building's name, level and worker. | ✅ Implemented |
| [UI-03](e2e-tests/ui.md#ui-03-citizen-window) | Citizen window | The citizen window shows the citizen's real name, skills and job. | ✅ Implemented |
| [UI-04](e2e-tests/ui.md#ui-04-build-tool-window) | Build tool window | The build tool opens on the selected pack and lists its categories. | ✅ Implemented |
| [UI-05](e2e-tests/ui.md#ui-05-research-tree) | Research tree | The university and research tree show the branch's research. | ✅ Implemented |
| [UI-06](e2e-tests/ui.md#ui-06-post-box-request-window) | Post box request window | The post box lists items that can be requested. | ✅ Implemented |
| [UI-07](e2e-tests/ui.md#ui-07-warehouse-window) | Warehouse window | The warehouse window opens with the right title. | ✅ Implemented |
| [UI-08](e2e-tests/ui.md#ui-08-human-input-is-ignored) | Human input is ignored | Real keyboard and mouse input can't reach a test window. | ✅ Implemented |
