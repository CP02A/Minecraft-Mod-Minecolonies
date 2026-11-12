package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.suites.PortFeatureTests;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records every positioned sound the server plays (all {@code Level#playSound} variants end here), so tests can check which sound the
 * game picked (PortFeatureTests, Domum Ornamentum material sounds).
 */
@Mixin(ServerLevel.class)
public abstract class SoundCaptureMixin
{
    @Inject(
        method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
        at = @At("HEAD")
    )
    private void minecoloniesGametest$recordSound(
        @Nullable final Entity except,
        final double x,
        final double y,
        final double z,
        final Holder<SoundEvent> sound,
        final SoundSource source,
        final float volume,
        final float pitch,
        final long seed,
        final CallbackInfo ci)
    {
        PortFeatureTests.recordSound((ServerLevel) (Object) this, sound.value(), x, y, z, volume, pitch);
    }
}
