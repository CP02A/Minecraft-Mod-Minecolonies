package com.minecolonies.gametest.framework;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.managers.interfaces.IRaiderManager;
import com.minecolonies.api.colony.workorders.IServerWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.core.MineColonies;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Checks the assumptions the framework relies on, so a change in MineColonies shows up as one clear failure instead of many flaky tests.
 */
public class FrameworkGuardTests
{
    /**
     * framework.md "Pathfinding stays deterministic": every running thread named {@code Minecolonies Pathfinding*} must be the thread of
     * {@code Pathfinding.getExecutor()}, the one executor the test server waits for at the end of each tick. If MineColonies adds a second
     * pathfinding thread, this fails instead of the other tests quietly becoming timing dependent.
     */
    @GameTest
    public void pathfindingRunsOnTheAwaitedExecutor(final GameTestHelper helper) throws Exception
    {
        final Thread awaited = PathfindingSync.pathfindingThread();
        final List<Thread> others = Thread.getAllStackTraces().keySet().stream()
          .filter(thread -> thread.getName().startsWith("Minecolonies Pathfinding"))
          .filter(thread -> thread != awaited)
          .toList();
        helper.assertTrue(others.isEmpty(), "Pathfinding threads the test server does not wait for: " + others);
        helper.succeed();
    }

    /**
     * Smoke test of the fixture helpers, so a broken helper shows up here and not in six test groups at once.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 200, skyAccess = true)
    public void fixtureHelpersWork(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] buildings = new IBuilding[3];
        final ICitizenData[] citizens = new ICitizenData[2];
        final IServerWorkOrder[] order = new IServerWorkOrder[1];
        final IRaiderManager.RaidSpawnResult[] raid = new IRaiderManager.RaidSpawnResult[1];
        final Identifier finished = Identifier.parse("minecolonies:civilian/agile");
        final Identifier started = Identifier.parse("minecolonies:civilian/athlete");

        fixture.flow()
          .run("set up colony with warehouse, residence and builder's hut", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              buildings[0] = fixture.placeBuilding(ModBuildings.wareHouse.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 52));
              buildings[1] = fixture.placeBuilding(ModBuildings.home.get(), 1, new BlockPos(52, ColonyFixture.GROUND_Y, 12));
              buildings[2] = fixture.placeBuilding(ModBuildings.builder.get(), 2, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
          })
          .run("fill warehouse", () -> fixture.fillWarehouse(new ItemStack(Items.OAK_LOG, 64), new ItemStack(Items.COBBLESTONE, 32)))
          .check("warehouse holds the items", () -> count(fixture, buildings[0], Items.OAK_LOG) == 64 && count(fixture, buildings[0], Items.COBBLESTONE) == 32,
            () -> "oak logs " + count(fixture, buildings[0], Items.OAK_LOG) + ", cobblestone " + count(fixture, buildings[0], Items.COBBLESTONE))
          .run("spawn a resident and a builder", () -> {
              citizens[0] = fixture.spawnCitizen(10, null, buildings[1]);
              citizens[1] = fixture.spawnCitizen(30, buildings[2], null);
          })
          .awaitUntil("both citizens have entities", 100, () -> citizens[0].getEntity().isPresent() && citizens[1].getEntity().isPresent(),
            () -> ColonyFixture.describe(citizens[0]) + " / " + ColonyFixture.describe(citizens[1]))
          .check("homes, job and skills are set",
            () -> citizens[0].getHomeBuilding() == buildings[1] && citizens[1].getWorkBuilding() == buildings[2] && citizens[1].getJob() != null
                    && citizens[1].getCitizenSkillHandler().getLevel(com.minecolonies.api.entity.citizen.Skill.Adaptability) == 30,
            () -> "home " + citizens[0].getHomeBuilding() + ", work " + citizens[1].getWorkBuilding() + ", job " + citizens[1].getJob())
          .run("set needs", () -> fixture.setCitizenNeeds(citizens[0], 5, 10))
          .check("needs applied", () -> citizens[0].getSaturation() == 5 && citizens[0].getEntity().get().getHealth() == 10,
            () -> "saturation " + citizens[0].getSaturation() + ", health " + citizens[0].getEntity().get().getHealth())
          .run("request an upgrade of the residence", () -> order[0] = fixture.addWorkOrder(WorkOrderType.UPGRADE, buildings[1]))
          .check("work order exists", () -> order[0] != null && order[0].getTargetLevel() == 2,
            () -> "order " + order[0])
          .run("research", () -> {
              fixture.completeResearch(finished);
              fixture.startResearch(started, 5);
          })
          .check("research finished / in progress",
            () -> fixture.colony().getResearchManager().getResearchTree().hasCompletedResearch(finished)
                    && fixture.colony().getResearchManager().getResearchTree().getResearchInProgress().stream().anyMatch(r -> r.getId().equals(started)),
            () -> "completed " + fixture.colony().getResearchManager().getResearchTree().getCompletedList())
          .run("config and time", () -> {
              fixture.setConfig(MineColonies.getConfig().getServer().creativeResolve, true);
              fixture.setTime(13000);
          })
          .check("config value and time changed", () -> MineColonies.getConfig().getServer().creativeResolve.get() && helper.getLevel().getOverworldClockTime() % 24000 == 13000,
            () -> "creativeResolve " + MineColonies.getConfig().getServer().creativeResolve.get() + ", time " + helper.getLevel().getOverworldClockTime())
          .run("start a raid", () -> raid[0] = fixture.startRaid("barbarian_raid", 2, new BlockPos(56, ColonyFixture.GROUND_Y, 56)))
          .check("raid started", () -> raid[0] == IRaiderManager.RaidSpawnResult.SUCCESS, () -> "result " + raid[0])
          .succeed();
    }

    /**
     * Fixture geometry and players: {@code rel} is the inverse of {@code abs}; the deep ground has stone below the first mining level and
     * no barrier floor; the owner starts clean and can be put into the player lists; a second (bare) colony gets huts and citizens; hiring
     * into the warehouse fails loudly.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 200, skyAccess = true)
    public void fixtureGeometryPlayersAndColonies(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BlockPos probe = new BlockPos(44, ColonyFixture.GROUND_Y, 12);
        final BlockPos below = new BlockPos(ColonyFixture.ARENA_SIZE / 2, -1, ColonyFixture.ARENA_SIZE / 2);
        final IBuilding[] buildings = new IBuilding[2];
        final IColony[] second = new IColony[1];
        final ICitizenData[] citizen = new ICitizenData[1];

        fixture.flow()
          .check("rel(abs(p)) == p", () -> fixture.rel(fixture.abs(probe)).equals(probe) && fixture.rel(fixture.abs(BlockPos.ZERO)).equals(BlockPos.ZERO),
            () -> "rel(abs(" + probe.toShortString() + ")) = " + fixture.rel(fixture.abs(probe)).toShortString())
          .check("the owner starts in creative mode with an empty inventory",
            () -> fixture.owner().isCreative() && fixture.owner().getInventory().isEmpty(),
            () -> "creative " + fixture.owner().isCreative() + ", inventory empty " + fixture.owner().getInventory().isEmpty())
          .run("deep ground", fixture::makeDeepGround)
          .check("stone from the arena floor down to below the first mining level (y -16), no barrier floor",
            () -> helper.getLevel().getBlockState(fixture.abs(below)).is(Blocks.STONE)
                    && helper.getLevel().getBlockState(fixture.abs(below).atY(-20)).is(Blocks.STONE)
                    && helper.getLevel().getBlockState(fixture.abs(below).atY(ColonyFixture.DEEP_GROUND_BOTTOM_Y)).is(Blocks.STONE),
            () -> "at " + fixture.abs(below).toShortString() + ": " + helper.getLevel().getBlockState(fixture.abs(below)) + ", at y -20: "
                    + helper.getLevel().getBlockState(fixture.abs(below).atY(-20)))
          .run("main colony with a warehouse, a bare second colony with a lumberjack and a worker", () -> {
              fixture.setConfig(MineColonies.getConfig().getServer().initialColonySize, 1);
              fixture.createColony(new BlockPos(16, ColonyFixture.GROUND_Y, 44)); // positions as in CON-01 and ROB-05
              fixture.setMoveIn(false);
              buildings[0] = fixture.placeBuilding(ModBuildings.wareHouse.get(), 1, new BlockPos(24, ColonyFixture.GROUND_Y, 30));
              second[0] = fixture.createBareColony(new BlockPos(62, ColonyFixture.GROUND_Y, 40), fixture.fakePlayer("E2EOwnerB", GameType.CREATIVE), "E2E colony B");
              buildings[1] = fixture.placeBuilding(second[0], ModBuildings.lumberjack.get(), 1, new BlockPos(58, ColonyFixture.GROUND_Y, 44));
              citizen[0] = fixture.spawnCitizen(second[0], 10, buildings[1], null);
          })
          .check("the second colony's worker belongs to it",
            () -> second[0] != fixture.colony() && citizen[0].getColony() == second[0] && citizen[0].getWorkBuilding() == buildings[1],
            () -> "colony " + citizen[0].getColony().getID() + ", work " + citizen[0].getWorkBuilding())
          .check("hiring into the warehouse fails with an explanation", () -> {
              try
              {
                  fixture.hire(fixture.colony().getCitizenManager().createAndRegisterCivilianData(), buildings[0]);
                  return false;
              }
              catch (final IllegalArgumentException e)
              {
                  return e.getMessage().contains("courier's hut");
              }
          }, () -> "no exception")
          .run("owner into the player lists", fixture::addOwnerToPlayerLists)
          .check("the owner is online", () -> helper.getLevel().players().contains(fixture.owner())
                                                 && helper.getLevel().getServer().getPlayerList().getPlayers().contains(fixture.owner()),
            () -> "level players " + helper.getLevel().players())
          .run("give the owner an item (the next test must not see it)", () -> fixture.owner().getInventory().add(new ItemStack(Items.DIAMOND)))
          .succeed();
    }

    private static int rerunAttempts;

    /**
     * {@code maxAttempts} works for colony tests: fails the first attempt on purpose, passes the second ({@link RerunTracking}).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 40, skyAccess = true, maxAttempts = 2)
    public void colonyTestsAreRerun(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final int attempt = ++rerunAttempts;
        fixture.flow()
          .check("this is a re-run (the first attempt fails on purpose)", () -> attempt >= 2, () -> "attempt " + attempt)
          .succeed();
    }

    private static int count(final ColonyFixture fixture, final IBuilding building, final Item item)
    {
        return fixture.countItems(building, stack -> stack.is(item));
    }
}
