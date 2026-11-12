[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Requests and logistics

The request system, the warehouse and couriers: how items get from where they are to where they're needed.

---

## LOG-01 Warehouse delivers a requested item

**Tests:** a worker's request is fulfilled from the warehouse by a courier.

**Setup**
- Warehouse with a courier, 64 oak planks in its racks.
- A worker hut whose worker requests 16 oak planks (the test opens the request).

**What happens**
1. The request system assigns the request. The courier carries the planks over.

**Passes when**
- The request is resolved by the warehouse resolver, not by the player resolver.
- The worker hut holds 16 more planks, the warehouse 16 fewer.
- The request is marked completed and removed.

**Time budget:** 3 minutes.

---

## LOG-02 Couriers bring products to the warehouse

**Tests:** finished goods are picked up from huts and stored in the warehouse.

**Setup**
- Warehouse with a courier. A lumberjack's hut whose inventory is full of logs (above what it keeps for itself).

**What happens**
1. The colony runs.

**Passes when**
- The courier picks up the surplus logs.
- The logs end up in the warehouse racks.
- Items the hut keeps on purpose (its tools and minimum stock) stay in the hut.

**Time budget:** 3 minutes.

---

## LOG-03 Crafting chain

**Tests:** a request for something nobody has is crafted, including its ingredients.

**Setup**
- Warehouse with a courier and logs only.
- A sawmill with a worker who knows the recipes for planks and sticks.
- A worker requests sticks.

**What happens**
1. The request system finds no sticks and no planks, and plans crafting: logs → planks → sticks.
2. The sawmill crafts both steps. The courier delivers.

**Passes when**
- The stick request is resolved by the sawmill's crafting resolver, with a child request for planks.
- The requesting hut gets the sticks.
- The logs used match the recipe amounts (no extra loss).

**Time budget:** 5 minutes.

---

## LOG-04 Requests the colony can't fulfill go to the player

**Tests:** if nothing in the colony can provide an item, the player is asked, and a postbox delivery completes it.

**Setup**
- Warehouse with a courier, but no diamonds anywhere and no crafter that can make them.
- A worker requests a diamond. A postbox in the colony.

**What happens**
1. The request ends up with the player resolver.
2. The test puts a diamond into the postbox (as a player would).

**Passes when**
- The request is assigned to the player resolver.
- After the diamond is in the postbox, a courier brings it to the worker and the request completes.

**Time budget:** 3 minutes.

---

## LOG-05 Minimum stock

**Tests:** huts keep the minimum stock the player set.

**Setup**
- Warehouse with a courier and 128 torches. A worker hut with a minimum stock of 32 torches and none in the hut.

**What happens**
1. The colony runs.
2. The test removes 20 torches from the hut.

**Passes when**
- The hut is filled up to 32 torches.
- After the removal, it's filled up to 32 again.
- It never requests more than the minimum.

**Time budget:** 5 minutes.

---

## LOG-06 Tools by hut level

**Tests:** workers request a tool they need, and only get tool tiers their hut level allows.

**Setup**
- A miner's hut at level 1 with a miner without a pickaxe.
- Warehouse with a wooden, a stone and a diamond pickaxe.

**What happens**
1. The miner requests a pickaxe. The courier delivers.
2. The test raises the hut to a level that allows diamond tools and breaks the miner's pickaxe.

**Passes when**
- At level 1, the miner gets a pickaxe within the level 1 tier limit, never the diamond one.
- At the higher level, the new request may be fulfilled by the diamond pickaxe.

**Time budget:** 5 minutes.

---

## LOG-07 Several couriers share the work

**Tests:** with more deliveries than one courier can handle, couriers split them up.

**Setup**
- Warehouse at a level that allows three couriers, three couriers hired.
- Ten requests from five huts, all available in the warehouse.

**What happens**
1. The colony runs.

**Passes when**
- All ten requests complete.
- Each courier delivered at least one.
- No item was delivered twice.

**Time budget:** 5 minutes.

---

## LOG-08 Requests are cancelled when a hut is removed

**Tests:** removing a hut doesn't leave orphaned requests or items in transit.

**Setup**
- A worker hut with three open requests, one of them being delivered by a courier.

**What happens**
1. The test removes the hut.

**Passes when**
- All three requests are cancelled.
- The courier's carried items go back to the warehouse.
- No request references the removed hut afterwards.

**Time budget:** 2 minutes.

---

## LOG-09 Full warehouse

**Tests:** when the warehouse is full, couriers stop dumping into it and the colony reports it.

**Setup**
- Warehouse with every rack slot full. A hut with surplus items to collect.

**What happens**
1. The colony runs.

**Passes when**
- No items are lost: the surplus stays in the hut or with the courier.
- The warehouse reports it's full (the interaction a player would see).
- After the test adds a rack, collection resumes.

**Time budget:** 3 minutes.

---

## LOG-10 Stash

**Tests:** items put into a stash are collected into the warehouse.

**Setup**
- A stash with 32 cobblestone. Warehouse with a courier.

**What happens**
1. The colony runs.

**Passes when**
- The stash is emptied.
- The warehouse holds the 32 cobblestone.

**Time budget:** 3 minutes.
