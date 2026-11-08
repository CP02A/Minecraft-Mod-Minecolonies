package com.ldtteam.shim.mixin.common;

import com.ldtteam.shim.neoforge.common.IShearable;
import net.minecraft.world.level.block.DryVegetationBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherSproutsBlock;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.ShortDryGrassBlock;
import net.minecraft.world.level.block.TallDryGrassBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WebBlock;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes the vanilla blocks NeoForge patches to be {@link IShearable} implement the shim interface.
 */
@Mixin({LeavesBlock.class, WebBlock.class, TallGrassBlock.class, DryVegetationBlock.class, ShortDryGrassBlock.class, TallDryGrassBlock.class,
  VineBlock.class, SeagrassBlock.class, TallSeagrassBlock.class, NetherSproutsBlock.class})
public abstract class ShearableBlocksMixin implements IShearable
{
}
