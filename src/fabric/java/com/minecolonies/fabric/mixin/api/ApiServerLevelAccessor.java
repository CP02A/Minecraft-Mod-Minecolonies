package com.minecolonies.fabric.mixin.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * Replaces the NeoForge access transformer entry for ServerLevel#navigatingMobs.
 */
@Mixin(ServerLevel.class)
public interface ApiServerLevelAccessor
{
    @Accessor("navigatingMobs")
    Set<Mob> minecolonies$getNavigatingMobs();
}
