[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Defense and raids

Guards, their training and gear, and raids on the colony.

---

## DEF-01 Guard kills a hostile mob

**Tests:** a guard notices a hostile mob in its area and kills it.

**Setup**
- A guard tower with a knight, sword and armor.
- A zombie spawned inside the guard's patrol area. Daytime protection for the zombie (it doesn't burn).

**What happens**
1. The guard fights.

**Passes when**
- The guard targets the zombie and kills it.
- No citizen and no player is hit by the guard.
- The guard gains XP.

**Time budget:** 2 minutes.

**As implemented (e2e run):** The guard (knight, stone sword, leather chestplate, level 1 guard tower) is moved into the open field next to the arena wall and a husk (a zombie that does not burn by day) is spawned 8 blocks away. A bystander citizen stands at the town hall. Checks: the husk dies after being hurt by the guard, the guard's total XP rose, neither the bystander nor the owner was hurt by the guard.

---

## DEF-02 Each guard type fights its own way

**Tests:** every guard type uses its weapon and style.

One test per guard type:

| Guard type | Checked behavior |
|---|---|
| Knight | melee with sword, blocks with shield |
| Huscarl | melee, its own weapon and gear |
| Ranger | keeps distance, shoots arrows |
| Marksman | keeps distance, shoots arrows, its own gear |
| Druid | throws potions, heals allies |
| Cavalry | fights mounted on its horse |

**Setup**
- The matching guard building with one guard of that type and its gear, the research it needs done (fixture).
- A few hostile mobs in range. For the druid, also an injured citizen nearby.

**What happens**
1. The guard fights.

**Passes when**
- The guard uses its expected weapon (melee hits, arrows fired, potions thrown, attacks from horseback).
- The mobs die.
- For the druid, the injured citizen gets healed.

**Time budget:** 3 minutes per type.

**As implemented (e2e run):** One test per type with a level 1 building (`def02_*`): knight (stone sword), huscarl (stone axe, research `combat/slicedanddecided`), ranger (bow + arrows), marksman (crossbow + arrows, research `combat/thathitthemark`) must kill the husk; melee types must hold their weapon while attacking, ranged types must fire arrows owned by the guard. **Druid (design changed):** the druid's potions don't kill - against a monster it throws slowness (weakness only with the magic-potion research), and it supports only allies that are in combat - so the druid test (barracks tower, the only building with a druid slot) checks that a druid potion is thrown and the husk gets slowness. The knight's shield blocking needs research `combat/avoidance` plus a shield and is not checked. **Cavalry** is not implemented (see DEF-10).

---

## DEF-03 Patrol, guard and follow modes

**Tests:** guards obey their task setting.

**Setup**
- A guard tower with a guard. Fake owner standing in the colony.

**What happens**
1. The test sets the guard to patrol, then to guard a fixed spot, then to follow the fake owner (who moves around).

**Passes when**
- Patrol: the guard moves between patrol points inside its area.
- Guard: the guard stays near the chosen spot.
- Follow: the guard stays close to the fake owner while the owner moves.

**Time budget:** 3 minutes.

**As implemented (e2e run):** As implemented: patrol (default, automatic mode) must yield at least 3 different patrol targets, the guard more than 8 blocks from the tower and never beyond `getPatrolDistance()`. Guard: the guard walks to the set guard spot; **design changed:** a guarding guard does not stay put the whole time - it leaves for errands (e.g. dumping its inventory at the hut after a few actions) and returns lazily (`guardMovement()` walks back with a 1-in-10 chance every 100 ticks) - so the test checks that after another 1500 ticks the guard is (back) within 6 blocks of the spot. Follow: the follow target is looked up in the server's player list and the level's player list, which the fake owner is not in; the test adds him to both for this phase (removed at the end). The owner then moves twice; the guard must come within 6 blocks each time (budget 6000 ticks, since a following guard can still fall asleep for up to 3000 ticks).

---

## DEF-04 Guards get gear by level

**Tests:** guards request the armor and weapons their building level allows and wear them.

**Setup**
- A guard tower at level 1 with an unequipped knight. A warehouse with leather, iron and diamond gear and a courier.

**What happens**
1. The knight requests gear. The test later raises the tower's level.

**Passes when**
- At level 1, the knight gets and wears gear within the level 1 limits, never diamond.
- After the upgrade, better gear is requested and worn.

**Time budget:** 5 minutes.

**As implemented (e2e run):** As implemented: level 1 guard tower, warehouse (leather, iron, diamond armor and stone, iron, diamond swords), a courier (courier's hut level 1), an unequipped knight. The knight must end up with a sword and a chestplate; everything worn is leather and he has no iron or diamond item (level 1 allows leather/gold armor and level-1 tools). Upgrade without a builder: the level 3 tower blueprint is pasted over the tower (build tool paste; the hut block's schematic data sets the level, as when a builder finishes an upgrade) and the tower reports level 3. Then an iron and a diamond chestplate are put into the tower, the knight loses his helmet and his sword is moved into the tower. **Design note:** guards look at their gear only while at their hut (PREPARING, `AbstractEntityAIFight.atBuildingActions`), which they visit when a weapon is missing or to dump loot (the dump cycle is far longer than the budget) - the moved sword sends him there. Checks: he takes the iron chestplate from the tower, requests a helmet and gets the warehouse's iron helmet (not the diamond one), holds a sword again, wears no diamond, and the diamond chestplate stays in the tower (level 3 allows armor up to iron).

---

## DEF-05 Injured guards retreat and recover

**Tests:** a badly hurt guard stops fighting, heals and returns.

**Setup**
- A guard set to low health, fighting a mob. Food available at its building.

**What happens**
1. The guard falls below its retreat threshold.

**Passes when**
- The guard leaves the fight and goes to recover.
- Its health goes back up.
- It returns to its task afterwards.

**Time budget:** 3 minutes.

**As implemented (e2e run):** As implemented: research `combat/regeneration` (retreat unlock), the tower's retreat setting on, a knight far from the tower attacks a slowed and weakened husk; the test then sets his health to 2. Checks: state GUARD_FLEE, then GUARD_REGEN within 8 blocks of the tower, then health >= 75% and back to another state. Food is not needed (regeneration is a potion effect).

---

## DEF-06 Raids by every culture

**Tests:** each raider culture arrives, attacks and the raid ends correctly.

One test per raid type:

| Raid | Arrives |
|---|---|
| Barbarians | by land, camp at the colony edge |
| Pirates | by ship (needs water by the colony) |
| Drowned pirates | from the water |
| Egyptians | by land |
| Norsemen | by land and by ship |
| Amazons | by land |

**Setup**
- A colony (with water arena for ship raids), two guard towers with guards, and the colony's raid level set by the fixture.

**What happens**
1. The test starts the raid through the raid manager.
2. Raiders spawn, march on the colony and fight the guards.

**Passes when**
- The raid event exists in the event manager with the right culture.
- Raiders spawn at the colony edge (ships spawn on water), the number fits the raid level and `maxRaiders`.
- Raiders head into the colony.
- When all raiders are dead, the raid event ends and is removed. Ship structures are cleaned up.

**Time budget:** 10 minutes per raid.

**As implemented (e2e run):** **Design changed:** the fixture forces raids without ships, so: barbarians, amazons, egyptians, norsemen (land) and pirates (the land variant `pirate_ground_raid`) are tested; drowned pirates and all ship raids are not implementable here (a ship raid needs a large body of deep water next to the colony; `drowned_pirate_raid` without ships returns NO_SPAWN_POINT). No guards: the test kills the raiders itself (lethal hits with a living attacker, since raiders cap damage without one), after checking: one raid event of the right type, 4 raiders spawned within 16 blocks of the spawn point (arena corner), raiders come within 14 blocks of the town hall; after the kill the event is removed, the colony is no longer raided and the camp fires are gone. The barbarian test also checks that a tiny colony (raid level < 75) gets TOO_SMALL for a natural-size raid and that `calculateRaiderAmount` never exceeds `maxRaiders` + 1.

---

## DEF-07 Surviving a raid without losses

**Tests:** citizens are happier after a raid in which nobody died.

**Setup**
- A raid as in DEF-06 that the guards win, with no citizen deaths.
- A second run in which one citizen is killed during the raid.

**What happens**
1. The raids run to the end.

**Passes when**
- In the first run, the citizens get the "raid without death" happiness modifier.
- In the second run they don't, and the death is counted in the colony statistics.

**Time budget:** 10 minutes.

**As implemented (e2e run):** As implemented: two citizens, a forced barbarian raid of 3 that the test ends by killing the raiders. Run 1: both citizens get the `raidwithoutdeath` modifier. Run 2: a raider-sourced attack kills one citizen during the raid (repeated hits: citizens cap the damage per hit); the other citizen gets no modifier, the colony's `death` statistic rose and the raid's lost-citizen count is > 0.

---

## DEF-08 Citizens flee from danger

**Tests:** unarmed citizens run away from monsters and raiders.

**Setup**
- Three working citizens and a hostile mob that moves toward them. No guards.

**What happens**
1. The mob approaches.

**Passes when**
- The citizens stop working and move away from the mob.
- They go back to work once the mob is gone.

**Time budget:** 2 minutes.

**As implemented (e2e run):** **Design changed:** citizens don't flee on sight. They switch to the FLEE state only when hurt (`EntityCitizen.performMoveAway`); while fleeing they keep away from monsters in range. The test: a working lumberjack and two unemployed citizens; a husk targets the lumberjack. Checks: the lumberjack enters FLEE, gets at least 8 blocks away from the husk; after the husk is removed he goes back to WORK; all three citizens survive.

---

## DEF-09 Training raises guard skills

**Tests:** the combat academy and archery train guards.

**Setup**
- A combat academy with a knight in training and an archery with a ranger in training, with their gear.

**What happens**
1. The trainees train.

**Passes when**
- The knight's combat skills rise, the ranger's archery skills rise.
- The training uses the right targets (dummies, other trainees or targets).

**Time budget:** 10 minutes.

**As implemented (e2e run):** As implemented: combat academy level 1 with a knight trainee (wooden sword + shield) and archery level 1 with a ranger trainee (bow + arrows). **Design changed:** skill levels rise only after a lot of experience, so the test checks that both trainees gain experience from training (total XP) and that the archer went through its shooting state.

---

## DEF-10 Stable and cavalry

**Tests:** the stablemaster provides horses and cavalry guards ride them.

**Setup**
- A stable with a stablemaster and horses, plus a guard building with a cavalry guard (research done by the fixture).

**What happens**
1. The cavalry guard gets a horse from the stable and patrols.

**Passes when**
- The cavalry guard is riding a horse that belongs to the stable.
- The stablemaster's worker slot limit (one) and the cavalry assignment rules from `StableCavalryBuildingModule` hold.
- If the horse dies, the guard gets a new one.

**Time budget:** 5 minutes.

**As implemented (e2e run):** **Not implemented** (see the protocol): the stablemaster's training of a horse is chance- and cooldown-driven (25% chance after a 50-decision cooldown) and only for horses inside the stable's corners; in the arena the horses were led away/stabled and no cavalry horse was registered within 12000 ticks.

---

## DEF-11 Raid timing follows the config

**Tests:** raids come no more often than the config allows.

**Setup**
- `enableColonyRaids` on, a minimum of two nights between raids, a colony big enough to be raided.

**What happens**
1. The colony runs through ten nights.

**Passes when**
- Raids start only at night.
- There are always at least two nights between raids.
- With `enableColonyRaids` off, no raid ever starts.

**Time budget:** ten nights.

**As implemented (e2e run):** As implemented: raids on, minimum 2 / average 3 nights between raids (with these values the mod raids every third night deterministically), 8 citizens with skills 80 (raid level >= 75), the owner added as an important colony player. Nights are made by setting the time (morning, then 13500); each new raid event is ended at once (status DONE) so the next night starts clean. Ten nights with raids on, then five nights with raids off. Two things the test has to provide: the game rule `spawnMobs` on (the test server has it off and `WorldUtil.isPeaceful` treats that as peaceful: no raids), and ground around the arena (a natural raid spawns 40+ blocks from the buildings; the test lays a grass ring and removes it afterwards).

