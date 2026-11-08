// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.SpecialPlantable), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Items/blocks that can be planted in a special way.
 */
public interface SpecialPlantable
{
    boolean canPlacePlantAtPosition(ItemStack itemStack, LevelReader level, BlockPos pos, @Nullable Direction direction);

    void spawnPlantAtPosition(ItemStack itemStack, LevelAccessor level, BlockPos pos, @Nullable Direction direction);

    default boolean villagerCanPlantItem(final Villager villager)
    {
        return false;
    }
}
