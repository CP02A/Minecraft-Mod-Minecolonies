package com.minecolonies.fabric.mixin.colony;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the NeoForge access transformer entry for ServerLevel#entityManager (used to drop stale citizen entities).
 */
@Mixin(ServerLevel.class)
public interface ServerLevelAccessor
{
    @Accessor("entityManager")
    PersistentEntitySectionManager<Entity> minecolonies$getEntityManager();
}
