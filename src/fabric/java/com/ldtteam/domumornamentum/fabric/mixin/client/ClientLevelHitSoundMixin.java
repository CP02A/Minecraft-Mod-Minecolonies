package com.ldtteam.domumornamentum.fabric.mixin.client;

import com.ldtteam.domumornamentum.fabric.MaterialSoundTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hit sound while mining a materially textured block follows its material (NeoForge: positional {@code getSoundType} for the
 * block-cracking sound).
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelHitSoundMixin
{
    @WrapOperation(
        method = "playBreakingSound",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;")
    )
    private SoundType domum$materialHitSound(final BlockState state, final Operation<SoundType> original, final BlockPos pos, final BlockState blockState)
    {
        return MaterialSoundTypes.get(original.call(state), state, (ClientLevel) (Object) this, pos, null);
    }
}
