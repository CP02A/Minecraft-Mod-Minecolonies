// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.IShearable), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Something that can be sheared. Vanilla shearable blocks (leaves, cobweb, grass, vines, seagrass, ...) implement this interface through
 * the shim mixin {@code com.ldtteam.shim.mixin.common.ShearableBlocksMixin}.
 */
public interface IShearable
{
    default boolean isShearable(@Nullable final Player player, final ItemStack item, final Level level, final BlockPos pos)
    {
        return !(this instanceof Shearable shearable) || shearable.readyForShearing();
    }

    /**
     * Performs the shear. On Fabric drops cannot be captured; entities drop their items directly in the world and an empty list is returned.
     */
    default List<ItemStack> onSheared(@Nullable final Player player, final ItemStack item, final Level level, final BlockPos pos)
    {
        if (this instanceof Shearable shearable && level instanceof ServerLevel serverLevel)
        {
            shearable.shear(serverLevel, player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS, item);
        }
        return Collections.emptyList();
    }

    default void spawnShearedDrop(final Level level, final BlockPos pos, final ItemStack drop)
    {
        if (this instanceof Entity entity && level instanceof ServerLevel serverLevel)
        {
            entity.spawnAtLocation(serverLevel, drop, 1);
        }
        else
        {
            level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), drop));
        }
    }
}
