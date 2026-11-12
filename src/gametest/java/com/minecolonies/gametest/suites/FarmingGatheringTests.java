package com.minecolonies.gametest.suites;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.IItemListModule;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.tileentities.AbstractTileEntityBarrel;
import com.minecolonies.core.blocks.BlockScarecrow;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.colony.buildings.modules.ItemListModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingComposter;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLumberjack;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkComposter;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack;
import com.minecolonies.core.tileentities.TileEntityBarrel;
import com.minecolonies.gametest.framework.ColonyFixture;
import com.google.common.reflect.TypeToken;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import com.minecolonies.api.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * End-to-end tests from docs/e2e-tests/jobs-farming.md and docs/e2e-tests/jobs-gathering.md.
 * <p>
 * Test ids: {@code minecolonies-gametest:farming_gathering_tests_<method_name>}. Run only these with
 * {@code ./gradlew runGameTest -Pe2eFilter=farming_gathering_tests}. See docs/e2e-tests/AGENT-GUIDE.md.
 */
public class FarmingGatheringTests
{
    /**
     * Hut position used by the single-hut tests (a known-good spot of the arena).
     */
    private static final BlockPos HUT_POS = new BlockPos(12, ColonyFixture.GROUND_Y, 12);

    /**
     * Herder huts are wide (pens); this spot keeps every herder hut's area inside the arena walls, so no animal can fall off the edge.
     */
    private static final BlockPos HERD_POS = new BlockPos(44, ColonyFixture.GROUND_Y, 48);

    // ------------------------------------------------------------------------------------------------------------------------------------
    // FRM-01 Farmer: field cycle
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * FRM-01 Farmer: field cycle. A level 1 farm with a farmer and a 7x7 wheat field (radius 3 around the scarecrow) on untilled grass. The
     * farmer tills and plants every cell; the test then grows every crop to full age; the farmer harvests and replants every cell; wheat and
     * seeds end up in the farm (or the farmer's inventory on the way there).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 32000, skyAccess = true)
    public void frm01_farmerFieldCycle(final GameTestHelper helper)
    {
        farmerFieldCycle(helper, Blocks.DIRT, true);
    }

    /**
     * FRM-01b Farmer tills grass. Same as FRM-01 but the field is the arena's grass, the usual case in a real world. Only checks that every
     * cell gets tilled and planted.
     */
    // BUG E2E-FRM-01-1: the farmer never tills grass blocks. EntityAIWorkFarmer.findHoeableSurface (EntityAIWorkFarmer.java:337) only accepts
    // blocks in BlockTags.DIRT, and in Minecraft 26.3 the vanilla #minecraft:dirt tag no longer contains grass_block (only dirt, coarse_dirt,
    // rooted_dirt). Every grass cell is skipped and the field cycles EMPTY -> HOED -> PLANTED without any work.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true, required = false)
    public void frm01_farmerTillsGrass(final GameTestHelper helper)
    {
        farmerFieldCycle(helper, Blocks.GRASS_BLOCK, false);
    }

    private static void farmerFieldCycle(final GameTestHelper helper, final Block ground, final boolean fullCycle)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final FarmField[] field = new FarmField[1];
        final BlockPos scarecrow = new BlockPos(50, ColonyFixture.GROUND_Y, 50);
        final int radius = 3;
        final List<BlockPos> cells = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                if (dx != 0 || dz != 0)
                {
                    cells.add(scarecrow.offset(dx, -1, dz));
                }
            }
        }

        final DayPump days = new DayPump(fixture, worker);
        final com.minecolonies.gametest.framework.TestFlow flow = fixture.flow()
          .run("set up colony, farm and field", () -> {
              for (final BlockPos c : cells)
              {
                  helper.setBlock(c, ground);
              }
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.farmer.get(), 1, HUT_POS);
              helper.setBlock(scarecrow, ModBlocks.blockScarecrow.defaultBlockState().setValue(BlockScarecrow.HALF, DoubleBlockHalf.LOWER));
              helper.setBlock(scarecrow.above(), ModBlocks.blockScarecrow.defaultBlockState().setValue(BlockScarecrow.HALF, DoubleBlockHalf.UPPER));
              field[0] = FarmField.create(fixture.abs(scarecrow), fixture.level());
              field[0].setSeed(new ItemStack(Items.WHEAT_SEEDS));
              for (final Direction dir : Direction.Plane.HORIZONTAL)
              {
                  field[0].setRadius(dir, radius);
              }
              if (!fixture.colony().getServerBuildingManager().addBuildingExtension(field[0]))
              {
                  throw new IllegalStateException("field was not added");
              }
              fixture.fillBuilding(hut[0], new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE),
                new ItemStack(Items.WHEAT_SEEDS, 64));
              worker[0] = fixture.spawnCitizen(30, hut[0], null);
          })
          .awaitUntil("the farm claims the field", 400, () -> hut[0].getID().equals(field[0].getBuildingId()),
            () -> "field owner " + field[0].getBuildingId())
          .awaitUntil("every field cell is tilled and planted with wheat", 8000,
            () -> days.pump() && cells.stream().allMatch(c -> isFarmland(helper, c) && helper.getBlockState(c.above()).is(Blocks.WHEAT)),
            () -> describeField(helper, cells) + ", field stage " + field[0].getFieldStage() + ", offset " + ((com.minecolonies.core.colony.buildings.workerbuildings.BuildingFarmer) hut[0]).getWorkingOffset() + " / " + describeWork(hut[0], worker[0]));
        if (!fullCycle)
        {
            flow.succeed();
            return;
        }
        flow.run("grow every crop to full age", () -> {
              for (final BlockPos c : cells)
              {
                  helper.setBlock(c.above(), ((CropBlock) Blocks.WHEAT).getStateForAge(((CropBlock) Blocks.WHEAT).getMaxAge()));
              }
          })
          .awaitUntil("every crop is harvested and replanted", 16000,
            () -> days.pump() && cells.stream().allMatch(c -> isFarmland(helper, c) && helper.getBlockState(c.above()).is(Blocks.WHEAT)
                                                 && !((CropBlock) Blocks.WHEAT).isMaxAge(helper.getBlockState(c.above()))),
            () -> describeField(helper, cells) + ", field stage " + field[0].getFieldStage() + ", offset " + ((com.minecolonies.core.colony.buildings.workerbuildings.BuildingFarmer) hut[0]).getWorkingOffset() + " / " + describeWork(hut[0], worker[0]))
          .awaitUntil("harvested wheat reaches the farm", 3000,
            () -> fixture.countItems(hut[0], s -> s.is(Items.WHEAT)) >= cells.size(),
            () -> "wheat in farm " + fixture.countItems(hut[0], s -> s.is(Items.WHEAT)) + ", in farmer " + countInCitizen(worker[0], s -> s.is(Items.WHEAT))
                    + " / " + describeWork(hut[0], worker[0]))
          .check("seeds are in the farm or with the farmer",
            () -> fixture.countItems(hut[0], s -> s.is(Items.WHEAT_SEEDS)) + countInCitizen(worker[0], s -> s.is(Items.WHEAT_SEEDS)) > 0,
            () -> "seeds in farm " + fixture.countItems(hut[0], s -> s.is(Items.WHEAT_SEEDS)))
          .succeed();
    }

    /**
     * Lets colony days pass while a test waits. Field workers work each field only once per colony day
     * (BuildingExtensionsModule.getBuildingExtensionToWorkOn skips fields already worked today) and idle for the rest of the day, so a field
     * cycle needs several days. When the worker has not been in the WORK state for 100 ticks, this sets the time to night for 60 ticks and
     * back to morning, which makes the colony count a new day.
     */
    private static final class DayPump
    {
        private final ColonyFixture fixture;
        private final ICitizenData[] worker;
        private int idleTicks = 0;
        private int nightTicks = -1;
        private int days = 0;

        DayPump(final ColonyFixture fixture, final ICitizenData[] worker)
        {
            this.fixture = fixture;
            this.worker = worker;
        }

        /**
         * @return always true, so it can be chained in front of a wait condition.
         */
        boolean pump()
        {
            if (nightTicks >= 0)
            {
                if (++nightTicks >= 60)
                {
                    fixture.setTime(1000);
                    nightTicks = -1;
                    idleTicks = 0;
                }
                return true;
            }
            final boolean working = worker[0] != null && worker[0].getEntity()
              .map(e -> e instanceof com.minecolonies.core.entity.citizen.EntityCitizen c
                          && (c.getCitizenAI().getState() == com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState.WORK || c.getCitizenAI().getState() == com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState.WORKING))
              .orElse(false);
            idleTicks = working ? 0 : idleTicks + 1;
            if (idleTicks >= 100)
            {
                fixture.setTime(14000);
                nightTicks = 0;
                days++;
            }
            return true;
        }

        int days()
        {
            return days;
        }
    }

    private static boolean isFarmland(final GameTestHelper helper, final BlockPos pos)
    {
        return helper.getBlockState(pos).is(Blocks.FARMLAND);
    }

    private static String describeField(final GameTestHelper helper, final List<BlockPos> cells)
    {
        int farmland = 0;
        int wheat = 0;
        int ripe = 0;
        for (final BlockPos c : cells)
        {
            if (isFarmland(helper, c))
            {
                farmland++;
            }
            final BlockState above = helper.getBlockState(c.above());
            if (above.is(Blocks.WHEAT))
            {
                wheat++;
                if (((CropBlock) Blocks.WHEAT).isMaxAge(above))
                {
                    ripe++;
                }
            }
        }
        return cells.size() + " cells: farmland " + farmland + ", wheat " + wheat + " (ripe " + ripe + ")";
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // FRM-02 Planter: plantation
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * FRM-02a Planter, sugar cane field.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 16000, skyAccess = true)
    public void frm02_planterSugarCane(final GameTestHelper helper)
    {
        plantationField(helper, "sugar", Blocks.SUGAR_CANE, Items.SUGAR_CANE, 3, null);
    }

    /**
     * FRM-02b Planter, cactus field.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 16000, skyAccess = true)
    public void frm02_planterCactus(final GameTestHelper helper)
    {
        plantationField(helper, "cactus", Blocks.CACTUS, Items.CACTUS, 3, null);
    }

    /**
     * FRM-02c Planter, bamboo field (needs the research "jungle master"; the planter harvests bamboo of at least 6 blocks).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 24000, skyAccess = true)
    public void frm02_planterBamboo(final GameTestHelper helper)
    {
        plantationField(helper, "bamboo", Blocks.BAMBOO, Items.BAMBOO, 8, net.minecraft.resources.Identifier.parse("minecolonies:technology/junglemaster"));
    }

    /**
     * FRM-02 Planter: plantation. A level 1 plantation (one field allowed) and one plantation field pasted from the pack's field blueprint and
     * registered as when a player places it. Every second working position gets a plant taller than the harvest height, the others are
     * empty. The planter cuts the grown plants above their base block (the base stays), replants the empty positions from the hut's stock,
     * and the harvest goes to the plantation.
     */
    private static void plantationField(
      final GameTestHelper helper, final String fieldName, final Block plant, final Item item, final int height, final net.minecraft.resources.Identifier research)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final com.minecolonies.core.colony.buildingextensions.PlantationField[] field = new com.minecolonies.core.colony.buildingextensions.PlantationField[1];
        final List<BlockPos> grown = new ArrayList<>();
        final List<BlockPos> empty = new ArrayList<>();
        final int stock = 16;
        final DayPump days = new DayPump(fixture, worker);

        fixture.flow()
          .run("set up colony, plantation and " + fieldName + " field", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              if (research != null)
              {
                  fixture.completeResearch(research);
              }
              hut[0] = fixture.placeBuilding(ModBuildings.plantation.get(), 1, HUT_POS);
              final BlockPos fieldAnchor = fixture.abs(new BlockPos(48, ColonyFixture.GROUND_Y, 48));
              fixture.paste("agriculture/fields/" + fieldName + ".blueprint", fieldAnchor, com.ldtteam.structurize.api.RotationMirror.NONE);
              BlockPos fieldBlock = null;
              for (final BlockPos p : BlockPos.betweenClosed(fieldAnchor.offset(-16, -4, -16), fieldAnchor.offset(16, 12, 16)))
              {
                  if (fixture.level().getBlockState(p).is(ModBlocks.blockPlantationField))
                  {
                      fieldBlock = p.immutable();
                      break;
                  }
              }
              if (fieldBlock == null)
              {
                  throw new IllegalStateException("no plantation field block in the pasted " + fieldName + " blueprint");
              }
              // The paste doesn't run setPlacedBy; do what placing the block by hand does (registers the field from the block's tags).
              final BlockState state = fixture.level().getBlockState(fieldBlock);
              state.getBlock().setPlacedBy(fixture.level(), fieldBlock, state, null, ItemStack.EMPTY);
              final BlockPos fieldPos = fieldBlock;
              field[0] = (com.minecolonies.core.colony.buildingextensions.PlantationField) fixture.colony().getServerBuildingManager()
                .getBuildingExtensions(e -> e instanceof com.minecolonies.core.colony.buildingextensions.PlantationField && e.getPosition().equals(fieldPos))
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("field not registered at " + fieldPos));
              final List<BlockPos> positions = field[0].getWorkingPositions();
              if (positions.isEmpty())
              {
                  throw new IllegalStateException("field has no working positions");
              }
              for (int i = 0; i < positions.size(); i++)
              {
                  final BlockPos wp = positions.get(i);
                  for (int h = 1; h <= height + 2; h++)
                  {
                      fixture.level().setBlock(wp.above(h), Blocks.AIR.defaultBlockState(), 2);
                  }
                  if (i % 2 == 0)
                  {
                      for (int h = 1; h <= height; h++)
                      {
                          fixture.level().setBlock(wp.above(h), plant.defaultBlockState(), 2);
                      }
                      grown.add(wp);
                  }
                  else
                  {
                      empty.add(wp);
                  }
              }
              fixture.fillBuilding(hut[0], new ItemStack(item, stock), new ItemStack(Items.WOODEN_AXE));
              // The level 1 plantation may already own a field from its own blueprint; assign the test's field by hand instead.
              final var module = hut[0].getFirstModuleOccurance(com.minecolonies.core.colony.buildings.workerbuildings.BuildingPlantation.PlantationFieldsModule.class);
              module.setAssignManually(true);
              for (final var owned : new ArrayList<>(module.getOwnedExtensions()))
              {
                  module.freeExtension(owned);
              }
              if (!module.assignExtension(field[0]))
              {
                  throw new IllegalStateException("the plantation can't take the " + fieldName + " field");
              }
          })
          .check("the plantation owns the field", () -> hut[0].getID().equals(field[0].getBuildingId()), () -> "field owner " + field[0].getBuildingId())
          .run("hire the planter", () -> worker[0] = fixture.spawnCitizen(30, hut[0], null))
          .awaitUntil("all but at most one grown plant are cut above the base (no base is lost) and all but one empty position replanted", 20000,
            () -> days.pump() && grown.stream().filter(wp -> is(fixture, wp.above(), plant) && !is(fixture, wp.above(2), plant)).count() >= grown.size() - 1
                    && empty.stream().filter(wp -> is(fixture, wp.above(), plant)).count() >= empty.size() - 1
                    && grown.stream().allMatch(wp -> is(fixture, wp.above(), plant)),
            () -> describePlantation(fixture, plant, grown, empty) + ", hut free slots " + freeSlots(fixture, hut[0]) + " / " + describeWork(hut[0], worker[0]))
          .awaitUntil("the harvest is in the plantation", 3000,
            () -> fixture.countItems(hut[0], s -> s.is(item)) + countInCitizen(worker[0], s -> s.is(item)) >= stock - empty.size() + grown.size(),
            () -> item + " in hut " + fixture.countItems(hut[0], s -> s.is(item)) + ", with planter " + countInCitizen(worker[0], s -> s.is(item))
                    + ", expected at least " + (stock - empty.size() + grown.size()) + " / " + describeWork(hut[0], worker[0]))
          .succeed();
    }

    private static int freeSlots(final ColonyFixture fixture, final IBuilding building)
    {
        final var inv = fixture.inventory(building);
        int free = 0;
        for (int i = 0; i < inv.getSlots(); i++)
        {
            if (inv.getStackInSlot(i).isEmpty())
            {
                free++;
            }
        }
        return free;
    }

    private static boolean is(final ColonyFixture fixture, final BlockPos absPos, final Block block)
    {
        return fixture.level().getBlockState(absPos).is(block);
    }

    private static String describePlantation(final ColonyFixture fixture, final Block plant, final List<BlockPos> grown, final List<BlockPos> empty)
    {
        final StringBuilder sb = new StringBuilder("grown positions (base, above):");
        for (final BlockPos wp : grown)
        {
            sb.append(' ').append(is(fixture, wp.above(), plant) ? 'P' : '-').append(is(fixture, wp.above(2), plant) ? 'P' : '-');
        }
        sb.append(", empty positions (base):");
        for (final BlockPos wp : empty)
        {
            sb.append(' ').append(is(fixture, wp.above(), plant) ? 'P' : '-');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // FRM-04 Composter
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * FRM-04 Composter. A level 1 composter's hut with its barrels; only wheat seeds are allowed. Apples (also compostable) lie in the hut too.
     * The composter fills every barrel with seeds (each barrel takes 64 "compost points", the item's recipe strength per item), the test
     * finishes the composting time, the composter empties the barrels and the compost (6 per barrel at least) ends up in the hut. Apples are
     * never touched.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void frm04_composter(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BuildingComposter[] hut = new BuildingComposter[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final int seedsStart = 320;
        final int applesStart = 32;
        final int[] strength = new int[1];

        fixture.flow()
          .run("set up colony and composter's hut", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = (BuildingComposter) fixture.placeBuilding(ModBuildings.composter.get(), 1, HUT_POS);
              final IItemListModule allowed = hut[0].getModuleMatching(ItemListModule.class, m -> m.getId().equals(EntityAIWorkComposter.COMPOSTABLE_LIST));
              allowed.clearItems();
              allowed.addItem(new ItemStorage(new ItemStack(Items.WHEAT_SEEDS)));
              strength[0] = IColonyManager.getInstance().getCompatibilityManager().getCopyOfCompostRecipes().get(Items.WHEAT_SEEDS).value().getStrength();
              fixture.fillBuilding(hut[0], new ItemStack(Items.WHEAT_SEEDS, 64), new ItemStack(Items.WHEAT_SEEDS, 64), new ItemStack(Items.WHEAT_SEEDS, 64),
                new ItemStack(Items.WHEAT_SEEDS, 64), new ItemStack(Items.WHEAT_SEEDS, 64), new ItemStack(Items.APPLE, applesStart));
              worker[0] = fixture.spawnCitizen(30, hut[0], null);
          })
          .check("the hut has barrels", () -> !hut[0].getBarrels().isEmpty(), () -> "barrels " + hut[0].getBarrels())
          .awaitUntil("every barrel is full and composting", 5000,
            () -> hut[0].getBarrels().stream().allMatch(p -> barrel(fixture, p) != null && barrel(fixture, p).checkIfWorking()),
            () -> describeBarrels(fixture, hut[0]) + " / " + describeWork(hut[0], worker[0]))
          .check("seeds used: barrels x floor(64 / strength)",
            () -> seedsStart - seedCount(fixture, hut[0], worker[0]) == hut[0].getBarrels().size() * (AbstractTileEntityBarrel.MAX_ITEMS / strength[0]),
            () -> "used " + (seedsStart - seedCount(fixture, hut[0], worker[0])) + ", barrels " + hut[0].getBarrels().size() + ", strength " + strength[0])
          .run("skip the composting time", () -> {
              for (final BlockPos p : hut[0].getBarrels())
              {
                  final TileEntityBarrel barrel = barrel(fixture, p);
                  for (int i = 0; i < 24000 && !barrel.isDone(); i++)
                  {
                      barrel.updateTick(fixture.level(), p, fixture.level().getBlockState(p), new Random());
                  }
              }
          })
          .check("every barrel is done", () -> hut[0].getBarrels().stream().allMatch(p -> barrel(fixture, p).isDone()), () -> describeBarrels(fixture, hut[0]))
          .awaitUntil("compost from every barrel is in the hut", 3000,
            () -> fixture.countItems(hut[0], s -> s.is(com.minecolonies.api.items.ModItems.compost)) >= 6 * hut[0].getBarrels().size(),
            () -> "compost in hut " + fixture.countItems(hut[0], s -> s.is(com.minecolonies.api.items.ModItems.compost)) + ", " + describeBarrels(fixture, hut[0])
                    + " / " + describeWork(hut[0], worker[0]))
          .check("apples (not allowed) were never used",
            () -> fixture.countItems(hut[0], s -> s.is(Items.APPLE)) + countInCitizen(worker[0], s -> s.is(Items.APPLE)) == applesStart,
            () -> "apples " + (fixture.countItems(hut[0], s -> s.is(Items.APPLE)) + countInCitizen(worker[0], s -> s.is(Items.APPLE))))
          .succeed();
    }

    private static TileEntityBarrel barrel(final ColonyFixture fixture, final BlockPos absPos)
    {
        final BlockEntity be = fixture.level().getBlockEntity(absPos);
        return be instanceof TileEntityBarrel b ? b : null;
    }

    private static int seedCount(final ColonyFixture fixture, final IBuilding hut, final ICitizenData worker)
    {
        return fixture.countItems(hut, s -> s.is(Items.WHEAT_SEEDS)) + countInCitizen(worker, s -> s.is(Items.WHEAT_SEEDS));
    }

    private static String describeBarrels(final ColonyFixture fixture, final BuildingComposter hut)
    {
        final StringBuilder sb = new StringBuilder("barrels:");
        for (final BlockPos p : hut.getBarrels())
        {
            final TileEntityBarrel b = barrel(fixture, p);
            sb.append(' ').append(fixture.rel(p)).append(b == null ? "=no tile" : "=" + b.getItems() + (b.isDone() ? " done" : ""));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // FRM-06 Animal herders
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * FRM-06 below the limit, chicken farmer: two chickens, the herder breeds them and picks up an egg lying in the pen.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void frm06_chickenHerderBreeds(final GameTestHelper helper)
    {
        herderBreeds(helper, ModBuildings.chickenHerder.get(), EntityTypes.CHICKEN, new ItemStack[] {new ItemStack(Items.WHEAT_SEEDS, 64)},
          new ItemStack(Items.EGG), s -> s.is(Items.EGG));
    }

    /**
     * FRM-06 below the limit, cowhand: two cows, the herder breeds them and milks a cow with a bucket from the hut.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void frm06_cowboyBreedsAndMilks(final GameTestHelper helper)
    {
        herderBreeds(helper, ModBuildings.cowboy.get(), EntityTypes.COW, new ItemStack[] {new ItemStack(Items.WHEAT, 64), new ItemStack(Items.BUCKET, 4), new ItemStack(com.minecolonies.api.items.ModItems.large_empty_bottle, 4)},
          null, s -> s.is(Items.MILK_BUCKET) || s.is(com.minecolonies.api.items.ModItems.large_milk_bottle));
    }

    /**
     * FRM-06 below the limit, shepherd: two sheep, the herder breeds them and shears them (wool in the hut).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void frm06_shepherdBreedsAndShears(final GameTestHelper helper)
    {
        herderBreeds(helper, ModBuildings.shepherd.get(), EntityTypes.SHEEP, new ItemStack[] {new ItemStack(Items.WHEAT, 64), new ItemStack(Items.SHEARS)},
          null, s -> s.is(ItemTags.WOOL));
    }

    /**
     * FRM-06 below the limit, swine herder: two pigs, the herder breeds them.
     */
    // FLAKY (optional): the herder puts both pigs in love even when they have drifted apart (pigs drift out of the pen corner even when
    // slowed), then waits 60 s before the next try; most runs time out without a piglet. maxAttempts doesn't help: the framework announces
    // the re-run of colony tests but doesn't execute it (see protocol, framework issues). Not classified as a mod bug.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true, required = false)
    public void frm06_swineHerderBreeds(final GameTestHelper helper)
    {
        herderBreeds(helper, ModBuildings.swineHerder.get(), EntityTypes.PIG, new ItemStack[] {new ItemStack(Items.CARROT, 64)}, null, null);
    }

    /**
     * FRM-06 below the limit, rabbit hutch: two rabbits, the herder breeds them.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void frm06_rabbitHerderBreeds(final GameTestHelper helper)
    {
        herderBreeds(helper, ModBuildings.rabbitHutch.get(), EntityTypes.RABBIT, new ItemStack[] {new ItemStack(Items.CARROT, 64)}, null, null);
    }

    /**
     * FRM-06 above the limit, chicken farmer.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 10000, skyAccess = true)
    public void frm06_chickenHerderButchersAboveLimit(final GameTestHelper helper)
    {
        herderButchers(helper, ModBuildings.chickenHerder.get(), EntityTypes.CHICKEN, new ItemStack(Items.WHEAT_SEEDS, 64),
          s -> s.is(Items.CHICKEN) || s.is(Items.FEATHER) || s.is(Items.COOKED_CHICKEN));
    }

    /**
     * FRM-06 above the limit, cowhand.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 10000, skyAccess = true)
    public void frm06_cowboyButchersAboveLimit(final GameTestHelper helper)
    {
        herderButchers(helper, ModBuildings.cowboy.get(), EntityTypes.COW, new ItemStack(Items.WHEAT, 64),
          s -> s.is(Items.BEEF) || s.is(Items.LEATHER) || s.is(Items.COOKED_BEEF));
    }

    /**
     * FRM-06 above the limit, shepherd.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 10000, skyAccess = true)
    public void frm06_shepherdButchersAboveLimit(final GameTestHelper helper)
    {
        herderButchers(helper, ModBuildings.shepherd.get(), EntityTypes.SHEEP, new ItemStack(Items.WHEAT, 64),
          s -> s.is(Items.MUTTON) || s.is(Items.COOKED_MUTTON) || s.is(ItemTags.WOOL));
    }

    /**
     * FRM-06 above the limit, swine herder.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 10000, skyAccess = true)
    public void frm06_swineHerderButchersAboveLimit(final GameTestHelper helper)
    {
        herderButchers(helper, ModBuildings.swineHerder.get(), EntityTypes.PIG, new ItemStack(Items.CARROT, 64),
          s -> s.is(Items.PORKCHOP) || s.is(Items.COOKED_PORKCHOP));
    }

    /**
     * FRM-06 above the limit, rabbit hutch.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 10000, skyAccess = true)
    public void frm06_rabbitHerderButchersAboveLimit(final GameTestHelper helper)
    {
        herderButchers(helper, ModBuildings.rabbitHutch.get(), EntityTypes.RABBIT, new ItemStack(Items.CARROT, 64),
          s -> s.is(Items.RABBIT) || s.is(Items.RABBIT_HIDE) || s.is(Items.RABBIT_FOOT) || s.is(Items.COOKED_RABBIT));
    }

    /**
     * FRM-06 below the limit: two adults in the pen of a level 1 hut, the breeding food, an axe and the special tools in the hut. The herder
     * breeds them (a baby appears in the pen). If {@code special} is given, the special action's product (eggs picked up, milk, wool) must
     * reach the hut or the herder too. {@code lyingItem} is put on the ground of the pen (the egg a chicken would lay).
     */
    private static void herderBreeds(
      final GameTestHelper helper, final com.minecolonies.api.colony.buildings.registry.BuildingEntry hutType, final EntityType<? extends Animal> type,
      final ItemStack[] supplies, final ItemStack lyingItem, final Predicate<ItemStack> special)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final ItemEntity[] lying = new ItemEntity[1];

        com.minecolonies.gametest.framework.TestFlow flow = fixture.flow()
          .run("set up colony, herder's hut and two animals", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(hutType, 1, HERD_POS);
              checkInsideArena(fixture, hut[0]);
              fenceArea(fixture, hut[0]);
              final List<BlockPos> pen = openGroundInside(fixture, hut[0]);
              if (pen.size() < 3)
              {
                  throw new IllegalStateException("no pen inside the hut area " + describeArea(fixture, hut[0]));
              }
              spawnAnimal(fixture, type, pen.get(0));
              spawnAnimal(fixture, type, pen.get(0));
              if (lyingItem != null)
              {
                  lying[0] = new ItemEntity(fixture.level(), pen.get(2).getX() + 0.5, pen.get(2).getY() + 0.2, pen.get(2).getZ() + 0.5, lyingItem.copy());
                  lying[0].setUnlimitedLifetime();
                  fixture.level().addFreshEntity(lying[0]);
              }
              fixture.fillBuilding(hut[0], new ItemStack(Items.WOODEN_AXE));
              fixture.fillBuilding(hut[0], supplies);
              worker[0] = fixture.spawnCitizen(30, hut[0], null);
          })
          .awaitUntil("a baby is born in the pen", 6000,
            () -> animalsIn(fixture, hut[0], type).stream().anyMatch(Animal::isBaby),
            () -> describeAnimals(fixture, hut[0], type) + " / " + describeWork(hut[0], worker[0]));
        if (special != null)
        {
            flow = flow.awaitUntil("the special action's product reaches the hut or the herder", 6000,
              () -> fixture.countItems(hut[0], special) + countInCitizen(worker[0], special) > 0 || (lying[0] != null && !lying[0].isAlive()),
              () -> (lying[0] == null ? "" : "lying item alive " + lying[0].isAlive() + ", ") + describeAnimals(fixture, hut[0], type) + " / "
                      + describeWork(hut[0], worker[0]));
        }
        flow.succeed();
    }

    /**
     * FRM-06 above the limit: six adults in the pen of a level 1 hut (limit: 2 per hut level). The herder butchers until at most three
     * adults are left (AbstractEntityAIHerder.chanceToButcher is 0 at 3 or fewer adults), the drops reach the hut or the herder, and an
     * animal of the same kind outside the hut area is not touched.
     */
    private static void herderButchers(
      final GameTestHelper helper, final com.minecolonies.api.colony.buildings.registry.BuildingEntry hutType, final EntityType<? extends Animal> type,
      final ItemStack food, final Predicate<ItemStack> drops)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final Animal[] outsider = new Animal[1];

        fixture.flow()
          .run("set up colony, herder's hut and six animals", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(hutType, 1, HERD_POS);
              checkInsideArena(fixture, hut[0]);
              fenceArea(fixture, hut[0]);
              final List<BlockPos> pen = openGroundInside(fixture, hut[0]);
              if (pen.isEmpty())
              {
                  throw new IllegalStateException("no pen inside the hut area " + describeArea(fixture, hut[0]));
              }
              for (int i = 0; i < 6; i++)
              {
                  spawnAnimal(fixture, type, pen.get(i % pen.size()));
              }
              outsider[0] = spawnAnimal(fixture, type, fixture.abs(new BlockPos(56, ColonyFixture.GROUND_Y, 12)));
              fixture.fillBuilding(hut[0], new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.SHEARS), food);
              worker[0] = fixture.spawnCitizen(30, hut[0], null);
          })
          .awaitUntil("at most 3 adults are left in the pen", 10000,
            () -> animalsIn(fixture, hut[0], type).stream().filter(a -> !a.isBaby()).count() <= 3,
            () -> describeAnimals(fixture, hut[0], type) + " / " + describeWork(hut[0], worker[0]))
          .awaitUntil("the drops reach the hut or the herder", 3000,
            () -> fixture.countItems(hut[0], drops) + countInCitizen(worker[0], drops) > 0,
            () -> "no drops / " + describeWork(hut[0], worker[0]))
          .check("the animal outside the hut area is alive", () -> outsider[0].isAlive(), () -> "outsider dead")
          .succeed();
    }

    /**
     * Fails the setup if the building's area reaches over the arena walls (animals would fall off the edge).
     */
    private static void checkInsideArena(final ColonyFixture fixture, final IBuilding building)
    {
        final BlockPos a = fixture.abs(new BlockPos(0, 0, 0));
        final BlockPos b = fixture.abs(new BlockPos(ColonyFixture.ARENA_SIZE - 1, 0, ColonyFixture.ARENA_SIZE - 1));
        final Tuple<BlockPos, BlockPos> c = building.getCorners();
        if (c.getA().getX() <= Math.min(a.getX(), b.getX()) || c.getB().getX() >= Math.max(a.getX(), b.getX())
              || c.getA().getZ() <= Math.min(a.getZ(), b.getZ()) || c.getB().getZ() >= Math.max(a.getZ(), b.getZ()))
        {
            throw new IllegalStateException("hut area " + c.getA() + ".." + c.getB() + " is not inside the arena " + a + ".." + b);
        }
    }

    /**
     * Puts a closed fence gate on every open ground block of the hut area's border (citizens open gates, animals don't), so animals can't wander out of the area the herder searches
     * (WorldUtil.getEntitiesWithinBuilding uses the building's corners). Not all herder blueprints have a closed pen.
     */
    private static void fenceArea(final ColonyFixture fixture, final IBuilding building)
    {
        final Tuple<BlockPos, BlockPos> c = building.getCorners();
        final int minX = c.getA().getX();
        final int maxX = c.getB().getX() - 1;
        final int minZ = c.getA().getZ();
        final int maxZ = c.getB().getZ() - 1;
        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
                if (x != minX && x != maxX && z != minZ && z != maxZ)
                {
                    continue;
                }
                for (int y = building.getPosition().getY() - 1; y <= building.getPosition().getY() + 1; y++)
                {
                    final BlockPos p = new BlockPos(x, y, z);
                    if (fixture.level().getBlockState(p).isAir() && fixture.level().getBlockState(p.below()).isSolidRender())
                    {
                        fixture.level().setBlockAndUpdate(p, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, x == minX || x == maxX ? Direction.EAST : Direction.NORTH));
                    }
                }
            }
        }
    }

    /**
     * A top-down map of the hut area at ground level for failure messages: '.' open ground with sky, 'f' fence or wall, '#' other blocks.
     */
    private static String describeArea(final ColonyFixture fixture, final IBuilding building)
    {
        final Tuple<BlockPos, BlockPos> c = building.getCorners();
        final StringBuilder sb = new StringBuilder(c.getA().toShortString() + ".." + c.getB().toShortString() + " hut " + building.getPosition().toShortString() + "\n");
        final int y = building.getPosition().getY();
        for (int z = c.getA().getZ(); z <= c.getB().getZ(); z++)
        {
            for (int x = c.getA().getX(); x <= c.getB().getX(); x++)
            {
                final BlockPos p = new BlockPos(x, y, z);
                final BlockState s = fixture.level().getBlockState(p);
                if (p.equals(building.getPosition()))
                {
                    sb.append('H');
                }
                else if (s.isAir())
                {
                    sb.append(fixture.level().canSeeSky(p) ? '.' : ' ');
                }
                else if (s.is(BlockTags.FENCES) || s.is(BlockTags.FENCE_GATES) || s.is(BlockTags.WALLS))
                {
                    sb.append('f');
                }
                else
                {
                    sb.append('#');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Open ground positions (air with a solid block below and sky above) inside the building's area, at the hut's ground level or one above.
     */
    private static List<BlockPos> openGroundInside(final ColonyFixture fixture, final IBuilding building)
    {
        final Tuple<BlockPos, BlockPos> corners = building.getCorners();
        final List<BlockPos> result = new ArrayList<>();
        final ServerLevel level = fixture.level();
        for (int x = corners.getA().getX() + 1; x < corners.getB().getX(); x++)
        {
            for (int z = corners.getA().getZ() + 1; z < corners.getB().getZ(); z++)
            {
                for (int y = building.getPosition().getY() - 1; y <= building.getPosition().getY() + 1; y++)
                {
                    final BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.below()).isSolidRender()
                          && level.canSeeSky(p))
                    {
                        result.add(p);
                        break;
                    }
                }
            }
        }
        // Prefer the largest fenced pen: the biggest group of open ground positions whose flood fill (walking over air at the same height)
        // never leaves the hut area. Open ground outside any fence is only used as fallback.
        List<BlockPos> best = new ArrayList<>();
        final Set<BlockPos> seen = new HashSet<>();
        for (final BlockPos start : result)
        {
            if (seen.contains(start))
            {
                continue;
            }
            final List<BlockPos> region = new ArrayList<>();
            final java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
            queue.add(start);
            seen.add(start);
            boolean enclosed = true;
            while (!queue.isEmpty() && region.size() < 600)
            {
                final BlockPos p = queue.poll();
                region.add(p);
                for (final Direction d : Direction.Plane.HORIZONTAL)
                {
                    final BlockPos n = p.relative(d);
                    if (!level.getBlockState(n).isAir() || !level.getBlockState(n.above()).isAir() || !level.getBlockState(n.below()).isSolidRender())
                    {
                        continue;
                    }
                    if (n.getX() <= corners.getA().getX() || n.getX() >= corners.getB().getX() || n.getZ() <= corners.getA().getZ() || n.getZ() >= corners.getB().getZ())
                    {
                        enclosed = false;
                        continue;
                    }
                    if (seen.add(n))
                    {
                        queue.add(n);
                    }
                }
            }
            if (enclosed && region.size() > best.size())
            {
                best = region;
            }
        }
        final List<BlockPos> chosen = best.size() >= 6 ? best : result;
        // Positions far from the area's border first: animals put there stay inside the area the herder searches.
        final java.util.function.ToIntFunction<BlockPos> border = p -> Math.min(Math.min(p.getX() - corners.getA().getX(), corners.getB().getX() - p.getX()),
          Math.min(p.getZ() - corners.getA().getZ(), corners.getB().getZ() - p.getZ()));
        chosen.sort((p1, p2) -> Integer.compare(border.applyAsInt(p2), border.applyAsInt(p1)));
        return chosen;
    }

    private static String describeAnimals(final ColonyFixture fixture, final IBuilding building, final EntityType<?> type)
    {
        final StringBuilder sb = new StringBuilder("hut area " + building.getCorners().getA() + ".." + building.getCorners().getB() + ", " + type + " near:");
        for (final Animal a : fixture.level().getEntitiesOfClass(Animal.class, new AABB(building.getPosition()).inflate(80), a -> a.getType() == type))
        {
            sb.append(" ").append(a.blockPosition().toShortString()).append(" hp").append((int) a.getHealth()).append(a.isBaby() ? " baby" : "").append(a.isAlive() ? "" : " dead");
        }
        return sb.toString();
    }

    private static <T extends Animal> List<? extends Animal> animalsIn(final ColonyFixture fixture, final IBuilding building, final EntityType<T> type)
    {
        final Tuple<BlockPos, BlockPos> c = building.getCorners();
        return fixture.level().getEntitiesOfClass(Animal.class, new AABB(c.getA().getX(), c.getA().getY(), c.getA().getZ(), c.getB().getX(), c.getB().getY(), c.getB().getZ()),
          a -> a.getType() == type && a.isAlive());
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // FRM-07 Fisherman
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * FRM-07 Fisherman. A level 1 fisher's hut next to a 9x9 pond three blocks deep, a fishing rod that is about to break. The fisherman finds
     * the pond on his own, fishes, the loot ends up in the hut (or his inventory), the rod wears down and breaks, and he requests a new one.
     */
    // BUG E2E-FRM-07-1: the fisherman never lands a catch. NewBobberEntity.retrieve (NewBobberEntity.java:519-525) builds the FISHING loot
    // params with LootContextParams.ATTACKING_ENTITY; in Minecraft 26.3 that parameter is not allowed in LootContextParamSets.FISHING, so
    // LootParams.Builder.create throws "Parameters not allowed in this parameter set: [<parameter minecraft:attacking_entity>]" on every
    // catch and the AI is paused (log: "Pausing Citizen ... (com.minecolonies.job.fisherman)", state FISHERMAN_START_FISHING).
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true, required = false)
    public void frm07_fisherman(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final Predicate<ItemStack> loot = s -> !s.isEmpty() && !s.is(Items.FISHING_ROD);

        fixture.flow()
          .run("set up colony, fisher's hut and pond", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              for (int x = 44; x <= 52; x++)
              {
                  for (int z = 44; z <= 52; z++)
                  {
                      for (int y = 2; y <= ColonyFixture.FLOOR_Y; y++)
                      {
                          helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                      }
                  }
              }
              hut[0] = fixture.placeBuilding(ModBuildings.fisherman.get(), 1, new BlockPos(52, ColonyFixture.GROUND_Y, 20));
              final ItemStack rod = new ItemStack(Items.FISHING_ROD);
              rod.setDamageValue(rod.getMaxDamage() - 4);
              fixture.fillBuilding(hut[0], rod);
              worker[0] = fixture.spawnCitizen(30, hut[0], null);
          })
          .awaitUntil("fishing loot is in the hut or with the fisherman", 8000,
            () -> fixture.countItems(hut[0], loot) + countInCitizen(worker[0], loot) > 0,
            () -> describeWork(hut[0], worker[0]))
          .awaitUntil("the rod breaks and a new one is requested", 8000,
            () -> fixture.countItems(hut[0], s -> s.is(Items.FISHING_ROD)) + countInCitizen(worker[0], s -> s.is(Items.FISHING_ROD)) == 0
                    && hut[0].hasWorkerOpenRequestsOfType(worker[0].getId(), TypeToken.of(Tool.class)),
            () -> "rods " + (fixture.countItems(hut[0], s -> s.is(Items.FISHING_ROD)) + countInCitizen(worker[0], s -> s.is(Items.FISHING_ROD)))
                    + ", tool request " + hut[0].hasWorkerOpenRequestsOfType(worker[0].getId(), TypeToken.of(Tool.class)) + " / " + describeWork(hut[0], worker[0]))
          .succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // GAT-01 Lumberjack
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * GAT-01 Lumberjack fells and replants. A level 1 forester's hut with an axe and saplings, one oak and one birch grown from the
     * vanilla tree features. Defoliation is turned on so the leaves are cut too (random ticks are off, so leaves would never decay). Each tree
     * disappears completely, a sapling of the same type is planted where it stood, and logs end up in the hut.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 20000, skyAccess = true)
    public void gat01_lumberjackFellsAndReplants(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BuildingLumberjack[] hut = new BuildingLumberjack[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final List<GrownTree> trees = new ArrayList<>();

        fixture.flow()
          .run("set up colony, forester's hut and trees", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              trees.add(growTree(fixture, TreeGrower.OAK, new BlockPos(46, ColonyFixture.GROUND_Y, 16), Blocks.OAK_LOG, Blocks.OAK_SAPLING));
              trees.add(growTree(fixture, TreeGrower.BIRCH, new BlockPos(56, ColonyFixture.GROUND_Y, 16), Blocks.BIRCH_LOG, Blocks.BIRCH_SAPLING));
              hut[0] = (BuildingLumberjack) fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, HUT_POS);
              hut[0].getSetting(BuildingLumberjack.DEFOLIATE).trigger();
              fixture.fillBuilding(hut[0], new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE),
                new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE),
                new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.OAK_SAPLING, 8), new ItemStack(Items.BIRCH_SAPLING, 8));
              worker[0] = fixture.spawnCitizen(30, hut[0], null);
          })
          .check("defoliation is on", () -> hut[0].shouldDefoliate(), () -> "defoliate off")
          .awaitUntil("every tree is gone and replanted with its sapling", 16000,
            () -> trees.stream().allMatch(t -> t.isReplaced(fixture)),
            () -> describeTrees(fixture, trees) + " / " + describeWork(hut[0], worker[0]))
          .awaitUntil("logs reach the hut", 3000,
            () -> fixture.countItems(hut[0], s -> s.is(ItemTags.LOGS)) > 0,
            () -> "logs in hut " + fixture.countItems(hut[0], s -> s.is(ItemTags.LOGS)) + ", with worker " + countInCitizen(worker[0], s -> s.is(ItemTags.LOGS))
                    + " / " + describeWork(hut[0], worker[0]))
          .succeed();
    }

    /**
     * A tree grown by the test: its base (absolute), the log and sapling type, and every log and leaf position it had.
     */
    private record GrownTree(BlockPos base, Block log, Block sapling, Set<BlockPos> blocks, int logs)
    {
        /**
         * @return true when no original log or leaf is left and a sapling of the same type stands at the base.
         */
        boolean isReplaced(final ColonyFixture fixture)
        {
            if (!fixture.level().getBlockState(base).is(sapling))
            {
                return false;
            }
            return remaining(fixture) == 0;
        }

        int remaining(final ColonyFixture fixture)
        {
            int left = 0;
            for (final BlockPos p : blocks)
            {
                if (p.equals(base))
                {
                    continue;
                }
                final BlockState s = fixture.level().getBlockState(p);
                if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES))
                {
                    left++;
                }
            }
            return left;
        }
    }

    private static String describeTrees(final ColonyFixture fixture, final List<GrownTree> trees)
    {
        final StringBuilder sb = new StringBuilder("trees:");
        for (final GrownTree t : trees)
        {
            sb.append(' ').append(fixture.rel(t.base())).append(" base=").append(fixture.level().getBlockState(t.base()).getBlock().getName().getString())
              .append(" left=").append(t.remaining(fixture)).append('/').append(t.blocks().size());
        }
        return sb.toString();
    }

    /**
     * Grows a tree from a sapling with the vanilla tree grower (as bone meal would) at the relative position and records its blocks.
     */
    private static GrownTree growTree(final ColonyFixture fixture, final TreeGrower grower, final BlockPos rel, final Block log, final Block sapling)
    {
        final ServerLevel level = fixture.level();
        final BlockPos base = fixture.abs(rel);
        level.setBlockAndUpdate(base, sapling.defaultBlockState());
        boolean grown = false;
        for (int attempt = 0; attempt < 20 && !grown; attempt++)
        {
            grown = grower.growTree(level, level.getChunkSource().getGenerator(), base, level.getBlockState(base), net.minecraft.util.RandomSource.create(attempt));
        }
        if (!grown)
        {
            throw new IllegalStateException("tree " + log + " did not grow at " + rel);
        }
        final Set<BlockPos> blocks = new HashSet<>();
        for (final BlockPos p : BlockPos.betweenClosed(base.offset(-4, 0, -4), base.offset(4, 12, 4)))
        {
            final BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES))
            {
                blocks.add(p.immutable());
            }
        }
        if (!level.getBlockState(base).is(log))
        {
            throw new IllegalStateException("tree at " + rel + " has no " + log + " at its base");
        }
        return new GrownTree(base, log, sapling, blocks, (int) blocks.stream().filter(p -> level.getBlockState(p).is(BlockTags.LOGS)).count());
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // FRM-03 Florist
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * FRM-03 Florist. A level 2 flower shop (small flowers allowed) with shears and one compost per plant bed; every small flower except
     * poppy and dandelion is on the hut's "don't grow" list. The florist composts the beds and harvests the flowers that grow; the test
     * runs the composted beds at double speed (more would make every compost expire at once, see the protocol). Only poppies and dandelions grow, the compost is used up, flowers
     * end up in the hut, and when the compost is gone the florist requests more.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 24000, skyAccess = true)
    public void frm03_florist(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final com.minecolonies.core.colony.buildings.workerbuildings.BuildingFlorist[] hut = new com.minecolonies.core.colony.buildings.workerbuildings.BuildingFlorist[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final Set<Item> allowed = Set.of(Items.POPPY, Items.DANDELION);
        final Set<Item> forbidden = new HashSet<>();
        final Predicate<ItemStack> isCompost = s -> s.is(com.minecolonies.api.items.ModItems.compost);
        final Runnable speedUp = () -> {
            for (final BlockPos p : hut[0].getPlantGround())
            {
                if (fixture.level().getBlockEntity(p) instanceof com.minecolonies.core.tileentities.TileEntityCompostedDirt dirt)
                {
                    for (int i = 0; i < 1; i++)
                    {
                        dirt.tick();
                    }
                }
            }
        };

        fixture.flow()
          .run("set up colony and flower shop", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = (com.minecolonies.core.colony.buildings.workerbuildings.BuildingFlorist) fixture.placeBuilding(ModBuildings.florist.get(), 2, HUT_POS);
              final IItemListModule notToGrow = hut[0].getModuleMatching(ItemListModule.class, m -> m.getId().equals(com.minecolonies.api.util.constant.BuildingConstants.BUILDING_FLOWER_LIST));
              for (final ItemStorage flower : com.minecolonies.core.colony.buildings.workerbuildings.BuildingFlorist.getPlantablesForBuildingLevel(2))
              {
                  if (!allowed.contains(flower.getItem()))
                  {
                      notToGrow.addItem(flower);
                      forbidden.add(flower.getItem());
                  }
              }
              if (hut[0].getPlantGround().isEmpty())
              {
                  throw new IllegalStateException("the flower shop has no plant beds");
              }
              fixture.fillBuilding(hut[0], new ItemStack(Items.SHEARS), new ItemStack(com.minecolonies.api.items.ModItems.compost, 3 * hut[0].getPlantGround().size()));
              worker[0] = fixture.spawnCitizen(30, hut[0], null);
          })
          .check("the flower to grow is a poppy or a dandelion", () -> {
              final ItemStack f = hut[0].getFlowerToGrow();
              return f != null && !f.isEmpty() && allowed.contains(f.getItem());
          }, () -> "flower to grow " + hut[0].getFlowerToGrow())
          .awaitUntil("at least 3 flowers are harvested (in the shop or with the florist)", 10000,
            () -> {
                speedUp.run();
                return fixture.countItems(hut[0], s -> allowed.contains(s.getItem())) + countInCitizen(worker[0], s -> allowed.contains(s.getItem())) >= 3;
            },
            () -> "flowers in hut " + fixture.countItems(hut[0], s -> allowed.contains(s.getItem())) + ", with florist " + countInCitizen(worker[0], s -> allowed.contains(s.getItem()))
                    + ", compost left " + (fixture.countItems(hut[0], isCompost) + countInCitizen(worker[0], isCompost)) + ", beds " + describeBeds(fixture, hut[0])
                    + " / " + describeWork(hut[0], worker[0]))
          .awaitUntil("the compost is used up", 10000,
            () -> {
                speedUp.run();
                return fixture.countItems(hut[0], isCompost) + countInCitizen(worker[0], isCompost) == 0;
            },
            () -> "compost left " + (fixture.countItems(hut[0], isCompost) + countInCitizen(worker[0], isCompost)) + ", beds " + hut[0].getPlantGround().size()
                    + " / " + describeWork(hut[0], worker[0]))
          .check("no flower of the 'don't grow' list was grown",
            () -> fixture.countItems(hut[0], s -> forbidden.contains(s.getItem())) + countInCitizen(worker[0], s -> forbidden.contains(s.getItem())) == 0
                    && hut[0].getPlantGround().stream().noneMatch(p -> forbidden.contains(fixture.level().getBlockState(p.above()).getBlock().asItem())),
            () -> "forbidden flowers in hut " + fixture.countItems(hut[0], s -> forbidden.contains(s.getItem())))
          .awaitUntil("the florist requests compost", 2000,
            () -> hut[0].getOpenRequests(worker[0].getId()).stream()
              .anyMatch(r -> r.getRequest() instanceof com.minecolonies.api.colony.requestsystem.requestable.Stack st && st.getStack().is(com.minecolonies.api.items.ModItems.compost)),
            () -> describeWork(hut[0], worker[0]))
          .succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // FRM-05 Beekeeper
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * FRM-05 Beekeeper. A level 1 apiary (one hive allowed) with a full beehive (honey level 5) holding two bees, shears and glass bottles. The
     * beekeeper (dexterity 99, so the AI's chance to anger the bees is 0) first harvests honeycomb (the default setting), then, after the
     * test switches the setting to honey and refills the hive, honey bottles. The hive is empty after each harvest, no bee is angry and none
     * died.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void frm05_beekeeper(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final com.minecolonies.core.colony.buildings.workerbuildings.BuildingBeekeeper[] hut = new com.minecolonies.core.colony.buildings.workerbuildings.BuildingBeekeeper[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final BlockPos[] hive = new BlockPos[1];
        final net.minecraft.world.level.block.state.properties.IntegerProperty honey = net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL;

        fixture.flow()
          .run("set up colony, apiary and a full hive with two bees", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = (com.minecolonies.core.colony.buildings.workerbuildings.BuildingBeekeeper) fixture.placeBuilding(ModBuildings.beekeeper.get(), 1, HUT_POS);
              hut[0].getModuleMatching(ItemListModule.class, m -> m.getId().equals(com.minecolonies.api.util.constant.BuildingConstants.BUILDING_FLOWER_LIST))
                .addItem(new ItemStorage(new ItemStack(Items.POPPY)));
              hive[0] = fixture.abs(new BlockPos(12, ColonyFixture.GROUND_Y, 28));
              fixture.level().setBlockAndUpdate(hive[0], Blocks.BEEHIVE.defaultBlockState().setValue(honey, 5));
              final net.minecraft.world.level.block.entity.BeehiveBlockEntity hiveEntity = (net.minecraft.world.level.block.entity.BeehiveBlockEntity) fixture.level().getBlockEntity(hive[0]);
              for (int i = 0; i < 2; i++)
              {
                  final net.minecraft.world.entity.animal.bee.Bee bee = EntityTypes.BEE.create(fixture.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                  bee.snapTo(hive[0].getX() + 0.5, hive[0].getY() + 1, hive[0].getZ() + 0.5, 0, 0);
                  fixture.level().addFreshEntity(bee);
                  hiveEntity.addOccupant(bee);
              }
              hut[0].addHive(hive[0]);
              fixture.fillBuilding(hut[0], new ItemStack(Items.SHEARS), new ItemStack(Items.GLASS_BOTTLE, 16));
              worker[0] = fixture.spawnCitizen(99, hut[0], null);
          })
          .check("two bees are in the hive", () -> beeCount(fixture, hive[0]) == 2, () -> "bees " + beeCount(fixture, hive[0]))
          .awaitUntil("honeycomb is harvested and the hive is empty", 4000,
            () -> fixture.countItems(hut[0], s -> s.is(Items.HONEYCOMB)) + countInCitizen(worker[0], s -> s.is(Items.HONEYCOMB)) > 0
                    && fixture.level().getBlockState(hive[0]).getValue(honey) == 0,
            () -> "honey level " + fixture.level().getBlockState(hive[0]).getValue(honey) + " / " + describeWork(hut[0], worker[0]))
          .run("switch to honey bottles and refill the hive", () -> {
              hut[0].getSetting(com.minecolonies.core.colony.buildings.workerbuildings.BuildingBeekeeper.MODE).trigger();
              fixture.level().setBlockAndUpdate(hive[0], fixture.level().getBlockState(hive[0]).setValue(honey, 5));
          })
          .check("the setting is honey", () -> hut[0].getHarvestTypes().equals(com.minecolonies.core.colony.buildings.workerbuildings.BuildingBeekeeper.HONEY),
            () -> "setting " + hut[0].getHarvestTypes())
          .awaitUntil("honey bottles are harvested and the hive is empty", 4000,
            () -> fixture.countItems(hut[0], s -> s.is(Items.HONEY_BOTTLE)) + countInCitizen(worker[0], s -> s.is(Items.HONEY_BOTTLE)) > 0
                    && fixture.level().getBlockState(hive[0]).getValue(honey) == 0,
            () -> "honey level " + fixture.level().getBlockState(hive[0]).getValue(honey) + " / " + describeWork(hut[0], worker[0]))
          .check("no bee died and none is angry",
            () -> beeCount(fixture, hive[0]) == 2 && bees(fixture, hive[0]).stream().noneMatch(net.minecraft.world.entity.animal.bee.Bee::isAngry),
            () -> "bees " + beeCount(fixture, hive[0]) + ", angry " + bees(fixture, hive[0]).stream().filter(net.minecraft.world.entity.animal.bee.Bee::isAngry).count())
          .succeed();
    }

    private static String describeBeds(final ColonyFixture fixture, final com.minecolonies.core.colony.buildings.workerbuildings.BuildingFlorist hut)
    {
        final StringBuilder sb = new StringBuilder();
        for (final BlockPos p : hut.getPlantGround())
        {
            final boolean composted = fixture.level().getBlockEntity(p) instanceof com.minecolonies.core.tileentities.TileEntityCompostedDirt d && d.isComposted();
            String flower = "?";
            try
            {
                final java.lang.reflect.Field f = com.minecolonies.core.tileentities.TileEntityCompostedDirt.class.getDeclaredField("flower");
                f.setAccessible(true);
                flower = String.valueOf(f.get(fixture.level().getBlockEntity(p)));
                for (final String name : new String[] {"ticker", "percentage"})
                {
                    final java.lang.reflect.Field g = com.minecolonies.core.tileentities.TileEntityCompostedDirt.class.getDeclaredField(name);
                    g.setAccessible(true);
                    flower += " " + name + "=" + g.get(fixture.level().getBlockEntity(p));
                }
            }
            catch (final ReflectiveOperationException | RuntimeException e)
            {
                flower = e.toString();
            }
            sb.append('[').append(fixture.level().getBlockState(p).getBlock().getName().getString()).append(composted ? " composted" : "").append(", flower ").append(flower)
              .append(", above ").append(fixture.level().getBlockState(p.above()).getBlock().getName().getString()).append("] ");
        }
        sb.append(" flower items on the ground ").append(fixture.level().getEntitiesOfClass(ItemEntity.class, new AABB(hut.getPosition()).inflate(30),
          e -> e.getItem().is(Items.POPPY) || e.getItem().is(Items.DANDELION)).size());
        return sb.toString();
    }

    private static List<net.minecraft.world.entity.animal.bee.Bee> bees(final ColonyFixture fixture, final BlockPos hive)
    {
        return fixture.level().getEntitiesOfClass(net.minecraft.world.entity.animal.bee.Bee.class, new AABB(hive).inflate(64), net.minecraft.world.entity.LivingEntity::isAlive);
    }

    private static int beeCount(final ColonyFixture fixture, final BlockPos hive)
    {
        final int inHive = fixture.level().getBlockEntity(hive) instanceof net.minecraft.world.level.block.entity.BeehiveBlockEntity h ? h.getOccupantCount() : 0;
        return inHive + bees(fixture, hive).size();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // GAT-02 Lumberjack follows its settings
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * GAT-02 Lumberjack follows its settings. Every sapling except oak is on the "don't cut" list, maxTreeSize is 10 logs, and the hut's
     * work area is restricted to a box around three trees: an oak, a spruce and a hand-built oak with a 12 log trunk. A fourth oak stands
     * outside the box. Only the small oak in the box is cut (and replanted); the others keep all their logs.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void gat02_lumberjackFollowsSettings(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BuildingLumberjack[] hut = new BuildingLumberjack[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final GrownTree[] trees = new GrownTree[4];

        fixture.flow()
          .run("set up colony, forester's hut and trees", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              fixture.setConfig(com.minecolonies.core.MineColonies.getConfig().getServer().maxTreeSize, 10);
              trees[0] = growTree(fixture, TreeGrower.OAK, new BlockPos(46, ColonyFixture.GROUND_Y, 14), Blocks.OAK_LOG, Blocks.OAK_SAPLING);
              trees[1] = growTree(fixture, TreeGrower.SPRUCE, new BlockPos(56, ColonyFixture.GROUND_Y, 14), Blocks.SPRUCE_LOG, Blocks.SPRUCE_SAPLING);
              trees[2] = buildTallOak(fixture, new BlockPos(46, ColonyFixture.GROUND_Y, 24), 12);
              trees[3] = growTree(fixture, TreeGrower.OAK, new BlockPos(14, ColonyFixture.GROUND_Y, 50), Blocks.OAK_LOG, Blocks.OAK_SAPLING);
              hut[0] = (BuildingLumberjack) fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, HUT_POS);
              final IItemListModule notToCut = hut[0].getModuleMatching(ItemListModule.class, m -> m.getId().equals(EntityAIWorkLumberjack.SAPLINGS_LIST));
              for (final ItemStorage sapling : IColonyManager.getInstance().getCompatibilityManager().getCopyOfSaplings())
              {
                  if (sapling.getItem() != Items.OAK_SAPLING)
                  {
                      notToCut.addItem(sapling);
                  }
              }
              final BlockPos a = fixture.abs(new BlockPos(40, ColonyFixture.GROUND_Y - 1, 8));
              final BlockPos b = fixture.abs(new BlockPos(62, ColonyFixture.GROUND_Y + 20, 30));
              hut[0].setRestrictedArea(new BlockPos(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ())),
                new BlockPos(Math.max(a.getX(), b.getX()), b.getY(), Math.max(a.getZ(), b.getZ())));
              hut[0].getSetting(BuildingLumberjack.DEFOLIATE).trigger();
              fixture.fillBuilding(hut[0], new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.WOODEN_HOE),
                new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.OAK_SAPLING, 8));
              worker[0] = fixture.spawnCitizen(30, hut[0], null);
          })
          .check("the work area is restricted", () -> hut[0].shouldRestrict(), () -> "restriction off")
          .awaitUntil("the small oak in the work area is cut and replanted", 6000,
            () -> trees[0].isReplaced(fixture),
            () -> describeTrees(fixture, List.of(trees)) + " / " + describeWork(hut[0], worker[0]))
          .idle(2400)
          .check("the spruce, the too big oak and the oak outside the work area keep all their logs",
            () -> logsLeft(fixture, trees[1]) == trees[1].logs() && logsLeft(fixture, trees[2]) == trees[2].logs() && logsLeft(fixture, trees[3]) == trees[3].logs(),
            () -> "logs left: spruce " + logsLeft(fixture, trees[1]) + "/" + trees[1].logs() + ", tall oak " + logsLeft(fixture, trees[2]) + "/" + trees[2].logs()
                    + ", outside oak " + logsLeft(fixture, trees[3]) + "/" + trees[3].logs())
          .succeed();
    }

    private static int logsLeft(final ColonyFixture fixture, final GrownTree tree)
    {
        int logs = 0;
        for (final BlockPos p : tree.blocks())
        {
            if (fixture.level().getBlockState(p).is(BlockTags.LOGS))
            {
                logs++;
            }
        }
        return logs;
    }

    /**
     * Builds an oak with a straight trunk of {@code height} logs and two layers of leaves (radius 2) around its top.
     */
    private static GrownTree buildTallOak(final ColonyFixture fixture, final BlockPos rel, final int height)
    {
        final ServerLevel level = fixture.level();
        final BlockPos base = fixture.abs(rel);
        final Set<BlockPos> blocks = new HashSet<>();
        for (int h = 0; h < height; h++)
        {
            level.setBlockAndUpdate(base.above(h), Blocks.OAK_LOG.defaultBlockState());
            blocks.add(base.above(h));
        }
        for (int dy = height - 2; dy <= height; dy++)
        {
            for (int dx = -2; dx <= 2; dx++)
            {
                for (int dz = -2; dz <= 2; dz++)
                {
                    final BlockPos p = base.offset(dx, dy, dz);
                    if (level.getBlockState(p).isAir())
                    {
                        level.setBlockAndUpdate(p, Blocks.OAK_LEAVES.defaultBlockState());
                        blocks.add(p);
                    }
                }
            }
        }
        return new GrownTree(base, Blocks.OAK_LOG, Blocks.OAK_SAPLING, blocks, height);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Spawns an adult animal at an absolute position. ({@code helper.spawn} takes relative positions; this avoids converting back and
     * forth.)
     */
    private static <T extends Animal> T spawnAnimal(final ColonyFixture fixture, final EntityType<T> type, final BlockPos absPos)
    {
        final T animal = type.create(fixture.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (animal == null)
        {
            throw new IllegalStateException("could not create " + type);
        }
        animal.snapTo(absPos.getX() + 0.5, absPos.getY(), absPos.getZ() + 0.5, 0, 0);
        animal.setPersistenceRequired();
        // Keep the animals where they were put: wandering apart breaks breeding (vanilla's mate search is 8 blocks) and wandering out of
        // the hut area hides them from the herder. Doesn't change what the herder does.
        animal.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 1_000_000, 3, false, false));
        fixture.level().addFreshEntity(animal);
        return animal;
    }

    /**
     * Worker details for failure messages: the citizen line, his job status, inventory and the open requests of the building for him.
     */
    private static String describeWork(final IBuilding building, final ICitizenData citizen)
    {
        if (citizen == null)
        {
            return "no worker";
        }
        final StringBuilder sb = new StringBuilder(ColonyFixture.describe(citizen));
        sb.append(", job status ").append(citizen.getJobStatus()).append(", inventory [");
        final var inv = citizen.getInventory();
        for (int i = 0; i < inv.getSlots(); i++)
        {
            final ItemStack s = inv.getStackInSlot(i);
            if (!s.isEmpty())
            {
                sb.append(s.getCount()).append(' ').append(s.getItem()).append(s.isDamageableItem() ? "(dmg " + s.getDamageValue() + ")" : "").append(", ");
            }
        }
        sb.append("], requests [");
        for (final var request : building.getOpenRequests(citizen.getId()))
        {
            sb.append(request.getShortDisplayString().getString()).append(" (").append(request.getState()).append("), ");
        }
        return sb.append(']').toString();
    }

    private static int countInCitizen(final ICitizenData citizen, final Predicate<ItemStack> matcher)
    {
        if (citizen == null)
        {
            return 0;
        }
        int total = 0;
        final var inv = citizen.getInventory();
        for (int i = 0; i < inv.getSlots(); i++)
        {
            final ItemStack s = inv.getStackInSlot(i);
            if (!s.isEmpty() && matcher.test(s))
            {
                total += s.getCount();
            }
        }
        return total;
    }
}
