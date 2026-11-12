package com.ldtteam.domumornamentum.fabric.mixin;

import com.ldtteam.domumornamentum.fabric.MaterialSoundTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fall sound of materially textured blocks follows their material (NeoForge: positional {@code getSoundType} in
 * {@code LivingEntity#playBlockFallSound}).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFallSoundMixin
{
    @WrapOperation(
        method = "playBlockFallSound",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;")
    )
    private SoundType domum$materialFallSound(final BlockState state, final Operation<SoundType> original)
    {
        final LivingEntity self = (LivingEntity) (Object) this;
        // Same position as vanilla's lookup: floor(x), floor(y - 0.2), floor(z).
        final BlockPos pos = BlockPos.containing(self.getX(), self.getY() - 0.2F, self.getZ());
        return MaterialSoundTypes.get(original.call(state), state, self.level(), pos, self);
    }
}
