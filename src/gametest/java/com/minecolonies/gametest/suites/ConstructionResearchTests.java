package com.minecolonies.gametest.suites;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.workorders.IServerWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.api.research.IGlobalResearch;
import com.minecolonies.api.research.IGlobalResearchTree;
import com.minecolonies.api.research.ILocalResearch;
import com.minecolonies.api.research.ILocalResearchTree;
import com.minecolonies.api.research.util.ResearchState;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingUniversity;
import com.minecolonies.core.colony.workorders.WorkOrderBuilding;
import com.minecolonies.core.colony.workorders.WorkOrderDecoration;
import com.minecolonies.gametest.framework.ColonyFixture;
import com.minecolonies.gametest.suites.construction.ConstructionSupport;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.minecolonies.api.research.util.ResearchConstants.HEALTH_BOOST;

/**
 * End-to-end tests from docs/e2e-tests/construction.md and docs/e2e-tests/research.md.
 * <p>
 * Test ids: {@code minecolonies-gametest:construction_research_tests_<method_name>}. Run only these with
 * {@code ./gradlew runGameTest -Pe2eFilter=construction_research_tests}. See docs/e2e-tests/AGENT-GUIDE.md.
 * <p>
 * Helpers (survival hut placement, material lists, blueprint comparison, decoration requests) are in
 * {@link ConstructionSupport}.
 */
public class ConstructionResearchTests
{
    private static final BlockPos CORNER_NW = new BlockPos(12, ColonyFixture.GROUND_Y, 12);
    private static final BlockPos CORNER_NE = new BlockPos(52, ColonyFixture.GROUND_Y, 12);
    private static final BlockPos CORNER_SW = new BlockPos(12, ColonyFixture.GROUND_Y, 52);
    private static final BlockPos CORNER_SE = new BlockPos(52, ColonyFixture.GROUND_Y, 52);


    private static final Identifier FIRST_AID  = Identifier.parse("minecolonies:civilian/firstaid");
    private static final Identifier FIRST_AID2 = Identifier.parse("minecolonies:civilian/firstaid2");
    private static final Identifier KEEN       = Identifier.parse("minecolonies:civilian/keen");
    private static final Identifier STAMINA    = Identifier.parse("minecolonies:civilian/stamina");

    // ====================================================================================================================================
    // Construction
    // ====================================================================================================================================

    /**
     * BLD-01 Builder builds a new hut: a residence placed with the survival build tool is built by the builder from materials in his hut.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 42000, skyAccess = true)
    public void bld01_builderBuildsNewHut(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] builder = new ICitizenData[1];
        final IServerWorkOrder[] order = new IServerWorkOrder[1];
        final Blueprint[] blueprint = new Blueprint[1];
        final double[] xpBefore = new double[1];

        fixture.flow()
          .run("colony, builder's hut, residence placed with the survival build tool", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              huts[1] = ConstructionSupport.placeHutForConstruction(fixture, ModBuildings.home.get(), CORNER_SE);
              blueprint[0] = ConstructionSupport.blueprint(fixture.level(), ConstructionSupport.subPath("residence1"));
              fixture.fillBuilding(huts[0], ConstructionSupport.materials(fixture.level(), blueprint[0], huts[1].getPosition()).toArray(ItemStack[]::new));
              fixture.fillBuilding(huts[0], ConstructionSupport.builderKit());
          })
          .check("residence registered at level 0", () -> huts[1].getBuildingLevel() == 0, () -> "level " + huts[1].getBuildingLevel())
          .run("hire builder, request the build", () -> {
              builder[0] = fixture.spawnCitizen(40, huts[0], null);
              xpBefore[0] = builder[0].getCitizenSkillHandler().getTotalXP();
              order[0] = fixture.addWorkOrder(WorkOrderType.BUILD, huts[1]);
          })
          .check("build work order created", () -> order[0] != null && order[0].getWorkOrderType() == WorkOrderType.BUILD, () -> "order " + order[0])
          .awaitUntil("work order claimed by the builder", 600,
            () -> huts[0].getID().equals(order[0].getClaimedBy()),
            () -> "claimed by " + order[0].getClaimedBy())
          .awaitUntil("work order done and removed", 40000,
            () -> fixture.colony().getWorkManager().getWorkOrder(order[0].getID()) == null,
            () -> ConstructionSupport.describeBuilder(huts[0], builder[0]) + ", residence level " + huts[1].getBuildingLevel())
          .check("residence is level 1", () -> huts[1].getBuildingLevel() == 1, () -> "level " + huts[1].getBuildingLevel())
          .check("world matches the residence1 blueprint",
            () -> ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 10).isEmpty(),
            () -> String.join("; ", ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 10)))
          .check("builder gained XP", () -> builder[0].getCitizenSkillHandler().getTotalXP() > xpBefore[0],
            () -> "xp before " + xpBefore[0] + ", now " + builder[0].getCitizenSkillHandler().getTotalXP())
          .succeed();
    }

    /**
     * BLD-02 Builder asks for missing materials: empty builder's hut, materials in a warehouse with a courier.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 44000, skyAccess = true)
    public void bld02_builderRequestsMaterials(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[4]; // builder, warehouse, courier hut, site
        final ICitizenData[] citizens = new ICitizenData[2];
        final IServerWorkOrder[] order = new IServerWorkOrder[1];
        final Blueprint[] blueprint = new Blueprint[1];
        final List<ItemStack> materials = new ArrayList<>();
        final Set<String> requested = new HashSet<>();
        final Set<String> resolvers = new HashSet<>();
        final int[] warehouseBefore = new int[1];

        fixture.flow()
          .run("colony, builder's hut, warehouse, courier, residence placed with the survival build tool", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              huts[1] = fixture.placeBuilding(ModBuildings.wareHouse.get(), 1, CORNER_NE);
              huts[2] = fixture.placeBuilding(ModBuildings.deliveryman.get(), 1, CORNER_SW);
              huts[3] = ConstructionSupport.placeHutForConstruction(fixture, ModBuildings.home.get(), CORNER_SE);
              blueprint[0] = ConstructionSupport.blueprint(fixture.level(), ConstructionSupport.subPath("residence1"));
              materials.addAll(ConstructionSupport.materials(fixture.level(), blueprint[0], huts[3].getPosition()));
              fixture.fillWarehouse(materials.stream().map(s -> s.copyWithCount(s.getCount() * 2)).toArray(ItemStack[]::new));
              fixture.fillBuilding(huts[0], ConstructionSupport.builderKit());
              warehouseBefore[0] = fixture.countItems(huts[1], s -> isMaterial(materials, s));
          })
          .run("hire courier and builder, request the build", () -> {
              citizens[0] = fixture.spawnCitizen(40, huts[2], null);
              citizens[1] = fixture.spawnCitizen(40, huts[0], null);
              order[0] = fixture.addWorkOrder(WorkOrderType.BUILD, huts[3]);
          })
          .check("build work order created", () -> order[0] != null, () -> "order " + order[0])
          .awaitUntil("work order done and removed (recording the builder's requests meanwhile)", 42000,
            () -> {
                for (final IRequest<? extends Stack> request : ConstructionSupport.openStackRequests(huts[0]))
                {
                    requested.add(key(request.getRequest().getStack()));
                    try
                    {
                        final IRequestResolver<?> resolver = fixture.colony().getRequestManager().getResolverForRequest(request.getId());
                        resolvers.add(resolver.getClass().getSimpleName());
                    }
                    catch (final IllegalArgumentException e)
                    {
                        // not assigned yet
                    }
                }
                return fixture.colony().getWorkManager().getWorkOrder(order[0].getID()) == null;
            },
            () -> ColonyFixture.describe(citizens[1]) + " / courier " + ColonyFixture.describe(citizens[0]) + ", requested " + requested + ", resolvers "
                    + resolvers)
          .check("the builder requested materials", () -> !requested.isEmpty(), () -> "no requests seen")
          .check("every requested item is on the blueprint's material list",
            () -> requested.stream().allMatch(k -> materials.stream().anyMatch(m -> key(m).equals(k))),
            () -> "requested " + requested + ", material list " + materials.stream().map(ConstructionResearchTests::key).toList())
          .check("requests were not resolved by the player", () -> resolvers.stream().noneMatch(r -> r.toLowerCase().contains("player")),
            () -> "resolvers " + resolvers)
          .check("materials left the warehouse", () -> fixture.countItems(huts[1], s -> isMaterial(materials, s)) < warehouseBefore[0],
            () -> "before " + warehouseBefore[0] + ", now " + fixture.countItems(huts[1], s -> isMaterial(materials, s)))
          .check("residence is level 1", () -> huts[3].getBuildingLevel() == 1, () -> "level " + huts[3].getBuildingLevel())
          .check("world matches the residence1 blueprint",
            () -> ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[3].getPosition(), 10).isEmpty(),
            () -> String.join("; ", ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[3].getPosition(), 10)))
          .succeed();
    }

    /**
     * BLD-03 Upgrade a hut: a level 1 lumberjack's hut is upgraded to level 2; the lumberjack keeps his job.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 30000, skyAccess = true)
    public void bld03_upgradeHut(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] citizens = new ICitizenData[2]; // builder, lumberjack
        final IServerWorkOrder[] order = new IServerWorkOrder[1];
        final Blueprint[] blueprint = new Blueprint[1];
        final int[] slotsBefore = new int[1];
        final boolean[] lostJob = new boolean[1];

        fixture.flow()
          .run("colony, level 2 builder's hut, level 1 lumberjack's hut with lumberjack", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 2, CORNER_NW);
              huts[1] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, CORNER_SE);
              blueprint[0] = ConstructionSupport.blueprint(fixture.level(), ConstructionSupport.subPath("lumberjack2"));
              fixture.fillBuilding(huts[0], ConstructionSupport.materials(fixture.level(), blueprint[0], huts[1].getPosition()).toArray(ItemStack[]::new));
              fixture.fillBuilding(huts[0], ConstructionSupport.builderKit());
              slotsBefore[0] = fixture.inventory(huts[1]).getSlots();
              citizens[1] = fixture.spawnCitizen(20, huts[1], null);
              citizens[0] = fixture.spawnCitizen(40, huts[0], null);
              order[0] = fixture.addWorkOrder(WorkOrderType.UPGRADE, huts[1]);
          })
          .check("upgrade work order to level 2 created", () -> order[0] != null && order[0].getTargetLevel() == 2, () -> "order " + order[0])
          .awaitUntil("work order done and removed (watching the lumberjack's job meanwhile)", 28000,
            () -> {
                if (citizens[1].getWorkBuilding() != huts[1] || citizens[1].getJob() == null)
                {
                    lostJob[0] = true;
                }
                return fixture.colony().getWorkManager().getWorkOrder(order[0].getID()) == null;
            },
            () -> ColonyFixture.describe(citizens[0]) + ", lumberjack's hut level " + huts[1].getBuildingLevel())
          .check("lumberjack's hut is level 2", () -> huts[1].getBuildingLevel() == 2, () -> "level " + huts[1].getBuildingLevel())
          .check("world matches the lumberjack2 blueprint",
            () -> ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 10).isEmpty(),
            () -> String.join("; ", ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 10)))
          .check("level 2 hut has more storage (racks of the new level registered)",
            () -> fixture.inventory(huts[1]).getSlots() > slotsBefore[0],
            () -> "slots before " + slotsBefore[0] + ", now " + fixture.inventory(huts[1]).getSlots())
          .check("lumberjack kept his job during and after the upgrade", () -> !lostJob[0] && citizens[1].getWorkBuilding() == huts[1],
            () -> "lost job during upgrade " + lostJob[0] + ", work building " + citizens[1].getWorkBuilding())
          .succeed();
    }

    /**
     * BLD-04 Repair a damaged hut: missing and wrong blocks are put back, the rest is untouched.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void bld04_repairDamagedHut(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] builder = new ICitizenData[1];
        final IServerWorkOrder[] order = new IServerWorkOrder[1];
        final Blueprint[] blueprint = new Blueprint[1];
        final Map<BlockPos, BlockState> untouched = new HashMap<>();
        final List<BlockPos> damaged = new ArrayList<>();

        fixture.flow()
          .run("colony, builder, finished residence; damage the residence", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              huts[1] = fixture.placeBuilding(ModBuildings.home.get(), 1, CORNER_SE);
              blueprint[0] = ConstructionSupport.blueprint(fixture.level(), ConstructionSupport.subPath("residence1"));
              final Map<BlockPos, BlockState> before = ConstructionSupport.snapshot(fixture.level(), blueprint[0], huts[1].getPosition());

              // Damage: remove 6 solid, non-hut blocks above ground and replace 3 with wrong blocks.
              int removed = 0;
              int replaced = 0;
              for (final Map.Entry<BlockPos, BlockState> entry : before.entrySet())
              {
                  final BlockState state = entry.getValue();
                  final BlockPos pos = entry.getKey();
                  final boolean candidate = pos.getY() > huts[1].getPosition().getY() && state.isSolidRender() && !state.hasBlockEntity()
                                              && !pos.equals(huts[1].getPosition()) && (pos.getX() + pos.getZ() + pos.getY()) % 7 == 0;
                  if (candidate && removed < 6)
                  {
                      fixture.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                      damaged.add(pos);
                      removed++;
                  }
                  else if (candidate && replaced < 3)
                  {
                      fixture.level().setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
                      damaged.add(pos);
                      replaced++;
                  }
              }
              if (damaged.size() < 9)
              {
                  throw new IllegalStateException("Only found " + damaged.size() + " blocks to damage");
              }
              // Expected end state of the undamaged positions: as before the damage (neighbours of removed blocks, e.g. fences or a
              // torch on a removed wall, change with the damage and must come back too).
              before.forEach((pos, state) -> {
                  if (!damaged.contains(pos))
                  {
                      untouched.put(pos, state);
                  }
              });

              fixture.fillBuilding(huts[0], ConstructionSupport.materials(fixture.level(), blueprint[0], huts[1].getPosition()).toArray(ItemStack[]::new));
              fixture.fillBuilding(huts[0], ConstructionSupport.builderKit());
              builder[0] = fixture.spawnCitizen(40, huts[0], null);
              order[0] = fixture.addWorkOrder(WorkOrderType.REPAIR, huts[1]);
          })
          .check("the damage shows in the blueprint comparison",
            () -> ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 100).size() >= damaged.size(),
            () -> "mismatches " + ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 100).size() + ", damaged " + damaged.size())
          .check("repair work order created", () -> order[0] != null && order[0].getWorkOrderType() == WorkOrderType.REPAIR, () -> "order " + order[0])
          .awaitUntil("work order done and removed", 11000,
            () -> fixture.colony().getWorkManager().getWorkOrder(order[0].getID()) == null,
            () -> ColonyFixture.describe(builder[0]))
          .check("world matches the residence1 blueprint again",
            () -> ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 10).isEmpty(),
            () -> String.join("; ", ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 10)))
          .check("undamaged blocks are as before the damage", () -> changed(fixture, untouched).isEmpty(), () -> String.join("; ", changed(fixture, untouched)))
          .check("building level unchanged", () -> huts[1].getBuildingLevel() == 1, () -> "level " + huts[1].getBuildingLevel())
          .succeed();
    }

    /**
     * BLD-05 Remove a hut: deconstruction takes the lumberjack's hut down and the builder gets the materials.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 24000, skyAccess = true)
    public void bld05_removeHut(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] citizens = new ICitizenData[2]; // builder, lumberjack
        final IServerWorkOrder[] order = new IServerWorkOrder[1];
        final Blueprint[] blueprint = new Blueprint[1];
        final int[] solidBefore = new int[1];
        final int[] woodBefore = new int[1];

        fixture.flow()
          .run("colony, builder, finished lumberjack's hut with a lumberjack", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              huts[1] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, CORNER_SE);
              blueprint[0] = ConstructionSupport.blueprint(fixture.level(), ConstructionSupport.subPath("lumberjack1"));
              solidBefore[0] = solidAboveGround(fixture, blueprint[0], huts[1].getPosition());
              fixture.fillBuilding(huts[0], ConstructionSupport.builderKit());
              citizens[1] = fixture.spawnCitizen(20, huts[1], null);
              citizens[0] = fixture.spawnCitizen(40, huts[0], null);
              woodBefore[0] = recovered(fixture, huts[0], citizens[0]);
              order[0] = fixture.addWorkOrder(WorkOrderType.REMOVE, huts[1]);
          })
          .check("removal work order created", () -> order[0] != null && order[0].getWorkOrderType() == WorkOrderType.REMOVE, () -> "order " + order[0])
          .awaitUntil("work order done and removed", 22000,
            () -> fixture.colony().getWorkManager().getWorkOrder(order[0].getID()) == null,
            () -> ColonyFixture.describe(citizens[0]) + ", solid blocks above ground " + solidAboveGround(fixture, blueprint[0], huts[1].getPosition()))
          .check("the hut's blocks above ground are gone (only the hut block may stay)",
            () -> solidAboveGround(fixture, blueprint[0], huts[1].getPosition()) <= 1,
            () -> "solid blocks above ground: before " + solidBefore[0] + ", now " + solidAboveGround(fixture, blueprint[0], huts[1].getPosition()))
          .check("building stays registered, marked deconstructed, keeping its level for a later pick-up",
            () -> fixture.colony().getServerBuildingManager().getBuilding(huts[1].getPosition()) == huts[1] && huts[1].isDeconstructed()
                    && huts[1].getBuildingLevel() == 1,
            () -> {
                final IBuilding b = fixture.colony().getServerBuildingManager().getBuilding(huts[1].getPosition());
                return b == null ? "not registered" : "registered, level " + b.getBuildingLevel() + ", deconstructed " + b.isDeconstructed();
            })
          .run("pick up the deconstructed hut (hut window button: 'remove' again)", () -> {
              fixture.owner().getInventory().clearContent();
              huts[1].requestRemoval(fixture.owner(), BlockPos.ZERO);
          })
          .check("building no longer registered, hut block gone, owner got the hut item",
            () -> fixture.colony().getServerBuildingManager().getBuilding(huts[1].getPosition()) == null
                    && fixture.level().getBlockState(huts[1].getPosition()).isAir()
                    && fixture.owner().getInventory().countItem(ModBuildings.lumberjack.get().getBuildingBlock().asItem()) == 1,
            () -> "registered " + (fixture.colony().getServerBuildingManager().getBuilding(huts[1].getPosition()) != null) + ", block "
                    + fixture.level().getBlockState(huts[1].getPosition()) + ", hut items "
                    + fixture.owner().getInventory().countItem(ModBuildings.lumberjack.get().getBuildingBlock().asItem()))
          .check("the lumberjack is unemployed", () -> citizens[1].getJob() == null && citizens[1].getWorkBuilding() == null,
            () -> "job " + citizens[1].getJob() + ", work building " + citizens[1].getWorkBuilding())
          .check("builder (inventory or hut) holds recovered materials", () -> recovered(fixture, huts[0], citizens[0]) > woodBefore[0],
            () -> "wood items before " + woodBefore[0] + ", now " + recovered(fixture, huts[0], citizens[0]))
          .succeed();
    }

    /**
     * BLD-06 Builder level limits: a level 1 builder doesn't take an order for level 2, and does once his hut is level 2.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 1200, skyAccess = true)
    public void bld06_builderLevelLimits(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] builder = new ICitizenData[1];
        final IServerWorkOrder[] refused = new IServerWorkOrder[1];
        final WorkOrderBuilding[] order = new WorkOrderBuilding[1];

        fixture.flow()
          .run("colony, level 1 builder with builder, level 1 lumberjack's hut", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              huts[1] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, CORNER_SE);
              builder[0] = fixture.spawnCitizen(40, huts[0], null);
              refused[0] = fixture.addWorkOrder(WorkOrderType.UPGRADE, huts[1]);
          })
          .check("the hut window refuses the level 2 upgrade (no builder of level 2)", () -> refused[0] == null, () -> "order " + refused[0])
          .run("add the level 2 upgrade order directly to the work manager", () -> {
              order[0] = WorkOrderBuilding.create(WorkOrderType.UPGRADE, huts[1]);
              fixture.colony().getWorkManager().addWorkOrder(order[0], false);
          })
          .check("order is in the work manager", () -> fixture.colony().getWorkManager().getWorkOrder(order[0].getID()) != null, () -> "missing")
          // Absence check: the work manager tries to assign orders every 20 ticks; 200 ticks are 10 rounds.
          .idle(200)
          .check("level 1 builder did not claim the level 2 order",
            () -> !order[0].isClaimed() && !((AbstractBuildingStructureBuilder) huts[0]).hasWorkOrder(),
            () -> "claimed by " + order[0].getClaimedBy())
          .run("raise the builder's hut to level 2", () -> huts[0].setBuildingLevel(2))
          .awaitUntil("builder claims the order", 400, () -> huts[0].getID().equals(order[0].getClaimedBy()),
            () -> "claimed by " + order[0].getClaimedBy())
          .succeed();
    }

    /**
     * BLD-07 Decorations from the build tool: a decoration work order is built and registers no building.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void bld07_decorationFromBuildTool(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] builderHut = new IBuilding[1];
        final ICitizenData[] builder = new ICitizenData[1];
        final Blueprint[] blueprint = new Blueprint[1];
        final IServerWorkOrder[] order = new IServerWorkOrder[1];
        final int[] buildings = new int[1];
        final BlockPos site = fixture.abs(CORNER_SE);
        final String[] deco = new String[1];

        fixture.flow()
          .run("colony, builder with the decoration's materials", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              builderHut[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              org.slf4j.LoggerFactory.getLogger(ConstructionResearchTests.class).info("BLD-07: material list of pedastal_oak (4 entities): {}",
                ConstructionSupport.materials(fixture.level(), ConstructionSupport.blueprint(fixture.level(), "decorations/statues/pedastal_oak.blueprint"), site));
              deco[0] = ConstructionSupport.entityFreeDecoration(fixture.level());
              blueprint[0] = ConstructionSupport.blueprint(fixture.level(), deco[0]);
              fixture.fillBuilding(builderHut[0], ConstructionSupport.materials(fixture.level(), blueprint[0], site).toArray(ItemStack[]::new));
              fixture.fillBuilding(builderHut[0], ConstructionSupport.builderKit());
              builder[0] = fixture.spawnCitizen(40, builderHut[0], null);
              buildings[0] = fixture.colony().getServerBuildingManager().getBuildings().size();
              ConstructionSupport.requestDecoration(fixture, deco[0], site, BlockPos.ZERO);
          })
          .awaitUntil("decoration work order exists", 200, () -> (order[0] = ConstructionSupport.orderAt(fixture.colony(), site)) != null, () -> "none")
          .check("it is a decoration order", () -> order[0] instanceof WorkOrderDecoration, () -> "order " + order[0])
          .awaitUntil("work order done and removed", 7000,
            () -> fixture.colony().getWorkManager().getWorkOrder(order[0].getID()) == null,
            () -> ConstructionSupport.describeBuilder(builderHut[0], builder[0]) + ", claimed by " + order[0].getClaimedBy() + ", material list "
                    + ConstructionSupport.materials(fixture.level(), blueprint[0], site) + ", blueprint blocks "
                    + blueprint[0].getBlockInfoAsList().stream().map(i -> i.getState().getBlock().toString()).distinct().toList() + ", entities " + blueprint[0].getEntitiesAsList().size())
          .check("world matches the decoration blueprint",
            () -> ConstructionSupport.mismatches(fixture.level(), blueprint[0], site, 10).isEmpty(),
            () -> String.join("; ", ConstructionSupport.mismatches(fixture.level(), blueprint[0], site, 10)))
          .check("no building registered for it",
            () -> fixture.colony().getServerBuildingManager().getBuilding(site) == null
                    && fixture.colony().getServerBuildingManager().getBuildings().size() == buildings[0],
            () -> "buildings before " + buildings[0] + ", now " + fixture.colony().getServerBuildingManager().getBuildings().size())
          .succeed();
    }

    /**
     * BLD-07 (E2E-BLD-07-1) A decoration with blueprint entities (item frames holding items): a builder given exactly the material list
     * the hut/decoration window computes finishes it without asking for more. Was a bug: Structurize's resource count
     * ({@code StructurePlacer.handleEntitySpawn} in simulate mode) returned after the first entity of a block position, so a second item
     * frame on the same block was missing from the list.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void bld07_decorationWithEntitiesMaterialList(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] builderHut = new IBuilding[1];
        final ICitizenData[] builder = new ICitizenData[1];
        final Blueprint[] blueprint = new Blueprint[1];
        final IServerWorkOrder[] order = new IServerWorkOrder[1];
        final List<ItemStack> materials = new ArrayList<>();
        final BlockPos site = fixture.abs(CORNER_SE);
        final String deco = "decorations/statues/pedastal_oak.blueprint";

        fixture.flow()
          .run("colony, builder with exactly the decoration's material list", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              builderHut[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              blueprint[0] = ConstructionSupport.blueprint(fixture.level(), deco);
              materials.addAll(ConstructionSupport.materials(fixture.level(), blueprint[0], site));
              org.slf4j.LoggerFactory.getLogger(ConstructionResearchTests.class).info("BLD-07: material list of pedastal_oak ({} entities): {}",
                blueprint[0].getEntitiesAsList().size(), materials);
              fixture.fillBuilding(builderHut[0], materials.toArray(ItemStack[]::new));
              fixture.fillBuilding(builderHut[0], ConstructionSupport.builderKit());
              builder[0] = fixture.spawnCitizen(40, builderHut[0], null);
              ConstructionSupport.requestDecoration(fixture, deco, site, BlockPos.ZERO);
          })
          .check("the decoration has entities", () -> !blueprint[0].getEntitiesAsList().isEmpty(), () -> "no entities in " + deco)
          .awaitUntil("decoration work order exists", 200, () -> (order[0] = ConstructionSupport.orderAt(fixture.colony(), site)) != null, () -> "none")
          .awaitUntil("work order done with only the listed materials", 7000,
            () -> fixture.colony().getWorkManager().getWorkOrder(order[0].getID()) == null,
            () -> ConstructionSupport.describeBuilder(builderHut[0], builder[0]) + ", material list " + materials)
          .check("world matches the decoration blueprint",
            () -> ConstructionSupport.mismatches(fixture.level(), blueprint[0], site, 10).isEmpty(),
            () -> String.join("; ", ConstructionSupport.mismatches(fixture.level(), blueprint[0], site, 10)))
          .succeed();
    }

    /**
     * BLD-08 Work orders are shared between builders: two builders take one order each; a manually assigned order goes to its builder.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 16000, skyAccess = true)
    public void bld08_workOrdersShared(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] builders = new ICitizenData[2];
        final Blueprint[] blueprint = new Blueprint[1];
        final IServerWorkOrder[] orders = new IServerWorkOrder[3];
        final BlockPos[] sites = {fixture.abs(CORNER_SW), fixture.abs(CORNER_SE), fixture.abs(new BlockPos(32, ColonyFixture.GROUND_Y, 54))};
        final Set<BlockPos> manualClaims = new HashSet<>();
        final String[] deco = new String[1];

        fixture.flow()
          .run("colony, two builders with materials", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              huts[1] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NE);
              deco[0] = ConstructionSupport.entityFreeDecoration(fixture.level());
              blueprint[0] = ConstructionSupport.blueprint(fixture.level(), deco[0]);
              for (final IBuilding hut : huts)
              {
                  for (int i = 0; i < 3; i++)
                  {
                      fixture.fillBuilding(hut, ConstructionSupport.materials(fixture.level(), blueprint[0], sites[i]).toArray(ItemStack[]::new));
                  }
                  fixture.fillBuilding(hut, ConstructionSupport.builderKit());
              }
              builders[0] = fixture.spawnCitizen(40, huts[0], null);
              builders[1] = fixture.spawnCitizen(40, huts[1], null);
              ConstructionSupport.requestDecoration(fixture, deco[0], sites[0], BlockPos.ZERO);
              ConstructionSupport.requestDecoration(fixture, deco[0], sites[1], BlockPos.ZERO);
          })
          .awaitUntil("both work orders exist", 200, () -> {
              orders[0] = ConstructionSupport.orderAt(fixture.colony(), sites[0]);
              orders[1] = ConstructionSupport.orderAt(fixture.colony(), sites[1]);
              return orders[0] != null && orders[1] != null;
          }, () -> "orders " + orders[0] + ", " + orders[1])
          .awaitUntil("both orders claimed", 400, () -> orders[0].isClaimed() && orders[1].isClaimed(),
            () -> "claimed by " + orders[0].getClaimedBy() + ", " + orders[1].getClaimedBy())
          .check("each order claimed by a different builder",
            () -> !orders[0].getClaimedBy().equals(orders[1].getClaimedBy())
                    && Set.of(orders[0].getClaimedBy(), orders[1].getClaimedBy()).equals(Set.of(huts[0].getID(), huts[1].getID())),
            () -> "claimed by " + orders[0].getClaimedBy() + ", " + orders[1].getClaimedBy())
          .run("request a third decoration assigned to the second builder", () ->
                 ConstructionSupport.requestDecoration(fixture, deco[0], sites[2], huts[1].getID()))
          .awaitUntil("third order exists", 200, () -> (orders[2] = ConstructionSupport.orderAt(fixture.colony(), sites[2])) != null, () -> "none")
          .awaitUntil("all three orders done (recording who claims the third)", 15000, () -> {
              if (fixture.colony().getWorkManager().getWorkOrder(orders[2].getID()) != null && orders[2].isClaimed())
              {
                  manualClaims.add(orders[2].getClaimedBy());
              }
              for (final IServerWorkOrder o : orders)
              {
                  if (fixture.colony().getWorkManager().getWorkOrder(o.getID()) != null)
                  {
                      return false;
                  }
              }
              return true;
          }, () -> ColonyFixture.describe(builders[0]) + " / " + ColonyFixture.describe(builders[1]))
          .check("the third order was only ever claimed by the second builder", () -> manualClaims.equals(Set.of(huts[1].getID())),
            () -> "claims " + manualClaims + ", second builder " + huts[1].getID())
          .check("all three decorations match the blueprint",
            () -> List.of(sites).stream().allMatch(s -> ConstructionSupport.mismatches(fixture.level(), blueprint[0], s, 10).isEmpty()),
            () -> List.of(sites).stream().map(s -> s.toShortString() + ": " + ConstructionSupport.mismatches(fixture.level(), blueprint[0], s, 5)).toList().toString())
          .succeed();
    }

    /**
     * BLD-09 Clearing the site: dirt bumps, grass, a small tree and a water pool on the build spot are cleared.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 42000, skyAccess = true)
    public void bld09_clearingTheSite(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] builder = new ICitizenData[1];
        final IServerWorkOrder[] order = new IServerWorkOrder[1];
        final Blueprint[] blueprint = new Blueprint[1];
        final BlockPos[] box = new BlockPos[2];

        fixture.flow()
          .run("colony, builder, residence site cluttered with terrain, plants, a tree and water", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              huts[1] = ConstructionSupport.placeHutForConstruction(fixture, ModBuildings.home.get(), CORNER_SE);
              blueprint[0] = ConstructionSupport.blueprint(fixture.level(), ConstructionSupport.subPath("residence1"));
              final BlockPos[] fp = ConstructionSupport.footprint(blueprint[0], huts[1].getPosition());
              box[0] = fp[0];
              box[1] = fp[1];
              clutter(fixture, fp, huts[1].getPosition());
              fixture.fillBuilding(huts[0], ConstructionSupport.materials(fixture.level(), blueprint[0], huts[1].getPosition()).toArray(ItemStack[]::new));
              fixture.fillBuilding(huts[0], new ItemStack(Items.DIRT, 64));
              fixture.fillBuilding(huts[0], ConstructionSupport.builderKit());
              builder[0] = fixture.spawnCitizen(40, huts[0], null);
              order[0] = fixture.addWorkOrder(WorkOrderType.BUILD, huts[1]);
          })
          .check("site is cluttered (water and plants inside the footprint)", () -> countIn(fixture, box, s -> s.getFluidState().is(Fluids.WATER)) > 0
                                                                                      && countIn(fixture, box, s -> s.getBlock() instanceof LeavesBlock) > 0,
            () -> "water " + countIn(fixture, box, s -> s.getFluidState().is(Fluids.WATER)))
          .check("build work order created", () -> order[0] != null, () -> "order " + order[0])
          .awaitUntil("work order done and removed", 40000,
            () -> fixture.colony().getWorkManager().getWorkOrder(order[0].getID()) == null,
            () -> ConstructionSupport.describeBuilder(huts[0], builder[0]) + ", residence level " + huts[1].getBuildingLevel())
          .check("world matches the residence1 blueprint", () -> ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 10).isEmpty(),
            () -> String.join("; ", ConstructionSupport.mismatches(fixture.level(), blueprint[0], huts[1].getPosition(), 10)))
          .check("no water, leaves, logs or grass plants left in the footprint",
            () -> leftoverClutter(fixture, blueprint[0], huts[1].getPosition()).isEmpty(),
            () -> "clutter the blueprint doesn't have: " + leftoverClutter(fixture, blueprint[0], huts[1].getPosition()))
          .succeed();
    }

    // ====================================================================================================================================
    // Research
    // ====================================================================================================================================

    /**
     * RES-01 Researcher finishes a research: progress only rises with a researcher; the finished research's effect is active.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 8000, skyAccess = true)
    public void res01_researcherFinishesResearch(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] university = new IBuilding[1];
        final ICitizenData[] researcher = new ICitizenData[1];
        final int total = IGlobalResearchTree.getInstance().getBranchData(Identifier.parse("minecolonies:civilian")).getBaseTime(1);
        final int[] progress = new int[1];

        fixture.flow()
          .run("colony, university with the cost items; start First Aid as a survival player", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              university[0] = fixture.placeBuilding(ModBuildings.university.get(), 1, CORNER_NW);
              fixture.fillBuilding(university[0], new ItemStack(Items.HAY_BLOCK, 8));
              fixture.owner().setGameMode(GameType.SURVIVAL);
              tryResearch(fixture, (BuildingUniversity) university[0], FIRST_AID);
              fixture.owner().setGameMode(GameType.CREATIVE);
          })
          .check("research started and cost taken", () -> local(fixture, FIRST_AID) != null && local(fixture, FIRST_AID).getState() == ResearchState.IN_PROGRESS
                                                            && fixture.countItems(university[0], s -> s.is(Items.HAY_BLOCK)) == 0,
            () -> "research " + describe(local(fixture, FIRST_AID)) + ", hay left " + fixture.countItems(university[0], s -> s.is(Items.HAY_BLOCK)))
          .run("skip ahead: progress to 3 steps before the end", () -> {
              local(fixture, FIRST_AID).setProgress(total - 3);
              progress[0] = total - 3;
          })
          // Absence check: research progresses on the colony's slow tick (every 500 ticks); 1100 ticks cover at least two.
          .idle(1100)
          .check("no progress without a researcher", () -> local(fixture, FIRST_AID).getProgress() == progress[0],
            () -> describe(local(fixture, FIRST_AID)))
          .run("hire a researcher", () -> researcher[0] = fixture.spawnCitizen(30, university[0], null))
          .awaitUntil("progress rises", 2000, () -> local(fixture, FIRST_AID).getProgress() > progress[0], () -> describe(local(fixture, FIRST_AID)))
          .awaitUntil("research finished", 4000, () -> local(fixture, FIRST_AID).getState() == ResearchState.FINISHED,
            () -> describe(local(fixture, FIRST_AID)) + ", " + ColonyFixture.describe(researcher[0]))
          .check("research is in the completed list and its effect is active",
            () -> tree(fixture).hasCompletedResearch(FIRST_AID) && fixture.colony().getResearchManager().getResearchEffects().getEffectStrength(HEALTH_BOOST) > 0,
            () -> "completed " + tree(fixture).getCompletedList() + ", health boost " + fixture.colony().getResearchManager().getResearchEffects().getEffectStrength(HEALTH_BOOST))
          .succeed();
    }

    /**
     * RES-02 Requirements block a research: Keen needs residences of total level 3; refused (no items taken) until the colony has them.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 600, skyAccess = true)
    public void res02_requirementsBlockResearch(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] university = new IBuilding[1];

        fixture.flow()
          .run("colony, university with 3 books, a level 1 residence; try Keen as a survival player", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              university[0] = fixture.placeBuilding(ModBuildings.university.get(), 1, CORNER_NW);
              fixture.placeBuilding(ModBuildings.home.get(), 1, CORNER_SE);
              fixture.fillBuilding(university[0], new ItemStack(Items.BOOK, 3));
              fixture.owner().setGameMode(GameType.SURVIVAL);
              tryResearch(fixture, (BuildingUniversity) university[0], KEEN);
          })
          .check("first attempt refused, no items taken", () -> local(fixture, KEEN) == null && fixture.countItems(university[0], s -> s.is(Items.BOOK)) == 3,
            () -> "research " + describe(local(fixture, KEEN)) + ", books " + fixture.countItems(university[0], s -> s.is(Items.BOOK)))
          .run("add a level 3 residence and try again", () -> {
              fixture.placeBuilding(ModBuildings.home.get(), 3, CORNER_SW);
              tryResearch(fixture, (BuildingUniversity) university[0], KEEN);
              fixture.owner().setGameMode(GameType.CREATIVE);
          })
          .check("second attempt starts the research and takes the books",
            () -> local(fixture, KEEN) != null && local(fixture, KEEN).getState() == ResearchState.IN_PROGRESS
                    && fixture.countItems(university[0], s -> s.is(Items.BOOK)) == 0,
            () -> "research " + describe(local(fixture, KEEN)) + ", books " + fixture.countItems(university[0], s -> s.is(Items.BOOK)))
          .succeed();
    }

    /**
     * RES-03 A research changes behavior: First Aid raises the citizens' real maximum health (entity attribute) by the effect's strength,
     * for citizens alive at that time and for citizens born later.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 600, skyAccess = true)
    public void res03_researchChangesBehavior(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] citizens = new ICitizenData[2];
        final double[] health = new double[2];

        fixture.flow()
          .run("colony with one citizen", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              citizens[0] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("citizen entity exists", 100, () -> citizens[0].getEntity().isPresent(), () -> "no entity")
          .run("measure, then finish First Aid", () -> {
              health[0] = citizens[0].getEntity().get().getAttributeValue(Attributes.MAX_HEALTH);
              fixture.completeResearch(FIRST_AID);
              health[1] = fixture.colony().getResearchManager().getResearchEffects().getEffectStrength(HEALTH_BOOST);
          })
          .check("the effect has a strength", () -> health[1] > 0, () -> "strength " + health[1])
          .check("existing citizen's max health rose by the effect strength",
            () -> Math.abs(citizens[0].getEntity().get().getAttributeValue(Attributes.MAX_HEALTH) - (health[0] + health[1])) < 0.01,
            () -> "before " + health[0] + ", strength " + health[1] + ", now " + citizens[0].getEntity().get().getAttributeValue(Attributes.MAX_HEALTH))
          .run("spawn a second citizen", () -> citizens[1] = fixture.spawnCitizen(10, null, null))
          .awaitUntil("second citizen entity exists", 100, () -> citizens[1].getEntity().isPresent(), () -> "no entity")
          .check("new citizen also has the raised max health",
            () -> Math.abs(citizens[1].getEntity().get().getAttributeValue(Attributes.MAX_HEALTH) - (health[0] + health[1])) < 0.01,
            () -> "new citizen max health " + citizens[1].getEntity().get().getAttributeValue(Attributes.MAX_HEALTH) + ", expected " + (health[0] + health[1]))
          .check("the raised health can really be filled (heal to max)", () -> {
              final var entity = citizens[1].getEntity().get();
              entity.setHealth(entity.getMaxHealth());
              return entity.getHealth() > health[0];
          }, () -> "health " + citizens[1].getEntity().get().getHealth())
          .succeed();
    }

    /**
     * RES-04 Unlock research: the hospital can't be built until Stamina is researched.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 600, skyAccess = true)
    public void res04_unlockResearch(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final IServerWorkOrder[] orders = new IServerWorkOrder[2];

        fixture.flow()
          .run("colony, builder, hospital hut placed with the survival build tool; request its build", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, CORNER_NW);
              fixture.spawnCitizen(20, huts[0], null);
              huts[1] = ConstructionSupport.placeHutForConstruction(fixture, ModBuildings.hospital.get(), CORNER_SE);
              orders[0] = fixture.addWorkOrder(WorkOrderType.BUILD, huts[1]);
          })
          .check("build refused without the research", () -> orders[0] == null, () -> "order " + orders[0])
          .run("finish Stamina and request again", () -> {
              fixture.completeResearch(STAMINA);
              orders[1] = fixture.addWorkOrder(WorkOrderType.BUILD, huts[1]);
          })
          .check("build accepted after the research", () -> orders[1] != null && orders[1].getWorkOrderType() == WorkOrderType.BUILD, () -> "order " + orders[1])
          .succeed();
    }

    /**
     * RES-05 Undoing a research: refused while a child research exists; otherwise the research is removed, its effect goes and the undo cost
     * is taken.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 600, skyAccess = true)
    public void res05_undoResearch(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final double[] strength = new double[2];
        final List<ItemStorage> cost = new ArrayList<>();
        final int[] costItems = new int[1];

        fixture.flow()
          .run("colony with First Aid and First Aid II finished; survival owner holding twice the undo cost", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              fixture.placeBuilding(ModBuildings.university.get(), 1, CORNER_NW);
              fixture.completeResearch(FIRST_AID);
              strength[0] = effect(fixture);
              fixture.completeResearch(FIRST_AID2);
              strength[1] = effect(fixture);
              cost.addAll(IGlobalResearchTree.getInstance().getResearchResetCosts(fixture.level().registryAccess()));
              fixture.owner().getInventory().clearContent();
              for (final ItemStorage c : cost)
              {
                  fixture.owner().getInventory().add(c.getItemStack().copyWithCount(c.getAmount() * 2));
              }
              costItems[0] = ownerCostItems(fixture, cost);
              fixture.owner().setGameMode(GameType.SURVIVAL);
          })
          .check("undo cost is configured and effects stack", () -> !cost.isEmpty() && costItems[0] > 0 && strength[1] > strength[0] && strength[0] > 0,
            () -> "cost " + cost + ", owner holds " + costItems[0] + ", strengths " + strength[0] + " / " + strength[1])
          .run("undo First Aid (has a finished child)", () -> resetResearch(fixture, FIRST_AID))
          .check("refused: still finished, effect unchanged, nothing taken",
            () -> local(fixture, FIRST_AID) != null && local(fixture, FIRST_AID).getState() == ResearchState.FINISHED && effect(fixture) == strength[1]
                    && ownerCostItems(fixture, cost) == costItems[0],
            () -> describe(local(fixture, FIRST_AID)) + ", effect " + effect(fixture) + ", cost items " + ownerCostItems(fixture, cost))
          .run("undo First Aid II", () -> resetResearch(fixture, FIRST_AID2))
          .check("First Aid II is gone, effect back to First Aid's strength, cost taken",
            () -> local(fixture, FIRST_AID2) == null && !tree(fixture).hasCompletedResearch(FIRST_AID2) && effect(fixture) == strength[0]
                    && ownerCostItems(fixture, cost) == costItems[0] / 2,
            () -> describe(local(fixture, FIRST_AID2)) + ", effect " + effect(fixture) + ", cost items " + ownerCostItems(fixture, cost))
          .run("undo First Aid", () -> resetResearch(fixture, FIRST_AID))
          .check("First Aid is gone, effect gone, cost taken",
            () -> local(fixture, FIRST_AID) == null && effect(fixture) == 0 && ownerCostItems(fixture, cost) == 0,
            () -> describe(local(fixture, FIRST_AID)) + ", effect " + effect(fixture) + ", cost items " + ownerCostItems(fixture, cost))
          .run("back to creative", () -> fixture.owner().setGameMode(GameType.CREATIVE))
          .succeed();
    }

    // ====================================================================================================================================
    // Helpers
    // ====================================================================================================================================

    private static String key(final ItemStack stack)
    {
        return stack.getItem().getDescriptionId() + "-" + stack.getComponentsPatch().hashCode();
    }

    private static boolean isMaterial(final List<ItemStack> materials, final ItemStack stack)
    {
        return materials.stream().anyMatch(m -> ItemStackUtils.compareItemStacksIgnoreStackSize(m, stack));
    }

    private static List<String> changed(final ColonyFixture fixture, final Map<BlockPos, BlockState> expected)
    {
        final List<String> result = new ArrayList<>();
        expected.forEach((pos, state) -> {
            final BlockState now = fixture.level().getBlockState(pos);
            if (!now.equals(state) && result.size() < 10)
            {
                result.add(fixture.rel(pos).toShortString() + ": was " + state + ", now " + now);
            }
        });
        return result;
    }

    private static int solidAboveGround(final ColonyFixture fixture, final Blueprint blueprint, final BlockPos anchor)
    {
        final BlockPos[] fp = ConstructionSupport.footprint(blueprint, anchor);
        final int groundY = fixture.abs(new BlockPos(0, ColonyFixture.GROUND_Y, 0)).getY();
        int count = 0;
        for (final BlockPos pos : BlockPos.betweenClosed(new BlockPos(fp[0].getX(), Math.max(groundY, fp[0].getY()), fp[0].getZ()), fp[1]))
        {
            if (!fixture.level().getBlockState(pos).isAir())
            {
                count++;
            }
        }
        return count;
    }

    private static int recovered(final ColonyFixture fixture, final IBuilding hut, final ICitizenData builder)
    {
        final Predicate<ItemStack> wood = s -> !s.isEmpty() && (s.is(net.minecraft.tags.ItemTags.LOGS) || s.is(net.minecraft.tags.ItemTags.PLANKS)
                                                                  || s.is(net.minecraft.tags.ItemTags.WOODEN_SLABS) || s.is(net.minecraft.tags.ItemTags.WOODEN_STAIRS)
                                                                  || s.is(Items.COBBLESTONE) || s.is(Items.DIRT));
        int count = fixture.countItems(hut, wood);
        for (int i = 0; i < builder.getInventory().getSlots(); i++)
        {
            final ItemStack stack = builder.getInventory().getStackInSlot(i);
            if (wood.test(stack))
            {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Puts terrain bumps, grass, a small tree and a water pool inside the footprint (not on the hut block's column).
     */
    private static void clutter(final ColonyFixture fixture, final BlockPos[] fp, final BlockPos hutPos)
    {
        final int groundY = fixture.abs(new BlockPos(0, ColonyFixture.GROUND_Y, 0)).getY();
        final int x0 = fp[0].getX() + 1;
        final int z0 = fp[0].getZ() + 1;
        final var level = fixture.level();
        // Dirt bumps
        for (int i = 0; i < 4; i++)
        {
            level.setBlockAndUpdate(new BlockPos(x0 + i, groundY, z0), Blocks.DIRT.defaultBlockState());
        }
        level.setBlockAndUpdate(new BlockPos(x0 + 1, groundY + 1, z0), Blocks.DIRT.defaultBlockState());
        // Grass plants
        for (int i = 0; i < 4; i++)
        {
            level.setBlockAndUpdate(new BlockPos(x0 + i, groundY, z0 + 2), Blocks.SHORT_GRASS.defaultBlockState());
        }
        // Small tree: 3 logs and a leaf cap (persistent, so they don't decay on their own)
        final BlockPos trunk = new BlockPos(x0 + 1, groundY, z0 + 5);
        for (int y = 0; y < 3; y++)
        {
            level.setBlockAndUpdate(trunk.above(y), Blocks.OAK_LOG.defaultBlockState());
        }
        for (final BlockPos leaf : BlockPos.betweenClosed(trunk.offset(-1, 2, -1), trunk.offset(1, 3, 1)))
        {
            if (level.getBlockState(leaf).isAir())
            {
                level.setBlockAndUpdate(leaf, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
            }
        }
        // Water pool (2x2 sources sunk into the floor)
        final BlockPos pool = new BlockPos(fp[1].getX() - 3, groundY - 1, fp[1].getZ() - 3);
        for (final BlockPos water : BlockPos.betweenClosed(pool, pool.offset(1, 0, 1)))
        {
            if (!water.equals(hutPos))
            {
                level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
            }
        }
    }

    /**
     * Positions of the blueprint where the world has water, grass plants, oak leaves or logs that the blueprint doesn't have there
     * (the residence itself is built from oak logs).
     */
    private static List<String> leftoverClutter(final ColonyFixture fixture, final Blueprint blueprint, final BlockPos anchor)
    {
        final List<String> result = new ArrayList<>();
        final BlockPos origin = anchor.subtract(blueprint.getPrimaryBlockOffset());
        for (final com.ldtteam.structurize.util.BlockInfo info : blueprint.getBlockInfoAsList())
        {
            final BlockPos pos = origin.offset(info.getPos());
            final BlockState world = fixture.level().getBlockState(pos);
            if (isClutter(world) && world.getBlock() != info.getState().getBlock() && result.size() < 20)
            {
                result.add(fixture.rel(pos).toShortString() + " " + world.getBlock() + " (blueprint " + info.getState().getBlock() + ")");
            }
        }
        return result;
    }

    private static boolean isClutter(final BlockState state)
    {
        return state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.FLOWING_WATER) || state.is(Blocks.SHORT_GRASS)
                 || state.is(Blocks.OAK_LEAVES) || state.is(Blocks.OAK_LOG);
    }

    private static int countIn(final ColonyFixture fixture, final BlockPos[] box, final Predicate<BlockState> matcher)
    {
        int count = 0;
        for (final BlockPos pos : BlockPos.betweenClosed(box[0], box[1]))
        {
            if (matcher.test(fixture.level().getBlockState(pos)))
            {
                count++;
            }
        }
        return count;
    }

    private static ILocalResearchTree tree(final ColonyFixture fixture)
    {
        return fixture.colony().getResearchManager().getResearchTree();
    }

    private static ILocalResearch local(final ColonyFixture fixture, final Identifier id)
    {
        final IGlobalResearch research = IGlobalResearchTree.getInstance().getResearch(id);
        return tree(fixture).getResearch(research.getBranch(), id);
    }

    private static String describe(final ILocalResearch research)
    {
        return research == null ? "not started" : research.getId() + " " + research.getState() + " progress " + research.getProgress();
    }

    private static double effect(final ColonyFixture fixture)
    {
        return fixture.colony().getResearchManager().getResearchEffects().getEffectStrength(HEALTH_BOOST);
    }

    /**
     * The server side of the research window's "research" button ({@code TryResearchMessage#onExecute}, not reset).
     */
    private static void tryResearch(final ColonyFixture fixture, final BuildingUniversity university, final Identifier id)
    {
        final IGlobalResearch research = IGlobalResearchTree.getInstance().getResearch(id);
        if (research.canResearch(university, tree(fixture)) || fixture.owner().isCreative())
        {
            tree(fixture).attemptBeginResearch(fixture.owner(), fixture.colony(), university, research);
        }
    }

    /**
     * The server side of the research window's "undo"/"cancel" button ({@code TryResearchMessage#onExecute} with reset).
     */
    private static void resetResearch(final ColonyFixture fixture, final Identifier id)
    {
        final ILocalResearch local = local(fixture, id);
        if (local != null)
        {
            tree(fixture).attemptResetResearch(fixture.owner(), fixture.colony(), local);
        }
    }

    private static int ownerCostItems(final ColonyFixture fixture, final List<ItemStorage> cost)
    {
        int count = 0;
        for (int i = 0; i < fixture.owner().getInventory().getContainerSize(); i++)
        {
            final ItemStack stack = fixture.owner().getInventory().getItem(i);
            if (cost.stream().anyMatch(c -> ItemStackUtils.compareItemStacksIgnoreStackSize(c.getItemStack(), stack, false, false)))
            {
                count += stack.getCount();
            }
        }
        return count;
    }
}
