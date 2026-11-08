// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.extensions.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/**
 * Client-only block extensions. Registered through {@link RegisterClientExtensionsEvent}.
 * FABRIC-PORT TODO: particle hooks are not consulted by vanilla on Fabric; they are kept for API compatibility.
 */
public interface IClientBlockExtensions
{
    IClientBlockExtensions DEFAULT = new IClientBlockExtensions() {};

    static IClientBlockExtensions of(final BlockState state)
    {
        return of(state.getBlock());
    }

    static IClientBlockExtensions of(final Block block)
    {
        return ClientExtensionsManager.BLOCK_EXTENSIONS.getOrDefault(block, DEFAULT);
    }

    default boolean addHitEffects(final BlockState state, final Level level, final HitResult target, final Object manager)
    {
        return false;
    }

    default boolean addDestroyEffects(final BlockState state, final Level level, final BlockPos pos, final Object manager)
    {
        return !state.shouldSpawnTerrainParticles();
    }
}
