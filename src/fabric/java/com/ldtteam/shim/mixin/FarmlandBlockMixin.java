package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.CommonHooks;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fires BlockEvent.FarmlandTrampleEvent (NeoForge CommonHooks#onFarmlandTrample) before vanilla farmland is trampled;
 * cancelling keeps the farmland.
 */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin
{
    @WrapWithCondition(method = "fallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToBaseBlock(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private boolean minecolonies$fireTrampleEvent(final FarmlandBlock self, final Entity entity, final BlockState state, final Level level, final BlockPos pos,
        final Level fallLevel, final BlockState fallState, final BlockPos fallPos, final Entity fallEntity, final double fallDistance)
    {
        return CommonHooks.onFarmlandTrample(level, pos, state, fallDistance, entity);
    }
}
