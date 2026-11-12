package com.minecolonies.gametest.ui;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.gametest.framework.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

/**
 * Server half of the UI tests (docs/e2e-tests/ui.md): prepares a colony with the {@link ColonyFixture} on the integrated server of the UI
 * client test, publishes it through {@link UiBridge}, and keeps it alive until the client test has taken its screenshots. Then the test
 * passes and the fixture deletes the colony as usual.
 * <p>
 * {@code manualOnly}: the headless run ({@code ./gradlew runGameTest}) never runs it; only {@code UiClientGameTest} starts it, by id
 * {@code minecolonies-gametest:ui_colony_setup_colony}.
 */
public class UiColonySetup
{
    /**
     * Test id selector used by the UI client test.
     */
    public static final String TEST_SELECTOR = "minecolonies-gametest:ui_colony_setup_*";

    public static final BlockPos BUILDER_POS    = new BlockPos(12, ColonyFixture.GROUND_Y, 12);
    public static final BlockPos WAREHOUSE_POS  = new BlockPos(52, ColonyFixture.GROUND_Y, 12);
    public static final BlockPos UNIVERSITY_POS = new BlockPos(12, ColonyFixture.GROUND_Y, 52);
    public static final BlockPos POSTBOX_POS    = new BlockPos(44, ColonyFixture.GROUND_Y, 44);

    /**
     * Skill levels of the builder: all different, so the citizen window check (UI-03) can't pass by accident.
     */
    public static Map<Skill, Integer> workerSkills()
    {
        final Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        int level = 3;
        for (final Skill skill : Skill.values())
        {
            skills.put(skill, level);
            level += 4;
        }
        return skills;
    }

    /**
     * 20 minutes at 20 ticks per second: the client test normally needs well under a minute. While a window pauses the game the server
     * doesn't tick, so time spent there doesn't count.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 24000, skyAccess = true, manualOnly = true)
    public void colony(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        fixture.flow()
          .run("set up the colony for the UI tests", () -> {
              try
              {
                  final IColony colony = fixture.createColony();
                  fixture.setMoveIn(false);
                  final IBuilding builder = fixture.placeBuilding(ModBuildings.builder.get(), 1, BUILDER_POS);
                  final IBuilding warehouse = fixture.placeBuilding(ModBuildings.wareHouse.get(), 1, WAREHOUSE_POS);
                  final IBuilding university = fixture.placeBuilding(ModBuildings.university.get(), 1, UNIVERSITY_POS);
                  final BlockPos postBox = placePostBox(fixture);

                  final ICitizenData worker = fixture.spawnCitizen(workerSkills(), builder, null, null);
                  final ICitizenData second = fixture.spawnCitizen(10, null, null);
                  fixture.startResearch(UiBridge.STARTED_RESEARCH, 0);

                  // The real player of the game window becomes an officer, so the colony sends him its data and he may open every window.
                  for (final ServerPlayer player : helper.getLevel().getServer().getPlayerList().getPlayers())
                  {
                      colony.getPermissions().addPlayer(player.getUUID(), player.getName().getString(), colony.getPermissions().getRankOfficer());
                  }

                  UiBridge.colony = new UiBridge.Colony(colony.getID(), colony.getCenter(), builder.getPosition(), warehouse.getPosition(),
                    university.getPosition(), postBox, worker.getId(), second.getId());
              }
              catch (final RuntimeException e)
              {
                  UiBridge.setupError = e.toString();
                  throw e;
              }
          })
          .awaitUntil("the UI client test has taken its screenshots", () -> UiBridge.clientDone, () -> "client not done yet")
          .succeed();
    }

    /**
     * The post box is a block without a blueprint: placed like a player places it, which registers the building.
     */
    private static BlockPos placePostBox(final ColonyFixture fixture)
    {
        final BlockPos abs = fixture.abs(POSTBOX_POS);
        final Block block = ModBlocks.blockPostBox;
        final BlockState state = block.defaultBlockState();
        fixture.level().setBlock(abs, state, Block.UPDATE_ALL);
        block.setPlacedBy(fixture.level(), abs, state, fixture.owner(), new ItemStack(block));
        if (fixture.colony().getServerBuildingManager().getBuilding(abs) == null)
        {
            throw new IllegalStateException("Post box at " + abs + " was not registered in the colony");
        }
        return abs;
    }
}
