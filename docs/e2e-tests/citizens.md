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
