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
