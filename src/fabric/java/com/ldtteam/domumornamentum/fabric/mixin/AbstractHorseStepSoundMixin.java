package com.ldtteam.domumornamentum.fabric.mixin;

import com.ldtteam.domumornamentum.fabric.MaterialSoundTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Horses override {@code playStepSound}; the first sound lookup is the block walked on (the second one is snow on top of it).
 */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseStepSoundMixin
{
    @WrapOperation(
        method = "playStepSound",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;", ordinal = 0)
    )
    private SoundType domum$materialStepSound(final BlockState state, final Operation<SoundType> original, final BlockPos pos, final BlockState blockState)
    {
        final AbstractHorse self = (AbstractHorse) (Object) this;
        return MaterialSoundTypes.get(original.call(state), state, self.level(), pos, self);
    }
}
