[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Citizen life

Everyday needs and reactions of citizens: food, sleep, weather, happiness, health, death and family.

---

## CIT-01 Eating when hungry

**Tests:** a hungry citizen with food in its inventory eats.

**Setup**
- Colony with one unemployed citizen. Its saturation is set low.
- Bread in the citizen's inventory.

**What happens**
1. The colony runs.

**Passes when**
- The citizen eats: saturation goes up and the bread count goes down.
- The citizen doesn't eat again while it's full.

**Time budget:** 1 minute.

**As tested (`cit01_eatingWhenHungry`):** "full" is `MAX_SATURATION` = 60, not 20: a citizen eats one item after the other until it reaches 60 or runs out (16 bread → about 12 eaten). The test waits until the citizen has left the eating states, then checks for 200 ticks that no more bread is eaten.

---

## CIT-02 Eating at the restaurant

**Tests:** hungry citizens without food go to the restaurant and the cook serves them.

**Setup**
- Colony with a restaurant (level 1), a hired cook and cooked food in the restaurant.
- Three citizens with low saturation and empty inventories.

**What happens**
1. The citizens walk to the restaurant. The cook hands out food.

**Passes when**
- All three citizens reach the restaurant and get food from the cook (the food leaves the restaurant's inventory).
- Their saturation goes up.
- Their happiness food modifier reflects the meal (and the "great food" modifier when the food is a high tier MineColonies meal).

**Time budget:** 3 minutes.

**As tested (`cit02_eatingAtTheRestaurant`):** bread is put on the restaurant menu and into the restaurant. The food happiness factor only counts once a citizen has a home and a full history of 10 meals (`CitizenHappinessHandler.getFoodFactor`), so instead the test checks that bread is in each guest's food history, that the colony's `food_served` statistic went up (food came from the cook) and that bread left the restaurant.

---

## CIT-03 No food anywhere

**Tests:** a starving citizen without any food source tells the player instead of silently suffering.

**Setup**
- Colony without a restaurant. One citizen with zero saturation and no food.

**What happens**
1. The colony runs.

**Passes when**
- The citizen has an open interaction (the chat bubble a player would see) saying it's hungry.
- The citizen's happiness food modifier is negative.
- After the test puts food in its inventory, it eats and the interaction goes away.

**Time budget:** 2 minutes.

**As tested (`cit03_noFoodAnywhere`):** the interaction is `com.minecolonies.coremod.ai.norestaurant`. The food happiness factor is about meal diversity and quality, not hunger (see CIT-02), so the test checks the *social* factor instead, which counts citizens with saturation ≤ 1.

---

## CIT-04 Sleeping at night

**Tests:** citizens go to their beds at night and get up in the morning.

**Setup**
- Colony with two residences and four citizens, each with a home.
- Time set to just before nightfall.

**What happens**
1. Night falls. Morning comes.

**Passes when**
- At night, each citizen walks home and is asleep in a bed of its own home (sleep AI state, bed position in its home).
- Two citizens never share a bed.
- In the morning they wake up and leave the beds.
- Each citizen gets the "slept tonight" happiness modifier.

**Time budget:** one night.

**As tested (`cit04_sleepingAtNight`):** two level 2 residences (2 beds each). "Slept tonight" is a day counter that `EntityAISleep` resets to 0 when the citizen lies down; the test first advances it to 3 days and checks it is 0 after sleeping. Night = `setTime(13000)`, morning = `setTime(1000)`.

---

## CIT-05 Rain

**Tests:** workers stop working in the rain unless the config says otherwise.

**Setup**
- A lumberjack and a farmer at work, `workersAlwaysWorkInRain` off.

**What happens**
1. The test starts rain. Then it stops the rain.
2. Second run with `workersAlwaysWorkInRain` on.

**Passes when**
- In the rain (first run), both workers stop their job and take shelter.
- When the rain stops, they go back to work.
- In the second run they keep working in the rain.

**Time budget:** 3 minutes.

**As tested (`cit05_rain`):** a lumberjack (no trees needed, its AI never goes idle) and a teacher instead of a farmer (a farmer without a field goes idle anyway). The teacher's hut allows rain work (`WorkerBuildingModule.canWorkDuringTheRain`), so it is the "keeps working" contrast. There is no "take shelter" behaviour: in the rain the worker switches to the citizen AI state IDLE with the BAD_WEATHER status. Both runs are in one test (config switched on for the second part).

---

## CIT-06 Happiness reacts to living conditions

**Tests:** each happiness factor moves in the right direction.

**Setup**
- Several citizens, each set up to isolate one factor: no home (homelessness), no job (unemployment), injured (health), no guards
  nearby (security), good food, a mystical site nearby, and one in perfect conditions as the baseline.

**What happens**
1. The colony runs long enough for happiness to update.

**Passes when**
- Each citizen's matching modifier is negative (or positive for good food and the mystical site) compared to the baseline.
- The total happiness is lower for the citizens with negative factors than for the baseline citizen.
- When the cause is fixed (the homeless citizen gets a home), the modifier recovers.

**Time budget:** 2 minutes.

**As tested (`cit06_happinessReactsToConditions`, `cit06b_negativeFactorsLowerHappiness`):** the health factor is about *disease* (sick = 0.5), not injury, so the "health" citizen is infected. Security, social and mystical site factors are colony wide, so they can't be isolated per citizen: security (no guards) is only checked to be below neutral; the mystical site is SRV-05. The baseline needs a level 3 home (housing factor = home level / 3) and a job in a hut of level ≤ 3. Good food = ten different MineColonies meals in the food history and a home. The total happiness is checked for the homeless (lower) and well fed (higher) citizens; that the unemployed and the sick citizen are less happy is `cit06b` (failed before the fix of bug E2E-CIT06-1, see protocol). Checks run in the tick after the infection, because with more than four citizens the disease spreads on contact.

---

## CIT-07 Healing

**Tests:** injured citizens heal, faster in a hospital.

**Setup**
- Two citizens at low health, one near a hospital with a healer, one without.

**What happens**
1. The colony runs.

**Passes when**
- Both regain health over time.
- The one at the hospital goes there and heals faster.

**Time budget:** 3 minutes.

**As tested (`cit07_healing`):** the healer heals *any* citizen under 10 health within 40 × hut level blocks of the hospital, so two patients in one arena can't be compared. The test injures one citizen (health 4) without a hospital and measures the ticks to 12 health, then builds a hospital with a healer, injures it again and checks it heals faster (typically 765 vs. 265–365 ticks).

---

## CIT-08 Disease and cure

**Tests:** a sick citizen goes to the hospital and the healer cures it with the right items.

**Setup**
- A hospital with a healer, and the cure items for one disease from `data/minecolonies/colony/diseases` in the hospital.
- One citizen infected with that disease by the test.

**What happens**
1. The citizen goes to the hospital. The healer treats it.

**Passes when**
- The citizen moves into a hospital bed.
- The cure items are used up.
- The disease is removed from the citizen's data and it returns to normal life.
- Without the cure items (second run), the healer requests exactly those items.

**Time budget:** 5 minutes.

**As tested (`cit08_diseaseAndCure`, `cit08b_healerRequestsCure`):** influenza (carrot + potato). `CitizenDiseaseHandler.setDisease` refuses while the colony has no more than `initialCitizenAmount` (4) citizens, so the test sets the disease directly. "Cure items used up" = one carrot and one potato fewer in hospital + healer + patient together. The second run is the separate test `cit08b`.

---

## CIT-09 Death, grave and mourning

**Tests:** when a citizen dies, a grave appears and others mourn.

**Setup**
- Colony with four citizens. Two of them are in the same family (partners).

**What happens**
1. The test kills one partner.

**Passes when**
- A grave block appears where it died, holding its inventory.
- The colony's grave manager knows the grave.
- The other partner is mourning (mourn state and the death happiness modifier).
- The dead citizen is removed from the citizen list.

**Time budget:** 1 minute.

**As tested (`cit09_deathGraveMourning`):** a citizen's damage per hit is capped, so the test hits it every tick until it dies. The mourn list is filled at death (partner only); the *mourning state* starts at the next wake-up (`CitizenManager.onWakeUp`), so the test sets night, then morning, and checks the partner is in the MOURN AI state. The partner relation is cleared at death.

---

## CIT-10 Children are born and grow up

**Tests:** with free housing, couples have children that later become adults.

**Setup**
- Colony with spare beds and two adult citizens who can have a child.
- The reproduction timer moved forward so a birth is due.

**What happens**
1. The reproduction manager runs.
2. The test moves the child's age forward to just before adulthood. The colony runs.

**Passes when**
- A child citizen appears with a home in the colony.
- The child doesn't take a job.
- After growing up it becomes an adult citizen that can be hired.

**Time budget:** 2 minutes.

**As tested (`cit10_childrenBornAndGrowUp`):** the reproduction timer (`ReproductionManager.childCreationTimer`) is set to 0 by reflection, with move-in on, a level 3 residence (one spare bed) and a woman and a man. A free lumberjack place exists while the child lives, to show it isn't hired. Growing up: the child AI's active time (`EntityAICitizenChild.aiActiveTime`) is set past 70000 by reflection; the child grows at the next check (every 500 ticks). Afterwards it is hired (or already auto-hired) into the free place.

---

## CIT-11 Workers gain experience

**Tests:** doing a job raises the right skills.

**Setup**
- A lumberjack with known skill levels, near grown trees.

**What happens**
1. The lumberjack fells several trees.

**Passes when**
- The lumberjack's XP went up.
- The skills the lumberjack job uses rise first (primary skill more than secondary), others stay unchanged.
- A level up raises the skill level exactly once per threshold.

**Time budget:** 10 minutes.

**As tested (`cit11_workersGainExperience`):** the lumberjack needs an axe *and* a hoe (or shears), and a level 1 hut only accepts low-level tools (wooden tools are used). Trees are built from logs and leaves. Experience also goes to the complementary skill of the primary and secondary skill (Athletics, Mana) and is taken from their adverse skills (Agility, Adaptability) (`CitizenExperienceHandler.addExperience`); only the remaining skills stay unchanged. The level-up rule is checked by adding exactly one threshold of XP to a skill. Note: citizens without a home get no XP for skills at level ≥ 10 (`CitizenSkillHandler.addXpToSkill`), so the test uses level 5.

---

## CIT-12 Finding the way

**Tests:** citizens get around a real village: doors, stairs, ladders, bridges and shallow water.

**Setup**
- A pathfinding arena: a closed house with a door, a second floor reached by stairs, a tower reached by a ladder, a river with a
  bridge and a shallow ford.
- One citizen and a series of target points.

**What happens**
1. The test sends the citizen to each target in turn (the same "walk to" used by the command).

**Passes when**
- The citizen reaches every target within its time budget.
- It opens and closes the door on the way.
- It doesn't take damage from falling or drowning.

**Time budget:** 2 minutes.

**As tested (`cit12_findingTheWay`):** the course is built by the test in the west half of the arena (house with door, stone platform with oak stairs, stone tower with a ladder, a 2 deep river with a plank bridge and a 1 deep ford). The walk uses the same mechanism as `/mc citizens walk` (an AI event calling `EntityNavigationUtils.walkToPos` until it arrives) with an arrival distance of 1 instead of 4. "No damage" = health never below 20.

---

## CIT-13 Getting unstuck

**Tests:** a citizen that gets stuck recovers on its own.

**Setup**
- A citizen trapped in a 1×1 hole, two blocks deep, with a job waiting elsewhere.

**What happens**
1. The colony runs.

**Passes when**
- The citizen gets out within the budget (by the stuck handling: digging out, or teleporting as the last resort).
- It resumes its job afterwards.

**Time budget:** 2 minutes.

**As tested (`cit13_gettingUnstuck`):** the hole is dug into the floor; the worker (a lumberjack whose hut is about 40 blocks away) is teleported into it. It gets out through the stuck handler's last resort (teleport) after roughly 2300–2800 ticks, which is longer than the 2 minute budget; the test allows 5000 ticks.

---

## CIT-14 Answering a citizen

**Tests:** citizen interactions can be answered and the citizen acts on the answer.

**Setup**
- A citizen with an open interaction that offers answers (for example a request it needs help with).

**What happens**
1. The test answers on behalf of the fake owner, using the same handler as the GUI.

**Passes when**
- The interaction closes.
- The citizen's state changes as the answer says (for example the request is cancelled when the player chooses to cancel).

**Time budget:** 30 seconds.

**As tested (`cit14_answeringACitizen`):** a lumberjack creates a synchronous request for a diamond (blocking `RequestBasedInteraction`); the test triggers the "cancel" answer via `ICitizenData.onResponseTriggered` (the server side of the GUI answer). The request is cancelled at once, the interaction is removed at the next citizen update.

---

## CIT-15 Leisure break

**Tests:** a working citizen takes a leisure break: it leaves work, spends the break on its leisure and goes back to work afterwards.

**Setup**
- A lumberjack (no trees, so it never goes idle by itself) with a home, working.

**What happens**
1. A leisure break starts, as the random roll in `CitizenData.update` starts one (about once per 144,000 ticks in a level 1 home).
2. The break lasts 3 minutes in the game; the test mod ends every break after 3 seconds (60 ticks, see
   [framework.md](framework.md#test-configuration)).

**Passes when**
- Within 2 seconds the citizen AI leaves work for its leisure: state `IDLE`, where the wander AI runs (walk around, or go to a
  leisure site, the home or the town hall, and sit or read there).
- While at leisure the worker AI is paused (its state doesn't change) and the status shows "house".
- The break ends after 60 ticks (±2) and the citizen was at leisure for at least 40 of them.
- The citizen goes back to work (citizen AI `WORK`/`WORKING`) within 5 seconds.

**Time budget:** 1 minute.

**As tested (`cit15_leisureBreak`):** the break is started with `fixture.startLeisureBreak(citizen)` (sets the remaining leisure time
to 3,600 ticks like the mod's roll; the test mod ends it 60 ticks later). In `CitizenAI.calculateNextState` a citizen with leisure time
left goes `IDLE` instead of `WORK` (unless its job can't be interrupted); the wander AI (`EntityAICitizenWander`) decides every 100
ticks in `IDLE`, so in 3 seconds the citizen may or may not start walking; the test checks the states, not a walk.
