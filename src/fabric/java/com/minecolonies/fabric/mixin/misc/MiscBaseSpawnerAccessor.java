package com.minecolonies.fabric.mixin.misc;

import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to the next spawn data of spawners (replaces the NeoForge access transformer), used to detect raid camp spawners being broken.
 */
@Mixin(BaseSpawner.class)
public interface MiscBaseSpawnerAccessor
{
    @Nullable
    @Accessor("nextSpawnData")
    SpawnData minecolonies$misc$getNextSpawnData();
}
