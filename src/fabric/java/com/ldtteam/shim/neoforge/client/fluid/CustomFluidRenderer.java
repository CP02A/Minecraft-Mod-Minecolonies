// Adapted from NeoForge 26.3 (net.neoforged.neoforge.client.fluid.CustomFluidRenderer), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.fluid;

import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Custom fluid renderer hook. Vanilla/Fabric never provides one, so callers always see {@code null} and use vanilla fluid rendering.
 */
@FunctionalInterface
public interface CustomFluidRenderer
{
    /**
     * @return true if vanilla fluid rendering should be skipped
     */
    boolean renderFluid(FluidRenderer fluidRenderer, FluidState fluidState, BlockAndTintGetter getter, BlockPos pos, FluidRenderer.Output output, BlockState blockState);
}
