package com.ldtteam.structurize.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.MaterialSystem;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;
import java.util.function.Function;

/**
 * Access to the package-private parts of 26.x's {@link MaterialRuleContext} (the replacement of 1.21's public
 * {@code SurfaceRules.Context}), so {@code BlockUtils#getWorldgenBlock} can evaluate the dimension's material (surface) rule at one
 * position the way {@code MaterialSystem#buildSurface} does during world generation.
 */
@Mixin(MaterialRuleContext.class)
public interface MaterialRuleContextInvoker
{
    @Invoker("<init>")
    static MaterialRuleContext structurize$create(
        final MaterialSystem system,
        final RandomState randomState,
        final DensityVolume expectedVolume,
        final DensitySamplerSet densitySamplers,
        final Function<BlockPos, Holder<Biome>> biomeGetter,
        final WorldGenerationContext context,
        @Nullable final Set<Holder<Biome>> possibleBiomes)
    {
        throw new AssertionError("mixin invoker");
    }

    @Invoker("updateXZ")
    void structurize$updateXZ(int blockX, int blockZ, int surfaceGradientX, int surfaceGradientZ);

    @Invoker("updateY")
    void structurize$updateY(int stoneDepthAbove, int stoneDepthBelow, int waterHeight, int blockY);
}
