package com.ldtteam.domumornamentum.fabric.mixin.client;

import com.ldtteam.domumornamentum.fabric.MaterialSoundTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Break sound (level event 2001) of materially textured blocks follows their material (NeoForge: positional {@code getSoundType}).
 * As on NeoForge, the material is only found while the block entity still exists: on the breaking player's own client (the event is
 * played before the block is removed). Other clients get the event after the removal and hear the block's own sound.
 */
@Mixin(LevelEventHandler.class)
public abstract class LevelEventHandlerBreakSoundMixin
{
    @Shadow
    @Final
    private ClientLevel level;

    @WrapOperation(
        method = "levelEvent",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;")
    )
    private SoundType domum$materialBreakSound(final BlockState state, final Operation<SoundType> original, final int eventType, final BlockPos pos,
        final int data)
    {
        return MaterialSoundTypes.get(original.call(state), state, level, pos, null);
    }
}
