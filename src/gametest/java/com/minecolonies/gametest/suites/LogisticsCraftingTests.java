package com.minecolonies.gametest.suites;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.colony.requestsystem.requestable.crafting.AbstractCrafting;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.crafting.ModCraftingTypes;
import com.minecolonies.api.crafting.RecipeStorage;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.tileentities.AbstractTileEntityRack;
import com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.CourierAssignmentModule;
import com.minecolonies.core.colony.buildings.modules.FurnaceUserModule;
import com.minecolonies.core.colony.buildings.modules.MinimumStockModule;
import com.minecolonies.core.colony.buildings.modules.RestaurantMenuModule;
import com.minecolonies.core.colony.requestsystem.management.IStandardRequestManager;
import com.minecolonies.gametest.framework.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.minecolonies.gametest.framework.ColonyFixture.GROUND_Y;

/**
 * End-to-end tests from docs/e2e-tests/logistics.md and docs/e2e-tests/jobs-crafting.md.
 * <p>
 * Test ids: {@code minecolonies-gametest:logistics_crafting_tests_<method_name>}. Run only these with
 * {@code ./gradlew runGameTest -Pe2eFilter=logistics_crafting_tests}. See docs/e2e-tests/AGENT-GUIDE.md.
 * <p>
 * Arena layout used by most tests (relative x/z, town hall at the center occupies x 17..40, z 22..39):
 * <ul>
 *     <li>warehouse: south-west corner (x 1..16, z 40..62);</li>
 *     <li>courier's hut: south-east corner;</li>
 *     <li>the hut under test / the requesting hut: the north strip (z 1..20) and the east strip (x 42..62, z 20..46).</li>
 * </ul>
 * Huts are placed by their footprint ({@link #placeInBox}): the blueprint's size and anchor offset decide the anchor position, so a hut
 * never crosses the arena walls or another hut.
 */
public class LogisticsCraftingTests
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogisticsCraftingTests.class);

    /** Minimum corner (x, z) of the warehouse's footprint. */
    private static final int[] WAREHOUSE_BOX   = {1, 40};
    /** Minimum corner (x, z) of the courier hut's footprint. */
    private static final int[] COURIER_BOX     = {50, 52};
    /** North-west box (x 1..20, z 1..20). */
    private static final int[] NORTH_WEST_BOX  = {1, 1};
    /** North-east box (x 42..62, z 1..20). */
    private static final int[] NORTH_EAST_BOX  = {42, 1};
    /** East box (x 44..62, z 22..46). */
    private static final int[] EAST_BOX        = {44, 22};

    // ====================================================================================================================================
    // LOG: requests and logistics
    // ====================================================================================================================================

    /**
     * LOG-01 Warehouse delivers a requested item: a hut's request for 16 oak planks is resolved by the warehouse and delivered by a courier.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void log01_warehouseDeliversRequest(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];
        final RequestWatch watch = new RequestWatch(fixture);

        fixture.flow()
          .run("colony with warehouse, courier and a lumberjack's hut", () -> {
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              fixture.fillWarehouse(new ItemStack(Items.OAK_PLANKS, 64));
          })
          .awaitUntil("courier entity exists", 200, lg::courierReady, () -> ColonyFixture.describe(lg.couriers.get(0)))
          .run("the hut requests 16 oak planks", () -> watch.token = hut[0].createRequest(new Stack(new ItemStack(Items.OAK_PLANKS, 16), 16, 16), false))
          .awaitUntil("request completed and removed from the request system", 3600, watch::isGone,
            () -> watch.describe() + "; hut planks " + count(fixture, hut[0], Items.OAK_PLANKS) + "; " + ColonyFixture.describe(lg.couriers.get(0)))
          .check("request was resolved by a warehouse resolver, never by the player",
            () -> !watch.resolversSeen.isEmpty() && watch.resolversSeen.stream().allMatch(r -> r.startsWith("Warehouse")),
            () -> "resolvers seen " + watch.resolversSeen)
          .check("hut received 16 planks, warehouse has 16 fewer",
            () -> count(fixture, hut[0], Items.OAK_PLANKS) == 16 && count(fixture, lg.warehouse, Items.OAK_PLANKS) == 48,
            () -> "hut " + count(fixture, hut[0], Items.OAK_PLANKS) + ", warehouse " + count(fixture, lg.warehouse, Items.OAK_PLANKS))
          .succeed();
    }

    /**
     * LOG-02 Couriers bring products to the warehouse: surplus logs in a lumberjack's hut are picked up and stored in the warehouse; the hut
     * keeps its axe and saplings. The pickup is started the way the hut's "force pickup" button does (a worker without trees never dumps
     * on its own).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void log02_couriersCollectProducts(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];

        fixture.flow()
          .run("colony with warehouse, courier and a lumberjack's hut with 128 logs, an axe and saplings", () -> {
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              fixture.fillBuilding(hut[0], new ItemStack(Items.OAK_LOG, 64), new ItemStack(Items.OAK_LOG, 64), new ItemStack(Items.STONE_AXE),
                new ItemStack(Items.OAK_SAPLING, 16));
          })
          .awaitUntil("courier entity exists", 200, lg::courierReady, () -> ColonyFixture.describe(lg.couriers.get(0)))
          .run("request a pickup (force pickup button)", () -> hut[0].createPickupRequest(64, true))
          .awaitUntil("all 128 logs are in the warehouse", 5500, () -> count(fixture, lg.warehouse, Items.OAK_LOG) == 128,
            () -> "warehouse logs " + count(fixture, lg.warehouse, Items.OAK_LOG) + ", hut logs " + count(fixture, hut[0], Items.OAK_LOG) + "; "
                    + ColonyFixture.describe(lg.couriers.get(0)))
          .check("hut has no logs left but kept its axe and saplings",
            () -> count(fixture, hut[0], Items.OAK_LOG) == 0 && count(fixture, hut[0], Items.STONE_AXE) == 1 && count(fixture, hut[0], Items.OAK_SAPLING) == 16,
            () -> "logs " + count(fixture, hut[0], Items.OAK_LOG) + ", axe " + count(fixture, hut[0], Items.STONE_AXE) + ", saplings "
                    + count(fixture, hut[0], Items.OAK_SAPLING) + ", warehouse axe " + count(fixture, lg.warehouse, Items.STONE_AXE))
          .succeed();
    }

    /**
     * LOG-04 Requests the colony can't fulfill go to the player: a diamond request ends with the player resolver; when the player puts a
     * diamond into a warehouse rack, the request is reassigned to the warehouse and a courier delivers it.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 60000, skyAccess = true)
    public void log04_unfulfillableGoesToPlayer(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];
        final RequestWatch watch = new RequestWatch(fixture);

        fixture.flow()
          .run("colony with warehouse (no diamonds), courier and a lumberjack's hut", () -> {
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              fixture.fillWarehouse(new ItemStack(Items.OAK_PLANKS, 64));
          })
          .awaitUntil("courier entity exists", 200, lg::courierReady, () -> ColonyFixture.describe(lg.couriers.get(0)))
          .run("the hut requests a diamond", () -> watch.token = hut[0].createRequest(new Stack(new ItemStack(Items.DIAMOND, 1), 1, 1), false))
          .awaitUntil("request is first parked with the retrying resolver", 400, () -> watch.currentResolver().contains("Retrying"), watch::describe)
          // The retrying resolver retries 3 times, every 1200 request-system ticks (one every 11 game ticks), before giving up.
          .awaitUntil("request ends with the player resolver", 50000, () -> watch.currentResolver().contains("Player"), watch::describe)
          .idle(100)
          .check("still with the player", () -> watch.currentResolver().contains("Player"), watch::describe)
          .run("the player puts a diamond into a warehouse rack", () -> insertIntoRack(fixture, lg.warehouse, new ItemStack(Items.DIAMOND)))
          .awaitUntil("request completed", 4000, watch::isGone,
            () -> watch.describe() + "; hut diamonds " + count(fixture, hut[0], Items.DIAMOND) + "; " + ColonyFixture.describe(lg.couriers.get(0)))
          .check("warehouse resolver took over and the diamond is in the hut",
            () -> watch.resolversSeen.stream().anyMatch(r -> r.startsWith("Warehouse")) && count(fixture, hut[0], Items.DIAMOND) == 1,
            () -> "resolvers " + watch.resolversSeen + ", hut diamonds " + count(fixture, hut[0], Items.DIAMOND))
          .succeed();
    }

    /**
     * LOG-05 Minimum stock: a hut with a minimum stock of one stack (64) of torches is filled up from the warehouse, and filled up again after
     * 20 are taken out. The minimum stock unit is a stack, not an item count.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void log05_minimumStock(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];
        final int[] maxSeen = new int[1];

        fixture.flow()
          .run("colony with warehouse (192 torches), courier and a lumberjack's hut", () -> {
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              fixture.fillWarehouse(new ItemStack(Items.TORCH, 64), new ItemStack(Items.TORCH, 64), new ItemStack(Items.TORCH, 64));
          })
          .awaitUntil("courier entity exists", 200, lg::courierReady, () -> ColonyFixture.describe(lg.couriers.get(0)))
          .run("set minimum stock: 1 stack of torches", () -> hut[0].getFirstModuleOccurance(MinimumStockModule.class).addMinimumStock(new ItemStack(Items.TORCH), 1))
          .awaitUntil("hut holds 64 torches", 3500, () -> {
              maxSeen[0] = Math.max(maxSeen[0], count(fixture, hut[0], Items.TORCH));
              return count(fixture, hut[0], Items.TORCH) == 64;
          }, () -> "hut torches " + count(fixture, hut[0], Items.TORCH) + ", open requests " + hut[0].getOpenRequestsByRequestableType() + "; "
                     + ColonyFixture.describe(lg.couriers.get(0)))
          .run("take 20 torches out of the hut", () -> removeItems(fixture, hut[0], Items.TORCH, 20))
          .check("hut holds 44 torches", () -> count(fixture, hut[0], Items.TORCH) == 44, () -> "hut torches " + count(fixture, hut[0], Items.TORCH))
          .awaitUntil("hut is refilled to 64 torches", 3500, () -> {
              maxSeen[0] = Math.max(maxSeen[0], count(fixture, hut[0], Items.TORCH));
              return count(fixture, hut[0], Items.TORCH) == 64;
          }, () -> "hut torches " + count(fixture, hut[0], Items.TORCH) + "; " + ColonyFixture.describe(lg.couriers.get(0)))
          .idle(300)
          .check("never more than the minimum, warehouse lost exactly what the hut got",
            () -> Math.max(maxSeen[0], count(fixture, hut[0], Items.TORCH)) == 64 && count(fixture, lg.warehouse, Items.TORCH) == 192 - 64 - 20,
            () -> "max seen in hut " + maxSeen[0] + ", hut now " + count(fixture, hut[0], Items.TORCH) + ", warehouse " + count(fixture, lg.warehouse, Items.TORCH))
          .succeed();
    }

    /**
     * LOG-10 Stash: 32 cobblestone put into a stash are collected into the warehouse.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void log10_stash(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] stash = new IBuilding[1];

        fixture.flow()
          .run("colony with warehouse, courier and a stash", () -> {
              lg.setUp(1);
              stash[0] = placeBlockBuilding(fixture, ModBlocks.blockStash, new BlockPos(50, GROUND_Y, 10));
          })
          .awaitUntil("courier entity exists", 200, lg::courierReady, () -> ColonyFixture.describe(lg.couriers.get(0)))
          .run("put 32 cobblestone into the stash", () -> fixture.fillBuilding(stash[0], new ItemStack(Items.COBBLESTONE, 32)))
          .awaitUntil("stash is empty and the warehouse holds 32 cobblestone", 3600,
            () -> count(fixture, stash[0], Items.COBBLESTONE) == 0 && count(fixture, lg.warehouse, Items.COBBLESTONE) == 32,
            () -> "stash " + count(fixture, stash[0], Items.COBBLESTONE) + ", warehouse " + count(fixture, lg.warehouse, Items.COBBLESTONE)
                    + ", stash requests " + stash[0].getOpenRequestsByRequestableType() + "; " + ColonyFixture.describe(lg.couriers.get(0)))
          .succeed();
    }

    /**
     * LOG-03 Crafting chain: a request for sticks, with only logs in the colony, is crafted by the sawmill in two steps (logs to planks,
     * planks to sticks).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void log03_craftingChain(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] worker = new ICitizenData[1];
        final RequestWatch watch = new RequestWatch(fixture);
        final RequestLog log = new RequestLog(fixture);

        fixture.flow()
          .run("colony with warehouse (16 oak logs), courier, sawmill knowing planks and sticks, lumberjack's hut", () -> {
              lg.setUp(1);
              huts[0] = placeInBox(fixture, ModBuildings.sawmill.get(), 1, NORTH_WEST_BOX);
              huts[1] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              worker[0] = fixture.spawnCitizen(skills(50), huts[0], null, null);
              fixture.fillWarehouse(new ItemStack(Items.OAK_LOG, 16));
              requireTaught(teachGrid(huts[0], 2, new ItemStack(Items.OAK_PLANKS, 4), Items.OAK_LOG), "planks");
              requireTaught(teachGrid(huts[0], 2, new ItemStack(Items.STICK, 4), Items.OAK_PLANKS, null, Items.OAK_PLANKS, null), "sticks");
          })
          .awaitUntil("courier and sawmill worker entities exist", 200, () -> lg.courierReady() && worker[0].getEntity().isPresent(), () -> "no entity yet")
          .run("the lumberjack's hut requests 8 sticks", () -> watch.token = huts[1].createRequest(new Stack(new ItemStack(Items.STICK, 8), 8, 8), false))
          .awaitUntil("stick request completed", 7500, () -> {
              log.record();
              return watch.isGone();
          }, () -> watch.describe() + "; hut sticks " + count(fixture, huts[1], Items.STICK) + "; requests seen " + log.seen + "; "
                     + ColonyFixture.describe(worker[0]) + "; " + ColonyFixture.describe(lg.couriers.get(0)))
          .check("stick request resolved by the sawmill's crafting resolver",
            () -> watch.resolversSeen.stream().anyMatch(r -> r.contains("Crafting")) && watch.resolverPositions.contains(huts[0].getPosition()),
            () -> "resolvers " + watch.resolversSeen + " at " + watch.resolverPositions + ", sawmill at " + huts[0].getPosition())
          .check("a child request for planks was crafted too",
            () -> log.seen.stream().anyMatch(s -> s.contains("oak_planks") && s.contains("Crafting")),
            () -> "requests seen " + log.seen)
          .check("hut got 8 sticks, exactly one log was used",
            () -> count(fixture, huts[1], Items.STICK) == 8 && colonyTotal(fixture, Items.OAK_LOG) == 15,
            () -> "sticks " + count(fixture, huts[1], Items.STICK) + ", oak logs left in colony " + colonyTotal(fixture, Items.OAK_LOG)
                    + ", planks left " + colonyTotal(fixture, Items.OAK_PLANKS))
          .succeed();
    }

    /**
     * LOG-07 Several couriers share the work: ten requests from five postboxes, three couriers; all requests complete, each courier
     * delivers, nothing is delivered twice.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void log07_couriersShareWork(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final List<IBuilding> boxes = new ArrayList<>();
        final List<RequestWatch> watches = new ArrayList<>();
        final Map<Integer, Set<String>> deliveriesByCourier = new java.util.HashMap<>();
        final Item[] items = {Items.OAK_PLANKS, Items.COBBLESTONE, Items.TORCH, Items.STICK, Items.STONE, Items.DIRT, Items.SAND, Items.GLASS, Items.COAL,
          Items.IRON_INGOT};
        final BlockPos[] boxPositions = {new BlockPos(8, GROUND_Y, 8), new BlockPos(20, GROUND_Y, 8), new BlockPos(32, GROUND_Y, 8),
          new BlockPos(44, GROUND_Y, 8), new BlockPos(56, GROUND_Y, 8)};

        fixture.flow()
          .run("colony with a level 2 warehouse, 3 couriers and 5 postboxes", () -> {
              lg.setUp(3, 2);
              for (final Item item : items)
              {
                  fixture.fillWarehouse(new ItemStack(item, 32));
              }
              for (final BlockPos pos : boxPositions)
              {
                  boxes.add(placeBlockBuilding(fixture, ModBlocks.blockPostBox, pos));
              }
          })
          .awaitUntil("courier entities exist", 200, lg::courierReady, () -> "no entity yet")
          .run("each postbox requests 8 of two different items", () -> {
              for (int i = 0; i < items.length; i++)
              {
                  final RequestWatch watch = new RequestWatch(fixture);
                  watch.token = boxes.get(i / 2).createRequest(new Stack(new ItemStack(items[i], 8), 8, 8), false);
                  watches.add(watch);
              }
          })
          .awaitUntil("all ten requests completed", 7500, () -> {
              for (final ICitizenData courier : lg.couriers)
              {
                  if (courier.getJob() instanceof final com.minecolonies.core.colony.jobs.JobDeliveryman job && job.getCurrentTask() != null
                        && job.getCurrentTask().getRequest() instanceof final Delivery delivery)
                  {
                      deliveriesByCourier.computeIfAbsent(courier.getId(), k -> new LinkedHashSet<>())
                        .add(delivery.getTarget().getInDimensionLocation().toShortString() + " " + delivery.getStack().getItem());
                  }
              }
              return watches.stream().allMatch(RequestWatch::isGone);
          }, () -> watches.stream().filter(w -> !w.isGone()).count() + " open; deliveries by courier " + deliveriesByCourier)
          .check("every postbox got exactly its 8 + 8 items, the warehouse lost exactly 8 of each",
            () -> {
                for (int i = 0; i < items.length; i++)
                {
                    if (count(fixture, boxes.get(i / 2), items[i]) != 8 || count(fixture, lg.warehouse, items[i]) != 24)
                    {
                        return false;
                    }
                }
                return true;
            },
            () -> {
                final StringBuilder text = new StringBuilder();
                for (int i = 0; i < items.length; i++)
                {
                    text.append(items[i]).append(": box ").append(count(fixture, boxes.get(i / 2), items[i])).append(", warehouse ")
                      .append(count(fixture, lg.warehouse, items[i])).append("; ");
                }
                return text.toString();
            })
          .check("each of the three couriers made at least one delivery",
            () -> lg.couriers.stream().allMatch(c -> deliveriesByCourier.containsKey(c.getId())),
            () -> "deliveries by courier " + deliveriesByCourier)
          .succeed();
    }

    /**
     * LOG-08 Requests are cancelled when a hut is removed: a hut with three open requests (one being carried by a courier) is broken; the
     * requests go away and the carried items go back to the warehouse.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 5000, skyAccess = true)
    public void log08_requestsCancelledOnHutRemoval(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];
        final List<RequestWatch> watches = new ArrayList<>();
        final BlockPos[] hutPos = new BlockPos[1];

        fixture.flow()
          .run("colony with warehouse (64 planks, 64 cobblestone), courier, lumberjack's hut", () -> {
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              hutPos[0] = hut[0].getPosition();
              fixture.fillWarehouse(new ItemStack(Items.OAK_PLANKS, 64), new ItemStack(Items.COBBLESTONE, 64));
          })
          .awaitUntil("courier entity exists", 200, lg::courierReady, () -> "no entity yet")
          .run("the hut requests planks, cobblestone and a diamond", () -> {
              for (final ItemStack stack : List.of(new ItemStack(Items.OAK_PLANKS, 16), new ItemStack(Items.COBBLESTONE, 16), new ItemStack(Items.DIAMOND)))
              {
                  final RequestWatch watch = new RequestWatch(fixture);
                  watch.token = hut[0].createRequest(new Stack(stack, stack.getCount(), stack.getCount()), false);
                  watches.add(watch);
              }
          })
          .awaitUntil("the courier carries planks or cobblestone", 3000,
            () -> courierHolds(lg.couriers.get(0), Items.OAK_PLANKS) + courierHolds(lg.couriers.get(0), Items.COBBLESTONE) > 0,
            () -> ColonyFixture.describe(lg.couriers.get(0)))
          .run("break the hut (as a player)", () -> {
              hut[0].destroy();
              fixture.level().removeBlock(hutPos[0], false);
          })
          .check("all three requests are gone or cancelled",
            () -> watches.stream().allMatch(w -> w.request() == null || w.request().getState().name().equals("CANCELLED")),
            () -> watches.stream().map(RequestWatch::describe).toList().toString())
          .awaitUntil("courier's items are back in the warehouse (64 planks, 64 cobblestone) and his inventory is empty", 2500,
            () -> count(fixture, lg.warehouse, Items.OAK_PLANKS) == 64 && count(fixture, lg.warehouse, Items.COBBLESTONE) == 64
                    && courierHolds(lg.couriers.get(0), Items.OAK_PLANKS) + courierHolds(lg.couriers.get(0), Items.COBBLESTONE) == 0,
            () -> "warehouse planks " + count(fixture, lg.warehouse, Items.OAK_PLANKS) + ", cobblestone " + count(fixture, lg.warehouse, Items.COBBLESTONE)
                    + ", courier planks " + courierHolds(lg.couriers.get(0), Items.OAK_PLANKS) + ", cobblestone "
                    + courierHolds(lg.couriers.get(0), Items.COBBLESTONE) + "; " + ColonyFixture.describe(lg.couriers.get(0)))
          .check("no request refers to the removed hut",
            () -> referencesTo(fixture, hutPos[0]).isEmpty(),
            () -> "requests referring to " + hutPos[0] + ": " + referencesTo(fixture, hutPos[0]))
          .succeed();
    }

    /**
     * LOG-09 Full warehouse: with every warehouse slot full, a courier who picked up surplus keeps it (nothing is lost) and the warehouse
     * notes that it is full; after a rack is added, the items are stored.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void log09_fullWarehouse(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];
        final BlockPos[] rackPos = new BlockPos[1];

        fixture.flow()
          .run("colony with a full warehouse, courier, lumberjack's hut with 64 cobblestone", () -> {
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              fillCompletely(fixture, lg.warehouse, Items.DIRT);
              fixture.fillBuilding(hut[0], new ItemStack(Items.COBBLESTONE, 64));
          })
          .awaitUntil("courier entity exists", 200, lg::courierReady, () -> "no entity yet")
          .run("request a pickup (force pickup button)", () -> hut[0].createPickupRequest(64, true))
          // 8000: citizens take random 3-minute leisure breaks (CitizenData.update); once the courier went on one and 3000 ran out.
          .awaitUntil("courier picked up cobblestone and the warehouse noted it is full", 8000,
            () -> courierHolds(lg.couriers.get(0), Items.COBBLESTONE) > 0 && warehouseFullNotified(lg.warehouse),
            () -> "courier cobblestone " + courierHolds(lg.couriers.get(0), Items.COBBLESTONE) + ", full noted " + warehouseFullNotified(lg.warehouse)
                    + "; " + ColonyFixture.describe(lg.couriers.get(0)))
          .idle(200)
          .check("no cobblestone lost: hut + courier = 64, none in the warehouse",
            () -> count(fixture, hut[0], Items.COBBLESTONE) + courierHolds(lg.couriers.get(0), Items.COBBLESTONE) == 64
                    && count(fixture, lg.warehouse, Items.COBBLESTONE) == 0,
            () -> "hut " + count(fixture, hut[0], Items.COBBLESTONE) + ", courier " + courierHolds(lg.couriers.get(0), Items.COBBLESTONE) + ", warehouse "
                    + count(fixture, lg.warehouse, Items.COBBLESTONE))
          .run("add a rack to the warehouse", () -> rackPos[0] = addRack(fixture, lg.warehouse))
          .awaitUntil("the courier's cobblestone is stored in the warehouse", 2500,
            () -> count(fixture, lg.warehouse, Items.COBBLESTONE) > 0 && courierHolds(lg.couriers.get(0), Items.COBBLESTONE) == 0,
            () -> "warehouse " + count(fixture, lg.warehouse, Items.COBBLESTONE) + ", courier " + courierHolds(lg.couriers.get(0), Items.COBBLESTONE)
                    + ", rack at " + rackPos[0] + "; " + ColonyFixture.describe(lg.couriers.get(0)))
          .check("still no cobblestone lost",
            () -> count(fixture, hut[0], Items.COBBLESTONE) + courierHolds(lg.couriers.get(0), Items.COBBLESTONE) + count(fixture, lg.warehouse, Items.COBBLESTONE) == 64,
            () -> "hut " + count(fixture, hut[0], Items.COBBLESTONE) + ", courier " + courierHolds(lg.couriers.get(0), Items.COBBLESTONE) + ", warehouse "
                    + count(fixture, lg.warehouse, Items.COBBLESTONE))
          .succeed();
    }

    // ====================================================================================================================================
    // CRF: crafting and processing
    // ====================================================================================================================================

    /**
     * CRF-02 Learning recipes and their limits: the sawmill accepts a wooden recipe, refuses an iron tool (the blacksmith takes it), refuses
     * recipes beyond its level limit, and takes more after an upgrade.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 600, skyAccess = true)
    public void crf02_recipeLimits(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final Item[][] woods = {
          {Items.OAK_LOG, Items.OAK_PLANKS}, {Items.SPRUCE_LOG, Items.SPRUCE_PLANKS}, {Items.BIRCH_LOG, Items.BIRCH_PLANKS},
          {Items.JUNGLE_LOG, Items.JUNGLE_PLANKS}, {Items.ACACIA_LOG, Items.ACACIA_PLANKS}, {Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS},
          {Items.MANGROVE_LOG, Items.MANGROVE_PLANKS}, {Items.CHERRY_LOG, Items.CHERRY_PLANKS}, {Items.PALE_OAK_LOG, Items.PALE_OAK_PLANKS},
          {Items.CRIMSON_STEM, Items.CRIMSON_PLANKS}, {Items.WARPED_STEM, Items.WARPED_PLANKS}, {Items.STRIPPED_OAK_LOG, Items.OAK_PLANKS}};
        final int[] limit = new int[1];
        final boolean[] results = new boolean[woods.length];

        fixture.flow()
          .run("colony with a level 1 sawmill and a blacksmith", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = placeInBox(fixture, ModBuildings.sawmill.get(), 1, NORTH_WEST_BOX);
              huts[1] = placeInBox(fixture, ModBuildings.blacksmith.get(), 1, NORTH_EAST_BOX);
          })
          .check("a wooden recipe (oak planks) is accepted by the sawmill",
            () -> teachGrid(huts[0], 2, new ItemStack(Items.OAK_PLANKS, 4), Items.OAK_LOG), () -> "refused")
          .check("an iron pickaxe is refused by the sawmill",
            () -> !teachGrid(huts[0], 3, new ItemStack(Items.IRON_PICKAXE), Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, null, Items.STICK, null, null,
              Items.STICK, null), () -> "accepted")
          .check("the iron pickaxe is accepted by the blacksmith",
            () -> teachGrid(huts[1], 3, new ItemStack(Items.IRON_PICKAXE), Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, null, Items.STICK, null, null,
              Items.STICK, null), () -> "refused")
          .run("teach more plank recipes until the sawmill refuses", () -> {
              limit[0] = -1;
              for (int i = 1; i < woods.length; i++)
              {
                  results[i] = teachGrid(huts[0], 2, new ItemStack(woods[i][1], 4), woods[i][0]);
                  if (!results[i] && limit[0] < 0)
                  {
                      limit[0] = i;
                  }
              }
          })
          .check("level 1 limit is reached after 2^1 * 5 = 10 recipes; later ones are refused",
            () -> limit[0] == 10 && !results[10] && !results[11], () -> "first refused at recipe #" + (limit[0] + 1) + ", results " + java.util.Arrays.toString(results))
          .run("upgrade the sawmill to level 2", () -> {
              huts[0].setBuildingLevel(2);
              huts[0].onUpgradeComplete(null, 2);
          })
          .check("after the upgrade, more recipes are accepted",
            () -> huts[0].getBuildingLevel() == 2 && teachGrid(huts[0], 2, new ItemStack(woods[10][1], 4), woods[10][0]),
            () -> "level " + huts[0].getBuildingLevel())
          .succeed();
    }

    /** CRF-01 Sawmill: oak log to oak planks. */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void crf01_sawmill(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.sawmill.get(), 1,
          hut -> requireTaught(teachGrid(hut, 2, new ItemStack(Items.OAK_PLANKS, 4), Items.OAK_LOG), "planks"),
          List.of(new ItemStack(Items.OAK_LOG, 2)), new ItemStack(Items.OAK_PLANKS, 8), Map.of(Items.OAK_LOG, 2)));
    }

    /** CRF-01 Stonemason: stone to stone bricks. */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void crf01_stonemason(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.stoneMason.get(), 1,
          hut -> requireTaught(teachGrid(hut, 2, new ItemStack(Items.STONE_BRICKS, 4), Items.STONE, Items.STONE, Items.STONE, Items.STONE), "stone bricks"),
          List.of(new ItemStack(Items.STONE, 8)), new ItemStack(Items.STONE_BRICKS, 8), Map.of(Items.STONE, 8)));
    }

    /** CRF-01 Blacksmith: iron ingots and sticks to an iron pickaxe. */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void crf01_blacksmith(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.blacksmith.get(), 1,
          hut -> requireTaught(teachGrid(hut, 3, new ItemStack(Items.IRON_PICKAXE), Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, null, Items.STICK, null,
            null, Items.STICK, null), "iron pickaxe"),
          List.of(new ItemStack(Items.IRON_INGOT, 6), new ItemStack(Items.STICK, 4)), new ItemStack(Items.IRON_PICKAXE), Map.of(Items.IRON_INGOT, 3, Items.STICK, 2)));
    }

    /** CRF-01 Fletcher: flint, stick and feather to arrows. */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void crf01_fletcher(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.fletcher.get(), 1,
          hut -> requireTaught(teachGrid(hut, 3, new ItemStack(Items.ARROW, 4), null, Items.FLINT, null, null, Items.STICK, null, null, Items.FEATHER, null), "arrows"),
          List.of(new ItemStack(Items.FLINT, 2), new ItemStack(Items.STICK, 2), new ItemStack(Items.FEATHER, 2)), new ItemStack(Items.ARROW, 8),
          Map.of(Items.FLINT, 2, Items.STICK, 2, Items.FEATHER, 2)));
    }

    /** CRF-01 Glassblower: glass to glass panes. */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void crf01_glassblower(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.glassblower.get(), 1,
          hut -> requireTaught(teachGrid(hut, 3, new ItemStack(Items.GLASS_PANE, 16), Items.GLASS, Items.GLASS, Items.GLASS, Items.GLASS, Items.GLASS, Items.GLASS,
            null, null, null), "glass panes"),
          List.of(new ItemStack(Items.GLASS, 12)), new ItemStack(Items.GLASS_PANE, 32), Map.of(Items.GLASS, 12)));
    }

    /** CRF-01 Dyer: white wool and red dye to red wool. */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void crf01_dyer(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.dyer.get(), 1,
          hut -> requireTaught(teachGrid(hut, 2, new ItemStack(Items.WOOL.red()), Items.WOOL.white(), Items.DYE.red(), null, null), "red wool"),
          List.of(new ItemStack(Items.WOOL.white(), 2), new ItemStack(Items.DYE.red(), 2)), new ItemStack(Items.WOOL.red(), 2), Map.of(Items.WOOL.white(), 2, Items.DYE.red(), 2)));
    }

    /** CRF-01 Mechanic: a piston. */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void crf01_mechanic(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.mechanic.get(), 1,
          hut -> requireTaught(teachGrid(hut, 3, new ItemStack(Items.PISTON), Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.COBBLESTONE, Items.IRON_INGOT,
            Items.COBBLESTONE, Items.COBBLESTONE, Items.REDSTONE, Items.COBBLESTONE), "piston"),
          List.of(new ItemStack(Items.OAK_PLANKS, 6), new ItemStack(Items.COBBLESTONE, 8), new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.REDSTONE, 2)),
          new ItemStack(Items.PISTON), Map.of(Items.OAK_PLANKS, 3, Items.COBBLESTONE, 4, Items.IRON_INGOT, 1, Items.REDSTONE, 1)));
    }

    /** CRF-01 Crusher: cobblestone to gravel (built-in recipe, 2 cobblestone per gravel). */
    // BUG E2E-CRF01-1: the crusher over-crafts a request: after reaching the request's crafting count, EntityAIWorkCrusher.craft() only
    // resets its counters (resetValues) and keeps calling crush(), which falls back to the crusher-mode recipe and crafts again until the
    // worker dumps. Request for 2 gravel uses 8 cobblestone instead of 4; the extra gravel stays in the crusher's inventory.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true, required = false)
    public void crf01_crusher(final GameTestHelper helper)
    {
        // The crusher also crushes on its own up to its daily limit (setting); set to 0 so only the request is crafted.
        crafterTest(helper, new Craft(ModBuildings.crusher.get(), 1,
          hut -> hut.getSetting(com.minecolonies.core.colony.buildings.workerbuildings.BuildingCrusher.DAILY_LIMIT).setValue(0),
          List.of(new ItemStack(Items.COBBLESTONE, 8)), new ItemStack(Items.GRAVEL, 2),
          Map.of(Items.COBBLESTONE, 4)));
    }

    /** CRF-01 Stone smeltery: cobblestone to stone (furnace recipe), with coal as fuel. */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void crf01_stoneSmeltery(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.stoneSmelter.get(), 1,
          hut -> requireTaught(teachFurnace(hut, new ItemStack(Items.STONE), Items.COBBLESTONE), "stone"),
          List.of(new ItemStack(Items.COBBLESTONE, 8), new ItemStack(Items.COAL, 16)), new ItemStack(Items.STONE, 4), Map.of(Items.COBBLESTONE, 4)), 7500);
    }

    /** CRF-01 Baker: wheat to bread (built-in recipes: 3 wheat to bread dough, dough baked to bread), with coal as fuel. */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void crf01_baker(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.bakery.get(), 1, hut -> {}, List.of(new ItemStack(Items.WHEAT, 12), new ItemStack(Items.COAL, 16)),
          new ItemStack(Items.BREAD, 2), Map.of(Items.WHEAT, 6)), 7500);
    }

    /** CRF-01 Concrete mixer: white concrete powder to white concrete (built-in recipe). */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void crf01_concreteMixer(final GameTestHelper helper)
    {
        crafterTest(helper, new Craft(ModBuildings.concreteMixer.get(), 1, hut -> {}, List.of(new ItemStack(Items.CONCRETE_POWDER.white(), 8), new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.STONE_SHOVEL)),
          new ItemStack(Items.CONCRETE.white(), 4), Map.of(Items.CONCRETE_POWDER.white(), 4)));
    }

    /**
     * LOG-06 Tools by hut level: a miner in a level 1 hut requests a pickaxe and gets one within the level 1 limit (not the diamond one);
     * after the hut reaches level 3 and the low pickaxes are gone, the new request is fulfilled with the diamond pickaxe.
     * <p>
     * The miner only asks for a pickaxe when his shaft reaches stone, and he extends the shaft only with cobblestone (the column behind
     * the ladder) and ladders. So the test makes the arena floor and the ground below it stone (the first block of the shaft needs a pickaxe)
     * and puts cobblestone and ladders into the hut.
     */
    // Was optional as "E2E-LOG06-1 orphaned delivery": not a mod bug. The shovel request was delivered and completed; the "tool requests"
    // set below collects every state a request had, so it listed the finished shovel request twice (FOLLOWUP_IN_PROGRESS with its delivery,
    // then COMPLETED). The miner was really waiting (NEEDS_ITEM) for 1-32 cobblestone that nobody had, and below the arena there was only
    // the barrier floor, so he never reached stone and never needed a pickaxe.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void log06_toolsByHutLevel(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] miner = new ICitizenData[1];
        final Set<String> toolRequests = new LinkedHashSet<>();

        fixture.flow()
          .run("colony with warehouse (wooden, stone, diamond pickaxe), courier, level 1 miner's hut (cobblestone, ladders) with a miner on stone ground", () -> {
              fixture.fillFloor(Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
              fixture.makeDeepGround();
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.miner.get(), 1, NORTH_EAST_BOX);
              fixture.fillWarehouse(new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.WOODEN_SHOVEL), new ItemStack(Items.STONE_SHOVEL));
              fixture.fillBuilding(hut[0], new ItemStack(Items.COBBLESTONE, 16), new ItemStack(Items.LADDER, 16));
              miner[0] = fixture.spawnCitizen(skills(30), hut[0], null, null);
          })
          .awaitUntil("the miner holds a pickaxe", 3500, () -> {
              recordToolRequests(fixture, hut[0], toolRequests);
              return pickaxeOf(miner[0]) != null;
          }, () -> "tool requests (every state seen) " + toolRequests + "; open requests of the hut " + openRequestsOf(fixture, hut[0]) + "; pickaxes: wooden " + whereIs(fixture, Items.WOODEN_PICKAXE) + ", stone "
                     + whereIs(fixture, Items.STONE_PICKAXE) + ", diamond " + whereIs(fixture, Items.DIAMOND_PICKAXE) + ", shovels " + whereIs(fixture, Items.WOODEN_SHOVEL)
                     + whereIs(fixture, Items.STONE_SHOVEL) + "; courier queue " + ((com.minecolonies.core.colony.jobs.JobDeliveryman) lg.couriers.get(0).getJob()).getTaskQueue()
                     + ", warehouse queue " + lg.warehouse.getModule(BuildingModules.WAREHOUSE_REQUEST_QUEUE).getMutableRequestList() + "; " + ColonyFixture.describe(miner[0]) + "; "
                     + ColonyFixture.describe(lg.couriers.get(0)))
          .check("level 1: the pickaxe is within the level 1 limit (not diamond), the diamond one is still in the warehouse",
            () -> pickaxeOf(miner[0]).getItem() != Items.DIAMOND_PICKAXE && count(fixture, lg.warehouse, Items.DIAMOND_PICKAXE) == 1,
            () -> "miner has " + pickaxeOf(miner[0]) + ", tool requests " + toolRequests)
          .run("raise the hut to level 3, take away the miner's pickaxe and the other low pickaxes", () -> {
              hut[0].setBuildingLevel(3);
              hut[0].onUpgradeComplete(null, 3);
              takeAll(miner[0], st -> st.is(net.minecraft.tags.ItemTags.PICKAXES));
              for (final IBuilding building : List.of(lg.warehouse, hut[0]))
              {
                  removeItems(fixture, building, Items.WOODEN_PICKAXE, count(fixture, building, Items.WOODEN_PICKAXE));
                  removeItems(fixture, building, Items.STONE_PICKAXE, count(fixture, building, Items.STONE_PICKAXE));
              }
          })
          .awaitUntil("the miner gets the diamond pickaxe", 4000, () -> {
              recordToolRequests(fixture, hut[0], toolRequests);
              return pickaxeOf(miner[0]) != null && pickaxeOf(miner[0]).is(Items.DIAMOND_PICKAXE);
          }, () -> "miner has " + pickaxeOf(miner[0]) + ", tool requests (every state seen) " + toolRequests + "; open requests of the hut " + openRequestsOf(fixture, hut[0]) + "; " + ColonyFixture.describe(miner[0]))
          .succeed();
    }

    /**
     * CRF-03 Furnaces and fuel (also covers CRF-01 "Smeltery": the smeltery takes no requests for ingots, it requests ores itself): a
     * smelter with 80 raw iron and 128 coal in the warehouse requests fuel from its allowed list, uses both furnaces (the level 1 hut has
     * one; the test places a second one, as a player would) and turns all 80 raw iron into ingots. The smelter loads a whole stack into one
     * furnace, so more than one stack of ore and fuel is needed for the second furnace to be used.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 26000, skyAccess = true)
    public void crf03_furnacesAndFuel(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final Set<BlockPos> furnacesUsed = new LinkedHashSet<>();
        final Set<String> fuelRequests = new LinkedHashSet<>();
        final List<ItemStorage> allowedFuel = new ArrayList<>();

        fixture.flow()
          .run("colony with warehouse (80 raw iron, 128 coal), courier, smeltery with a smelter", () -> {
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.smeltery.get(), 1, NORTH_WEST_BOX);
              fixture.fillWarehouse(new ItemStack(Items.RAW_IRON, 64), new ItemStack(Items.RAW_IRON, 16), new ItemStack(Items.COAL, 64),
                new ItemStack(Items.COAL, 64));
              worker[0] = fixture.spawnCitizen(skills(50), hut[0], null, null);
              allowedFuel.addAll(hut[0].getModule(BuildingModules.ITEMLIST_FUEL).getList());
              if (furnaces(hut[0]).size() < 2)
              {
                  // the level 1 smeltery has one furnace: the player places a second one in the hut
                  addBlockInHut(fixture, hut[0], Blocks.FURNACE.defaultBlockState());
              }
          })
          .check("the smeltery has two furnaces and coal is an allowed fuel",
            () -> furnaces(hut[0]).size() >= 2 && allowedFuel.stream().anyMatch(st -> st.getItemStack().is(Items.COAL)),
            () -> "furnaces " + furnaces(hut[0]) + ", fuel list " + allowedFuel)
          .awaitUntil("all 80 raw iron became iron ingots", 25000, () -> {
              recordFurnaceUse(fixture, hut[0], furnacesUsed);
              recordFuelRequests(fixture, hut[0], fuelRequests);
              return colonyTotal(fixture, Items.IRON_INGOT) == 80 && colonyTotal(fixture, Items.RAW_IRON) == 0;
          }, () -> "ingots " + whereIs(fixture, Items.IRON_INGOT) + ", raw iron " + whereIs(fixture, Items.RAW_IRON) + ", furnaces used " + furnacesUsed
                     + ", fuel requests " + fuelRequests + "; " + ColonyFixture.describe(worker[0]))
          .check("fuel was requested only from the allowed list",
            () -> !fuelRequests.isEmpty() && fuelRequests.stream().allMatch(r -> allowedFuel.stream().anyMatch(a -> r.equals(a.getItemStack().getItem().toString()))),
            () -> "fuel requested " + fuelRequests + ", allowed " + allowedFuel)
          .check("both furnaces were used", () -> furnacesUsed.size() >= 2, () -> "furnaces used " + furnacesUsed)
          .succeed();
    }

    /**
     * CRF-04 Concrete mixer and water: 16 white concrete powder in the hut become 16 white concrete (requested by another hut); the hut's
     * water is still in place afterwards.
     */
    // Budget: 3,950-5,400 ticks in earlier runs; once not done after 7,000 (the mixer still harvesting), so 14,000.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 15000, skyAccess = true)
    public void crf04_concreteMixerWater(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] worker = new ICitizenData[1];
        final RequestWatch watch = new RequestWatch(fixture);
        final int[] water = new int[1];

        fixture.flow()
          .run("colony with warehouse, courier, concrete mixer with 16 white concrete powder, lumberjack's hut", () -> {
              lg.setUp(1);
              huts[0] = placeInBox(fixture, ModBuildings.concreteMixer.get(), 1, NORTH_WEST_BOX);
              huts[1] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              worker[0] = fixture.spawnCitizen(skills(50), huts[0], null, null);
              fixture.fillBuilding(huts[0], new ItemStack(Items.CONCRETE_POWDER.white(), 16));
              fixture.fillWarehouse(new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.STONE_SHOVEL));
              water[0] = countWater(fixture, huts[0]);
          })
          .check("the hut has water", () -> water[0] > 0, () -> "water blocks " + water[0])
          .awaitUntil("entities exist", 200, () -> lg.courierReady() && worker[0].getEntity().isPresent(), () -> "no entity yet")
          .run("the lumberjack's hut requests 16 white concrete",
            () -> watch.token = huts[1].createRequest(new Stack(new ItemStack(Items.CONCRETE.white(), 16), 16, 16), false))
          .awaitUntil("request completed", 14000, watch::isGone,
            () -> watch.describe() + "; concrete " + whereIs(fixture, Items.CONCRETE.white()) + ", powder " + whereIs(fixture, Items.CONCRETE_POWDER.white())
                    + "; " + ColonyFixture.describe(worker[0]))
          .check("16 white concrete arrived, the powder is used up",
            () -> count(fixture, huts[1], Items.CONCRETE.white()) == 16 && colonyTotal(fixture, Items.CONCRETE_POWDER.white()) == 0,
            () -> "concrete " + whereIs(fixture, Items.CONCRETE.white()) + ", powder " + whereIs(fixture, Items.CONCRETE_POWDER.white()))
          .check("the water (source blocks) is still in place", () -> countWater(fixture, huts[0]) == water[0],
            () -> "water blocks " + countWater(fixture, huts[0]) + ", before " + water[0])
          .succeed();
    }

    /**
     * CRF-05 Sifter meshes: a sifter with gravel and an almost worn-out string mesh sifts; every drop is on the string mesh's gravel loot
     * list; the mesh wears out; without a mesh nothing more is sifted.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void crf05_sifterMesh(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final int[] gravel = new int[1];
        final Item mesh = com.minecolonies.api.items.ModItems.sifterMeshString;
        final Set<Item> allowed = Set.of(Items.IRON_NUGGET, Items.FLINT, Items.COAL, Items.GRAVEL);

        fixture.flow()
          .run("colony with a sifter, 64 gravel and a string mesh with 3 uses left", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = placeInBox(fixture, ModBuildings.sifter.get(), 1, NORTH_WEST_BOX);
              final ItemStack meshStack = new ItemStack(mesh);
              meshStack.setDamageValue(meshStack.getMaxDamage() - 3);
              fixture.fillBuilding(hut[0], new ItemStack(Items.GRAVEL, 64), meshStack);
              worker[0] = fixture.spawnCitizen(skills(50), hut[0], null, null);
          })
          .awaitUntil("the mesh is worn out (no mesh left in hut or worker)", 6000, () -> colonyTotal(fixture, mesh) == 0,
            () -> "mesh " + whereIs(fixture, mesh) + ", gravel " + colonyTotal(fixture, Items.GRAVEL) + "; " + ColonyFixture.describe(worker[0]))
          .check("some gravel was sifted and every drop is on the string mesh's gravel loot list",
            () -> colonyTotal(fixture, Items.GRAVEL) < 64 && itemsInColony(fixture).stream().allMatch(allowed::contains),
            () -> "gravel " + colonyTotal(fixture, Items.GRAVEL) + ", items in colony " + itemsInColony(fixture))
          .run("remember the gravel count", () -> gravel[0] = colonyTotal(fixture, Items.GRAVEL))
          .idle(1200)
          .check("without a mesh nothing more is sifted", () -> colonyTotal(fixture, Items.GRAVEL) == gravel[0],
            () -> "gravel " + colonyTotal(fixture, Items.GRAVEL) + ", before " + gravel[0])
          .succeed();
    }

    /**
     * CRF-01 Alchemist: water bottles and nether wart to awkward potions in a brewing stand, with blaze powder as fuel. The recipe is
     * taught through the server side of the teaching message (the Fabric port's brewing teaching window can't compute the output, see the
     * protocol).
     */
    // Shears in the warehouse: between brews the alchemist may go harvesting mistletoe (random), which needs shears; without them he
    // waited in NEEDS_ITEM on a tool request only the retrying resolver handles, and the brewed potions were never delivered (once).
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 15000, skyAccess = true)
    public void crf01_alchemist(final GameTestHelper helper)
    {
        final ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        final ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        crafterTest(helper, new Craft(ModBuildings.alchemist.get(), 1,
          hut -> requireTaught(teachBrewing(hut, awkward.copyWithCount(3), water, Items.NETHER_WART), "awkward potion"),
          List.of(water.copyWithCount(1), water.copyWithCount(1), water.copyWithCount(1), water.copyWithCount(1), water.copyWithCount(1), water.copyWithCount(1),
            new ItemStack(Items.NETHER_WART, 2), new ItemStack(Items.BLAZE_POWDER, 4), new ItemStack(Items.SHEARS)),
          awkward.copyWithCount(3), Map.of(Items.NETHER_WART, 1)), 14000);
    }

    /**
     * CRF-08 Cook keeps the restaurant stocked: with cooked beef and baked potatoes on the menu and raw beef, potatoes and raw chicken in the
     * warehouse, the cook stocks the restaurant with the menu foods and never cooks chicken.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void crf08_cookStocksMenu(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] cook = new ICitizenData[1];

        fixture.flow()
          .run("colony with warehouse (raw beef, potatoes, raw chicken, coal), courier, restaurant with a cook and a two-food menu", () -> {
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.cook.get(), 1, NORTH_WEST_BOX);
              fixture.fillWarehouse(new ItemStack(Items.BEEF, 16), new ItemStack(Items.POTATO, 16), new ItemStack(Items.CHICKEN, 16), new ItemStack(Items.COAL, 32));
              cook[0] = fixture.spawnCitizen(skills(50), hut[0], null, null);
              final RestaurantMenuModule menu = hut[0].getFirstModuleOccurance(RestaurantMenuModule.class);
              menu.addMenuItem(new ItemStack(Items.COOKED_BEEF));
              menu.addMenuItem(new ItemStack(Items.BAKED_POTATO));
          })
          .check("the menu holds the two foods", () -> hut[0].getFirstModuleOccurance(RestaurantMenuModule.class).getMenu().size() == 2,
            () -> "menu " + hut[0].getFirstModuleOccurance(RestaurantMenuModule.class).getMenu())
          .awaitUntil("the restaurant holds cooked beef and baked potatoes", 11000,
            () -> count(fixture, hut[0], Items.COOKED_BEEF) > 0 && count(fixture, hut[0], Items.BAKED_POTATO) > 0,
            () -> "cooked beef " + whereIs(fixture, Items.COOKED_BEEF) + ", baked potato " + whereIs(fixture, Items.BAKED_POTATO) + ", raw beef "
                    + whereIs(fixture, Items.BEEF) + ", potato " + whereIs(fixture, Items.POTATO) + "; " + ColonyFixture.describe(cook[0]))
          .check("no food that is not on the menu was made", () -> colonyTotal(fixture, Items.COOKED_CHICKEN) == 0,
            () -> "cooked chicken " + whereIs(fixture, Items.COOKED_CHICKEN))
          .succeed();
    }

    /**
     * CRF-01 Kitchen (chef): a MineColonies meal (stew trencher: manchet bread, tomato, cabbage, 2 onions) from its built-in recipe. The chef's
     * meals need a level 4 kitchen; the default pack has no kitchen, so the hut comes from the "Dark Oak Treehouse" pack.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void crf01_kitchen(final GameTestHelper helper)
    {
        final Item tomato = modItem("tomato");
        final Item cabbage = modItem("cabbage");
        final Item onion = modItem("onion");
        crafterTest(helper, new Craft(ModBuildings.kitchen.get(), 4, hut -> {},
          List.of(new ItemStack(com.minecolonies.api.items.ModItems.manchet_bread, 2), new ItemStack(tomato, 2), new ItemStack(cabbage, 2), new ItemStack(onion, 4)),
          new ItemStack(com.minecolonies.api.items.ModItems.stew_trencher), Map.of(com.minecolonies.api.items.ModItems.manchet_bread, 1, tomato, 1, cabbage, 1, onion, 2),
          KITCHEN_PACK), 7500);
    }

    /**
     * CRF-06 Alchemist harvesting: grown nether wart on the hut's soul sand is harvested and replanted, and mistletoe is gathered from the
     * hut's leaves (with shears from the warehouse). Brewing in the brewing stands is covered by {@link #crf01_alchemist}.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 45000, skyAccess = true, maxAttempts = 5, requiredSuccesses = 1)
    public void crf06_alchemistHarvests(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final List<BlockPos> grown = new ArrayList<>();
        final boolean[] harvested = new boolean[1];

        fixture.flow()
          .run("colony with warehouse (shears), courier, alchemist's tower with grown nether wart and leaves", () -> {
              lg.setUp(1);
              hut[0] = placeInBox(fixture, ModBuildings.alchemist.get(), 1, NORTH_WEST_BOX);
              fixture.fillWarehouse(new ItemStack(Items.SHEARS), new ItemStack(Items.NETHER_WART, 16), new ItemStack(Items.BLAZE_POWDER, 8));
              final var alchemist = (com.minecolonies.core.colony.buildings.workerbuildings.BuildingAlchemist) hut[0];
              if (alchemist.getAllSoilPositions().isEmpty())
              {
                  addBlockInHut(fixture, hut[0], Blocks.SOUL_SAND.defaultBlockState());
              }
              if (alchemist.getAllLeavePositions().isEmpty())
              {
                  addBlockInHut(fixture, hut[0], Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
              }
              for (final BlockPos soil : alchemist.getAllSoilPositions())
              {
                  if (fixture.level().getBlockState(soil).is(Blocks.SOUL_SAND))
                  {
                      fixture.level().setBlockAndUpdate(soil.above(),
                        Blocks.NETHER_WART.defaultBlockState().setValue(net.minecraft.world.level.block.NetherWartBlock.AGE, 3));
                      grown.add(soil.above());
                  }
              }
              worker[0] = fixture.spawnCitizen(skills(50), hut[0], null, null);
          })
          .check("the hut has soul sand with grown nether wart and leaves", () -> !grown.isEmpty()
              && !((com.minecolonies.core.colony.buildings.workerbuildings.BuildingAlchemist) hut[0]).getAllLeavePositions().isEmpty(),
            () -> "grown wart " + grown + ", leaves " + ((com.minecolonies.core.colony.buildings.workerbuildings.BuildingAlchemist) hut[0]).getAllLeavePositions().size())
          .awaitUntil("a grown nether wart was harvested and replanted, and mistletoe was gathered", 43000, () -> {
              for (final BlockPos pos : grown)
              {
                  final BlockState state = fixture.level().getBlockState(pos);
                  if (!state.is(Blocks.NETHER_WART) || state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE) < 3)
                  {
                      harvested[0] = true;
                  }
              }
              return harvested[0] && replanted(fixture, grown) && colonyTotal(fixture, com.minecolonies.api.items.ModItems.mistletoe) > 0;
          }, () -> "harvested " + harvested[0] + ", replanted " + replanted(fixture, grown) + ", wart blocks " + grown.stream().map(p -> {
              final BlockState state = fixture.level().getBlockState(p);
              return state.is(Blocks.NETHER_WART) ? "age " + state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE) : state.getBlock().toString();
          }).toList() + ", mistletoe " + whereIs(fixture, com.minecolonies.api.items.ModItems.mistletoe) + ", shears " + whereIs(fixture, Items.SHEARS) + "; "
                     + ColonyFixture.describe(worker[0]))
          .succeed();
    }

    /**
     * CRF-07 Enchanter: with a station (a lumberjack's hut with its worker) assigned, the enchanter visits it, uses a book to gain mana, and
     * once he has enough mana turns an ancient tome into an enchanted book in the hut. (The drain gives the enchanter mana; it takes no XP
     * from anyone, see the design notes.) Which citizen is drained is checked in {@link #crf07_enchanterDrainsStationWorker}.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 16000, skyAccess = true)
    public void crf07_enchanter(final GameTestHelper helper)
    {
        enchanterTest(helper, false);
    }

    /**
     * CRF-07 Enchanter, drain target: the citizen the enchanter drains must be a worker of the assigned station, not the enchanter himself.
     */
    // BUG E2E-CRF07-1: EntityAIWorkEnchanter.gatherAndDrain() picks the citizen to drain from getModuleForJob().getAssignedEntities(),
    // i.e. the enchanter's own work module, so the enchanter "drains" himself (and the random enchant goes to his own inventory) instead of
    // a worker of the station building it walked to.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true, required = false)
    public void crf07_enchanterDrainsStationWorker(final GameTestHelper helper)
    {
        enchanterTest(helper, true);
    }

    private static void enchanterTest(final GameTestHelper helper, final boolean checkDrainTarget)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] citizens = new ICitizenData[2];
        final int[] mana = new int[1];
        final Set<String> drained = new LinkedHashSet<>();
        final double[] sourceXp = new double[1];

        fixture.flow()
          .run("colony with an enchanter's tower (16 books, 2 ancient tomes) and a lumberjack's hut with a lumberjack as station", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = placeInBox(fixture, ModBuildings.enchanter.get(), 1, NORTH_WEST_BOX);
              huts[1] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              fixture.fillBuilding(huts[0], new ItemStack(Items.BOOK, 16), new ItemStack(com.minecolonies.api.items.ModItems.ancientTome, 2));
              final Map<Skill, Integer> enchanterSkills = skills(30);
              enchanterSkills.put(Skill.Mana, 9);
              enchanterSkills.put(Skill.Knowledge, 50);
              citizens[0] = fixture.spawnCitizen(enchanterSkills, huts[0], null, null);
              citizens[1] = fixture.spawnCitizen(skills(30), huts[1], null, null);
              huts[0].getModule(BuildingModules.ENCHANTER_STATIONS).addWorker(huts[1].getPosition());
          })
          .awaitUntil("entities exist", 200, () -> citizens[0].getEntity().isPresent() && citizens[1].getEntity().isPresent(), () -> "no entity yet")
          .run("remember mana and the station worker's XP", () -> {
              mana[0] = citizens[0].getCitizenSkillHandler().getLevel(Skill.Mana);
              sourceXp[0] = citizens[1].getCitizenSkillHandler().getTotalXP();
          })
          .awaitUntil("the enchanter drained once (a book used)", 6000, () -> {
              recordDrainTarget(citizens[0], drained);
              return colonyTotal(fixture, Items.BOOK) < 16;
          }, () -> "books " + colonyTotal(fixture, Items.BOOK) + ", mana " + citizens[0].getCitizenSkillHandler().getLevel(Skill.Mana) + ", drained " + drained
                     + "; " + ColonyFixture.describe(citizens[0]))
          .check(checkDrainTarget ? "the drained citizen is the station's worker" : "the drain raised the enchanter's mana",
            () -> checkDrainTarget ? drained.contains(citizens[1].getName()) && !drained.contains(citizens[0].getName())
                    : citizens[0].getCitizenSkillHandler().getLevel(Skill.Mana) > mana[0] || colonyTotal(fixture, Items.ENCHANTED_BOOK) > 0,
            () -> "drained " + drained + " (enchanter " + citizens[0].getName() + ", station worker " + citizens[1].getName() + "), mana "
                    + citizens[0].getCitizenSkillHandler().getLevel(Skill.Mana))
          .awaitUntil("an enchanted book is in the hut", 9000, () -> checkDrainTarget || count(fixture, huts[0], Items.ENCHANTED_BOOK) > 0,
            () -> "enchanted books " + whereIs(fixture, Items.ENCHANTED_BOOK) + ", tomes " + whereIs(fixture, com.minecolonies.api.items.ModItems.ancientTome)
                    + ", mana " + citizens[0].getCitizenSkillHandler().getLevel(Skill.Mana) + "; " + ColonyFixture.describe(citizens[0]))
          .check("the station worker lost no XP", () -> citizens[1].getCitizenSkillHandler().getTotalXP() >= sourceXp[0],
            () -> "xp " + citizens[1].getCitizenSkillHandler().getTotalXP() + ", before " + sourceXp[0])
          .succeed();
    }

    /**
     * A crafter test for one hut (CRF-01): the hut with a worker, the recipe taught (or built in), ingredients in the warehouse, a courier,
     * and a lumberjack's hut that requests the product.
     *
     * @param type     the crafter's hut.
     * @param level    hut level.
     * @param teach    teaches the recipe (no-op for built-in recipes).
     * @param stock    what goes into the warehouse.
     * @param product  the requested product and amount.
     * @param uses     how many of each ingredient the request must use up, exactly.
     */
    private record Craft(BuildingEntry type, int level, java.util.function.Consumer<IBuilding> teach, List<ItemStack> stock, ItemStack product,
                         Map<Item, Integer> uses, String pack)
    {
        Craft(final BuildingEntry type, final int level, final java.util.function.Consumer<IBuilding> teach, final List<ItemStack> stock,
          final ItemStack product, final Map<Item, Integer> uses)
        {
            this(type, level, teach, stock, product, uses, ColonyFixture.DEFAULT_PACK);
        }
    }

    private static void crafterTest(final GameTestHelper helper, final Craft craft)
    {
        // Usually 1,300-5,100 ticks; once (crf01_fletcher, full run) the courier was still delivering after 5,500. A courier stuck on
        // his path is teleported by the stuck handler after about 2,500 ticks (CIT-13), so the budget leaves room for that.
        crafterTest(helper, craft, 11000);
    }

    private static void crafterTest(final GameTestHelper helper, final Craft craft, final int budget)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Logistics lg = new Logistics(fixture);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] worker = new ICitizenData[1];
        final RequestWatch watch = new RequestWatch(fixture);
        final RequestLog log = new RequestLog(fixture);
        final Map<Item, Integer> before = new java.util.HashMap<>();
        final double[] xp = new double[1];
        final Item product = craft.product().getItem();
        final int amount = craft.product().getCount();

        fixture.flow()
          .run("colony with warehouse, courier, the crafter's hut with a worker, and a requesting lumberjack's hut", () -> {
              lg.setUp(1);
              fixture.usePack(craft.pack());
              huts[0] = placeInBox(fixture, craft.type(), craft.level(), NORTH_WEST_BOX, craft.pack());
              fixture.usePack(ColonyFixture.DEFAULT_PACK);
              huts[1] = placeInBox(fixture, ModBuildings.lumberjack.get(), 1, NORTH_EAST_BOX);
              worker[0] = fixture.spawnCitizen(skills(50), huts[0], null, null);
              for (final ItemStack stack : craft.stock())
              {
                  fixture.fillWarehouse(stack.copy());
              }
              craft.teach().accept(huts[0]);
          })
          .awaitUntil("courier and crafter entities exist", 200, () -> lg.courierReady() && worker[0].getEntity().isPresent(), () -> "no entity yet")
          .run("the lumberjack's hut requests " + amount + " " + product, () -> {
              craft.uses().keySet().forEach(item -> before.put(item, colonyTotal(fixture, item)));
              xp[0] = worker[0].getCitizenSkillHandler().getTotalXP();
              watch.token = huts[1].createRequest(new Stack(craft.product().copy(), amount, amount), false);
          })
          .awaitUntil("request completed", budget, () -> {
              log.record();
              return watch.isGone();
          }, () -> watch.describe() + "; product at requester " + count(fixture, huts[1], product) + ", in crafter hut " + count(fixture, huts[0], product)
                     + "; requests seen " + log.seen + "; " + ColonyFixture.describe(worker[0]) + "; " + ColonyFixture.describe(lg.couriers.get(0)))
          .check("request resolved by this hut's crafting resolver",
            () -> watch.resolverPositions.contains(huts[0].getPosition()) && watch.resolversSeen.stream().anyMatch(r -> r.contains("Crafting")),
            () -> "resolvers " + watch.resolversSeen + " at " + watch.resolverPositions + ", hut at " + huts[0].getPosition())
          .check("the product arrived in the requested amount",
            () -> count(fixture, huts[1], product) == amount, () -> "requester has " + count(fixture, huts[1], product) + " of " + amount + "; where: "
                                                                     + whereIs(fixture, product))
          .check("exactly the recipe's ingredients were used up",
            () -> craft.uses().entrySet().stream().allMatch(e -> before.get(e.getKey()) - colonyTotal(fixture, e.getKey()) == e.getValue()),
            () -> craft.uses().keySet().stream().map(item -> item + " used " + (before.get(item) - colonyTotal(fixture, item)) + " (expected "
                                                               + craft.uses().get(item) + ")").toList() + "; product: " + whereIs(fixture, product)
                    + "; requests seen " + log.seen)
          .check("the crafter gained XP", () -> worker[0].getCitizenSkillHandler().getTotalXP() > xp[0],
            () -> "xp before " + xp[0] + ", after " + worker[0].getCitizenSkillHandler().getTotalXP())
          .succeed();
    }

    // ====================================================================================================================================
    // Helpers
    // ====================================================================================================================================

    /**
     * Teaches a crafting-grid recipe the way the teaching window does (AddRemoveRecipeMessage): one input per grid slot (null = empty),
     * grid size 2 or 3.
     *
     * @return whether the hut accepted it.
     */
    static boolean teachGrid(final IBuilding hut, final int gridSize, final ItemStack output, final Item... grid)
    {
        final List<ItemStorage> inputs = new ArrayList<>();
        for (int i = 0; i < gridSize * gridSize; i++)
        {
            final Item item = i < grid.length ? grid[i] : null;
            inputs.add(new ItemStorage(item == null ? ItemStack.EMPTY : new ItemStack(item)));
        }
        final AbstractCraftingBuildingModule module =
          hut.getModuleMatching(AbstractCraftingBuildingModule.class, m -> m.canLearn(ModCraftingTypes.SMALL_CRAFTING.get()));
        if (module == null)
        {
            throw new IllegalStateException(hut.getBuildingDisplayName() + " has no crafting module");
        }
        final IRecipeStorage storage = RecipeStorage.builder()
          .withInputs(inputs)
          .withPrimaryOutput(output)
          .withSecondaryOutputs(List.of())
          .withGridSize(gridSize)
          .withIntermediate(Blocks.AIR)
          .build();
        return module.addRecipe(IColonyManager.getInstance().getRecipeManager().checkOrAddRecipe(storage));
    }

    /**
     * Teaches a furnace recipe the way the furnace teaching window does.
     */
    static boolean teachFurnace(final IBuilding hut, final ItemStack output, final Item input)
    {
        final AbstractCraftingBuildingModule module =
          hut.getModuleMatching(AbstractCraftingBuildingModule.class, m -> m.canLearn(ModCraftingTypes.SMELTING.get()));
        if (module == null)
        {
            throw new IllegalStateException(hut.getBuildingDisplayName() + " has no smelting module");
        }
        final IRecipeStorage storage = RecipeStorage.builder()
          .withInputs(List.of(new ItemStorage(new ItemStack(input))))
          .withPrimaryOutput(output)
          .withGridSize(1)
          .withIntermediate(Blocks.FURNACE)
          .build();
        return module.addRecipe(IColonyManager.getInstance().getRecipeManager().checkOrAddRecipe(storage));
    }

    static void requireTaught(final boolean accepted, final String what)
    {
        if (!accepted)
        {
            throw new IllegalStateException("The hut refused the recipe for " + what);
        }
    }

    /**
     * Total of an item in all of the colony's buildings and citizens' inventories.
     */
    static int colonyTotal(final ColonyFixture fixture, final Item item)
    {
        int total = 0;
        for (final IBuilding building : fixture.colony().getServerBuildingManager().getBuildings().values())
        {
            try
            {
                total += count(fixture, building, item);
            }
            catch (final IllegalStateException e)
            {
                // building without inventory
            }
        }
        for (final ICitizenData citizen : fixture.colony().getCitizenManager().getCitizens())
        {
            total += courierHolds(citizen, item);
        }
        return total;
    }

    /**
     * Where an item is in the colony: per building and per citizen, for failure messages.
     */
    static String whereIs(final ColonyFixture fixture, final Item item)
    {
        final StringBuilder text = new StringBuilder();
        for (final IBuilding building : fixture.colony().getServerBuildingManager().getBuildings().values())
        {
            try
            {
                final int n = count(fixture, building, item);
                if (n > 0)
                {
                    text.append(building.getSchematicName()).append(' ').append(n).append("; ");
                }
            }
            catch (final IllegalStateException e)
            {
                // no inventory
            }
        }
        for (final ICitizenData citizen : fixture.colony().getCitizenManager().getCitizens())
        {
            final int n = courierHolds(citizen, item);
            if (n > 0)
            {
                text.append("citizen ").append(citizen.getName()).append(' ').append(n).append("; ");
            }
        }
        return text.length() == 0 ? "nowhere" : text.toString();
    }

    @Nullable
    static ItemStack pickaxeOf(final ICitizenData citizen)
    {
        final var inv = citizen.getInventory();
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            if (inv.getStackInSlot(slot).is(net.minecraft.tags.ItemTags.PICKAXES))
            {
                return inv.getStackInSlot(slot);
            }
        }
        return null;
    }

    static void takeAll(final ICitizenData citizen, final java.util.function.Predicate<ItemStack> matcher)
    {
        final var inv = citizen.getInventory();
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            if (matcher.test(inv.getStackInSlot(slot)))
            {
                inv.extractItem(slot, inv.getStackInSlot(slot).getCount(), false);
            }
        }
    }

    static void recordToolRequests(final ColonyFixture fixture, final IBuilding hut, final Set<String> seen)
    {
        for (final IRequest<?> request : allRequests(fixture))
        {
            if (request.getRequest() instanceof final Tool tool && request.getRequester().getLocation().getInDimensionLocation().equals(hut.getPosition()))
            {
                final StringBuilder children = new StringBuilder();
                for (final IToken<?> child : request.getChildren())
                {
                    final IRequest<?> c = requestOrNull(fixture, child);
                    children.append(c == null ? "gone" : c.getRequest().getClass().getSimpleName() + " " + c.getState() + " " + resolverName(fixture, child)).append(',');
                }
                seen.add(tool.getEquipmentType().getRegistryName().getPath() + " levels " + tool.getMinLevel() + ".." + tool.getMaxLevel() + " " + request.getState()
                           + " -> " + resolverName(fixture, request.getId()) + " children [" + children + "]");
            }
        }
    }

    static void recordFuelRequests(final ColonyFixture fixture, final IBuilding hut, final Set<String> seen)
    {
        for (final IRequest<?> request : allRequests(fixture))
        {
            if (request.getRequest() instanceof final StackList list
                  && request.getRequester().getLocation().getInDimensionLocation().equals(hut.getPosition())
                  && list.getStacks().stream().noneMatch(st -> st.is(Items.RAW_IRON)))
            {
                list.getStacks().forEach(st -> seen.add(st.getItem().toString()));
            }
        }
    }

    static List<BlockPos> furnaces(final IBuilding hut)
    {
        return hut.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces();
    }

    static void recordFurnaceUse(final ColonyFixture fixture, final IBuilding hut, final Set<BlockPos> used)
    {
        for (final BlockPos pos : furnaces(hut))
        {
            if (fixture.level().getBlockEntity(pos) instanceof final AbstractFurnaceBlockEntity furnace)
            {
                for (int slot = 0; slot < furnace.getContainerSize(); slot++)
                {
                    if (!furnace.getItem(slot).isEmpty())
                    {
                        used.add(pos);
                    }
                }
            }
        }
    }

    static int countWater(final ColonyFixture fixture, final IBuilding hut)
    {
        final var corners = hut.getCorners();
        int water = 0;
        for (final BlockPos pos : BlockPos.betweenClosed(corners.getA(), corners.getB()))
        {
            if (fixture.level().getFluidState(pos).is(FluidTags.WATER) && fixture.level().getFluidState(pos).isSource())
            {
                water++;
            }
        }
        return water;
    }

    /**
     * The set of items in all of the colony's buildings and citizens' inventories.
     */
    static Set<Item> itemsInColony(final ColonyFixture fixture)
    {
        final Set<Item> items = new LinkedHashSet<>();
        for (final IBuilding building : fixture.colony().getServerBuildingManager().getBuildings().values())
        {
            try
            {
                final var handler = fixture.inventory(building);
                for (int slot = 0; slot < handler.getSlots(); slot++)
                {
                    if (!handler.getStackInSlot(slot).isEmpty())
                    {
                        items.add(handler.getStackInSlot(slot).getItem());
                    }
                }
            }
            catch (final IllegalStateException e)
            {
                // no inventory
            }
        }
        for (final ICitizenData citizen : fixture.colony().getCitizenManager().getCitizens())
        {
            final var inv = citizen.getInventory();
            for (int slot = 0; slot < inv.getSlots(); slot++)
            {
                if (!inv.getStackInSlot(slot).isEmpty())
                {
                    items.add(inv.getStackInSlot(slot).getItem());
                }
            }
        }
        return items;
    }

    /**
     * Teaches a brewing recipe the way the brewing stand teaching window builds it (3 potions + 1 ingredient, grid 1, brewing stand).
     */
    static boolean teachBrewing(final IBuilding hut, final ItemStack output, final ItemStack potion, final Item ingredient)
    {
        final AbstractCraftingBuildingModule module =
          hut.getModuleMatching(AbstractCraftingBuildingModule.class, m -> m.canLearn(ModCraftingTypes.BREWING.get()));
        if (module == null)
        {
            throw new IllegalStateException(hut.getBuildingDisplayName() + " has no brewing module");
        }
        final IRecipeStorage storage = RecipeStorage.builder()
          .withInputs(List.of(new ItemStorage(potion, 3, false), new ItemStorage(new ItemStack(ingredient))))
          .withPrimaryOutput(output)
          .withGridSize(1)
          .withIntermediate(Blocks.BREWING_STAND)
          .build();
        return module.addRecipe(IColonyManager.getInstance().getRecipeManager().checkOrAddRecipe(storage));
    }

    /** Structure pack with a kitchen (the default pack has none). */
    static final String KITCHEN_PACK = "Dark Oak Treehouse";

    static Item modItem(final String path)
    {
        final Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecolonies", path));
        if (item == null || item == Items.AIR)
        {
            throw new IllegalStateException("No item minecolonies:" + path);
        }
        return item;
    }

    static boolean replanted(final ColonyFixture fixture, final List<BlockPos> wartPositions)
    {
        for (final BlockPos pos : wartPositions)
        {
            final BlockState state = fixture.level().getBlockState(pos);
            if (state.is(Blocks.NETHER_WART) && state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE) < 3)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Records the citizen the enchanter's AI is currently draining (private field {@code citizenToGatherFrom}).
     */
    static void recordDrainTarget(final ICitizenData enchanter, final Set<String> drained)
    {
        if (enchanter.getJob() == null || enchanter.getJob().getWorkerAI() == null)
        {
            return;
        }
        try
        {
            final var field = com.minecolonies.core.entity.ai.workers.service.EntityAIWorkEnchanter.class.getDeclaredField("citizenToGatherFrom");
            field.setAccessible(true);
            final Object target = field.get(enchanter.getJob().getWorkerAI());
            if (target instanceof final ICitizenData data)
            {
                drained.add(data.getName());
            }
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException(e);
        }
    }

    static int courierHolds(final ICitizenData citizen, final Item item)
    {
        final var inv = citizen.getInventory();
        int total = 0;
        for (int slot = 0; slot < inv.getSlots(); slot++)
        {
            if (inv.getStackInSlot(slot).is(item))
            {
                total += inv.getStackInSlot(slot).getCount();
            }
        }
        return total;
    }

    /**
     * Requests in the system whose requester or delivery target is at the given position.
     */
    static List<String> referencesTo(final ColonyFixture fixture, final BlockPos pos)
    {
        final List<String> found = new ArrayList<>();
        for (final IRequest<?> request : allRequests(fixture))
        {
            if (request.getRequester().getLocation().getInDimensionLocation().equals(pos))
            {
                found.add(request.getId() + " requester " + request.getRequest() + " " + request.getState());
            }
            else if (request.getRequest() instanceof final Delivery delivery
                       && delivery.getTarget().getInDimensionLocation().equals(pos))
            {
                found.add(request.getId() + " delivery target " + delivery.getStack() + " " + request.getState());
            }
        }
        return found;
    }

    /**
     * Fills every free slot of the building's inventory with full stacks of the item.
     */
    static void fillCompletely(final ColonyFixture fixture, final IBuilding building, final Item item)
    {
        final var handler = fixture.inventory(building);
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            ItemStack rest = new ItemStack(item, 64);
            while (!rest.isEmpty())
            {
                final ItemStack after = handler.insertItem(slot, rest, false);
                if (after.getCount() == rest.getCount())
                {
                    break;
                }
                rest = after;
            }
        }
    }

    /**
     * Whether the warehouse sent its "warehouse is full" notification (TileEntityWareHouse.lastNotification is set when it does).
     */
    static boolean warehouseFullNotified(final IBuilding warehouse)
    {
        try
        {
            final var field = com.minecolonies.core.tileentities.TileEntityWareHouse.class.getDeclaredField("lastNotification");
            field.setAccessible(true);
            return field.getLong(warehouse.getTileEntity()) != 0;
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Places a rack on a free floor spot inside the building and registers it, as when a player places a rack in a hut.
     */
    static BlockPos addRack(final ColonyFixture fixture, final IBuilding building)
    {
        return addBlockInHut(fixture, building, ModBlocks.blockRack.defaultBlockState());
    }

    /**
     * Places a block on a free floor spot inside the building and registers it with the building, as when a player places it in a hut.
     */
    static BlockPos addBlockInHut(final ColonyFixture fixture, final IBuilding building, final BlockState rack)
    {
        final var corners = building.getCorners();
        final BlockPos hutPos = building.getPosition();
        for (int dx = -6; dx <= 6; dx++)
        {
            for (int dz = -6; dz <= 6; dz++)
            {
                final BlockPos pos = hutPos.offset(dx, 0, dz);
                if (pos.getX() <= corners.getA().getX() || pos.getZ() <= corners.getA().getZ() || pos.getX() >= corners.getB().getX() || pos.getZ() >= corners.getB().getZ())
                {
                    continue;
                }
                if (fixture.level().getBlockState(pos).isAir() && fixture.level().getBlockState(pos.above()).isAir()
                      && fixture.level().getBlockState(pos.below()).isSolidRender())
                {
                    fixture.level().setBlock(pos, rack, Block.UPDATE_ALL);
                    building.registerBlockPosition(rack, pos, fixture.level());
                    return pos;
                }
            }
        }
        throw new IllegalStateException("No free spot for a rack in " + building.getBuildingDisplayName());
    }

    /**
     * Records every request seen in the colony (requestable + resolver), for checks on child requests.
     */
    private static final class RequestLog
    {
        final ColonyFixture fixture;
        final Set<String>   seen = new LinkedHashSet<>();

        RequestLog(final ColonyFixture fixture)
        {
            this.fixture = fixture;
        }

        void record()
        {
            for (final IRequest<?> request : allRequests(fixture))
            {
                final Object requestable = request.getRequest();
                final String item;
                if (requestable instanceof final Stack stack)
                {
                    item = stack.getStack().getItem().toString();
                }
                else if (requestable instanceof final AbstractCrafting crafting)
                {
                    item = crafting.getStack().getItem().toString();
                }
                else if (requestable instanceof final Delivery delivery)
                {
                    item = delivery.getStack().getItem().toString();
                }
                else
                {
                    item = "";
                }
                seen.add(requestable.getClass().getSimpleName() + " " + item + " -> " + resolverName(fixture, request.getId()));
            }
        }
    }

    /**
     * Standard logistics setup: colony (move-in off), warehouse in the south-west corner, courier's hut in the south-east corner, and
     * couriers hired into the courier's hut and assigned to the warehouse.
     */
    private static final class Logistics
    {
        final ColonyFixture      fixture;
        final List<ICitizenData> couriers = new ArrayList<>();
        IBuilding warehouse;
        IBuilding courierHut;

        Logistics(final ColonyFixture fixture)
        {
            this.fixture = fixture;
        }

        void setUp(final int courierCount)
        {
            setUp(courierCount, 1);
        }

        void setUp(final int courierCount, final int warehouseLevel)
        {
            fixture.createColony();
            fixture.setMoveIn(false);
            warehouse = placeInBox(fixture, ModBuildings.wareHouse.get(), warehouseLevel, WAREHOUSE_BOX);
            courierHut = placeInBox(fixture, ModBuildings.deliveryman.get(), 1, COURIER_BOX);
            // A courier's hut employs one courier: further couriers get their own huts in the east strip.
            final int[][] extraBoxes = {{44, 22}, {44, 34}, {52, 22}};
            for (int i = 0; i < courierCount; i++)
            {
                final IBuilding home = i == 0 ? courierHut : placeInBox(fixture, ModBuildings.deliveryman.get(), 1, extraBoxes[i - 1]);
                final ICitizenData courier = fixture.spawnCitizen(skills(50), home, null, null);
                couriers.add(courier);
                final CourierAssignmentModule module = warehouse.getFirstModuleOccurance(CourierAssignmentModule.class);
                if (!module.hasAssignedCitizen(courier) && !module.assignCitizen(courier))
                {
                    throw new IllegalStateException("Could not assign courier " + courier.getName() + " to the warehouse");
                }
            }
        }

        boolean courierReady()
        {
            return couriers.stream().allMatch(c -> c.getEntity().isPresent());
        }
    }

    /**
     * Records which resolvers a request was assigned to while it existed, and whether it is gone (completed and received).
     */
    private static final class RequestWatch
    {
        final ColonyFixture fixture;
        final Set<String>   resolversSeen = new LinkedHashSet<>();
        final Set<BlockPos> resolverPositions = new LinkedHashSet<>();
        IToken<?> token;
        String    lastState = "<not created>";

        RequestWatch(final ColonyFixture fixture)
        {
            this.fixture = fixture;
        }

        IRequest<?> request()
        {
            return token == null ? null : requestOrNull(fixture, token);
        }

        String currentResolver()
        {
            final IRequest<?> request = request();
            if (request == null)
            {
                return "<none>";
            }
            lastState = request.getState().name();
            final String resolver = resolverName(fixture, token);
            resolversSeen.add(resolver);
            try
            {
                resolverPositions.add(fixture.colony().getRequestManager().getResolverForRequest(token).getLocation().getInDimensionLocation());
            }
            catch (final Exception e)
            {
                // not assigned
            }
            return resolver;
        }

        boolean isGone()
        {
            currentResolver();
            return token != null && request() == null;
        }

        String describe()
        {
            return "request " + token + " state " + lastState + ", resolver now " + currentResolver() + ", resolvers seen " + resolversSeen;
        }
    }

    @Nullable
    static IRequest<?> requestOrNull(final ColonyFixture fixture, final IToken<?> token)
    {
        return ((IStandardRequestManager) fixture.colony().getRequestManager()).getRequestHandler().getRequestOrNull(token);
    }

    static String resolverName(final ColonyFixture fixture, final IToken<?> token)
    {
        try
        {
            final IRequestResolver<?> resolver = fixture.colony().getRequestManager().getResolverForRequest(token);
            return resolver == null ? "<unassigned>" : resolver.getClass().getSimpleName();
        }
        catch (final Exception e)
        {
            return "<unassigned>";
        }
    }

    /**
     * The requests of a hut that are not finished yet, with state and resolver (for failure messages).
     */
    static String openRequestsOf(final ColonyFixture fixture, final IBuilding hut)
    {
        final StringBuilder sb = new StringBuilder("[");
        for (final IRequest<?> request : allRequests(fixture))
        {
            if (request.getRequester().getLocation().getInDimensionLocation().equals(hut.getPosition())
                  && request.getState().ordinal() < RequestState.COMPLETED.ordinal())
            {
                sb.append(request.getShortDisplayString().getString()).append(' ').append(request.getState()).append(' ')
                  .append(resolverName(fixture, request.getId())).append("; ");
            }
        }
        return sb.append(']').toString();
    }

    static Collection<IRequest<?>> allRequests(final ColonyFixture fixture)
    {
        return new ArrayList<>(((IStandardRequestManager) fixture.colony().getRequestManager()).getRequestIdentitiesDataStore().getIdentities().values());
    }

    static Map<Skill, Integer> skills(final int level)
    {
        final Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        for (final Skill skill : Skill.values())
        {
            skills.put(skill, level);
        }
        return skills;
    }

    static int count(final ColonyFixture fixture, final IBuilding building, final Item item)
    {
        return fixture.countItems(building, stack -> stack.is(item));
    }

    /**
     * Places a hut so that its blueprint footprint starts at the given minimum (x, z) corner. The anchor is raised if the blueprint reaches
     * below the arena floor.
     */
    static IBuilding placeInBox(final ColonyFixture fixture, final BuildingEntry type, final int level, final int[] minCorner)
    {
        return placeInBox(fixture, type, level, minCorner, ColonyFixture.DEFAULT_PACK);
    }

    /**
     * Same as {@link #placeInBox(ColonyFixture, BuildingEntry, int, int[])} with the blueprint from the given pack (the fixture must be set
     * to the same pack with {@code usePack}).
     */
    static IBuilding placeInBox(final ColonyFixture fixture, final BuildingEntry type, final int level, final int[] minCorner, final String pack)
    {
        final String name = type.getRegistryName().getPath() + level;
        final Path file = StructurePacks.findBlueprint(pack, name);
        if (file == null)
        {
            throw new IllegalStateException("No blueprint " + name + " in " + pack);
        }
        final String subPath = StructurePacks.getStructurePack(pack).getSubPath(file).replace('\\', '/');
        final Blueprint blueprint = StructurePacks.getBlueprint(pack, subPath, fixture.level().registryAccess());
        final BlockPos offset = blueprint.getPrimaryBlockOffset();
        final int y = Math.max(GROUND_Y, offset.getY());
        final BlockPos anchor = new BlockPos(minCorner[0] + offset.getX(), y, minCorner[1] + offset.getZ());
        LOGGER.info("Placing {} at relative {} (footprint x {}..{}, z {}..{})", name, anchor.toShortString(), minCorner[0], minCorner[0] + blueprint.getSizeX() - 1,
          minCorner[1], minCorner[1] + blueprint.getSizeZ() - 1);
        return fixture.placeBuilding(type, level, anchor);
    }

    /**
     * Places a single-block colony building (stash, postbox) the way a player does: set the block, then run its placement hook, which
     * registers the building with the colony.
     */
    static IBuilding placeBlockBuilding(final ColonyFixture fixture, final Block block, final BlockPos relPos)
    {
        final BlockPos abs = fixture.abs(relPos);
        final BlockState state = block.defaultBlockState();
        fixture.level().setBlock(abs, state, Block.UPDATE_ALL);
        block.setPlacedBy(fixture.level(), abs, state, fixture.owner(), new ItemStack(block));
        final IBuilding building = fixture.colony().getServerBuildingManager().getBuilding(abs);
        if (building == null)
        {
            throw new IllegalStateException(block + " at " + abs + " was not registered as a building");
        }
        return building;
    }

    /**
     * Inserts a stack into the first rack of a building that takes it, the way a player puts items into a rack.
     */
    static void insertIntoRack(final ColonyFixture fixture, final IBuilding building, final ItemStack stack)
    {
        ItemStack rest = stack.copy();
        for (final BlockPos pos : building.getContainers())
        {
            final BlockEntity be = fixture.level().getBlockEntity(pos);
            if (be instanceof final AbstractTileEntityRack rack)
            {
                for (int slot = 0; slot < rack.getInventory().getSlots() && !rest.isEmpty(); slot++)
                {
                    rest = rack.getInventory().insertItem(slot, rest, false);
                }
            }
            if (rest.isEmpty())
            {
                return;
            }
        }
        throw new IllegalStateException("No rack room in " + building.getBuildingDisplayName() + " for " + rest);
    }

    /**
     * Removes up to {@code amount} items from a building's combined inventory.
     */
    static void removeItems(final ColonyFixture fixture, final IBuilding building, final Item item, final int amount)
    {
        int left = amount;
        final var handler = fixture.inventory(building);
        for (int slot = 0; slot < handler.getSlots() && left > 0; slot++)
        {
            if (handler.getStackInSlot(slot).is(item))
            {
                left -= handler.extractItem(slot, left, false).getCount();
            }
        }
        if (left > 0)
        {
            throw new IllegalStateException("Could only remove " + (amount - left) + " of " + amount + " " + item);
        }
    }
}
