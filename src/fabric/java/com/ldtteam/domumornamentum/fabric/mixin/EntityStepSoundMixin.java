package com.ldtteam.domumornamentum.fabric.mixin;

import com.ldtteam.domumornamentum.fabric.MaterialSoundTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Step sound of materially textured blocks follows their material (NeoForge: positional {@code getSoundType} in
 * {@code Entity#playStepSound}).
 */
@Mixin(Entity.class)
public abstract class EntityStepSoundMixin
{
    @WrapOperation(
        method = "playStepSound",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;")
    )
    private SoundType domum$materialStepSound(final BlockState state, final Operation<SoundType> original, final BlockPos pos, final BlockState blockState)
    {
        final Entity self = (Entity) (Object) this;
        return MaterialSoundTypes.get(original.call(state), state, self.level(), pos, self);
    }
}
