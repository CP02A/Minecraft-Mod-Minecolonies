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
