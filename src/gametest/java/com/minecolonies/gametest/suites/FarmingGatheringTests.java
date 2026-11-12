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
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner;
import com.minecolonies.core.colony.buildings.modules.MinerLevelManagementModule;
import com.minecolonies.core.entity.ai.workers.util.MineNode;
import com.minecolonies.core.entity.ai.workers.util.MinerLevel;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.Vec2i;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkComposter;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack;
import com.minecolonies.core.tileentities.TileEntityBarrel;
import com.minecolonies.gametest.framework.ColonyFixture;
import com.google.common.reflect.TypeToken;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void frm01_farmerTillsGrass(final GameTestHelper helper)
    {
        farmerFieldCycle(helper, Blocks.GRASS_BLOCK, false);
    }

    /**
     * FRM-01c Farmer tills other soils. Same as FRM-01b on a field of coarse dirt, rooted dirt, podzol and mycelium (in turn). On 1.21.1 all
     * of them were in #minecraft:dirt and the farmer turned them straight into farmland; in 26.3 they are only in
     * #minecraft:substrate_overworld (and vanilla's hoe makes coarse/rooted dirt plain dirt first). Every cell must end up tilled and planted.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void frm01_farmerTillsOtherSoils(final GameTestHelper helper)
    {
        farmerFieldCycle(helper, new Block[] {Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.PODZOL, Blocks.MYCELIUM}, false);
    }

    private static void farmerFieldCycle(final GameTestHelper helper, final Block ground, final boolean fullCycle)
    {
        farmerFieldCycle(helper, new Block[] {ground}, fullCycle);
    }

    /**
     * @param grounds the field's soil; cell i gets {@code grounds[i % grounds.length]}.
     */
    private static void farmerFieldCycle(final GameTestHelper helper, final Block[] grounds, final boolean fullCycle)
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
              for (int i = 0; i < cells.size(); i++)
              {
                  helper.setBlock(cells.get(i), grounds[i % grounds.length]);
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
        final List<String> notFarmland = new ArrayList<>();
        for (final BlockPos c : cells)
        {
            if (isFarmland(helper, c))
            {
                farmland++;
            }
            else
            {
                notFarmland.add(c.toShortString() + " " + BuiltInRegistries.BLOCK.getKey(helper.getBlockState(c).getBlock()).getPath()
                                  + " under " + BuiltInRegistries.BLOCK.getKey(helper.getBlockState(c.above()).getBlock()).getPath());
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
        return cells.size() + " cells: farmland " + farmland + ", wheat " + wheat + " (ripe " + ripe + ")"
                 + (notFarmland.isEmpty() || notFarmland.size() > 4 ? "" : ", not farmland " + notFarmland);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // FRM-02 Planter: plantation
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * FRM-02a Planter, sugar cane field.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 45000, skyAccess = true)
    public void frm02_planterSugarCane(final GameTestHelper helper)
    {
        plantationField(helper, "sugar", Blocks.SUGAR_CANE, Items.SUGAR_CANE, 3, null);
    }

    /**
     * FRM-02b Planter, cactus field.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 45000, skyAccess = true)
    public void frm02_planterCactus(final GameTestHelper helper)
    {
        plantationField(helper, "cactus", Blocks.CACTUS, Items.CACTUS, 3, null);
    }

    /**
     * FRM-02c Planter, bamboo field (needs the research "jungle master"; the planter harvests bamboo of at least 6 blocks).
     */
    // Budget 40,000 (shared wait): bamboo took 11,200-19,700 ticks and once ran past the old 20,000.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 45000, skyAccess = true)
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
          .awaitUntil("all but at most one grown plant are cut above the base (no base is lost) and all but one empty position replanted", 40000,
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
    // Was optional (FLAKY): the pigs drifted more than 8 blocks apart and the herder fed them anyway (E2E-FRM-06-1), then waited 60 s.
    // keepWithinMateRange fixes this on the test side: 100 of 100 repeated runs passed (at most 1,194 ticks).
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
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
        final BlockPos[] penSpot = new BlockPos[1];

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
              penSpot[0] = pen.get(0);
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
            () -> {
                keepWithinMateRange(fixture, hut[0], type, penSpot[0]);
                return animalsIn(fixture, hut[0], type).stream().anyMatch(Animal::isBaby);
            },
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
     * Vanilla's mate search ({@code BreedGoal}) only looks 8 blocks around an animal in love. The test animals are slowed so they stay in the
     * pen, which also means two animals that have wandered more than 8 blocks apart stay apart for a long time. The herder still feeds such
     * a pair (bug E2E-FRM-06-1, fixed), so they never meet, and he waits 60 s before the next try; with bad luck the budget runs out
     * (seen once in a full run, 5,148 ticks in 1 of 200 repeated runs). While neither animal is in love and they are more than 6 blocks
     * apart, the second one is put back next to the first. An animal that got out of the hut area anyway (pigs once slipped past the
     * fence gates; the herder only sees animals inside the area) is put back on {@code penSpot}. Doesn't change what the herder does;
     * {@link #frm06_herderFeedsPairOutOfMateRange} covers the bug itself.
     */
    private static void keepWithinMateRange(final ColonyFixture fixture, final IBuilding hut, final EntityType<? extends Animal> type, final BlockPos penSpot)
    {
        final Tuple<BlockPos, BlockPos> c = hut.getCorners();
        final AABB area = new AABB(c.getA().getX(), c.getA().getY(), c.getA().getZ(), c.getB().getX(), c.getB().getY(), c.getB().getZ());
        for (final Animal outside : fixture.level().getEntitiesOfClass(Animal.class, area.inflate(16),
          a -> a.getType() == type && a.isAlive() && !area.intersects(a.getBoundingBox())))
        {
            outside.teleportTo(penSpot.getX() + 0.5, penSpot.getY(), penSpot.getZ() + 0.5);
        }
        final List<? extends Animal> adults = animalsIn(fixture, hut, type).stream().filter(a -> !a.isBaby()).toList();
        if (adults.size() == 2 && !adults.get(0).isInLove() && !adults.get(1).isInLove() && adults.get(0).distanceTo(adults.get(1)) > 6)
        {
            final Animal first = adults.get(0);
            adults.get(1).teleportTo(first.getX(), first.getY(), first.getZ());
        }
    }

    /**
     * FRM-06 below the limit, the mate range: two chickens 9 blocks apart in the hut area, slowed to a standstill so they stay there.
     * Vanilla's BreedGoal finds a partner only within 8 blocks; the herder used to pair animals up to 10 blocks apart
     * (AbstractEntityAIHerder.DISTANCE_TO_BREED, E2E-FRM-06-1, fixed: now 8). Expected: in 3000 ticks the herder never puts both in love while they can't find each other.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 6000, skyAccess = true)
    public void frm06_herderFeedsPairOutOfMateRange(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final String[] fedOutOfRange = new String[1];

        fixture.flow()
          .run("set up colony, chicken farmer's hut and two chickens 9 blocks apart", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.chickenHerder.get(), 1, HERD_POS);
              checkInsideArena(fixture, hut[0]);
              fenceArea(fixture, hut[0]);
              // Every open ground position of the (fenced) hut area, not only the largest enclosed pen, so two positions 9 blocks apart exist.
              final List<BlockPos> pen = new ArrayList<>();
              final Tuple<BlockPos, BlockPos> c = hut[0].getCorners();
              for (int x = c.getA().getX() + 2; x < c.getB().getX() - 1; x++)
              {
                  for (int z = c.getA().getZ() + 2; z < c.getB().getZ() - 1; z++)
                  {
                      for (int y = hut[0].getPosition().getY() - 2; y <= hut[0].getPosition().getY() + 1; y++)
                      {
                          final BlockPos p = new BlockPos(x, y, z);
                          if (fixture.level().getBlockState(p).isAir() && fixture.level().getBlockState(p.above()).isAir()
                                && fixture.level().getBlockState(p.below()).isSolidRender())
                          {
                              pen.add(p);
                              break;
                          }
                      }
                  }
              }
              BlockPos a = null;
              BlockPos b = null;
              for (final BlockPos p : pen)
              {
                  for (final BlockPos q : pen)
                  {
                      final double d = Math.sqrt(p.distSqr(q));
                      if (a == null && p.getY() == q.getY() && d >= 8.6 && d <= 9.6)
                      {
                          a = p;
                          b = q;
                      }
                  }
              }
              if (a == null)
              {
                  throw new IllegalStateException("no two pen positions 9 blocks apart " + describeArea(fixture, hut[0]) + " open ground " + pen.stream().map(fixture::rel).toList());
              }
              for (final BlockPos p : List.of(a, b))
              {
                  // Can't walk at all (speed 0): the two stay 9 blocks apart. Breeding needs no walking from the herder's side.
                  spawnAnimal(fixture, EntityTypes.CHICKEN, p)
                    .addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 1_000_000, 100, false, false));
              }
              fixture.fillBuilding(hut[0], new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WHEAT_SEEDS, 64));
              worker[0] = fixture.spawnCitizen(30, hut[0], null);
          })
          .awaitUntil("3000 ticks pass, or the herder puts both chickens in love while they are out of mate range", 3100,
            () -> {
                final List<? extends Animal> adults = animalsIn(fixture, hut[0], EntityTypes.CHICKEN).stream().filter(c -> !c.isBaby()).toList();
                if (fedOutOfRange[0] == null && adults.size() == 2 && adults.get(0).isInLove() && adults.get(1).isInLove()
                      && adults.get(0).distanceTo(adults.get(1)) > 8.0)
                {
                    fedOutOfRange[0] = String.format("both in love %.1f blocks apart on tick %d", adults.get(0).distanceTo(adults.get(1)), helper.getTick());
                }
                return fedOutOfRange[0] != null || helper.getTick() >= 3000;
            },
            () -> describeAnimals(fixture, hut[0], EntityTypes.CHICKEN) + " / " + describeWork(hut[0], worker[0]))
          .check("the herder only fed chickens that can find each other (vanilla mate search: 8 blocks)", () -> fedOutOfRange[0] == null,
            () -> fedOutOfRange[0] + ", " + describeAnimals(fixture, hut[0], EntityTypes.CHICKEN))
          .succeed();
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
     * A top-down map of the hut area at ground level for failure messages: '.' air with sky light, ' ' air without (the light may lag behind a paste), 'f' fence or wall, '#' other blocks.
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
     * Open ground positions (two blocks of air with a solid block below) inside the building's area, at the hut's ground level or one above.
     * <p>
     * No sky check: {@code Level.canSeeSky} reads the sky light, and the light engine updates the pasted hut asynchronously. Mostly it hadn't
     * yet, so every position counted as under the sky and the animals went into the chicken farmer's fenced, roofed coop (the only open
     * ground at this height: its pen is sunk two blocks lower). When the light was already updated under full-run load, the coop counted as
     * covered and the setup failed with "no pen inside the hut area" (E2E flaky test, round 2). Without the check the result is the one of
     * the passing runs, every time.
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
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.below()).isSolidRender())
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
            sb.append(" ").append(a.blockPosition().toShortString()).append(" hp").append((int) a.getHealth()).append(a.isBaby() ? " baby" : "").append(a.isInLove() ? " in love" : "").append(a.isAlive() ? "" : " dead");
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
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
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
     * FRM-03 Compost on a bed that sat idle (E2E-FRM-03-1): a composted dirt block that was not composted for more than the compost's
     * lifetime (300 s, simulated by ticking its block entity directly) gets compost; the compost must still be active a few seconds later.
     * Was a bug: the timer counted while the bed was idle, so the first update after composting expired the new compost at once.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 400, skyAccess = true)
    public void frm03_compostLastsOnIdleBed(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BlockPos bed = fixture.abs(ColonyFixture.ARENA_CENTER.offset(3, 0, 3));
        final com.minecolonies.core.tileentities.TileEntityCompostedDirt[] dirt = new com.minecolonies.core.tileentities.TileEntityCompostedDirt[1];

        fixture.flow()
          .run("place a plant bed and let it sit idle for 301 s", () -> {
              fixture.level().setBlockAndUpdate(bed, com.minecolonies.api.blocks.ModBlocks.blockCompostedDirt.defaultBlockState());
              fixture.level().setBlockAndUpdate(bed.above(), Blocks.AIR.defaultBlockState());
              if (!(fixture.level().getBlockEntity(bed) instanceof final com.minecolonies.core.tileentities.TileEntityCompostedDirt d))
              {
                  throw new IllegalStateException("no composted dirt block entity at " + bed + ": " + fixture.level().getBlockEntity(bed));
              }
              dirt[0] = d;
              for (int i = 0; i < 301 * 20; i++)
              {
                  d.tick();
              }
          })
          .check("the idle bed is not composted", () -> !dirt[0].isComposted(), () -> "composted")
          // Chance 0: no flower grows, so only the compost's lifetime decides whether it stays composted.
          .run("compost the bed", () -> dirt[0].compost(0.0, new ItemStack(Items.POPPY)))
          .idle(100)
          .check("the compost is still active 5 s later", () -> dirt[0].isComposted(), () -> "compost expired at once")
          .succeed();
    }

    /**
     * FRM-03 Florist. A level 2 flower shop (small flowers allowed) with shears and one compost per plant bed; every small flower except
     * poppy and dandelion is on the hut's "don't grow" list. The florist composts the beds and harvests the flowers that grow; the test
     * runs the composted beds at double speed. Only poppies and dandelions grow, the compost is used up, flowers
     * end up in the hut, and when the compost is gone the florist requests more.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 34000, skyAccess = true)
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
          // Since the compost timer fix (E2E-FRM-03-1) each compost lasts its full 300 s (150 s at the test's double speed), so using up
          // 3 per bed ends at about test tick 11,000-12,000 whatever the step started at; 10,000 from the step start was too tight.
          .awaitUntil("the compost is used up", 20000,
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
    // GAT-03 / GAT-04 Miner
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Miner's hut position: south of the arena center, so the shaft and the first ring of nodes lie under the middle of the deep arena.
     */
    private static final BlockPos MINE_POS = new BlockPos(32, ColonyFixture.GROUND_Y, 44);

    /**
     * Town hall position for the mine tests (out of the way of the mine).
     */
    private static final BlockPos MINE_TOWN_HALL_POS = new BlockPos(12, ColonyFixture.GROUND_Y, 12);

    /**
     * GAT-03 Miner digs the shaft. A level 1 mine in the deep arena (stone down to y -32) with ladders, cobblestone, torches, wooden
     * pickaxes and shovels and four extra racks. The miner digs the 7x7 shaft with a ladder and a cobblestone column behind it down to the
     * first mining level and builds the level's main shaft structure; the dug blocks end up in the hut. Materials the hut doesn't have are
     * handed over by the creative owner ("fulfil" in the request window, see {@link OwnerFulfils}); without that the framed blocks of the
     * main shaft come from the retrying resolver only after about 40,000 ticks. The "no fall, no damage" part is GAT-03b.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 40000, skyAccess = true)
    public void gat03_minerDigsShaft(final GameTestHelper helper)
    {
        minerShaft(helper, false);
    }

    /**
     * GAT-03b The miner digs the shaft safely. Same as GAT-03; the miner must never fall more than three blocks and never take damage.
     */
    // Was optional (bug E2E-GAT-03-1, fixed): the miner stopped on the ladder to mine, MovementHandler stayed in JUMPING with a forward
    // input and walked him sideways off the ladder; he fell 3.3-4 blocks.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 40000, skyAccess = true)
    public void gat03_minerDigsShaftSafely(final GameTestHelper helper)
    {
        minerShaft(helper, true);
    }

    private static void minerShaft(final GameTestHelper helper, final boolean safety)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Mine mine = new Mine(fixture);

        final com.minecolonies.gametest.framework.TestFlow flow = fixture.flow()
          .run("set up deep arena, colony and level 1 mine", () -> mine.setUp(1));
        if (safety)
        {
            flow.awaitUntil("the miner digs the shaft and builds the first mining level without a fall or damage", 34000, () -> {
                mine.tick();
                if (mine.maxDamage > 0.0f || mine.maxFall > 3.0)
                {
                    throw new IllegalStateException("the miner fell " + mine.maxFall + " blocks and took " + mine.maxDamage + " damage / " + mine.describe());
                }
                return mine.levels() >= 1;
            }, mine::describe).succeed();
            return;
        }
        flow.awaitUntil("the miner digs the shaft and builds the first mining level", 34000, () -> mine.tick() && mine.levels() >= 1, mine::describe)
          .check("ladders and the cobblestone column go all the way down to the level", () -> mine.shaftProblems().isEmpty(), () -> mine.shaftProblems() + " / " + mine.describe())
          .awaitUntil("mined blocks reach the hut", 4000, () -> mine.tick() && mine.minedInHut() >= 64, mine::describe)
          .succeed();
    }

    /**
     * GAT-04 Miner mines a level. Same mine as GAT-03; when the first level is built, the test limits the mine's depth (the hut's "max
     * depth" setting) to that level, puts two iron ores into each of the level's first nodes and a cave hole (air) into a wall corner of the
     * first node. The miner builds out two nodes with their supports (framed blocks, fences, torch); the hole is closed and the ores of those
     * nodes end up in the hut.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 64000, skyAccess = true)
    public void gat04_minerMinesNodes(final GameTestHelper helper)
    {
        minerNodes(helper, false);
    }

    /**
     * GAT-04b Miner seals water next to a node. Like GAT-04 with a water source (a flooded cave) one block past the open end of the first
     * node, inside the range the miner checks for liquids before a node (EntityAIStructureMiner.executeStructurePlacement). After the first
     * node is built the source must be replaced by the fill block.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 54000, skyAccess = true)
    public void gat04_minerSealsWater(final GameTestHelper helper)
    {
        minerNodes(helper, true);
    }

    private static void minerNodes(final GameTestHelper helper, final boolean water)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Mine mine = new Mine(fixture);
        final List<MineNode> prepared = new ArrayList<>();
        final List<MineNode> firstNode = new ArrayList<>();
        final BlockPos[] source = new BlockPos[1];
        final BlockPos[] hole = new BlockPos[1];

        final com.minecolonies.gametest.framework.TestFlow flow = fixture.flow()
          .run("set up deep arena, colony and level 1 mine", () -> mine.setUp(1))
          .awaitUntil("the miner builds the first mining level", 34000, () -> mine.tick() && mine.levels() >= 1, mine::describe)
          .run("keep the miner on this level; ores, a cave hole " + (water ? "and water " : "") + "in the first nodes", () -> {
              final MinerLevel level = mine.level();
              mine.hut.getSetting(BuildingMiner.MAX_DEPTH).setValue(level.getDepth() + 3);
              final MineNode ladder = level.getLadderNode();
              for (final MineNode node : level.getNodes().values())
              {
                  if (node.getStatus() != MineNode.NodeStatus.COMPLETED && node.getStyle() == MineNode.NodeType.TUNNEL)
                  {
                      prepared.add(node);
                      fixture.level().setBlockAndUpdate(new BlockPos(node.getX(), level.getDepth() + 1, node.getZ()), Blocks.IRON_ORE.defaultBlockState());
                      fixture.level().setBlockAndUpdate(new BlockPos(node.getX(), level.getDepth() + 2, node.getZ()), Blocks.IRON_ORE.defaultBlockState());
                  }
              }
              if (prepared.isEmpty())
              {
                  throw new IllegalStateException("no open node on the level: " + level);
              }
              // The first node the miner builds is the head of the open-node queue (MinerLevel.getRandomNode).
              final MineNode first = mine.module().getActiveNode();
              firstNode.add(first);
              // Wall corners of a tunnel node are solid in every rotation.
              hole[0] = new BlockPos(first.getX() + 3, level.getDepth() + 2, first.getZ() + 3);
              fixture.level().setBlockAndUpdate(hole[0], Blocks.AIR.defaultBlockState());
              if (water)
              {
                  final int dx = Integer.signum(first.getX() - ladder.getX());
                  final int dz = Integer.signum(first.getZ() - ladder.getZ());
                  source[0] = new BlockPos(first.getX() + 4 * dx, level.getDepth() + 2, first.getZ() + 4 * dz);
                  fixture.level().setBlockAndUpdate(source[0], Blocks.WATER.defaultBlockState());
              }
              Log.getLogger().info("[GAT-04] prepared nodes {}, first {}, water {}, hole {}", prepared, first, source[0], hole[0]);
          });
        if (water)
        {
            flow.awaitUntil("the miner builds the first node", 16000, () -> mine.tick() && mine.completedNodes(firstNode) >= 1, mine::describe)
              .check("the water next to the first node is replaced by the fill block", () -> mine.isFill(source[0]) && !hasFluidNear(fixture, source[0], 4),
                () -> "at the source " + fixture.level().getBlockState(source[0]) + ", fluid within 4 blocks " + hasFluidNear(fixture, source[0], 4))
              .succeed();
            return;
        }
        flow.awaitUntil("the miner builds out two nodes", 24000, () -> mine.tick() && mine.completedNodes(prepared) >= 2, mine::describe)
          .check("built nodes have their supports (framed blocks, fences, torch)", () -> mine.unsupportedNodes(prepared).isEmpty(),
            () -> mine.unsupportedNodes(prepared) + " / " + mine.describe())
          .check("the cave hole in the first node's wall is closed", () -> fixture.level().getBlockState(hole[0]).isSolid(),
            () -> "at hole " + fixture.level().getBlockState(hole[0]))
          .awaitUntil("the ores of the built nodes reach the hut", 6000,
            () -> mine.tick() && mine.oresInHut() >= 2 * mine.completedNodes(prepared),
            () -> "iron ores in hut " + mine.oresInHut() + ", with the miner " + countInCitizen(mine.worker, FarmingGatheringTests::isIronOre) + ", expected "
                    + 2 * mine.completedNodes(prepared) + " / " + mine.describe())
          .succeed();
    }

    private static boolean isIronOre(final ItemStack stack)
    {
        return stack.is(Items.IRON_ORE) || stack.is(Items.RAW_IRON) || stack.is(Items.DEEPSLATE_IRON_ORE);
    }

    private static boolean hasFluidNear(final ColonyFixture fixture, final BlockPos center, final int range)
    {
        for (final BlockPos p : BlockPos.betweenClosed(center.offset(-range, -range, -range), center.offset(range, range, range)))
        {
            if (!fixture.level().getFluidState(p).isEmpty())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * A level 1+ mine with its miner in the deep arena, and what the test observes every tick (damage, fall distance, requests).
     */
    private static final class Mine
    {
        final ColonyFixture fixture;
        final OwnerFulfils owner;
        BuildingMiner hut;
        ICitizenData worker;
        float maxDamage = 0;
        double maxFall = 0;
        int ticks = 0;
        int fallLines = 0;

        Mine(final ColonyFixture fixture)
        {
            this.fixture = fixture;
            this.owner = new OwnerFulfils(fixture);
        }

        void setUp(final int hutLevel)
        {
            fixture.makeDeepGround();
            fixture.createColony(MINE_TOWN_HALL_POS);
            fixture.setMoveIn(false);
            hut = (BuildingMiner) fixture.placeBuilding(ModBuildings.miner.get(), hutLevel, MINE_POS);
            // Room for what the miner digs out: four racks next to the hut, registered like racks a player places.
            for (int i = 0; i < 4; i++)
            {
                addRack(fixture, hut, new BlockPos(40 + i, ColonyFixture.GROUND_Y, 48));
            }
            fixture.fillBuilding(hut, new ItemStack(Items.LADDER, 64), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.TORCH, 64),
              new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.WOODEN_PICKAXE),
              new ItemStack(Items.WOODEN_SHOVEL), new ItemStack(Items.WOODEN_SHOVEL));
            worker = fixture.spawnCitizen(50, hut, null);
            Log.getLogger().info("[MINE] hut {} corners {} ladder {} cobble {} depth limit {}", hut.getPosition(), hut.getCorners(), hut.getLadderLocation(),
              hut.getCobbleLocation(), hut.getDepthLimit(fixture.level()));
        }

        /**
         * Called every tick from the waits: tracks damage and falls, hands over requested materials. Always true.
         */
        boolean tick()
        {
            ticks++;
            worker.getEntity().ifPresent(e -> {
                final float damage = e.getMaxHealth() - e.getHealth();
                if (damage > maxDamage || (e.fallDistance > 3.0 && e.fallDistance > maxFall))
                {
                    Log.getLogger().info("[MINE] tick {}: damage {} fall {} at {} on {} / {}", ticks, damage, e.fallDistance, e.blockPosition(),
                      fixture.level().getBlockState(e.blockPosition().below()), describe());
                }
                if (e.fallDistance > 0.5 && fallLines++ < 80)
                {
                    Log.getLogger().info("[MINE] tick {}: falling {} y {} climbable {} state {} pos {}", ticks, e.fallDistance, e.getY(), e.onClimbable(),
                      worker.getJob().getWorkerAI() == null ? "-" : worker.getJob().getWorkerAI().getState(), e.blockPosition());
                }
                maxDamage = Math.max(maxDamage, damage);
                maxFall = Math.max(maxFall, e.fallDistance);
            });
            owner.fulfil(hut, worker);
            if (ticks % 2000 == 0)
            {
                Log.getLogger().info("[MINE] tick {}: {}", ticks, describe());
            }
            return true;
        }

        MinerLevelManagementModule module()
        {
            return hut.getFirstModuleOccurance(MinerLevelManagementModule.class);
        }

        int levels()
        {
            return module().getNumberOfLevels();
        }

        MinerLevel level()
        {
            return module().getLevels().get(0);
        }

        int lastLadder()
        {
            return com.minecolonies.core.util.WorkerUtil.getLastLadder(hut.getLadderLocation(), fixture.level());
        }

        /**
         * Positions between the hut's top ladder and the first level where the ladder or the cobblestone column behind it is missing.
         */
        List<String> shaftProblems()
        {
            final List<String> problems = new ArrayList<>();
            final BlockPos ladder = hut.getLadderLocation();
            final BlockPos cobble = hut.getCobbleLocation();
            final int bottom = level().getDepth();
            for (int y = ladder.getY(); y >= bottom; y--)
            {
                final BlockState l = fixture.level().getBlockState(new BlockPos(ladder.getX(), y, ladder.getZ()));
                if (!l.is(BlockTags.CLIMBABLE))
                {
                    problems.add("y " + y + " ladder column " + l.getBlock().getName().getString());
                }
                final BlockState c = fixture.level().getBlockState(new BlockPos(cobble.getX(), y, cobble.getZ()));
                if (!c.isSolid())
                {
                    problems.add("y " + y + " back column " + c.getBlock().getName().getString());
                }
            }
            return problems;
        }

        /**
         * Mined blocks in the hut: cobblestone, stone and dirt, minus the 64 cobblestone the test put in and what the owner handed over.
         */
        int minedInHut()
        {
            final int mined = fixture.countItems(hut, s -> s.is(Items.COBBLESTONE) || s.is(Items.STONE) || s.is(Items.DIRT));
            return mined - 64 - owner.given(Items.COBBLESTONE);
        }

        int oresInHut()
        {
            return fixture.countItems(hut, FarmingGatheringTests::isIronOre);
        }

        int completedNodes(final List<MineNode> nodes)
        {
            int done = 0;
            for (final MineNode node : nodes)
            {
                final MineNode current = level().getNodes().get(new Vec2i(node.getX(), node.getZ()));
                if (current != null && current.getStatus() == MineNode.NodeStatus.COMPLETED)
                {
                    done++;
                }
            }
            return done;
        }

        /**
         * Completed nodes whose 7x7x5 box lacks the tunnel blueprint's supports (16 framed blocks, 4 fences, a torch).
         */
        List<String> unsupportedNodes(final List<MineNode> nodes)
        {
            final List<String> problems = new ArrayList<>();
            for (final MineNode node : nodes)
            {
                final MineNode current = level().getNodes().get(new Vec2i(node.getX(), node.getZ()));
                if (current == null || current.getStatus() != MineNode.NodeStatus.COMPLETED)
                {
                    continue;
                }
                int framed = 0;
                int fences = 0;
                int torches = 0;
                int air = 0;
                final BlockPos base = new BlockPos(node.getX(), level().getDepth(), node.getZ());
                for (final BlockPos p : BlockPos.betweenClosed(base.offset(-3, 0, -3), base.offset(3, 4, 3)))
                {
                    final BlockState s = fixture.level().getBlockState(p);
                    final String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
                    if (id.startsWith("domum_ornamentum:"))
                    {
                        framed++;
                    }
                    else if (s.is(BlockTags.FENCES))
                    {
                        fences++;
                    }
                    else if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH))
                    {
                        torches++;
                    }
                    else if (s.isAir())
                    {
                        air++;
                    }
                }
                if (framed < 16 || fences < 4 || torches < 1 || air < 20)
                {
                    problems.add("node " + node.getX() + "," + node.getZ() + ": framed " + framed + ", fences " + fences + ", torches " + torches + ", air " + air);
                }
            }
            return problems;
        }

        boolean isFill(final BlockPos pos)
        {
            return fixture.level().getBlockState(pos).is(hut.getSetting(BuildingMiner.FILL_BLOCK).getValue().getBlock());
        }

        String describe()
        {
            final MinerLevelManagementModule module = module();
            final StringBuilder sb = new StringBuilder();
            sb.append("last ladder y ").append(lastLadder()).append(", top ladder ").append(hut.getLadderLocation()).append(", levels ").append(module.getNumberOfLevels());
            for (final MinerLevel l : module.getLevels())
            {
                sb.append(" [depth ").append(l.getDepth()).append(", nodes ").append(l.getNumberOfNodes()).append(", built ").append(l.getNumberOfBuiltNodes()).append(']');
            }
            sb.append(", active node ").append(module.getLevels().isEmpty() ? "-" : String.valueOf(module.getActiveNode()));
            sb.append(", damage ").append(maxDamage).append(", fall ").append(maxFall);
            sb.append(", owner gave ").append(owner.given);
            sb.append(", hut cobble ").append(fixture.countItems(hut, s -> s.is(Items.COBBLESTONE))).append(" free slots ").append(freeSlots(fixture, hut));
            sb.append(" / ").append(describeWork(hut, worker));
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // GAT-05 Quarrier
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Simple quarry position: its 18x23 footprint (x -9..+8, z -19..+3 around the hut block) lies in the middle of the arena.
     */
    private static final BlockPos QUARRY_POS = new BlockPos(36, ColonyFixture.GROUND_Y, 40);

    /**
     * Number of shaft layers (from the top) the test waits for.
     */
    private static final int QUARRY_LAYERS = 3;

    /**
     * GAT-05 Quarrier. A level 1 mine (the quarrier's workplace) and a simple quarry on stone (stone floor and deep arena). The quarrier is
     * hired in the mine and assigned to the quarry. He loads the quarry's shaft blueprint and digs it out layer by layer from the top. The test
     * waits for the top three layers: every position the blueprint has as air is dug out, a lower layer is only touched after the layer above
     * is done, the stone right outside the shaft's footprint is untouched, and the dug blocks end up in the quarry (where the quarrier dumps).
     * Materials the colony can't supply (framed blocks of the shaft, more tools) are handed over by the creative owner ({@link OwnerFulfils}).
     */
    // Budget: 23,700-28,300 ticks in most runs; once 34,000 were not enough (a 6,000-tick stall fetching tools and dumping, then 17
    // blocks left), so 60,000.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 70000, skyAccess = true)
    public void gat05_quarrierDigsLayers(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Quarry quarry = new Quarry(fixture);

        fixture.flow()
          .run("set up stone ground, colony, mine and simple quarry", quarry::setUp)
          .awaitUntil("the quarrier loads the shaft blueprint", 4000, () -> quarry.tick() && quarry.blueprintLoaded(), quarry::describe)
          .run("record the shaft's layers and the stone around it", quarry::recordLayers)
          .awaitUntil("the top " + QUARRY_LAYERS + " layers are dug out", 60000, () -> quarry.tick() && quarry.layersDone() >= QUARRY_LAYERS, quarry::describe)
          .check("layers were dug top down", () -> quarry.orderProblems.isEmpty(), () -> quarry.orderProblems + " / " + quarry.describe())
          .check("nothing outside the quarry area was dug", () -> quarry.changedOutside().isEmpty(), () -> quarry.changedOutside() + " / " + quarry.describe())
          .awaitUntil("the dug blocks reach the quarry", 4000, () -> quarry.tick() && quarry.dugInQuarry() >= 64, quarry::describe)
          .succeed();
    }

    /**
     * Quarry test state: buildings, the shaft layers to dig and the stone around them.
     */
    private static final class Quarry
    {
        final ColonyFixture fixture;
        final OwnerFulfils owner;
        BuildingMiner mine;
        IBuilding quarry;
        ICitizenData worker;
        int ticks = 0;
        /**
         * Positions the blueprint has as air, per layer (index 0 = top layer).
         */
        final List<List<BlockPos>> layers = new ArrayList<>();
        final java.util.Map<BlockPos, BlockState> outside = new java.util.HashMap<>();
        final List<String> orderProblems = new ArrayList<>();
        int done = 0;

        Quarry(final ColonyFixture fixture)
        {
            this.fixture = fixture;
            this.owner = new OwnerFulfils(fixture);
        }

        void setUp()
        {
            fixture.fillFloor(Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
            fixture.makeDeepGround();
            fixture.createColony(MINE_TOWN_HALL_POS);
            fixture.setMoveIn(false);
            mine = (BuildingMiner) fixture.placeBuilding(ModBuildings.miner.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 52));
            quarry = fixture.placeBuilding(ModBuildings.simpleQuarry.get(), 1, QUARRY_POS);
            fixture.fillBuilding(mine, new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.WOODEN_PICKAXE),
              new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.WOODEN_SHOVEL), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.TORCH, 32));
            worker = fixture.spawnCitizen(50, null, null);
            fixture.hire(worker, mine, com.minecolonies.api.colony.jobs.ModJobs.quarrier.get());
            if (!quarry.getFirstModuleOccurance(com.minecolonies.core.colony.buildings.modules.QuarryModule.class).assignCitizen(worker))
            {
                throw new IllegalStateException("quarrier could not be assigned to the quarry");
            }
        }

        boolean tick()
        {
            ticks++;
            owner.fulfil(mine, worker);
            if (!layers.isEmpty())
            {
                // Top down: no position of a lower layer may be dug while the layer above still has undug positions.
                while (done < layers.size() && remaining(done) == 0)
                {
                    done++;
                }
                for (int i = done + 1; i < layers.size(); i++)
                {
                    final int dug = layers.get(i).size() - remaining(i);
                    if (dug > 0 && orderProblems.size() < 5)
                    {
                        orderProblems.add("tick " + ticks + ": layer " + i + " has " + dug + " dug while layer " + done + " has " + remaining(done) + " left");
                    }
                }
            }
            if (ticks % 2000 == 0)
            {
                Log.getLogger().info("[QUARRY] tick {}: {}", ticks, describe());
            }
            return true;
        }

        boolean blueprintLoaded()
        {
            return mine.getWorkOrder() != null && mine.getWorkOrder().getBlueprint() != null;
        }

        void recordLayers()
        {
            final var workOrder = mine.getWorkOrder();
            final var blueprint = workOrder.getBlueprint();
            final BlockPos origin = workOrder.getLocation().subtract(blueprint.getPrimaryBlockOffset());
            Log.getLogger().info("[QUARRY] blueprint {} size {}x{}x{} offset {} location {} quarry {} corners {}", blueprint.getName(), blueprint.getSizeX(), blueprint.getSizeY(),
              blueprint.getSizeZ(), blueprint.getPrimaryBlockOffset(), workOrder.getLocation(), quarry.getPosition(), quarry.getCorners());
            for (int layer = 0; layer < QUARRY_LAYERS + 1; layer++)
            {
                final int y = blueprint.getSizeY() - 1 - layer;
                final List<BlockPos> air = new ArrayList<>();
                for (int x = 0; x < blueprint.getSizeX(); x++)
                {
                    for (int z = 0; z < blueprint.getSizeZ(); z++)
                    {
                        final BlockPos local = new BlockPos(x, y, z);
                        final BlockState state = blueprint.getBlockState(local);
                        if (state != null && state.isAir())
                        {
                            air.add(origin.offset(local));
                        }
                    }
                }
                layers.add(air);
            }
            // The stone right outside the footprint, from the floor down to two layers below the last checked layer.
            final int top = origin.getY() + blueprint.getSizeY() - 1;
            final int bottom = top - QUARRY_LAYERS - 2;
            for (int x = -1; x <= blueprint.getSizeX(); x++)
            {
                for (int z = -1; z <= blueprint.getSizeZ(); z++)
                {
                    if (x >= 0 && x < blueprint.getSizeX() && z >= 0 && z < blueprint.getSizeZ())
                    {
                        continue;
                    }
                    for (int y = bottom; y < top; y++)
                    {
                        final BlockPos p = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
                        outside.put(p, fixture.level().getBlockState(p));
                    }
                }
            }
        }

        int remaining(final int layer)
        {
            int left = 0;
            for (final BlockPos p : layers.get(layer))
            {
                if (!fixture.level().getBlockState(p).isAir())
                {
                    left++;
                }
            }
            return left;
        }

        int layersDone()
        {
            return done;
        }

        List<String> changedOutside()
        {
            final List<String> changed = new ArrayList<>();
            for (final var e : outside.entrySet())
            {
                final BlockState now = fixture.level().getBlockState(e.getKey());
                if (now != e.getValue() && changed.size() < 10)
                {
                    changed.add(fixture.rel(e.getKey()).toShortString() + " " + e.getValue().getBlock().getName().getString() + " -> " + now.getBlock().getName().getString());
                }
            }
            return changed;
        }

        int dugInQuarry()
        {
            return fixture.countItems(quarry, s -> s.is(Items.COBBLESTONE) || s.is(Items.STONE));
        }

        String describe()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("layers done ").append(done).append('/').append(layers.size());
            for (int i = 0; i < layers.size(); i++)
            {
                sb.append(" [").append(i).append(": ").append(remaining(i)).append('/').append(layers.get(i).size()).append(" left]");
            }
            sb.append(", work order ").append(mine.getWorkOrder() == null ? "-" : mine.getWorkOrder().getStructurePath());
            sb.append(", progress ").append(mine.getProgress() == null ? "-" : mine.getProgress().toString());
            sb.append(", quarry stone/cobble ").append(dugInQuarry()).append(", owner gave ").append(owner.given);
            sb.append(" / ").append(describeWork(mine, worker));
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // GAT-06 Nether worker
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Nether mine position (its footprint is x -11..+5, z -9..+4 around the hut block).
     */
    private static final BlockPos NETHER_POS = new BlockPos(20, ColonyFixture.GROUND_Y, 14);

    /**
     * GAT-06 Nether expedition. A level 1 nether mine with a nether worker, the "nether expedition log" research done, the trip's inputs
     * (64 cobblestone, 32 torches, 16 ladders: the level 1 trip recipe), wooden axe, pickaxe, shovel and sword, flint and steel, leather armor
     * and bread on the hut's menu. The worker lights the hut's portal and leaves through it (the citizen entity is removed and travels); the
     * test skips the travel time. Back at the portal the returned worker works off the trip (fights and finds are simulated); this variant
     * also runs the hut's morning update four times, as four mornings would (the trip period starts over), which once was needed to get
     * the worker going (E2E-GAT-06-1, fixed; {@link #gat06_netherWorkerReturnsAfterTravel} covers it). Passes when the trip is logged and either the worker comes back and the loot reaches the hut, or the worker died
     * on the way and the log says so (the expedition rules).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 40000, skyAccess = true)
    public void gat06_netherExpedition(final GameTestHelper helper)
    {
        netherExpedition(helper, true);
    }

    /**
     * GAT-06b Nether worker works off the trip when the travel ends. Same as GAT-06 without the four mornings: when the travel time is over
     * the returned worker should process the trip and bring the loot home.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 24000, skyAccess = true)
    public void gat06_netherWorkerReturnsAfterTravel(final GameTestHelper helper)
    {
        netherExpedition(helper, false);
    }

    private static void netherExpedition(final GameTestHelper helper, final boolean passTripPeriod)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker[] hut = new com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final java.util.function.Supplier<com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog> log =
          () -> hut[0].getFirstModuleOccurance(com.minecolonies.core.colony.buildings.modules.ExpeditionLogModule.class).getLog();
        final java.util.function.Supplier<String> describe = () -> {
            final StringBuilder sb = new StringBuilder("log ").append(log.get().getStatus()).append(", loot ").append(log.get().getLoot().size())
              .append(", mobs ").append(log.get().getMobs().size())
              .append(", travelling ").append(fixture.colony().getTravellingManager().isTravelling(worker[0]))
              .append(", portal ").append(hut[0].getPortalLocation() == null ? "-" : fixture.level().getBlockState(hut[0].getPortalLocation()).getBlock().getName().getString())
              .append(", citizen in colony ").append(fixture.colony().getCitizenManager().getCivilian(worker[0].getId()) != null);
            if (worker[0].getJob() instanceof final com.minecolonies.core.colony.jobs.JobNetherWorker job)
            {
                sb.append(", job in nether ").append(job.isInNether()).append(", trip results to work off ").append(job.getCraftedResults().size())
                  .append(", to bring home ").append(job.getProcessedResults().size());
            }
            worker[0].getEntity().ifPresent(e -> sb.append(", invisible ").append(e.isInvisible()));
            return sb.append(" / ").append(describeWork(hut[0], worker[0])).toString();
        };

        final com.minecolonies.gametest.framework.TestFlow flow = fixture.flow()
          .run("set up colony and nether mine", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              fixture.completeResearch(net.minecraft.resources.Identifier.parse("minecolonies:technology/netherlog"));
              hut[0] = (com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker) fixture.placeBuilding(ModBuildings.netherWorker.get(), 1, NETHER_POS);
              hut[0].getModule(com.minecolonies.core.colony.buildings.modules.BuildingModules.NETHERMINER_MENU).addMenuItem(new ItemStack(Items.BREAD));
              fixture.fillBuilding(hut[0], new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.TORCH, 32), new ItemStack(Items.LADDER, 16),
                new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.WOODEN_SHOVEL), new ItemStack(Items.WOODEN_SWORD),
                new ItemStack(Items.FLINT_AND_STEEL), new ItemStack(Items.BREAD, 32), new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.LEATHER_CHESTPLATE),
                new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS));
              worker[0] = fixture.spawnCitizen(50, hut[0], null);
          })
          .check("the hut has a portal", () -> hut[0].getPortalLocation() != null, () -> "no portal tag")
          .awaitUntil("the nether worker lights the portal and leaves through it", 6000,
            () -> fixture.colony().getTravellingManager().isTravelling(worker[0]) && worker[0].getEntity().isEmpty(), describe)
          .check("the expedition is logged as in progress", () -> log.get().getStatus() == com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog.Status.IN_PROGRESS, describe)
          .run("skip the travel time", () -> fixture.colony().getTravellingManager().startTravellingTo(worker[0],
            fixture.colony().getTravellingManager().getTravellingTargetFor(worker[0]).orElseThrow(), 0))
          .awaitUntil("the worker is back at the portal", 8000, () -> worker[0].getEntity().isPresent(), describe);
        if (passTripPeriod)
        {
            flow.idle(200)
              .run("four mornings: the hut's trip period starts over", () -> {
                  for (int i = 0; i < 4; i++)
                  {
                      hut[0].onWakeUp();
                  }
              });
        }
        flow.awaitUntil("the expedition ends", passTripPeriod ? 12000 : 4000,
            () -> log.get().getStatus() == com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog.Status.COMPLETED
                  || log.get().getStatus() == com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog.Status.KILLED, describe)
          .awaitUntil("the worker is back and the loot is in the hut (or the worker died and the log says so)", 8000, () -> {
              if (log.get().getStatus() == com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog.Status.KILLED)
              {
                  return true;
              }
              final boolean back = worker[0].getEntity().map(e -> !e.isInvisible() && e.blockPosition().closerThan(hut[0].getPosition(), 10)).orElse(false);
              return back && lootInHut(fixture, hut[0], log.get()) > 0;
          }, () -> "loot in hut " + lootInHut(fixture, hut[0], log.get()) + " / " + describe.get())
          .check("the log lists the trip's loot or the death", () -> log.get().getStatus() == com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog.Status.KILLED
                                                                   || !log.get().getLoot().isEmpty(), describe)
          .run("report the outcome", () -> Log.getLogger().info("[GAT-06] expedition outcome: {}", describe.get()))
          .succeed();
    }

    /**
     * Items in the hut that the expedition log lists as loot and that the test didn't put into the hut.
     */
    private static int lootInHut(final ColonyFixture fixture, final IBuilding hut, final com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog log)
    {
        int count = 0;
        for (final ItemStorage loot : log.getLoot())
        {
            final ItemStack stack = loot.getItemStack();
            if (stack.is(Items.COBBLESTONE) || stack.is(Items.TORCH) || stack.is(Items.LADDER) || stack.is(Items.BREAD))
            {
                continue;
            }
            count += fixture.countItems(hut, s -> ItemStack.isSameItem(s, stack));
        }
        return count;
    }

    /**
     * Places a rack at a relative position and registers it with the building, as when a player places a rack for a hut.
     */
    private static void addRack(final ColonyFixture fixture, final IBuilding building, final BlockPos rel)
    {
        final BlockPos pos = fixture.abs(rel);
        final BlockState rack = ModBlocks.blockRack.defaultBlockState();
        fixture.level().setBlock(pos, rack, Block.UPDATE_ALL);
        building.registerBlockPosition(rack, pos, fixture.level());
    }

    /**
     * The creative owner pressing "fulfil" in a worker's request window for every request the colony can't resolve (assigned to the
     * retrying or the player resolver): the requested stack goes into the worker's inventory (into the hut if that is full) and the request
     * is overruled, as {@code RequestWindowCitizen} + {@code TransferItemsToCitizenRequestMessage} + {@code UpdateRequestStateMessage} do.
     * For tests whose subject isn't logistics.
     */
    private static final class OwnerFulfils
    {
        final ColonyFixture fixture;
        final java.util.Map<String, Integer> given = new java.util.TreeMap<>();

        OwnerFulfils(final ColonyFixture fixture)
        {
            this.fixture = fixture;
        }

        int given(final Item item)
        {
            return given.getOrDefault(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString(), 0);
        }

        void fulfil(final IBuilding building, final ICitizenData citizen)
        {
            final var manager = fixture.colony().getRequestManager();
            for (final var request : new ArrayList<>(building.getOpenRequests(citizen.getId())))
            {
                if (request.getState() != com.minecolonies.api.colony.requestsystem.request.RequestState.IN_PROGRESS
                      || !(request.getRequest() instanceof final com.minecolonies.api.colony.requestsystem.requestable.IDeliverable deliverable))
                {
                    continue;
                }
                final Object resolver;
                try
                {
                    resolver = manager.getResolverForRequest(request.getId());
                }
                catch (final IllegalArgumentException e)
                {
                    continue;
                }
                if (!(resolver instanceof com.minecolonies.api.colony.requestsystem.resolver.retrying.IRetryingRequestResolver)
                      && !(resolver instanceof com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver))
                {
                    continue;
                }
                final List<ItemStack> display = request.getDisplayStacks();
                if (display.isEmpty() || display.get(0).isEmpty())
                {
                    continue;
                }
                final ItemStack stack = display.get(0).copy();
                stack.setCount(Math.max(1, Math.min(deliverable.getCount(), stack.getMaxStackSize())));
                ItemStack rest = com.minecolonies.api.util.InventoryUtils.addItemStackToItemHandlerWithResult(citizen.getInventory(), stack.copy());
                if (!rest.isEmpty())
                {
                    rest = com.minecolonies.api.util.InventoryUtils.addItemStackToItemHandlerWithResult(fixture.inventory(building), rest);
                }
                if (!rest.isEmpty())
                {
                    continue;
                }
                manager.overruleRequest(request.getId(), stack.copy());
                given.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
            }
        }
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
