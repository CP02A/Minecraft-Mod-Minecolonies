package com.minecolonies.fabric.mixin.colony;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Replaces the NeoForge access transformer entry for PersistentEntitySectionManager#stopTracking.
 */
@Mixin(PersistentEntitySectionManager.class)
public interface PersistentEntitySectionManagerInvoker<T extends EntityAccess>
{
    @Invoker("stopTracking")
    void minecolonies$stopTracking(T entity);
}
