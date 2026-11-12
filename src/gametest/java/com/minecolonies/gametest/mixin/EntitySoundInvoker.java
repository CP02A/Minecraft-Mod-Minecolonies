package com.minecolonies.gametest.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Calls the step sound the way walking does ({@code Entity#walkingStepSound} → {@code playStepSound}), for the Domum sound tests.
 */
@Mixin(Entity.class)
public interface EntitySoundInvoker
{
    @Invoker("playStepSound")
    void minecoloniesGametest$playStepSound(BlockPos pos, BlockState state);
}
