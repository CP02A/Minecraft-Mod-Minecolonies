package com.minecolonies.core.colony;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Replacements for NeoForge entity extension methods that do not exist on Fabric.
 */
public final class EntityCompat
{
    private EntityCompat()
    {
    }

    /**
     * Replacement for NeoForge's {@code Entity#isAddedToLevel()}: true once the entity is tracked (visible) by its level.
     */
    public static boolean isAddedToLevel(final Entity entity)
    {
        final Level level = entity.level();
        if (level == null || entity.isRemoved())
        {
            return false;
        }
        if (level instanceof ServerLevel serverLevel)
        {
            return serverLevel.getEntity(entity.getUUID()) == entity;
        }
        return level.getEntity(entity.getId()) == entity;
    }
}
