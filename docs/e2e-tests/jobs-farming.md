[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Jobs: farming, plants and animals

Workers who grow plants, keep animals or fish.

---

## FRM-01 Farmer: field cycle

**Tests:** the farmer tills, plants, harvests and replants an assigned field.

**Setup**
- A farm (level 1) with a farmer and an assigned field (field block) set to wheat, on untilled dirt.
- Wheat seeds in the farm.

**What happens**
1. The farmer tills and plants the field.
2. The test grows all crops to full size (skipping the growth time).
3. The farmer harvests and replants.

**Passes when**
- Every field block inside the field is tilled and planted with wheat.
- After growth, every crop is harvested and replanted (no bare farmland left).
- The farm's inventory holds the harvested wheat and seeds.

**Time budget:** 10 minutes.

**As implemented** (`FarmingGatheringTests`): the field (7x7 around the scarecrow) is plain dirt, the farm holds wooden hoes (a level 1 hut only allows wood/gold tools) and seeds. The farmer works each field only once per colony day (`BuildingExtensionsModule.getBuildingExtensionToWorkOn` skips fields already worked today) and idles for the rest of the day, so the test lets a night pass whenever the farmer has been idle for 100 ticks. `frm01_farmerTillsGrass` repeats the first half on grass, the usual case in a real world, and fails (bug E2E-FRM-01-1, optional test).

---

## FRM-02 Planter: plantation

**Tests:** the planter grows and harvests plantation crops without destroying the base plants.

**Setup**
- A plantation with a planter and plantation fields for sugar cane, cactus and bamboo.
- The plants already grown taller than the harvest height.

**What happens**
1. The planter works through the fields.

**Passes when**
- The upper parts are harvested and the base block of each plant is left standing.
- Missing plants are replanted from the hut's stock.
- The harvest is in the plantation's inventory.

**Time budget:** 10 minutes.

**As implemented** (`FarmingGatheringTests`): one test per field type (`frm02_planterSugarCane`, `frm02_planterCactus`, `frm02_planterBamboo`), because a level 1 plantation owns only one field. The field blueprint from the pack is pasted and registered as when placed by hand, and assigned to the plantation manually. Every second working position gets a plant above the harvest height (3 blocks, bamboo 8 with the research "jungle master" done), the others are empty. Like the farmer, the planter needs several colony days; the test lets nights pass when he idles. Bamboo: the planter reliably leaves one position for very long, so the test accepts all but one position per kind.

---

## FRM-03 Florist

**Tests:** the florist grows the chosen flowers using compost.

**Setup**
- A flower shop with a florist, compost and the planter blocks of the hut.
- Two flower types selected in the hut's settings.

**What happens**
1. The florist works.

**Passes when**
- Only the selected flowers are grown.
- Compost is used up.
- Flowers end up in the hut's inventory.
- Without compost, the florist requests it.

**Time budget:** 10 minutes.

**As implemented** (`FarmingGatheringTests`): a level 2 flower shop (level 1 only grows poppies and dandelions anyway); every small flower except poppy and dandelion is on the hut's "don't grow" list (the list is a blacklist). 3 compost per plant bed; the beds run at double speed. Checks: at least 3 flowers harvested, only poppies/dandelions, compost used up, then a compost request. More speed-up is not possible: a composted bed's timer also runs while the bed is not composted, so with a faster timer every compost expires at its first update (see protocol).

---

## FRM-04 Composter

**Tests:** the composter turns compostable items into compost in its barrels.

**Setup**
- A composter's hut with a composter, its barrels, and compostable items set as allowed inputs.

**What happens**
1. The composter fills the barrels. The test skips the barrels' composting time.
2. The composter empties the barrels.

**Passes when**
- The input items are used up in the expected amounts.
- Compost appears in the hut's inventory.
- Items not on the allowed list are never used.

**Time budget:** 10 minutes.

**As implemented** (`FarmingGatheringTests`): only wheat seeds are allowed; apples (compostable but not allowed) also lie in the hut and must stay untouched. The expected use is exact: barrels x floor(64 / recipe strength) seeds. The test finishes the composting by calling the barrels' update method until they are done.

---

## FRM-05 Beekeeper

**Tests:** the beekeeper harvests full hives and keeps the bees.

**Setup**
- An apiary with a beekeeper, hives at full honey level with bees, shears and glass bottles.

**What happens**
1. The beekeeper works.

**Passes when**
- Honeycomb (with shears) and honey bottles (with bottles) end up in the hut.
- The hives' honey level goes back to empty.
- The bees aren't angered (smoke or campfire handling as the hut does it) and no bee dies.

**Time budget:** 5 minutes.

**As implemented** (`FarmingGatheringTests`): a level 1 apiary owns one hive (placed by the test and assigned with `addHive`) with two bees inside. "Both" needs research, so the test runs honeycomb (default setting) first, then switches to honey and refills the hive. The beekeeper has dexterity 99: the AI angers the bees on purpose with a chance of (50 - dex/2)%, which is intended behavior.

---

## FRM-06 Animal herders

**Tests:** each herder keeps its animal count at the hut's limit: breeds when below, butchers when above, and uses its special
action.

One test per herder:

| Herder hut | Animal | Special action checked |
|---|---|---|
| Chicken farmer | chickens | collects eggs |
| Cowhand | cows | milks cows with buckets |
| Shepherd | sheep | shears sheep, dyes per setting |
| Swine herder | pigs | — |
| Rabbit hutch | rabbits | — |

**Setup**
- The herder's hut with a worker, its breeding food and the tools it needs (axe, shears, buckets).
- First run: two animals in the pen (below the limit). Second run: more animals than the limit.

**What happens**
1. The herder works.

**Passes when**
- Below the limit, the herder breeds and the count rises up to the limit and not beyond.
- Above the limit, the herder butchers until the count is back at the limit, and the drops end up in the hut.
- The special action happens (eggs, milk buckets, wool).
- No animal outside the pen is touched.

**Time budget:** 10 minutes per herder.

**As implemented** (`FarmingGatheringTests`): the limit is soft by design: `AbstractEntityAIHerder.chanceToButcher` is 0 with 3 or fewer adults and grows with (adults / (2 x hut level))^4; breeding does not check the limit. So "below the limit" checks that a baby is born and the special action happens (egg picked up from the ground, milk with the cowhand's default large bottles, wool), "above the limit" puts 6 adults in a level 1 pen and waits until at most 3 adults are left, checks the drops and that an animal outside the hut area survives. The test closes the hut area's border with fence gates and slows the animals, because several pens are open and wandering animals leave the area the herder searches. Dyeing (1% per shear at level 1) is not checked. While a breeding test waits, two animals more than 6 blocks apart (and not in love) are put back together: vanilla's mate search reaches only 8 blocks, the herder feeds pairs up to 10 blocks apart (bug E2E-FRM-06-1), and slowed animals that drifted apart would stay apart. `frm06_herderFeedsPairOutOfMateRange` (optional) shows the bug: two chickens that can't move, 9 blocks apart; the herder feeds both.

---

## FRM-07 Fisherman

**Tests:** the fisherman finds water and fishes.

**Setup**
- A fisher's hut by a pond (water arena) with a fisherman and a fishing rod.

**What happens**
1. The fisherman walks to the water and fishes.

**Passes when**
- The fisherman finds a valid fishing spot on its own.
- Fish (or other fishing loot) end up in the hut.
- The rod wears down, and a broken rod leads to a request for a new one.

**Time budget:** 10 minutes.

**As implemented** (`FarmingGatheringTests`): a 9x9 pond 3 deep next to a level 1 fisher's hut, a rod 4 uses from breaking. Fails: bug E2E-FRM-07-1 (optional test).

