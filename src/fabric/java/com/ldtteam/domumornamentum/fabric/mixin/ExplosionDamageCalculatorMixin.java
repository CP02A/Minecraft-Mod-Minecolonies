package com.ldtteam.domumornamentum.fabric.mixin;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces NeoForge's {@code IBlockExtension#getExplosionResistance(state, level, pos, explosion)} for materially
 * textured blocks: their explosion resistance follows the block of their main material.
 */
@Mixin(ExplosionDamageCalculator.class)
public class ExplosionDamageCalculatorMixin
{
    @WrapOperation(
        method = "getBlockExplosionResistance",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getExplosionResistance()F")
    )
    private float domum$materialExplosionResistance(
        final Block block,
        final Operation<Float> original,
        final Explosion explosion,
        final BlockGetter level,
        final BlockPos pos,
        final BlockState state,
        final FluidState fluid)
    {
        if (block instanceof final IMateriallyTexturedBlock texturedBlock)
        {
            return texturedBlock.getDOExplosionResistance((s, l, p, e) -> original.call(block), state, level, pos, explosion);
        }
        return original.call(block);
    }
}
