// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.FarmlandWaterManager), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simplified farmland water ticket manager: tickets are axis aligned boxes in which farmland counts as watered.
 */
public class FarmlandWaterManager
{
    private static final Map<LevelReader, List<AABB>> TICKETS = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Adds a box in which farmland is considered watered. Returns a handle that can be removed with {@link #removeAABBTicket(Level, AABB)}.
     */
    public static AABB addAABBTicket(final Level level, final AABB aabb)
    {
        TICKETS.computeIfAbsent(level, l -> new CopyOnWriteArrayList<>()).add(aabb);
        return aabb;
    }

    public static void removeAABBTicket(final Level level, final AABB aabb)
    {
        final List<AABB> list = TICKETS.get(level);
        if (list != null)
        {
            list.remove(aabb);
        }
    }

    public static boolean hasBlockWaterTicket(final LevelReader level, final BlockPos pos)
    {
        final List<AABB> list = TICKETS.get(level);
        if (list == null || list.isEmpty())
        {
            return false;
        }
        final double x = pos.getX() + 0.5D;
        final double y = pos.getY() + 0.5D;
        final double z = pos.getZ() + 0.5D;
        for (final AABB box : list)
        {
            if (box.contains(x, y, z))
            {
                return true;
            }
        }
        return false;
    }
}
