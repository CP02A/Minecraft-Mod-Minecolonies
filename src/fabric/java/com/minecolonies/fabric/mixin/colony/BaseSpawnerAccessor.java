package com.minecolonies.fabric.mixin.colony;

import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the NeoForge access transformer entries for BaseSpawner (used by the pirate raid spawner setup).
 */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor
{
    @Accessor("nextSpawnData")
    SpawnData minecolonies$getNextSpawnData();

    @Accessor("requiredPlayerRange")
    void minecolonies$setRequiredPlayerRange(int range);
}
