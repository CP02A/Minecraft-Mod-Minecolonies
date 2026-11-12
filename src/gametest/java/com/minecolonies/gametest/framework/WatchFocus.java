package com.minecolonies.gametest.framework;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.util.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Watch mode camera target (docs/e2e-tests/framework.md, "Watch mode"): the part of a running test's area where something happens, as a
 * list of boxes (one per hut and per entity). The client test ({@code WatchClientGameTest}) frames all of them from above.
 * <p>
 * In a colony test that is the town hall, every citizen and the huts they work or live in, plus every other living entity in the area
 * (animals, raiders, visitors), so the camera follows the citizens as they move. Before the colony exists, and in tests without a colony,
 * it is the whole test area (a small test area gives a close-up). Server thread only.
 */
public final class WatchFocus
{
    /**
     * @param bounds the box around all parts (for the log).
     * @param parts  what must be in the picture: huts, entities, or the whole test area.
     */
    public record Focus(AABB bounds, List<AABB> parts)
    {
    }

    /**
     * Smallest box edge, so a single citizen is not framed as a tiny point.
     */
    private static final double MIN_SIZE = 12;

    /**
     * Highest hut box used for framing: blueprint corners include the air above the roof (the town hall's reach 20 blocks up), which
     * would push the camera far back for nothing.
     */
    private static final double MAX_HUT_HEIGHT = 12;

    private WatchFocus()
    {
    }

    /**
     * @param level the level of the test.
     * @param area  the test area (structure bounds).
     * @return what to frame.
     */
    public static Focus of(final ServerLevel level, final AABB area)
    {
        final Set<IBuilding> buildings = new LinkedHashSet<>();
        final List<AABB> parts = new ArrayList<>();
        boolean colonyFound = false;
        for (final IColony colony : IColonyManager.getInstance().getColonies(level))
        {
            if (!area.contains(Vec3.atCenterOf(colony.getCenter())))
            {
                continue;
            }
            colonyFound = true;
            final IBuilding townHall = colony.getServerBuildingManager().getTownHall();
            if (townHall != null)
            {
                buildings.add(townHall);
            }
            for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
            {
                if (citizen.getWorkBuilding() != null)
                {
                    buildings.add(citizen.getWorkBuilding());
                }
                if (citizen.getHomeBuilding() != null)
                {
                    buildings.add(citizen.getHomeBuilding());
                }
            }
            if (colony.getCitizenManager().getCitizens().isEmpty())
            {
                buildings.addAll(colony.getServerBuildingManager().getBuildings().values());
            }
        }
        for (final IBuilding building : buildings)
        {
            parts.add(clip(cornersOf(building), area));
        }
        if (colonyFound)
        {
            // Citizens are entities in the area too: below, with the animals, raiders and visitors.
            for (final LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && !(e instanceof Player)))
            {
                parts.add(entity.getBoundingBox());
            }
        }
        if (parts.isEmpty())
        {
            return new Focus(area, List.of(area));
        }

        // A single citizen or a small hut should not fill the whole picture: add a box of the minimum size around the center.
        AABB bounds = parts.getFirst();
        for (final AABB part : parts)
        {
            bounds = bounds.minmax(part);
        }
        final Vec3 center = bounds.getCenter();
        parts.add(new AABB(center.x - MIN_SIZE / 2, bounds.minY, center.z - MIN_SIZE / 2, center.x + MIN_SIZE / 2, bounds.minY + 4, center.z + MIN_SIZE / 2));
        return new Focus(bounds.minmax(parts.getLast()), parts);
    }

    /**
     * Keeps a hut box inside the test area sideways (huts can reach past the arena wall; mines may go below it) and caps its height.
     */
    private static AABB clip(final AABB box, final AABB area)
    {
        return new AABB(Math.max(box.minX, area.minX), box.minY, Math.max(box.minZ, area.minZ),
          Math.min(box.maxX, area.maxX), Math.min(box.maxY, box.minY + MAX_HUT_HEIGHT), Math.min(box.maxZ, area.maxZ));
    }

    @Nullable
    private static AABB cornersOf(final IBuilding building)
    {
        final Tuple<BlockPos, BlockPos> corners = building.getCorners();
        if (corners == null || corners.getA() == null || corners.getB() == null)
        {
            return new AABB(building.getPosition());
        }
        return AABB.encapsulatingFullBlocks(corners.getA(), corners.getB());
    }
}
