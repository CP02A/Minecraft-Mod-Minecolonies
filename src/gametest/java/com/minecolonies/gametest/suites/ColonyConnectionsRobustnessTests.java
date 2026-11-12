package com.minecolonies.gametest.suites;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.util.ColonyUtils;
import com.minecolonies.core.MineColonies;
import com.minecolonies.gametest.framework.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * End-to-end tests from docs/e2e-tests/colony.md, docs/e2e-tests/connections.md and docs/e2e-tests/robustness.md.
 * <p>
 * Test ids: {@code minecolonies-gametest:colony_connections_robustness_tests_<method_name>}. Run only these with
 * {@code ./gradlew runGameTest -Pe2eFilter=colony_connections_robustness_tests}. See docs/e2e-tests/AGENT-GUIDE.md.
 */
public class ColonyConnectionsRobustnessTests
{
    /**
     * COL-01 Found a colony: placing a town hall founds a working colony owned by the right player.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 100, skyAccess = true)
    public void col01_foundColony(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BlockPos townHall = fixture.abs(ColonyFixture.ARENA_CENTER);
        final int coloniesBefore = IColonyManager.getInstance().getColonies(helper.getLevel()).size();

        fixture.flow()
          .run("place town hall and found colony", fixture::createColony)
          .awaitUntil("colony registered at the town hall",
            () -> IColonyManager.getInstance().getColonyByPosFromWorld(helper.getLevel(), townHall) != null,
            () -> "colonies: " + IColonyManager.getInstance().getColonies(helper.getLevel()).size())
          .check("exactly one new colony, at the town hall, in the overworld",
            () -> IColonyManager.getInstance().getColonies(helper.getLevel()).size() == coloniesBefore + 1
                    && fixture.colony().getCenter().equals(townHall)
                    && fixture.colony().getDimension() == Level.OVERWORLD,
            () -> "colonies " + IColonyManager.getInstance().getColonies(helper.getLevel()).size() + " (before " + coloniesBefore + "), center "
                    + fixture.colony().getCenter() + ", dimension " + fixture.colony().getDimension().identifier())
          .check("owner is the fake player, with the owner rank",
            () -> ColonyFixture.OWNER_ID.equals(fixture.colony().getPermissions().getOwner())
                    && fixture.colony().getPermissions().getRank(ColonyFixture.OWNER_ID) == fixture.colony().getPermissions().getRankOwner(),
            () -> "owner " + fixture.colony().getPermissions().getOwner() + " (" + fixture.colony().getPermissions().getOwnerName() + "), rank "
                    + fixture.colony().getPermissions().getRank(ColonyFixture.OWNER_ID).getName())
          .check("town hall registered at level 1",
            () -> {
                final IBuilding hall = fixture.colony().getServerBuildingManager().getTownHall();
                return hall != null && hall.getPosition().equals(townHall) && hall.getBuildingLevel() == 1;
            },
            () -> {
                final IBuilding hall = fixture.colony().getServerBuildingManager().getTownHall();
                return hall == null ? "no town hall" : "town hall at " + hall.getPosition() + ", level " + hall.getBuildingLevel();
            })
          .check("chunks within initialColonySize of the town hall are claimed by the colony",
            () -> unclaimedChunks(fixture.colony(), helper).isEmpty(),
            () -> "not claimed by colony " + fixture.colony().getID() + ": " + unclaimedChunks(fixture.colony(), helper))
          .succeed();
    }

    /**
     * Framework check for real pathing: a hired worker spawned in the far corner of the arena walks to his hut
     * (a lumberjack: unlike a builder without work orders, he never goes idle, so he heads to his hut at once). Validates that citizens get
     * paths from the background pathfinding thread on the test server (the end-of-tick pathfinding wait).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 3000, skyAccess = true)
    public void workerWalksToHut(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];

        fixture.flow()
          .run("found colony, place lumberjack's hut, hire a lumberjack", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              worker[0] = fixture.spawnCitizen(20, hut[0], null);
          })
          .awaitUntil("lumberjack entity exists", 200, () -> worker[0].getEntity().isPresent(), () -> "no entity yet")
          .run("move the lumberjack to the far corner", () -> {
              final BlockPos corner = fixture.abs(new BlockPos(56, ColonyFixture.GROUND_Y, 56));
              worker[0].getEntity().get().teleportTo(corner.getX() + 0.5, corner.getY(), corner.getZ() + 0.5);
          })
          .awaitUntil("lumberjack walks to within 4 blocks of his hut", 2500,
            () -> horizontalDistance(worker[0], hut[0].getPosition()) <= 4,
            () -> ColonyFixture.describe(worker[0]) + ", distance " + String.format("%.1f", horizontalDistance(worker[0], hut[0].getPosition()))
                    + ", hut at " + hut[0].getPosition().toShortString())
          .succeed();
    }

    private static double horizontalDistance(final ICitizenData citizen, final BlockPos target)
    {
        return citizen.getEntity()
          .map(entity -> Math.sqrt(Math.pow(entity.getX() - (target.getX() + 0.5), 2) + Math.pow(entity.getZ() - (target.getZ() + 0.5), 2)))
          .orElse(Double.MAX_VALUE);
    }

    private static List<ChunkPos> unclaimedChunks(final IColony colony, final GameTestHelper helper)
    {
        final int radius = MineColonies.getConfig().getServer().initialColonySize.get();
        final ChunkPos center = ChunkPos.containing(colony.getCenter());
        final List<ChunkPos> unclaimed = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                final int x = center.x() + dx;
                final int z = center.z() + dz;
                if (ColonyUtils.getOwningColony(helper.getLevel().getChunk(x, z)) != colony.getID())
                {
                    unclaimed.add(new ChunkPos(x, z));
                }
            }
        }
        return unclaimed;
    }
}
