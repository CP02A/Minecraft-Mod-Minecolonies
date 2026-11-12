package com.ldtteam.domumornamentum.fabric.mixin;

import com.ldtteam.domumornamentum.fabric.MaterialSoundTypes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Place sound (event, volume and pitch) of materially textured blocks follows their material (NeoForge: positional
 * {@code getSoundType} and {@code getPlaceSound(state, level, pos, player)} in {@code BlockItem#place}).
 */
@Mixin(BlockItem.class)
public abstract class BlockItemPlaceSoundMixin
{
    @WrapOperation(
        method = "place",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;")
    )
    private SoundType domum$materialPlaceSoundType(
        final BlockState state,
        final Operation<SoundType> original,
        @Local final Level level,
        @Local final BlockPos pos,
        @Local final Player player)
    {
        return MaterialSoundTypes.get(original.call(state), state, level, pos, player);
    }

    @WrapOperation(
        method = "place",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;getPlaceSound(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/sounds/SoundEvent;")
    )
    private SoundEvent domum$materialPlaceSound(
        final BlockItem item,
        final BlockState state,
        final Operation<SoundEvent> original,
        @Local final Level level,
        @Local final BlockPos pos,
        @Local final Player player)
    {
        final SoundEvent vanilla = original.call(item, state);
        final SoundType stateSound = state.getSoundType();
        final SoundType material = MaterialSoundTypes.get(stateSound, state, level, pos, player);
        return material == stateSound ? vanilla : material.getPlaceSound();
    }
}
