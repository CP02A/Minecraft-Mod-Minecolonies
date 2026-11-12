[← Back to the test overview](../E2E-tests.md) · [How the tests work](framework.md)

# Jobs: crafting and processing

Workers who turn materials into products, at a crafting table, a furnace, a brewing stand or with special machinery.

---

## CRF-01 Every crafter crafts its recipes

**Tests:** each crafting hut makes a typical recipe from start to finish when there is a request for it.

One test per hut, all following the same steps:

| Hut | Example recipe checked |
|---|---|
| Sawmill | oak log → oak planks |
| Stonemason | stone → stone bricks |
| Blacksmith | iron ingots + sticks → iron pickaxe |
| Fletcher | flint + stick + feather → arrows |
| Baker | wheat → bread |
| Kitchen (chef) | MineColonies meal from its ingredients (*as implemented:* stew trencher from manchet bread, tomato, cabbage, 2 onions; the chef's meals need a level 4 kitchen, and the default pack has no kitchen, so the hut comes from the "Dark Oak Treehouse" pack) |
| Glassblower | glass → glass panes |
| Dyer | white wool + dye → colored wool |
| Mechanic | redstone recipe (for example a piston) |
| Crusher | cobblestone → gravel |
| Sifter | gravel with a mesh → sifting loot (*covered by CRF-05*) |
| Smeltery | raw iron → iron ingot (*not a request crafter:* the smeltery refuses taught recipes and requests ores itself; covered by CRF-03) |
| Stone smeltery | cobblestone → stone |
| Concrete mixer | concrete powder → concrete |
| Alchemist | water bottle + nether wart → awkward potion |

**Setup**
- The hut with a hired worker, the recipe learned or built in, and the ingredients for two crafts in the warehouse.
- Warehouse with a courier. Another hut requests the product.

**What happens**
1. The request reaches the crafter. The courier brings ingredients. The crafter crafts. The courier delivers.

**Passes when**
- The request is resolved by this hut.
- The product arrives at the requesting hut in the requested amount.
- Exactly the recipe's ingredients were used up.
- The crafter gained XP.

*As implemented:* the requesting hut is a lumberjack's hut without a worker; recipes are taught through the server side of
`AddRemoveRecipeMessage` (same storage as the teaching windows build). "Ingredients used" counts all buildings and citizens'
inventories. Notes: the crusher also crushes on its own up to its daily limit (setting), so the test sets the limit to 0; the concrete
mixer needs a pickaxe/shovel (put into the warehouse); furnace crafters (stone smeltery, baker) and the alchemist (blaze powder) get fuel
in the warehouse. The baker's bread is a chain of built-in recipes (3 wheat → dough, dough baked → bread).

**Time budget:** 5 minutes per hut.

---

## CRF-02 Learning recipes and their limits

**Tests:** a hut accepts only recipes that fit it, and only as many custom recipes as its level allows.

**Setup**
- A sawmill (level 1) and a blacksmith.

**What happens**
1. The test teaches recipes through the same code as the teaching GUI: a wooden recipe to the sawmill, an iron tool to the sawmill,
   then more recipes than the level 1 limit.

**Passes when**
- The wooden recipe is accepted and the iron tool recipe is refused (it belongs to the blacksmith).
- Recipes beyond the level limit are refused (*as implemented:* the limit is 2^level x 5 = 10 at level 1, counting only taught recipes).
- After upgrading the sawmill, more recipes can be taught.

**Time budget:** 30 seconds.

---

## CRF-03 Furnaces and fuel

**Tests:** furnace workers keep their furnaces fueled and filled.

**Setup**
- A smeltery with a smelter, two furnaces in the hut, 32 raw iron, and fuel in the warehouse (not in the hut). A courier.
- *As implemented:* the level 1 smeltery has one furnace; the test places a second one in the hut. The smelter loads a whole stack of ore
  and of fuel into one furnace, so with 32 raw iron the second furnace is never needed: the test uses 80 raw iron and 128 coal (both in
  the warehouse). Passes when all 80 are ingots (anywhere in the colony).

**What happens**
1. The smelter requests fuel, fills the furnaces and collects the results.

**Passes when**
- The smelter requests fuel only from the allowed fuel list.
- Both furnaces are used.
- All 32 raw iron become ingots in the hut.

**Time budget:** 10 minutes.

---

## CRF-04 Concrete mixer and water

**Tests:** the concrete mixer hardens powder with water from its hut.

**Setup**
- A concrete mixer's hut with a worker and 16 white concrete powder.

**What happens**
1. The worker places the powder in the hut's water and mines the hardened concrete.

**Passes when**
- 16 white concrete end up in the hut.
- The water in the hut is still in place afterwards.

*As implemented:* the mixer only works on requests, so a lumberjack's hut requests 16 white concrete (delivered there); the powder is in
the mixer's hut, a pickaxe and shovel in the warehouse. Water is compared as source blocks (just after the paste the water is still
flowing).

**Time budget:** 5 minutes.

---

## CRF-05 Sifter meshes

**Tests:** the sifter needs a mesh, uses it up and only drops the loot its mesh allows.

**Setup**
- A sifter's hut with a worker, gravel and a string mesh.

**What happens**
1. The sifter sifts many times.

**Passes when**
- All drops are on the string mesh's loot list.
- The mesh wears out and the sifter requests a new one. (*As implemented:* the sifter doesn't request a mesh, it shows a "no mesh"
  interaction; the test checks that the mesh, given with 3 uses left, wears out and that no gravel is sifted for 1200 ticks afterwards.)
- Without any mesh, nothing is sifted.

**Time budget:** 10 minutes.

---

## CRF-06 Alchemist

**Tests:** the alchemist brews potions in its brewing stands and harvests its ingredients.

**Setup**
- An alchemist's tower with a worker, brewing stands, blaze powder, water bottles and nether wart. Grown nether wart and mistletoe
  in the hut's growing spots.

**What happens**
1. The alchemist brews and harvests.

**Passes when**
- The requested potion is brewed and ends up in the hut.
- The brewing stands are fueled with blaze powder.
- The grown nether wart and mistletoe are harvested and replanted.

*As implemented:* brewing (request, blaze powder fuel) is covered by CRF-01 "Alchemist". This test sets the nether wart on the hut's soul
sand to fully grown and passes when a grown wart was harvested, a wart was replanted (age 0) and mistletoe was gathered. Mistletoe isn't
replanted: the alchemist hits the hut's leaves with shears (from the warehouse) until one drops (1 in 40 per hit). The alchemist picks
these jobs at random (about 2 in 30 decisions each, a decision every ~400 ticks), so the budget is large (43,000 ticks per attempt, up to 3 attempts; 21,000-57,000 ticks seen).

**Time budget:** 10 minutes.

---

## CRF-07 Enchanter

**Tests:** the enchanter draws experience from other citizens and enchants items.

**Setup**
- An enchanter's tower with a worker and enchanting stations, citizens with XP assigned as sources, and books in the hut.

**What happens**
1. The enchanter visits the assigned citizens, then enchants.

**Passes when**
- The source citizens' XP goes down and the enchanter's mana or progress goes up.
- Enchanted books end up in the hut.
- Citizens not assigned as sources keep their XP.

*As implemented:* the "drain" takes no XP from anyone: it uses up a book, gives the enchanter 1 mana and may enchant a random item in the
drained citizen's inventory. Sources are huts (`EnchanterStationsModule`), not citizens. The test: a lumberjack's hut with its worker as
station, 16 books and 2 ancient tomes in the tower, enchanter mana 9 (the level 1 threshold is 10). Passes when a book was used, mana went
up, an enchanted book (from an ancient tome) is in the hut, and the station worker lost no XP. A second test checks that the drained
citizen is the station's worker (it failed on bug E2E-CRF07-1, the enchanter drained himself; fixed).

**Time budget:** 10 minutes.

---

## CRF-08 Cook keeps the restaurant stocked

**Tests:** the cook cooks the dishes on the restaurant menu.

**Setup**
- A restaurant with a cook, a menu of two foods, raw ingredients in the warehouse and a courier.

**What happens**
1. The cook requests ingredients, cooks and stocks the restaurant.

**Passes when**
- The restaurant holds the menu foods.
- Foods not on the menu are not made.
- Citizens who come to eat get only menu foods (see [CIT-02](citizens.md#cit-02-eating-at-the-restaurant)). (*Not checked here.*)

*As implemented:* menu cooked beef + baked potato; raw beef, potatoes, raw chicken and coal in the warehouse; passes when the restaurant
holds both menu foods and no cooked chicken exists.

**Time budget:** 10 minutes.
