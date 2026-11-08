package com.minecolonies.fabric.mixin.api;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes ChunkMap#getVisibleChunkIfPresent (was public through the NeoForge access transformer).
 */
@Mixin(ChunkMap.class)
public interface ChunkMapInvoker
{
    @Invoker("getVisibleChunkIfPresent")
    ChunkHolder minecolonies$getVisibleChunkIfPresent(long key);
}
